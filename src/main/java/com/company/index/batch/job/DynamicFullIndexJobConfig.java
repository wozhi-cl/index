package com.company.index.batch.job;

import com.company.index.batch.reader.DynamicRdbFullReader;
import com.company.index.batch.writer.DynamicWriterFactory;
import com.company.index.batch.check.IndexCheckService;
import com.company.index.common.model.DynamicRecord;
import com.company.index.common.service.RecordBuilderService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.JobExecution;
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
 * 动态全量索引 Job 配置
 * 使用 DynamicRecord 和字段映射配置
 */
@Configuration
public class DynamicFullIndexJobConfig {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private DynamicRdbFullReader dynamicRdbFullReader;

    @Autowired
    private DynamicWriterFactory dynamicWriterFactory;

    @Autowired
    private IndexCheckService indexCheckService;

    @Autowired
    private RecordBuilderService recordBuilderService;

    @Value("${index.parallelism.chunkSize:1000}")
    private int chunkSize;

    @Value("${index.retry.maxAttempts:3}")
    private int maxRetryAttempts;

    @Value("${index.dataSource.table:orders}")
    private String sourceTable;

    @Value("${index.indexTarget.indexName:order_index}")
    private String indexName;

    /**
     * 动态全量索引 Job
     */
    @Bean("dynamicFullIndexJob")
    public Job dynamicFullIndexJob() {
        return new JobBuilder("dynamicFullIndexJob", jobRepository)
                .start(dynamicCleanupStep())
                .next(dynamicReadProcessWriteStep())
                .next(dynamicFullCheckStep())
                .on("FAILED").to(dynamicRetryDecisionStep())
                .from(dynamicRetryDecisionStep()).on("RETRY").to(dynamicCleanupStep())
                .from(dynamicRetryDecisionStep()).on("*").to(dynamicRebuildStep())
                .from(dynamicFullCheckStep()).on("*").to(dynamicFinishStep())
                .end()
                .build();
    }

    /**
     * 清理步骤
     */
    @Bean
    public Step dynamicCleanupStep() {
        return new StepBuilder("dynamicCleanupStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("======================================");
                    System.out.println("开始清理索引: " + indexName);
                    dynamicWriterFactory.deleteIndex();
                    dynamicWriterFactory.createIndexIfNotExists();
                    System.out.println("索引清理完成！");
                    System.out.println("======================================");
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    /**
     * 读取处理写入步骤
     */
    @Bean
    public Step dynamicReadProcessWriteStep() {
        return new StepBuilder("dynamicReadProcessWriteStep", jobRepository)
                .<DynamicRecord, DynamicRecord>chunk(chunkSize, transactionManager)
                .reader(dynamicFullReader())
                .processor(dynamicFullProcessor())
                .writer(dynamicFullWriter())
                .faultTolerant()
                .retryLimit(maxRetryAttempts)
                .retry(RuntimeException.class)
                .retry(org.springframework.dao.DataAccessException.class)
                .skipLimit(10)
                .skip(RuntimeException.class)
                .skip(org.springframework.dao.DataAccessException.class)
                .listener(new org.springframework.batch.core.ItemWriteListener<DynamicRecord>() {
                    @Override
                    public void beforeWrite(org.springframework.batch.item.Chunk<? extends DynamicRecord> items) { }
                    
                    @Override
                    public void afterWrite(org.springframework.batch.item.Chunk<? extends DynamicRecord> items) { }
                    
                    @Override
                    public void onWriteError(Exception ex, org.springframework.batch.item.Chunk<? extends DynamicRecord> items) {
                        System.err.println("[动态全量] 写入失败，将重试。错误: " + ex.getMessage());
                        System.err.println("[动态全量] 失败的记录数: " + items.size());
                    }
                })
                .listener(new org.springframework.batch.core.SkipListener<DynamicRecord, DynamicRecord>() {
                    @Override
                    public void onSkipInRead(Throwable t) {
                        System.err.println("[动态全量] 读取时跳过记录，错误: " + t.getMessage());
                    }
                    
                    @Override
                    public void onSkipInWrite(DynamicRecord item, Throwable t) {
                        System.err.println("[动态全量] 跳过失败记录: " + item.getKeyValue() + "，错误: " + t.getMessage());
                    }
                    
                    @Override
                    public void onSkipInProcess(DynamicRecord item, Throwable t) {
                        System.err.println("[动态全量] 处理时跳过记录: " + item.getKeyValue() + "，错误: " + t.getMessage());
                    }
                })
                .build();
    }

    /**
     * 检查步骤
     */
    @Bean
    public Step dynamicFullCheckStep() {
        return new StepBuilder("dynamicFullCheckStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    boolean isValid = indexCheckService.checkIndexIntegrity();
                    if (!isValid) {
                        System.out.println("[动态全量] 索引完整性检查失败，将触发重建流程");
                        contribution.setExitStatus(org.springframework.batch.core.ExitStatus.FAILED);
                    } else {
                        System.out.println("[动态全量] 索引完整性检查通过");
                        contribution.setExitStatus(org.springframework.batch.core.ExitStatus.COMPLETED);
                    }
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    /**
     * 重试决策步骤
     */
    @Bean
    public Step dynamicRetryDecisionStep() {
        return new StepBuilder("dynamicRetryDecisionStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    JobExecution jobExecution = 
                        chunkContext.getStepContext().getStepExecution().getJobExecution();
                    ExecutionContext jobContext = 
                        jobExecution.getExecutionContext();
                    
                    int retryCount = jobContext.getInt("dynamicFullIndexRetryCount", 0);
                    System.out.println("======================================");
                    System.out.println("[动态全量] 索引检查失败，当前重试次数: " + retryCount + " / " + maxRetryAttempts);
                    
                    if (retryCount < maxRetryAttempts) {
                        jobContext.putInt("dynamicFullIndexRetryCount", retryCount + 1);
                        System.out.println("[动态全量] 将重新执行全量索引流程（第 " + (retryCount + 1) + " 次重试）");
                        System.out.println("======================================");
                        contribution.setExitStatus(new org.springframework.batch.core.ExitStatus("RETRY"));
                    } else {
                        System.out.println("[动态全量] 已达到最大重试次数 " + maxRetryAttempts);
                        System.out.println("[动态全量] 全量索引失败，执行清理操作");
                        System.out.println("======================================");
                        contribution.setExitStatus(org.springframework.batch.core.ExitStatus.FAILED);
                    }
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    /**
     * 重建步骤
     */
    @Bean
    public Step dynamicRebuildStep() {
        return new StepBuilder("dynamicRebuildStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("======================================");
                    System.out.println("[动态全量] 开始重建索引: " + indexName);
                    dynamicWriterFactory.deleteIndex();
                    dynamicWriterFactory.createIndexIfNotExists();
                    System.out.println("[动态全量] 索引重建完成，建议手动重新运行全量索引任务");
                    System.out.println("======================================");
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    /**
     * 完成步骤
     */
    @Bean
    public Step dynamicFinishStep() {
        return new StepBuilder("dynamicFinishStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("======================================");
                    System.out.println("[动态全量] 开始执行索引结束处理...");
                    dynamicWriterFactory.finishIndex();
                    Object stats = dynamicWriterFactory.getIndexStats();
                    System.out.println("[动态全量] 索引统计信息: " + stats);
                    System.out.println("[动态全量] 索引结束处理完成！");
                    System.out.println("======================================");
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    // ===== Readers, Processors, Writers =====

    @Bean
    public ItemReader<DynamicRecord> dynamicFullReader() {
        return dynamicRdbFullReader.createPagingReader(chunkSize);
    }

    @Bean
    public ItemProcessor<DynamicRecord, DynamicRecord> dynamicFullProcessor() {
        return record -> {
            // 转换为索引记录
            return recordBuilderService.convertToIndexRecord(record, indexName);
        };
    }

    @Bean
    public ItemWriter<DynamicRecord> dynamicFullWriter() {
        return dynamicWriterFactory.getWriter();
    }
}

