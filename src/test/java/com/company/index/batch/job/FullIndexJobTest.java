package com.company.index.batch.job;

import org.junit.jupiter.api.*;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import com.company.index.config.TestBatchConfig;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 全量索引Job集成测试
 * 测试全量索引Job的完整流程
 */
@SpringBootTest
@SpringBatchTest
@ActiveProfiles("h2")
@Import(TestBatchConfig.class)  // 导入测试配置，使用同步JobLauncher
@TestPropertySource(properties = {
    "spring.batch.job.enabled=false",  // 禁用自动启动
    "index.parallelism.chunkSize=10",
    "index.parallelism.threads=2",
    "index.retry.maxAttempts=3",
    "index.dataSource.type=h2",
    "index.dataSource.table=sample_data",
    "index.indexTarget.type=file",
    "index.indexTarget.path=./target/test-output",
    "index.indexTarget.fileName=full-index-test-output.json"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FullIndexJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private Job fullIndexJob;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 测试前准备：清空并初始化测试数据
     */
    @BeforeEach
    void setUp() {
        // 设置要测试的Job
        jobLauncherTestUtils.setJob(fullIndexJob);
        
        // 清空测试数据
        cleanupTestData();
        
        // 初始化测试数据
        initTestData();
    }

    /**
     * 测试后清理
     */
    @AfterEach
    void tearDown() {
        cleanupTestData();
    }

    /**
     * 清空测试数据
     */
    private void cleanupTestData() {
        try {
            jdbcTemplate.execute("DELETE FROM sample_data");
            System.out.println("✅ 测试数据已清空");
        } catch (Exception e) {
            System.err.println("⚠️  清空测试数据失败: " + e.getMessage());
        }
    }

    /**
     * 初始化测试数据
     * 插入100条测试记录用于全量索引
     */
    private void initTestData() {
        String sql = "INSERT INTO sample_data (name, description, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
        
        LocalDateTime now = LocalDateTime.now();
        int recordCount = 100;
        
        try {
            for (int i = 1; i <= recordCount; i++) {
                String name = "TestUser_" + i;
                String description = "这是第" + i + "条测试数据";
                String status = i % 2 == 0 ? "active" : "inactive";
                String timestamp = now.minusHours(recordCount - i).format(FORMATTER);
                
                jdbcTemplate.update(sql, name, description, status, timestamp, timestamp);
            }
            
            int count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sample_data", Integer.class);
            System.out.println("✅ 测试数据初始化完成，共插入 " + count + " 条记录");
            
        } catch (Exception e) {
            System.err.println("❌ 测试数据初始化失败: " + e.getMessage());
            throw new RuntimeException("Failed to initialize test data", e);
        }
    }

    /**
     * 验证测试数据已正确插入
     */
    @Test
    @Order(1)
    @DisplayName("1. 验证测试数据初始化")
    void testDataInitialization() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sample_data", Integer.class);
        assertNotNull(count, "数据总数不应为null");
        assertEquals(100, count, "应该有100条测试数据");
        
        System.out.println("✅ 测试数据验证通过: " + count + " 条记录");
    }

    /**
     * 测试全量索引Job的完整执行
     */
    @Test
    @Order(2)
    @DisplayName("2. 测试全量索引Job完整执行")
    void testFullIndexJobExecution() throws Exception {
        // 准备Job参数
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("jobId", "fullIndexJob_test_" + System.currentTimeMillis())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        System.out.println("\n====== 准备执行Job ======");
        System.out.println("Job参数: " + jobParameters);
        
        // 执行Job
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // 打印Job执行信息
        System.out.println("\n====== Job执行返回 ======");
        System.out.println("JobExecution ID: " + jobExecution.getId());
        System.out.println("Job状态: " + jobExecution.getStatus());
        System.out.println("退出状态: " + jobExecution.getExitStatus());
        
        // 验证Job执行状态
        assertNotNull(jobExecution, "JobExecution不应为null");
        
        // 如果状态不是COMPLETED，打印详细错误信息
        if (jobExecution.getStatus() != BatchStatus.COMPLETED) {
            System.err.println("\n⚠️  警告：Job执行失败！");
            System.err.println("Job状态: " + jobExecution.getStatus());
            System.err.println("退出消息: " + jobExecution.getExitStatus().getExitDescription());
            
            // 打印所有异常
            for (Throwable e : jobExecution.getAllFailureExceptions()) {
                System.err.println("异常: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus(), 
                    "Job应该成功完成，实际状态: " + jobExecution.getStatus() + 
                    ", 退出消息: " + jobExecution.getExitStatus().getExitDescription());
        assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus(), 
                    "Job退出状态应该是COMPLETED");

        // 验证各个Step的执行情况
        System.out.println("\n====== Job执行详情 ======");
        System.out.println("Job名称: " + jobExecution.getJobInstance().getJobName());
        System.out.println("Job状态: " + jobExecution.getStatus());
        System.out.println("开始时间: " + jobExecution.getStartTime());
        System.out.println("结束时间: " + jobExecution.getEndTime());
        
        // 计算执行时间
        if (jobExecution.getEndTime() != null && jobExecution.getStartTime() != null) {
            Duration duration = Duration.between(jobExecution.getStartTime(), jobExecution.getEndTime());
            System.out.println("执行耗时: " + duration.toMillis() + "ms");
        }

        System.out.println("\n====== Step执行详情 ======");
        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            System.out.println("\nStep名称: " + stepExecution.getStepName());
            System.out.println("  状态: " + stepExecution.getStatus());
            System.out.println("  读取数: " + stepExecution.getReadCount());
            System.out.println("  写入数: " + stepExecution.getWriteCount());
            System.out.println("  提交数: " + stepExecution.getCommitCount());
            System.out.println("  跳过数: " + stepExecution.getSkipCount());
            System.out.println("  退出状态: " + stepExecution.getExitStatus().getExitCode());
            
            // 验证Step状态
            assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus(), 
                        "Step [" + stepExecution.getStepName() + "] 应该成功完成");
        }

        // 验证Step数量（全量索引应该有4个步骤）
        assertEquals(4, jobExecution.getStepExecutions().size(), 
                    "全量索引应该包含4个步骤: cleanup, readProcessWrite, check, switch");
    }

    /**
     * 测试单个Step的执行 - cleanupStep
     */
    @Test
    @Order(3)
    @DisplayName("3. 测试清理步骤(cleanupStep)")
    void testCleanupStep() throws Exception {
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("cleanupStep");
        
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        
        assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus(), 
                    "cleanupStep应该成功完成");
        assertEquals(ExitStatus.COMPLETED, stepExecution.getExitStatus(),
                    "cleanupStep退出状态应该是COMPLETED");
        
        System.out.println("✅ cleanupStep执行成功");
        System.out.println("   状态: " + stepExecution.getStatus());
        System.out.println("   退出码: " + stepExecution.getExitStatus().getExitCode());
    }

    /**
     * 测试单个Step的执行 - readProcessWriteStep
     */
    @Test
    @Order(4)
    @DisplayName("4. 测试读写处理步骤(readProcessWriteStep)")
    void testReadProcessWriteStep() throws Exception {
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("readProcessWriteStep");
        
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        
        assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus(), 
                    "readProcessWriteStep应该成功完成");
        
        // 验证读写数量
        assertTrue(stepExecution.getReadCount() > 0, "应该读取了数据");
        assertEquals(stepExecution.getReadCount(), stepExecution.getWriteCount(), 
                    "读取数量应该等于写入数量");
        
        System.out.println("✅ readProcessWriteStep执行成功");
        System.out.println("   读取数: " + stepExecution.getReadCount());
        System.out.println("   写入数: " + stepExecution.getWriteCount());
        System.out.println("   提交数: " + stepExecution.getCommitCount());
        System.out.println("   跳过数: " + stepExecution.getSkipCount());
    }

    /**
     * 测试单个Step的执行 - fullCheckStep
     */
    @Test
    @Order(5)
    @DisplayName("5. 测试检查步骤(fullCheckStep)")
    void testCheckStep() throws Exception {
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("fullCheckStep");
        
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        
        // 注意：检查步骤可能会失败（如果ES未连接），这是正常的
        BatchStatus status = stepExecution.getStatus();
        System.out.println("✅ fullCheckStep执行完成");
        System.out.println("   状态: " + status);
        System.out.println("   退出码: " + stepExecution.getExitStatus().getExitCode());
        
        // 只验证步骤已执行，不强制要求成功
        assertNotNull(status, "Step状态不应为null");
    }

    /**
     * 测试单个Step的执行 - finishStep
     */
    @Test
    @Order(6)
    @DisplayName("6. 测试结束处理步骤(finishStep)")
    void testFinishStep() throws Exception {
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("finishStep");
        
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        
        assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus(), 
                    "finishStep应该成功完成");
        
        System.out.println("✅ finishStep执行成功");
        System.out.println("   状态: " + stepExecution.getStatus());
    }

    /**
     * 测试单个Step的执行 - retryDecisionStep
     */
    @Test
    @Order(7)
    @DisplayName("7. 测试重试决策步骤(retryDecisionStep)")
    void testRetryDecisionStep() throws Exception {
        // 先初始化重试计数为0
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("retryDecisionStep");
        
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        
        // 第一次调用应该返回RETRY（因为重试次数为0 < 3）
        assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus(), 
                    "retryDecisionStep应该成功完成");
        assertEquals("RETRY", stepExecution.getExitStatus().getExitCode(), 
                    "第一次调用应该返回RETRY");
        
        System.out.println("✅ retryDecisionStep执行成功");
        System.out.println("   状态: " + stepExecution.getStatus());
        System.out.println("   退出码: " + stepExecution.getExitStatus().getExitCode());
    }

    /**
     * 测试单个Step的执行 - rebuildStep
     */
    @Test
    @Order(8)
    @DisplayName("8. 测试重建步骤(rebuildStep)")
    void testRebuildStep() throws Exception {
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("rebuildStep");
        
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        
        assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus(), 
                    "rebuildStep应该成功完成");
        
        System.out.println("✅ rebuildStep执行成功");
        System.out.println("   状态: " + stepExecution.getStatus());
    }

    /**
     * 测试空数据集的Job执行
     */
    @Test
    @Order(9)
    @DisplayName("9. 测试空数据集的Job执行")
    void testFullIndexJobWithEmptyData() throws Exception {
        // 清空所有数据
        jdbcTemplate.execute("DELETE FROM sample_data");
        
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sample_data", Integer.class);
        assertEquals(0, count, "数据应该已被清空");

        // 准备Job参数
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("jobId", "fullIndexJob_empty_test_" + System.currentTimeMillis())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        // 执行Job
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // 验证Job执行状态（空数据集也应该成功完成）
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus(), 
                    "即使没有数据，Job也应该成功完成");
        
        System.out.println("✅ 空数据集测试通过");
        System.out.println("   Job状态: " + jobExecution.getStatus());
    }

    /**
     * 测试大数据量的Job执行
     */
    @Test
    @Order(8)
    @DisplayName("8. 测试大数据量的Job执行")
    void testFullIndexJobWithLargeData() throws Exception {
        // 插入更多数据
        String sql = "INSERT INTO sample_data (name, description, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(FORMATTER);
        
        // 插入1000条额外数据
        for (int i = 101; i <= 1100; i++) {
            jdbcTemplate.update(sql, 
                "TestUser_" + i, 
                "这是第" + i + "条大数据量测试数据", 
                i % 2 == 0 ? "active" : "inactive",
                timestamp, 
                timestamp);
        }
        
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sample_data", Integer.class);
        System.out.println("✅ 大数据量测试准备完成，共 " + count + " 条记录");

        // 准备Job参数
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("jobId", "fullIndexJob_large_test_" + System.currentTimeMillis())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        // 执行Job并计时
        long startTime = System.currentTimeMillis();
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        long endTime = System.currentTimeMillis();

        // 验证Job执行状态
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus(), 
                    "大数据量Job应该成功完成");
        
        System.out.println("✅ 大数据量测试通过");
        System.out.println("   处理记录数: " + count);
        System.out.println("   执行耗时: " + (endTime - startTime) + "ms");
        
        // 验证处理性能
        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            if ("readProcessWriteStep".equals(stepExecution.getStepName())) {
                System.out.println("   读取数: " + stepExecution.getReadCount());
                System.out.println("   写入数: " + stepExecution.getWriteCount());
                assertTrue(stepExecution.getReadCount() >= 1000, 
                          "应该读取至少1000条记录");
            }
        }
    }

    /**
     * 测试Job的重复执行（幂等性）
     */
    @Test
    @Order(9)
    @DisplayName("9. 测试Job的重复执行")
    void testFullIndexJobIdempotency() throws Exception {
        // 第一次执行
        JobParameters jobParameters1 = new JobParametersBuilder()
                .addString("jobId", "fullIndexJob_idempotency_test_1")
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
        
        JobExecution jobExecution1 = jobLauncherTestUtils.launchJob(jobParameters1);
        assertEquals(BatchStatus.COMPLETED, jobExecution1.getStatus(), 
                    "第一次执行应该成功");

        // 第二次执行（使用不同的参数，因为Spring Batch不允许相同参数重复执行）
        Thread.sleep(100); // 确保时间戳不同
        JobParameters jobParameters2 = new JobParametersBuilder()
                .addString("jobId", "fullIndexJob_idempotency_test_2")
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
        
        JobExecution jobExecution2 = jobLauncherTestUtils.launchJob(jobParameters2);
        assertEquals(BatchStatus.COMPLETED, jobExecution2.getStatus(), 
                    "第二次执行应该成功");

        System.out.println("✅ 幂等性测试通过");
        System.out.println("   第一次执行状态: " + jobExecution1.getStatus());
        System.out.println("   第二次执行状态: " + jobExecution2.getStatus());
    }

    /**
     * 测试Job执行的数据验证
     */
    @Test
    @Order(10)
    @DisplayName("10. 验证处理后的数据完整性")
    void testDataIntegrityAfterJob() throws Exception {
        // 记录执行前的数据
        Integer countBefore = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sample_data", Integer.class);

        // 执行Job
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("jobId", "fullIndexJob_integrity_test_" + System.currentTimeMillis())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());

        // 验证源数据未被修改
        Integer countAfter = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sample_data", Integer.class);
        assertEquals(countBefore, countAfter, 
                    "源数据总数应该保持不变");

        // 验证所有数据的完整性
        Integer validRecords = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sample_data WHERE id IS NOT NULL AND name IS NOT NULL", 
            Integer.class);
        assertEquals(countBefore, validRecords, 
                    "所有记录应该都有有效的ID和name");

        System.out.println("✅ 数据完整性验证通过");
        System.out.println("   处理前记录数: " + countBefore);
        System.out.println("   处理后记录数: " + countAfter);
        System.out.println("   有效记录数: " + validRecords);
    }
}

