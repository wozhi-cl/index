package com.company.index.batch.job;

import com.company.index.batch.reader.DataReader;
import com.company.index.batch.writer.DataWriter;
import com.company.index.batch.reader.H2Reader;
import com.company.index.batch.reader.MySQLReader;
import com.company.index.batch.reader.OracleReader;
import com.company.index.batch.writer.FileWriter;
import com.company.index.batch.writer.ElasticsearchWriter;
import com.company.index.batch.writer.GetQuickWriter;
import com.company.index.batch.check.IndexCheckService;
import com.company.index.common.model.SourceRecord;
import com.company.index.common.model.IndexDocument;
import com.company.index.common.service.RecordBuilderService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 全量索引 Job 配置
 * 流程：
 * 1. cleanupStep - 删除旧索引
 * 2. readProcessWriteStep - 读取处理写入
 * 3. fullCheckStep - 检查索引完整性
 * ├─ 检查成功 → finishStep - 结束处理
 * └─ 检查失败 → retryDecisionStep - 判断是否重试
 * ├─ 未达最大重试次数 → 返回 cleanupStep（重新开始）
 * └─ 已达最大重试次数 → rebuildStep（清理并报告失败）
 * <p>
 * 注意：
 * - 最多重试3次全量索引流程（可配置）
 * - readProcessWriteStep 内部也有 chunk-level 重试
 */
@Configuration
public class FullIndexJobConfig {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private H2Reader h2Reader;

    @Autowired
    private MySQLReader mysqlReader;

    @Autowired
    private OracleReader oracleReader;

    @Autowired
    private FileWriter fileWriter;

    @Autowired
    private ElasticsearchWriter elasticsearchWriter;

    @Autowired
    private GetQuickWriter getQuickWriter;

    @Autowired
    private RecordBuilderService recordBuilderService;

    @Autowired
    private IndexCheckService indexCheckService;

    @Value("${index.parallelism.chunkSize:1000}")
    private int chunkSize;

    @Value("${index.dataSource.type:h2}")
    private String dataSourceType;

    @Value("${index.indexTarget.type:file}")
    private String indexTargetType;

    @Value("${index.retry.maxAttempts:3}")
    private int maxRetryAttempts;

    /**
     * 全量索引 Job
     */
    @Bean
    public Job fullIndexJob() {
        return new JobBuilder("fullIndexJob", jobRepository)
                .start(cleanupStep())
                .next(readProcessWriteStep())
                .next(fullCheckStep())
                .on("SUCCESS").to(finishStep())
                .on("FAILED").to(retryDecisionStep())
                .from(retryDecisionStep())
                .on("RETRY").to(cleanupStep())
                .on("FAIL").to(rebuildStep())
                .end()
                .build();
    }

    /**
     * 清理步骤 - 删除旧索引
     */
    @Bean
    public Step cleanupStep() {
        return new StepBuilder("cleanupStep", jobRepository)
                .tasklet(cleanupTasklet(), transactionManager)
                .build();
    }

    /**
     * 读取处理写入步骤
     */
    @Bean
    public Step readProcessWriteStep() {
        return new StepBuilder("readProcessWriteStep", jobRepository)
                .<SourceRecord, IndexDocument>chunk(chunkSize, transactionManager)
                .reader(dynamicFullReader())
                .processor(sourceToIndexProcessor())
                .writer(dynamicIndexWriter())
                .faultTolerant()
                .retryLimit(3)
                .retry(Exception.class)
                .build();
    }

    /**
     * 全量检查步骤
     */
    @Bean
    public Step fullCheckStep() {
        return new StepBuilder("fullCheckStep", jobRepository)
                .tasklet(fullCheckTasklet(), transactionManager)
                .build();
    }

    /**
     * 完成步骤
     */
    @Bean
    public Step finishStep() {
        return new StepBuilder("finishStep", jobRepository)
                .tasklet(finishTasklet(), transactionManager)
                .build();
    }

    /**
     * 重试决策步骤
     */
    @Bean
    public Step retryDecisionStep() {
        return new StepBuilder("retryDecisionStep", jobRepository)
                .tasklet(retryDecisionTasklet(), transactionManager)
                .build();
    }

    /**
     * 重建步骤
     */
    @Bean
    public Step rebuildStep() {
        return new StepBuilder("rebuildStep", jobRepository)
                .tasklet(rebuildTasklet(), transactionManager)
                .build();
    }

    /**
     * 动态全量读取器
     */
    @Bean
    public ItemReader<SourceRecord> dynamicFullReader() {
        DataReader reader = getDataReader();
        return reader.createFullReader(chunkSize);
    }

    /**
     * 动态索引写入器
     */
    @Bean
    public ItemWriter<IndexDocument> dynamicIndexWriter() {
        DataWriter writer = getDataWriter();
        return writer.createIndexWriter();
    }

    /**
     * 源数据到索引数据的处理器
     */
    @Bean
    public ItemProcessor<SourceRecord, IndexDocument> sourceToIndexProcessor() {
        return sourceRecord -> {
            if (sourceRecord == null) {
                return null;
            }

            // 验证源记录
            if (!recordBuilderService.validateSourceRecord(sourceRecord)) {
                System.err.println("Invalid source record: " + sourceRecord);
                return null;
            }

            // 转换为索引文档
            IndexDocument indexDoc = recordBuilderService.convertToIndexDocument(sourceRecord, "order_index");

            System.out.println("Processing: " + sourceRecord.getKeyValue() + " -> " + indexDoc.getKeyValue());
            return indexDoc;
        };
    }

    /**
     * 清理任务
     */
    @Bean
    public Tasklet cleanupTasklet() {
        return (contribution, chunkContext) -> {
            try {
                DataWriter writer = getDataWriter();
                writer.deleteIndex();
                System.out.println("✓ 清理旧索引完成");
                return RepeatStatus.FINISHED;
            } catch (Exception e) {
                System.err.println("✗ 清理旧索引失败: " + e.getMessage());
                throw e;
            }
        };
    }

    /**
     * 全量检查任务
     */
    @Bean
    public Tasklet fullCheckTasklet() {
        return (contribution, chunkContext) -> {
            boolean isHealthy = indexCheckService.checkIndexIntegrity();
            if (isHealthy) {
                System.out.println("✓ 全量索引检查通过");
                contribution.setExitStatus(org.springframework.batch.core.ExitStatus.COMPLETED);
            } else {
                System.err.println("✗ 全量索引检查失败");
                contribution.setExitStatus(org.springframework.batch.core.ExitStatus.FAILED);
            }
            return RepeatStatus.FINISHED;
        };
    }

    /**
     * 完成任务
     */
    @Bean
    public Tasklet finishTasklet() {
        return (contribution, chunkContext) -> {
            try {
                DataWriter writer = getDataWriter();
                writer.finishWrite();
                System.out.println("✓ 全量索引完成");
                return RepeatStatus.FINISHED;
            } catch (Exception e) {
                System.err.println("✗ 完成索引失败: " + e.getMessage());
                throw e;
            }
        };
    }

    /**
     * 重试决策任务
     */
    @Bean
    public Tasklet retryDecisionTasklet() {
        return (contribution, chunkContext) -> {
            // 获取当前重试次数
            int retryCount = chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().getInt("retryCount", 0);

            if (retryCount < maxRetryAttempts) {
                retryCount++;
                chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().putInt("retryCount", retryCount);
                System.out.println("重试第 " + retryCount + " 次全量索引");
                contribution.setExitStatus(org.springframework.batch.core.ExitStatus.COMPLETED);
            } else {
                System.err.println("已达到最大重试次数 " + maxRetryAttempts + "，停止重试");
                contribution.setExitStatus(org.springframework.batch.core.ExitStatus.FAILED);
            }
            return RepeatStatus.FINISHED;
        };
    }

    /**
     * 重建任务
     */
    @Bean
    public Tasklet rebuildTasklet() {
        return (contribution, chunkContext) -> {
            try {
                DataWriter writer = getDataWriter();
                writer.deleteIndex();
                System.out.println("✗ 全量索引重建失败，已清理索引");
                return RepeatStatus.FINISHED;
            } catch (Exception e) {
                System.err.println("✗ 重建清理失败: " + e.getMessage());
                throw e;
            }
        };
    }

    /**
     * 根据配置获取数据读取器
     */
    private DataReader getDataReader() {
        switch (dataSourceType.toLowerCase()) {
            case "h2":
                return h2Reader;
            case "mysql":
                return mysqlReader;
            case "oracle":
                return oracleReader;
            default:
                throw new IllegalArgumentException("Unsupported data source type: " + dataSourceType);
        }
    }

    /**
     * 根据配置获取数据写入器
     */
    private DataWriter getDataWriter() {
        switch (indexTargetType.toLowerCase()) {
            case "file":
                return fileWriter;
            case "elasticsearch":
            case "es":
                return elasticsearchWriter;
            case "getquick":
            case "gq":
                return getQuickWriter;
            default:
                throw new IllegalArgumentException("Unsupported index target type: " + indexTargetType);
        }
    }
}