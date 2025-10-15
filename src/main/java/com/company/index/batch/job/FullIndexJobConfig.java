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

    /**
     * 全量索引 Job
     */
    @Bean
    public Job fullIndexJob() {
        return new JobBuilder("fullIndexJob", jobRepository)
                .start(cleanupStep())
                .next(readProcessWriteStep())
                .next(fullCheckStep())
                .next(switchStep())
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
     */
    @Bean
    public Step readProcessWriteStep() {
        return new StepBuilder("readProcessWriteStep", jobRepository)
                .<SourceRecord, SourceRecord>chunk(chunkSize, transactionManager)
                .reader(fullReader())
                .processor(fullProcessor())
                .writer(fullWriter())
                .faultTolerant()
                .retryLimit(3)
                .retry(Exception.class)
                .build();
    }

    /**
     * 检查步骤：索引完整性检查
     */
    @Bean
    public Step fullCheckStep() {
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
     * 切换步骤：别名切换或最终化处理
     */
    @Bean
    public Step switchStep() {
        return new StepBuilder("switchStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    try {
                        // 这里可以实现别名切换逻辑
                        // 对于 ES，可以切换别名指向新索引
                        // 对于 GetQuick，可以发布索引
                        return null;
                    } catch (Exception e) {
                        throw new RuntimeException("Index switch failed", e);
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
