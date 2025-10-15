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

    @Value("${index.parallelism.chunkSize:1000}")
    private int chunkSize;

    @Value("${index.parallelism.threads:4}")
    private int threads;

    @Value("${index.incremental.overlapMinutes:5}")
    private int overlapMinutes;

    /**
     * 增量索引 Job
     */
    @Bean
    public Job incrementalIndexJob() {
        return new JobBuilder("incrementalIndexJob", jobRepository)
                .start(deltaReadProcessWriteStep())
                .next(checkStep())
                .next(conditionalRebuildStep())
                .build();
    }

    /**
     * 增量读取处理写入步骤
     */
    @Bean
    public Step deltaReadProcessWriteStep() {
        return new StepBuilder("deltaReadProcessWriteStep", jobRepository)
                .<SourceRecord, SourceRecord>chunk(chunkSize, transactionManager)
                .reader(deltaReader())
                .processor(deltaProcessor())
                .writer(deltaWriter())
                .faultTolerant()
                .retryLimit(3)
                .retry(Exception.class)
                .skipLimit(100)
                .skip(Exception.class)
                .build();
    }

    /**
     * 检查步骤：索引完整性检查
     */
    @Bean
    public Step checkStep() {
        return new StepBuilder("checkStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    try {
                        boolean isValid = indexCheckService.checkIndexIntegrity();
                        if (!isValid) {
                            throw new RuntimeException("Index integrity check failed");
                        }
                        return null;
                    } catch (Exception e) {
                        throw new RuntimeException("Index check failed", e);
                    }
                }, transactionManager)
                .build();
    }

    /**
     * 条件重建步骤：根据检查结果决定是否重建
     */
    @Bean
    public Step conditionalRebuildStep() {
        return new StepBuilder("conditionalRebuildStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    try {
                        // 检查是否需要重建
                        if (indexRetryService.shouldRebuild()) {
                            // 执行重建策略
                            indexRetryService.executeRebuildStrategy();
                        }
                        return null;
                    } catch (Exception e) {
                        throw new RuntimeException("Conditional rebuild failed", e);
                    }
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
