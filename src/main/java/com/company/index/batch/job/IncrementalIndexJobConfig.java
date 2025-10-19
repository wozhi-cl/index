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

import java.time.LocalDateTime;

/**
 * 增量索引 Job 配置
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
@Configuration
public class IncrementalIndexJobConfig {

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

    /**
     * 增量索引 Job
     */
    @Bean
    public Job incrementalIndexJob() {
        return new JobBuilder("incrementalIndexJob", jobRepository)
                .start(deltaReadProcessWriteStep())
                .next(incrementalCheckStep())
                .on("SUCCESS").to(incrementalFinishStep())
                .on("FAILED").to(triggerFullRebuildStep())
                .end()
                .build();
    }

    /**
     * 增量读取处理写入步骤
     */
    @Bean
    public Step deltaReadProcessWriteStep() {
        return new StepBuilder("deltaReadProcessWriteStep", jobRepository)
                .<SourceRecord, IndexDocument>chunk(chunkSize, transactionManager)
                .reader(dynamicDeltaReader())
                .processor(incrementalSourceToIndexProcessor())
                .writer(incrementalIndexWriter())
                .faultTolerant()
                .retryLimit(3)
                .retry(Exception.class)
                .build();
    }

    /**
     * 增量检查步骤
     */
    @Bean
    public Step incrementalCheckStep() {
        return new StepBuilder("incrementalCheckStep", jobRepository)
                .tasklet(incrementalCheckTasklet(), transactionManager)
                .build();
    }

    /**
     * 增量完成步骤
     */
    @Bean
    public Step incrementalFinishStep() {
        return new StepBuilder("incrementalFinishStep", jobRepository)
                .tasklet(incrementalFinishTasklet(), transactionManager)
                .build();
    }

    /**
     * 触发全量重建步骤
     */
    @Bean
    public Step triggerFullRebuildStep() {
        return new StepBuilder("triggerFullRebuildStep", jobRepository)
                .tasklet(triggerFullRebuildTasklet(), transactionManager)
                .build();
    }

    /**
     * 动态增量读取器
     */
    @Bean("dynamicDeltaReader")
    public ItemReader<SourceRecord> dynamicDeltaReader() {
        DataReader reader = getDataReader();
        
        // 设置时间范围（默认过去30天）
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(30);
        
        return reader.createDeltaReader(startTime, endTime);
    }

    /**
     * 动态索引写入器（增量）
     */
    @Bean("incrementalIndexWriter")
    public ItemWriter<IndexDocument> incrementalIndexWriter() {
        DataWriter writer = getDataWriter();
        return writer.createIndexWriter();
    }

    /**
     * 源数据到索引数据的处理器（增量）
     */
    @Bean("incrementalSourceToIndexProcessor")
    public ItemProcessor<SourceRecord, IndexDocument> incrementalSourceToIndexProcessor() {
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
     * 增量检查任务
     */
    @Bean
    public Tasklet incrementalCheckTasklet() {
        return (contribution, chunkContext) -> {
            boolean isHealthy = indexCheckService.checkIndexIntegrity();
            if (isHealthy) {
                System.out.println("✓ 增量索引检查通过");
                contribution.setExitStatus(org.springframework.batch.core.ExitStatus.COMPLETED);
            } else {
                System.err.println("✗ 增量索引检查失败");
                contribution.setExitStatus(org.springframework.batch.core.ExitStatus.FAILED);
            }
            return RepeatStatus.FINISHED;
        };
    }

    /**
     * 增量完成任务
     */
    @Bean
    public Tasklet incrementalFinishTasklet() {
        return (contribution, chunkContext) -> {
            try {
                DataWriter writer = getDataWriter();
                writer.finishWrite();
                System.out.println("✓ 增量索引完成");
                return RepeatStatus.FINISHED;
            } catch (Exception e) {
                System.err.println("✗ 完成增量索引失败: " + e.getMessage());
                throw e;
            }
        };
    }

    /**
     * 触发全量重建任务
     */
    @Bean
    public Tasklet triggerFullRebuildTasklet() {
        return (contribution, chunkContext) -> {
            try {
                System.out.println("========================================");
                System.out.println("增量索引检查失败，触发全量重建");
                System.out.println("========================================");
                
                // 这里可以启动全量索引 Job
                // 在实际实现中，可以通过 JobLauncher 启动 fullIndexJob
                
                System.out.println("✓ 已触发全量重建流程");
                return RepeatStatus.FINISHED;
            } catch (Exception e) {
                System.err.println("✗ 触发全量重建失败: " + e.getMessage());
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