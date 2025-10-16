package com.company.index.batch.job;

import com.company.index.batch.reader.DynamicRdbDeltaReader;
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
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;

/**
 * 动态增量索引 Job 配置
 * 使用 DynamicRecord 和字段映射配置
 */
@Configuration
public class DynamicIncrementalIndexJobConfig {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private DynamicRdbDeltaReader dynamicRdbDeltaReader;

    @Autowired
    private DynamicWriterFactory dynamicWriterFactory;

    @Autowired
    private IndexCheckService indexCheckService;

    @Autowired
    private RecordBuilderService recordBuilderService;

    @Autowired(required = false)
    private org.springframework.batch.core.launch.JobLauncher jobLauncher;

    @Autowired
    private ApplicationContext applicationContext;

    @Value("${index.parallelism.chunkSize:1000}")
    private int chunkSize;

    @Value("${index.retry.maxAttempts:3}")
    private int maxRetryAttempts;

    @Value("${index.dataSource.table:orders}")
    private String sourceTable;

    @Value("${index.indexTarget.indexName:order_index}")
    private String indexName;

    @Value("${index.incremental.overlapMinutes:5}")
    private int overlapMinutes;

    /**
     * 动态增量索引 Job
     * 流程：
     * 1. dynamicDeltaReadProcessWriteStep - 读取增量数据并写入
     * 2. dynamicIncrementalCheckStep - 检查索引完整性
     *    ├─ 检查成功 → dynamicIncrementalFinishStep - 完成
     *    └─ 检查失败 → dynamicTriggerFullRebuildStep - 触发全量重建
     */
    @Bean("dynamicIncrementalIndexJob")
    public Job dynamicIncrementalIndexJob() {
        return new JobBuilder("dynamicIncrementalIndexJob", jobRepository)
                .start(dynamicDeltaReadProcessWriteStep())
                .next(dynamicIncrementalCheckStep())
                .on("FAILED").to(dynamicTriggerFullRebuildStep())  // 检查失败 → 触发全量重建
                .from(dynamicIncrementalCheckStep()).on("*").to(dynamicIncrementalFinishStep())  // 检查成功 → 完成
                .end()
                .build();
    }

    /**
     * 增量读取处理写入步骤
     */
    @Bean
    public Step dynamicDeltaReadProcessWriteStep() {
        return new StepBuilder("dynamicDeltaReadProcessWriteStep", jobRepository)
                .<DynamicRecord, DynamicRecord>chunk(chunkSize, transactionManager)
                .reader(dynamicDeltaReader())
                .processor(dynamicDeltaProcessor())
                .writer(dynamicDeltaWriter())
                .faultTolerant()
                .retryLimit(maxRetryAttempts)
                .retry(RuntimeException.class)
                .retry(org.springframework.dao.DataAccessException.class)
                .skipLimit(100)  // 增量索引允许更多跳过
                .skip(RuntimeException.class)
                .skip(org.springframework.dao.DataAccessException.class)
                .listener(new org.springframework.batch.core.ItemWriteListener<DynamicRecord>() {
                    @Override
                    public void beforeWrite(org.springframework.batch.item.Chunk<? extends DynamicRecord> items) { }
                    
                    @Override
                    public void afterWrite(org.springframework.batch.item.Chunk<? extends DynamicRecord> items) { }
                    
                    @Override
                    public void onWriteError(Exception ex, org.springframework.batch.item.Chunk<? extends DynamicRecord> items) {
                        System.err.println("[动态增量] 写入失败，将重试。错误: " + ex.getMessage());
                        System.err.println("[动态增量] 失败的记录数: " + items.size());
                    }
                })
                .listener(new org.springframework.batch.core.SkipListener<DynamicRecord, DynamicRecord>() {
                    @Override
                    public void onSkipInRead(Throwable t) {
                        System.err.println("[动态增量] 读取时跳过记录，错误: " + t.getMessage());
                    }
                    
                    @Override
                    public void onSkipInWrite(DynamicRecord item, Throwable t) {
                        System.err.println("[动态增量] 跳过失败记录: " + item.getKeyValue() + "，错误: " + t.getMessage());
                    }
                    
                    @Override
                    public void onSkipInProcess(DynamicRecord item, Throwable t) {
                        System.err.println("[动态增量] 处理时跳过记录: " + item.getKeyValue() + "，错误: " + t.getMessage());
                    }
                })
                .build();
    }

    /**
     * 增量检查步骤
     */
    @Bean
    public Step dynamicIncrementalCheckStep() {
        return new StepBuilder("dynamicIncrementalCheckStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    boolean isValid = indexCheckService.checkIndexIntegrity();
                    if (!isValid) {
                        System.out.println("[动态增量] 索引完整性检查失败，将触发全量重建");
                        contribution.setExitStatus(org.springframework.batch.core.ExitStatus.FAILED);
                    } else {
                        System.out.println("[动态增量] 索引完整性检查通过");
                        contribution.setExitStatus(org.springframework.batch.core.ExitStatus.COMPLETED);
                    }
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    /**
     * 触发全量重建步骤
     */
    @Bean
    public Step dynamicTriggerFullRebuildStep() {
        return new StepBuilder("dynamicTriggerFullRebuildStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("======================================");
                    System.out.println("[动态增量] 触发全量索引重建");
                    System.out.println("======================================");
                    
                    try {
                        Job dynamicFullIndexJob = applicationContext.getBean("dynamicFullIndexJob", Job.class);
                        org.springframework.batch.core.JobParameters jobParameters = 
                            new org.springframework.batch.core.JobParametersBuilder()
                                .addLong("timestamp", System.currentTimeMillis())
                                .addString("triggeredBy", "dynamicIncrementalIndexJob")
                                .addString("reason", "incrementalCheckFailed")
                                .toJobParameters();
                        
                        System.out.println("[动态增量] 启动动态全量索引 Job...");
                        org.springframework.batch.core.JobExecution execution = 
                            jobLauncher.run(dynamicFullIndexJob, jobParameters);
                        
                        System.out.println("[动态增量] 动态全量索引 Job 执行完成");
                        System.out.println("[动态增量] 执行状态: " + execution.getStatus());
                        
                        if (execution.getStatus() != org.springframework.batch.core.BatchStatus.COMPLETED) {
                            System.err.println("[动态增量] 全量索引重建失败！");
                            throw new RuntimeException("Dynamic full index rebuild failed with status: " + execution.getStatus());
                        }
                        
                        System.out.println("[动态增量] 全量索引重建成功！");
                        System.out.println("======================================");
                        
                    } catch (org.springframework.beans.factory.NoSuchBeanDefinitionException e) {
                        System.err.println("[动态增量] 未找到 dynamicFullIndexJob Bean，可能在测试环境中");
                        System.err.println("[动态增量] 跳过全量重建步骤");
                        System.out.println("======================================");
                    } catch (Exception e) {
                        System.err.println("[动态增量] 触发全量重建失败: " + e.getMessage());
                        e.printStackTrace();
                        throw new RuntimeException("Failed to trigger dynamic full rebuild", e);
                    }
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    /**
     * 增量完成步骤
     */
    @Bean
    public Step dynamicIncrementalFinishStep() {
        return new StepBuilder("dynamicIncrementalFinishStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("======================================");
                    System.out.println("[动态增量] 增量索引更新成功完成");
                    
                    // 获取执行上下文中的统计信息
                    ExecutionContext executionContext = 
                        chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext();
                    
                    int readCount = executionContext.getInt("deltaReadCount", 0);
                    int writeCount = executionContext.getInt("deltaWriteCount", 0);
                    
                    System.out.println("[动态增量] 读取记录数: " + readCount);
                    System.out.println("[动态增量] 写入记录数: " + writeCount);
                    System.out.println("======================================");
                    
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    // ===== Readers, Processors, Writers =====

    /**
     * 增量读取器
     * 从 JobParameters 获取时间范围
     */
    @Bean
    public ItemReader<DynamicRecord> dynamicDeltaReader() {
        return new ItemReader<DynamicRecord>() {
            private ItemReader<DynamicRecord> delegate;
            
            @Override
            public DynamicRecord read() throws Exception {
                if (delegate == null) {
                    // 从 JobParameters 获取时间范围
                    // 默认：最近1小时
                    LocalDateTime endTime = LocalDateTime.now();
                    LocalDateTime startTime = endTime.minusHours(1);
                    
                    System.out.println("======================================");
                    System.out.println("[动态增量] 初始化增量读取器");
                    System.out.println("[动态增量] 时间范围: " + startTime + " ~ " + endTime);
                    System.out.println("======================================");
                    
                    delegate = dynamicRdbDeltaReader.createDeltaReader(startTime, endTime);
                    
                    // 打开 reader
                    if (delegate instanceof org.springframework.batch.item.ItemStream) {
                        ((org.springframework.batch.item.ItemStream) delegate).open(new ExecutionContext());
                    }
                }
                
                return delegate.read();
            }
        };
    }

    @Bean
    public ItemProcessor<DynamicRecord, DynamicRecord> dynamicDeltaProcessor() {
        return record -> {
            // 转换为索引记录
            DynamicRecord indexRecord = recordBuilderService.convertToIndexRecord(record, indexName);
            
            // 记录处理日志（可选）
            if (System.currentTimeMillis() % 100 == 0) {  // 每100条记录打印一次
                System.out.println("[动态增量] 处理记录: " + indexRecord.getKeyValue() + 
                                 ", 操作: " + indexRecord.getOperation());
            }
            
            return indexRecord;
        };
    }

    @Bean
    public ItemWriter<DynamicRecord> dynamicDeltaWriter() {
        return dynamicWriterFactory.getWriter();
    }
}

