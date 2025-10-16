package com.company.index.batch.job;

import com.company.index.batch.reader.ReaderFactory;
import com.company.index.batch.writer.WriterFactory;
import com.company.index.batch.check.IndexCheckService;
import com.company.index.batch.retry.IndexRetryService;
import com.company.index.common.model.SourceRecord;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
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

import java.time.LocalDateTime;

/**
 * 增量索引 Job 配置
 * 流程：读取增量数据 → 处理 → 写入 → 检查 → 条件重建
 */
@Configuration
public class IncrementalIndexJobConfig {

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

    @Autowired
    private org.springframework.batch.core.launch.JobLauncher jobLauncher;

    @Autowired
    private org.springframework.context.ApplicationContext applicationContext;

    @Value("${index.parallelism.chunkSize:1000}")
    private int chunkSize;

    @Value("${index.parallelism.threads:4}")
    private int threads;

    @Value("${index.incremental.overlapMinutes:5}")
    private int overlapMinutes;

    @Value("${index.retry.maxAttempts:3}")
    private int maxRetryAttempts;

    /**
     * 增量索引 Job
     * 流程：
     * 1. deltaReadProcessWriteStep - 读取增量数据并写入（支持重试）
     * 2. incrementalCheckStep - 检查索引完整性
     *    ├─ 检查成功 → incrementalFinishStep - 完成
     *    └─ 检查失败 → triggerFullRebuildStep - 触发全量重建
     * 
     * 注意：
     * - 增量索引检查失败时，会调用全量索引 Job 进行重建
     * - 全量索引 Job 自带重试机制（最多3次）
     */
    @Bean
    public Job incrementalIndexJob() {
        return new JobBuilder("incrementalIndexJob", jobRepository)
                .start(deltaReadProcessWriteStep())
                .next(incrementalCheckStep())
                .on("FAILED").to(triggerFullRebuildStep())  // 检查失败 → 触发全量重建
                .from(incrementalCheckStep()).on("*").to(incrementalFinishStep())  // 检查成功 → 完成
                .end()
                .build();
    }

    /**
     * 增量读取处理写入步骤
     * 配置了容错和重试机制：
     * - 重试次数：3次（配置）
     * - 跳过次数：100次（增量允许更多跳过）
     * - 失败后会重试，如果重试3次仍失败则跳过该记录
     */
    @Bean
    public Step deltaReadProcessWriteStep() {
        return new StepBuilder("deltaReadProcessWriteStep", jobRepository)
                .<SourceRecord, SourceRecord>chunk(chunkSize, transactionManager)
                .reader(deltaReader())
                .processor(deltaProcessor())
                .writer(deltaWriter())
                .faultTolerant()
                .retryLimit(maxRetryAttempts)
                .retry(RuntimeException.class)
                .retry(org.springframework.dao.DataAccessException.class)
                .skipLimit(100)  // 增量索引允许更多跳过
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
                        System.err.println("[增量索引] 写入失败，将重试。错误: " + ex.getMessage());
                        System.err.println("[增量索引] 失败的记录数: " + items.size());
                    }
                })
                .listener(new org.springframework.batch.core.SkipListener<SourceRecord, SourceRecord>() {
                    @Override
                    public void onSkipInRead(Throwable t) {
                        System.err.println("[增量索引] 读取时跳过记录，错误: " + t.getMessage());
                    }
                    
                    @Override
                    public void onSkipInWrite(SourceRecord item, Throwable t) {
                        System.err.println("[增量索引] 跳过失败记录: " + item.getId() + "，错误: " + t.getMessage());
                    }
                    
                    @Override
                    public void onSkipInProcess(SourceRecord item, Throwable t) {
                        System.err.println("[增量索引] 处理时跳过记录: " + item.getId() + "，错误: " + t.getMessage());
                    }
                })
                .build();
    }

    /**
     * 增量检查步骤：索引完整性检查
     */
    @Bean
    public Step incrementalCheckStep() {
        return new StepBuilder("incrementalCheckStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    boolean isValid = indexCheckService.checkIndexIntegrity();
                    if (!isValid) {
                        System.out.println("[增量索引] 索引完整性检查失败，将触发全量重建");
                        contribution.setExitStatus(org.springframework.batch.core.ExitStatus.FAILED);
                    } else {
                        System.out.println("[增量索引] 索引完整性检查通过");
                        contribution.setExitStatus(org.springframework.batch.core.ExitStatus.COMPLETED);
                    }
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    /**
     * 触发全量重建步骤
     * 当增量索引检查失败时，启动全量索引 Job 进行重建
     */
    @Bean
    public Step triggerFullRebuildStep() {
        return new StepBuilder("triggerFullRebuildStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("======================================");
                    System.out.println("[增量索引] 触发全量索引重建");
                    System.out.println("======================================");
                    
                    try {
                        // 从 ApplicationContext 获取 fullIndexJob（避免循环依赖）
                        Job fullIndexJob = applicationContext.getBean("fullIndexJob", Job.class);
                        
                        // 创建 Job 参数（包含触发来源信息）
                        org.springframework.batch.core.JobParameters jobParameters = 
                            new org.springframework.batch.core.JobParametersBuilder()
                                .addLong("timestamp", System.currentTimeMillis())
                                .addString("triggeredBy", "incrementalIndexJob")
                                .addString("reason", "incrementalCheckFailed")
                                .toJobParameters();
                        
                        System.out.println("[增量索引] 启动全量索引 Job...");
                        
                        // 同步启动全量索引 Job（会等待其完成）
                        // 全量索引 Job 内部有重试机制，最多重试3次
                        org.springframework.batch.core.JobExecution execution = 
                            jobLauncher.run(fullIndexJob, jobParameters);
                        
                        System.out.println("[增量索引] 全量索引 Job 执行完成");
                        System.out.println("[增量索引] 执行状态: " + execution.getStatus());
                        
                        if (execution.getStatus() != org.springframework.batch.core.BatchStatus.COMPLETED) {
                            System.err.println("[增量索引] 全量索引重建失败！");
                            throw new RuntimeException("Full index rebuild failed with status: " + execution.getStatus());
                        }
                        
                        System.out.println("[增量索引] 全量索引重建成功！");
                        System.out.println("======================================");
                        
                    } catch (org.springframework.beans.factory.NoSuchBeanDefinitionException e) {
                        System.err.println("[增量索引] 未找到 fullIndexJob Bean，可能在测试环境中");
                        System.err.println("[增量索引] 跳过全量重建步骤");
                        System.out.println("======================================");
                    } catch (Exception e) {
                        System.err.println("[增量索引] 触发全量重建失败: " + e.getMessage());
                        e.printStackTrace();
                        throw new RuntimeException("Failed to trigger full rebuild", e);
                    }
                    
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
    
    /**
     * 增量索引完成步骤
     */
    @Bean
    public Step incrementalFinishStep() {
        return new StepBuilder("incrementalFinishStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("======================================");
                    System.out.println("[增量索引] 索引更新成功完成");
                    System.out.println("======================================");
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    /**
     * 增量读取器
     */
    @Bean
    public ItemReader<SourceRecord> deltaReader() {
        // 计算时间窗口
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusMinutes(5); // 默认 5 分钟窗口

        return readerFactory.createDeltaReader(startTime, endTime, overlapMinutes);
    }

    /**
     * 增量处理器
     */
    @Bean
    public ItemProcessor<SourceRecord, SourceRecord> deltaProcessor() {
        return new ItemProcessor<SourceRecord, SourceRecord>() {
            @Override
            public SourceRecord process(SourceRecord item) throws Exception {
                if (item == null) {
                    return null;
                }

                // 数据验证
                if (item.getId() == null || item.getId().trim().isEmpty()) {
                    throw new IllegalArgumentException("Invalid record: missing ID");
                }

                // 处理不同类型的记录
                String type = item.getType();
                switch (type.toUpperCase()) {
                    case "INSERT":
                        // 新增记录：设置处理时间戳
                        item.setTimestamp(LocalDateTime.now());
                        break;
                    case "UPDATE":
                        // 更新记录：保持原时间戳，添加处理时间戳
                        item.setTimestamp(LocalDateTime.now());
                        break;
                    case "DELETE":
                        // 删除记录：保持原时间戳
                        break;
                    default:
                        // 未知类型：默认为更新
                        item.setType("UPDATE");
                        item.setTimestamp(LocalDateTime.now());
                        break;
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
     * 增量写入器
     */
    @Bean
    public ItemWriter<SourceRecord> deltaWriter() {
        return writerFactory.createWriter();
    }
}
