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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Oracle → File 集成测试
 * 使用Docker中的真实Oracle数据库
 */
@SpringBootTest(classes = com.company.index.Application.class)
@SpringBatchTest
@Import(com.company.index.config.TestBatchConfig.class)
@ActiveProfiles({"test-oracle-file"})
@TestExecutionListeners(listeners = {
    DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class
}, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@TestPropertySource(properties = {
    "spring.batch.job.enabled=false",
    "index.retry.maxAttempts=3"
})
public class OracleToFileJobTest extends AbstractJobTest {

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
        return "File";
    }

    @Override
    protected String getTestProfile() {
        return "test-oracle-file";
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
        // 清理测试输出文件
        String outputFile = "./target/test-output/oracle-to-file-output.json";
        java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(outputFile));
    }

    @Test
    public void testOracleToFileFullIndexJob() throws Exception {
        // 准备输出目录
        String outputPath = "./target/test-output";
        File outputDir = new File(outputPath);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // 构建Job参数
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .addString("jobType", "FULL")
                .addString("dataSourceType", "oracle")
                .addString("indexTargetType", "file")
                .toJobParameters();

        // 执行Job
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // 验证执行结果
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus(),
            "Job应该成功完成");

        // 验证输出目录中有文件生成（文件名带时间戳）
        File[] files = outputDir.listFiles((dir, name) -> 
            name.startsWith("oracle-to-file-output-") && name.endsWith(".json"));
        
        assertNotNull(files, "输出目录应该存在");
        assertTrue(files.length > 0, 
            "输出目录应该包含至少一个文件: " + outputPath);

        // 验证最新的输出文件内容不为空
        File latestFile = files[files.length - 1];
        long fileSize = latestFile.length();
        assertTrue(fileSize > 0, "输出文件不应为空: " + latestFile.getName());

        System.out.println("输出文件: " + latestFile.getAbsolutePath() + " (大小: " + fileSize + " 字节)");

        // 打印测试总结
        printTestSummary(jobExecution, true);

        // 验证至少读取了一些数据
        assertTrue(jobExecution.getStepExecutions().stream()
                .anyMatch(step -> step.getReadCount() > 0),
            "应该至少读取了一些数据");
    }

    @Test
    public void testOracleConnectionAndQuery() throws Exception {
        // 简单的连接测试
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .addString("jobType", "FULL")
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // 验证Job能够成功启动并连接Oracle
        assertNotNull(jobExecution);
        assertNotEquals("FAILED", jobExecution.getExitStatus().getExitCode(),
            "Job不应该失败，请检查Oracle连接");
    }

}

