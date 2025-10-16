package com.company.index.batch.job;

import com.company.index.batch.job.base.AbstractJobTest;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Oracle → Elasticsearch 集成测试
 * 使用Docker中的真实Oracle和Elasticsearch
 */
@SpringBootTest(classes = com.company.index.Application.class)
@SpringBatchTest
@Import(com.company.index.config.TestBatchConfig.class)
@ActiveProfiles({"test-oracle-es"})
@TestExecutionListeners(listeners = {
    DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class
}, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@TestPropertySource(properties = {
    "spring.batch.job.enabled=false",
    "index.retry.maxAttempts=3"
})
public class OracleToElasticsearchJobTest extends AbstractJobTest {

    @Autowired
    @Qualifier("fullIndexJob")
    private Job fullIndexJob;

    @Override
    protected String getJobName() {
        jobLauncherTestUtils.setJob(fullIndexJob);
        return "fullIndexJob";
    }

    @Override
    protected String getDataSourceType() {
        return "Oracle (Docker)";
    }

    @Override
    protected String getIndexTargetType() {
        return "Elasticsearch";
    }

    @Override
    protected String getTestProfile() {
        return "test-oracle-es";
    }

    @Override
    protected void prepareTestData() throws Exception {
        System.out.println("准备 Oracle 测试数据...");
        // Oracle 使用 Docker 容器，数据由 oracle_init.sql 初始化
        // 这里可以添加额外的测试数据（如果需要）
    }

    @Override
    protected void cleanupTestData() throws Exception {
        System.out.println("清理 Oracle 测试数据...");
        // 如果需要清理 Elasticsearch 索引，可以在这里实现
    }

    @Test
    public void testOracleToElasticsearchFullIndexJob() throws Exception {
        // 构建Job参数
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .addString("jobType", "FULL")
                .addString("dataSourceType", "oracle")
                .addString("indexTargetType", "elasticsearch")
                .toJobParameters();

        // 执行Job
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // 验证执行结果
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus(),
            "Job应该成功完成");

        // 打印测试总结
        printTestSummary(jobExecution, true);

        // 验证至少读取了一些数据
        assertTrue(jobExecution.getStepExecutions().stream()
                .anyMatch(step -> step.getReadCount() > 0),
            "应该至少读取了一些数据");

        // 验证写入了数据
        assertTrue(jobExecution.getStepExecutions().stream()
                .anyMatch(step -> step.getWriteCount() > 0),
            "应该至少写入了一些数据到Elasticsearch");
    }

    @Test
    public void testOracleIncrementalIndex() throws Exception {
        // 构建增量索引Job参数
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .addString("jobType", "INCREMENTAL")
                .addString("dataSourceType", "oracle")
                .addString("indexTargetType", "elasticsearch")
                .toJobParameters();

        // 执行Job
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // 验证执行结果（增量索引可能没有数据，所以只检查是否完成）
        assertNotNull(jobExecution);
        assertNotEquals("FAILED", jobExecution.getExitStatus().getExitCode(),
            "增量索引Job不应该失败");

        // 打印测试总结
        printTestSummary(jobExecution, true);
    }

}

