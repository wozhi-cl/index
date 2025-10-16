package com.company.index.batch.job;

import com.company.index.batch.reader.ReaderFactory;
import com.company.index.batch.writer.WriterFactory;
import com.company.index.batch.check.IndexCheckService;
import com.company.index.batch.retry.IndexRetryService;
import com.company.index.common.model.SourceRecord;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 全量索引 Job 配置
 * 流程：清理 → 读取处理写入 → 检查 → 切换
 */
@Configuration
public class FullIndexJobConfig {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ReaderFactory readerFactory;

    @Autowired
    private WriterFactory writerFactory;

    @Autowired
    private IndexCheckService indexCheckService;

    @Autowired
    private IndexRetryService indexRetryService;

    @Value("${index.parallelism.chunkSize:1000}")
    private int chunkSize;

    @Value("${index.parallelism.threads:4}")
    private int threads;

    @Value("${index.retry.maxAttempts:3}")
    private int maxRetryAttempts;

    /**
     * 全量索引 Job
     * 流程：
     * 1. cleanupStep - 删除旧索引
     * 2. readProcessWriteStep - 读取处理写入
     * 3. fullCheckStep - 检查索引完整性
     *    ├─ 检查成功 → finishStep - 结束处理
     *    └─ 检查失败 → retryDecisionStep - 判断是否重试
     *       ├─ 未达最大重试次数 → 返回 cleanupStep（重新开始）
     *       └─ 已达最大重试次数 → rebuildStep（清理并报告失败）
     * 
     * 注意：
     * - 最多重试3次全量索引流程（可配置）
     * - readProcessWriteStep 内部也有 chunk-level 重试
     */
    @Bean
    public Job fullIndexJob() {
        return new JobBuilder("fullIndexJob", jobRepository)
                .start(cleanupStep())
                .next(readProcessWriteStep())
                .next(fullCheckStep())
                .on("FAILED").to(retryDecisionStep())  // 检查失败 → 重试决策
                .from(retryDecisionStep()).on("RETRY").to(cleanupStep())  // 重试 → 回到开始
                .from(retryDecisionStep()).on("*").to(rebuildStep())  // 超过重试次数 → 重建
                .from(fullCheckStep()).on("*").to(finishStep())  // 检查成功 → 完成
                .end()
                .build();
    }
    
    /**
     * 重试决策步骤
     * 判断当前重试次数，决定是重新执行全量索引还是放弃
     */
    @Bean
    public Step retryDecisionStep() {
        return new StepBuilder("retryDecisionStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    JobExecution jobExecution = 
                        chunkContext.getStepContext().getStepExecution().getJobExecution();
                    ExecutionContext jobContext = 
                        jobExecution.getExecutionContext();
                    
                    int retryCount = jobContext.getInt("fullIndexRetryCount", 0);
                    System.out.println("======================================");
                    System.out.println("索引检查失败，当前重试次数: " + retryCount + " / " + maxRetryAttempts);
                    
                    if (retryCount < maxRetryAttempts) {
                        // 增加重试次数
                        jobContext.putInt("fullIndexRetryCount", retryCount + 1);
                        System.out.println("将重新执行全量索引流程（第 " + (retryCount + 1) + " 次重试）");
                        System.out.println("======================================");
                        
                        // 设置退出状态为 RETRY，触发回到 cleanupStep
                        contribution.setExitStatus(new org.springframework.batch.core.ExitStatus("RETRY"));
                    } else {
                        System.out.println("已达到最大重试次数 " + maxRetryAttempts);
                        System.out.println("全量索引失败，执行清理操作");
                        System.out.println("======================================");
                        
                        // 设置退出状态为 FAILED
                        contribution.setExitStatus(org.springframework.batch.core.ExitStatus.FAILED);
                    }
                    
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    /**
     * 清理步骤：删除旧索引
     */
    @Bean
    public Step cleanupStep() {
        return new StepBuilder("cleanupStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    try {
                        writerFactory.deleteIndex();
                        return null;
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to cleanup index", e);
                    }
                }, transactionManager)
                .build();
    }

    /**
     * 读取处理写入步骤
     * 配置了容错和重试机制：
     * - 重试次数：3次（配置）
     * - 跳过次数：10次
     * - 失败后会重试，如果重试3次仍失败则跳过该记录
     */
    @Bean
    public Step readProcessWriteStep() {
        return new StepBuilder("readProcessWriteStep", jobRepository)
                .<SourceRecord, SourceRecord>chunk(chunkSize, transactionManager)
                .reader(fullReader())
                .processor(fullProcessor())
                .writer(fullWriter())
                .faultTolerant()
                .retryLimit(maxRetryAttempts)
                .retry(RuntimeException.class)
                .retry(org.springframework.dao.DataAccessException.class)
                .skipLimit(10)
                .skip(RuntimeException.class)
                .skip(org.springframework.dao.DataAccessException.class)
                .listener(new org.springframework.batch.core.ItemWriteListener<SourceRecord>() {
                    @Override
                    public void beforeWrite(org.springframework.batch.item.Chunk<? extends SourceRecord> items) {
                        // 写入前的处理（可选）
                    }
                    
                    @Override
                    public void afterWrite(org.springframework.batch.item.Chunk<? extends SourceRecord> items) {
                        // 写入成功后的处理（可选）
                    }
                    
                    @Override
                    public void onWriteError(Exception ex, org.springframework.batch.item.Chunk<? extends SourceRecord> items) {
                        System.err.println("写入失败，将重试。错误: " + ex.getMessage());
                        System.err.println("失败的记录数: " + items.size());
                    }
                })
                .listener(new org.springframework.batch.core.SkipListener<SourceRecord, SourceRecord>() {
                    @Override
                    public void onSkipInRead(Throwable t) {
                        System.err.println("读取时跳过记录，错误: " + t.getMessage());
                    }
                    
                    @Override
                    public void onSkipInWrite(SourceRecord item, Throwable t) {
                        System.err.println("跳过失败记录: " + item.getId() + "，错误: " + t.getMessage());
                    }
                    
                    @Override
                    public void onSkipInProcess(SourceRecord item, Throwable t) {
                        System.err.println("处理时跳过记录: " + item.getId() + "，错误: " + t.getMessage());
                    }
                })
                .build();
    }

    /**
     * 检查步骤：索引完整性检查
     * 如果检查失败，返回 FAILED 状态，触发重建流程
     */
    @Bean
    public Step fullCheckStep() {
        return new StepBuilder("fullCheckStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    try {
                        boolean isValid = indexCheckService.checkIndexIntegrity();
                        if (!isValid) {
                            System.out.println("索引完整性检查失败，将触发重建流程");
                            contribution.setExitStatus(org.springframework.batch.core.ExitStatus.FAILED);
                        } else {
                            System.out.println("索引完整性检查通过");
                            contribution.setExitStatus(org.springframework.batch.core.ExitStatus.COMPLETED);
                        }
                        return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                    } catch (Exception e) {
                        System.err.println("索引检查异常: " + e.getMessage());
                        contribution.setExitStatus(org.springframework.batch.core.ExitStatus.FAILED);
                        return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                    }
                }, transactionManager)
                .build();
    }
    
    /**
     * 重建步骤：检查失败后重新建索引
     * 清理并重新执行数据写入
     */
    @Bean
    public Step rebuildStep() {
        return new StepBuilder("rebuildStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    try {
                        System.out.println("开始重建索引...");
                        
                        // 1. 清理失败的索引
                        writerFactory.deleteIndex();
                        
                        // 2. 重新创建索引
                        writerFactory.createIndexIfNotExists();
                        
                        System.out.println("索引重建完成，建议手动重新运行全量索引任务");
                        
                        return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                    } catch (Exception e) {
                        throw new RuntimeException("索引重建失败", e);
                    }
                }, transactionManager)
                .build();
    }

    /**
     * 结束处理步骤：索引最终化处理
     * - Elasticsearch: 刷新索引并切换别名（可选）
     * - GetQuick: 发布索引使其生效
     * - File: 输出完成信息和统计
     */
    @Bean
    public Step finishStep() {
        return new StepBuilder("finishStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    try {
                        System.out.println("======================================");
                        System.out.println("开始执行索引结束处理...");
                        
                        // 调用 WriterFactory 的结束处理方法
                        writerFactory.finishIndex();
                        
                        // 获取并打印索引统计信息
                        Object stats = writerFactory.getIndexStats();
                        System.out.println("索引统计信息: " + stats);
                        
                        System.out.println("索引结束处理完成！");
                        System.out.println("======================================");
                        
                        return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                    } catch (Exception e) {
                        throw new RuntimeException("索引结束处理失败", e);
                    }
                }, transactionManager)
                .build();
    }

    /**
     * 全量读取器
     */
    @Bean
    public ItemReader<SourceRecord> fullReader() {
        return readerFactory.createFullReader(chunkSize);
    }

    /**
     * 全量处理器
     */
    @Bean
    public ItemProcessor<SourceRecord, SourceRecord> fullProcessor() {
        return new ItemProcessor<SourceRecord, SourceRecord>() {
            @Override
            public SourceRecord process(SourceRecord item) throws Exception {
                // 数据清洗和转换
                if (item == null) {
                    return null;
                }

                // 设置处理时间戳
                item.setTimestamp(java.time.LocalDateTime.now());

                // 数据验证
                if (item.getId() == null || item.getId().trim().isEmpty()) {
                    throw new IllegalArgumentException("Invalid record: missing ID");
                }

                // 数据清洗
                if (item.getData() != null) {
                    // 移除空值字段
                    item.getData().entrySet().removeIf(entry -> entry.getValue() == null);
                }

                return item;
            }
        };
    }

    /**
     * 全量写入器
     */
    @Bean
    public ItemWriter<SourceRecord> fullWriter() {
        return writerFactory.createWriter();
    }
}
