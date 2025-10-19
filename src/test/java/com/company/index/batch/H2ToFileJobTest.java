package com.company.index.batch;

import com.company.index.batch.job.FullIndexJobConfig;
import com.company.index.batch.job.IncrementalIndexJobConfig;
import com.company.index.common.TestDataGenerator;
import com.company.index.config.TestBatchConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * H2 到 File 的 Job 测试
 */
@SpringBootTest(classes = {TestBatchConfig.class})
@ActiveProfiles("h2-file")
public class H2ToFileJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    @Qualifier("fullIndexJob")
    private Job fullIndexJob;

    @Autowired
    @Qualifier("incrementalIndexJob")
    private Job incrementalIndexJob;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private TestDataGenerator testDataGenerator;

    @BeforeEach
    void setUp() throws Exception {
        // 创建测试表和数据
        testDataGenerator.createTable("orders");
        testDataGenerator.insertTestData("orders", 100);
        
        System.out.println("✓ H2 测试数据准备完成");
    }

    @Test
    void testFullIndexJob() throws Exception {
        // 设置 Job
        jobLauncherTestUtils.setJob(fullIndexJob);
        
        // 创建 Job 参数
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
        
        // 运行 Job
        System.out.println("========================================");
        System.out.println("开始运行 H2 到 File 全量索引 Job");
        System.out.println("========================================");
        
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        
        // 验证结果
        assertEquals("COMPLETED", jobExecution.getExitStatus().getExitCode());
        
        System.out.println("✓ H2 到 File 全量索引 Job 执行成功");
        System.out.println("Job 状态: " + jobExecution.getExitStatus().getExitCode());
        System.out.println("处理记录数: " + jobExecution.getStepExecutions().iterator().next().getReadCount());
    }

    @Test
    void testIncrementalIndexJob() throws Exception {
        // 设置 Job
        jobLauncherTestUtils.setJob(incrementalIndexJob);
        
        // 创建 Job 参数
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
        
        // 运行 Job
        System.out.println("========================================");
        System.out.println("开始运行 H2 到 File 增量索引 Job");
        System.out.println("========================================");
        
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        
        // 验证结果
        assertEquals("COMPLETED", jobExecution.getExitStatus().getExitCode());
        
        System.out.println("✓ H2 到 File 增量索引 Job 执行成功");
        System.out.println("Job 状态: " + jobExecution.getExitStatus().getExitCode());
        System.out.println("处理记录数: " + jobExecution.getStepExecutions().iterator().next().getReadCount());
    }
}
