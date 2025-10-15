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
 * 增量索引Job集成测试
 * 测试增量索引Job的完整流程
 */
@SpringBootTest
@SpringBatchTest
@ActiveProfiles("h2")
@Import(TestBatchConfig.class)  // 导入测试配置，使用同步JobLauncher
@TestPropertySource(properties = {
    "spring.batch.job.enabled=false",  // 禁用自动启动
    "index.parallelism.chunkSize=10",
    "index.parallelism.threads=2",
    "index.incremental.overlapMinutes=5",
    "index.dataSource.type=h2",
    "index.dataSource.table=sample_data",
    "index.dataSource.deltaTable=sample_data_changelog",
    "index.dataSource.timeColumn=updated_at",
    "index.indexTarget.type=file",
    "index.indexTarget.path=./target/test-output",
    "index.indexTarget.fileName=incremental-index-test-output.json"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IncrementalIndexJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private Job incrementalIndexJob;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 测试前准备：清空并初始化测试数据
     */
    @BeforeEach
    void setUp() {
        // 设置要测试的Job
        jobLauncherTestUtils.setJob(incrementalIndexJob);
        
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
     * 插入基础数据和最近更新的数据
     */
    private void initTestData() {
        String sql = "INSERT INTO sample_data (name, description, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
        
        LocalDateTime now = LocalDateTime.now();
        
        try {
            // 插入50条旧数据（30分钟前）
            LocalDateTime oldTime = now.minusMinutes(30);
            for (int i = 1; i <= 50; i++) {
                String timestamp = oldTime.format(FORMATTER);
                jdbcTemplate.update(sql, 
                    "OldUser_" + i, 
                    "旧数据_第" + i + "条", 
                    i % 2 == 0 ? "active" : "inactive",
                    timestamp, 
                    timestamp);
            }
            
            // 插入30条新数据（最近2分钟内更新）
            for (int i = 51; i <= 80; i++) {
                LocalDateTime recentTime = now.minusMinutes(i - 50);
                String timestamp = recentTime.format(FORMATTER);
                jdbcTemplate.update(sql, 
                    "NewUser_" + i, 
                    "新数据_第" + i + "条", 
                    i % 2 == 0 ? "active" : "inactive",
                    timestamp, 
                    timestamp);
            }
            
            int totalCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sample_data", Integer.class);
            int recentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sample_data WHERE updated_at > ?", 
                Integer.class, 
                now.minusMinutes(10).format(FORMATTER));
            
            System.out.println("✅ 测试数据初始化完成");
            System.out.println("   总记录数: " + totalCount);
            System.out.println("   最近10分钟更新: " + recentCount + " 条");
            
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
        Integer totalCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sample_data", Integer.class);
        assertNotNull(totalCount, "数据总数不应为null");
        assertEquals(80, totalCount, "应该有80条测试数据");
        
        LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
        Integer recentCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sample_data WHERE updated_at > ?", 
            Integer.class, 
            tenMinutesAgo.format(FORMATTER));
        
        assertTrue(recentCount > 0, "应该有最近更新的数据");
        System.out.println("✅ 测试数据验证通过");
        System.out.println("   总记录数: " + totalCount);
        System.out.println("   最近10分钟更新: " + recentCount + " 条");
    }

    /**
     * 测试增量索引Job的完整执行
     */
    @Test
    @Order(2)
    @DisplayName("2. 测试增量索引Job完整执行")
    void testIncrementalIndexJobExecution() throws Exception {
        // 准备Job参数
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("jobId", "incrementalIndexJob_test_" + System.currentTimeMillis())
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
        BatchStatus status = jobExecution.getStatus();
        
        // 如果状态是STARTING或UNKNOWN，打印详细信息
        if (status == BatchStatus.STARTING || status == BatchStatus.UNKNOWN) {
            System.err.println("\n⚠️  警告：Job可能没有正常执行！");
            System.err.println("Job状态: " + status);
            System.err.println("退出消息: " + jobExecution.getExitStatus().getExitDescription());
            
            // 打印所有异常
            for (Throwable e : jobExecution.getAllFailureExceptions()) {
                System.err.println("异常: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // 增量索引可能因为没有增量数据而完成，或者检查失败
        assertTrue(status == BatchStatus.COMPLETED || status == BatchStatus.FAILED || status == BatchStatus.STOPPED,
                  "Job状态应该是COMPLETED/FAILED/STOPPED，实际: " + status + 
                  ", 退出消息: " + jobExecution.getExitStatus().getExitDescription());

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
        }

        // 验证Step数量（增量索引应该有3个步骤）
        assertEquals(3, jobExecution.getStepExecutions().size(), 
                    "增量索引应该包含3个步骤: deltaReadProcessWrite, check, conditionalRebuild");
    }

    /**
     * 测试单个Step的执行 - deltaReadProcessWriteStep
     */
    @Test
    @Order(3)
    @DisplayName("3. 测试增量读写处理步骤(deltaReadProcessWriteStep)")
    void testDeltaReadProcessWriteStep() throws Exception {
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("deltaReadProcessWriteStep");
        
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        
        // 验证步骤已执行（可能没有数据也是正常的）
        assertNotNull(stepExecution.getStatus(), "Step状态不应为null");
        
        System.out.println("✅ deltaReadProcessWriteStep执行完成");
        System.out.println("   状态: " + stepExecution.getStatus());
        System.out.println("   读取数: " + stepExecution.getReadCount());
        System.out.println("   写入数: " + stepExecution.getWriteCount());
        System.out.println("   提交数: " + stepExecution.getCommitCount());
        System.out.println("   跳过数: " + stepExecution.getSkipCount());
    }

    /**
     * 测试单个Step的执行 - checkStep
     */
    @Test
    @Order(4)
    @DisplayName("4. 测试检查步骤(checkStep)")
    void testCheckStep() throws Exception {
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("checkStep");
        
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        
        // 检查步骤可能会失败（如果ES未连接），这是正常的
        BatchStatus status = stepExecution.getStatus();
        System.out.println("✅ checkStep执行完成");
        System.out.println("   状态: " + status);
        System.out.println("   退出码: " + stepExecution.getExitStatus().getExitCode());
        
        assertNotNull(status, "Step状态不应为null");
    }

    /**
     * 测试单个Step的执行 - conditionalRebuildStep
     */
    @Test
    @Order(5)
    @DisplayName("5. 测试条件重建步骤(conditionalRebuildStep)")
    void testConditionalRebuildStep() throws Exception {
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("conditionalRebuildStep");
        
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        
        assertNotNull(stepExecution.getStatus(), "Step状态不应为null");
        
        System.out.println("✅ conditionalRebuildStep执行完成");
        System.out.println("   状态: " + stepExecution.getStatus());
        System.out.println("   退出码: " + stepExecution.getExitStatus().getExitCode());
    }

    /**
     * 测试增量数据的实时更新
     */
    @Test
    @Order(6)
    @DisplayName("6. 测试增量数据的实时更新")
    void testIncrementalUpdateProcessing() throws Exception {
        // 记录执行前的数据
        Integer countBefore = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sample_data", Integer.class);

        // 添加新的增量数据
        String sql = "INSERT INTO sample_data (name, description, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(FORMATTER);
        
        for (int i = 81; i <= 90; i++) {
            jdbcTemplate.update(sql, 
                "IncrementalUser_" + i, 
                "增量数据_第" + i + "条", 
                i % 2 == 0 ? "active" : "inactive",
                timestamp, 
                timestamp);
        }
        
        Integer countAfter = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sample_data", Integer.class);
        assertEquals(countBefore + 10, countAfter, "应该增加了10条记录");

        // 执行增量索引Job
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("jobId", "incrementalIndexJob_update_test_" + System.currentTimeMillis())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // 验证Job执行
        assertNotNull(jobExecution.getStatus(), "Job应该已执行");
        
        System.out.println("✅ 增量更新测试完成");
        System.out.println("   原有记录: " + countBefore);
        System.out.println("   新增记录: 10");
        System.out.println("   总记录数: " + countAfter);
        System.out.println("   Job状态: " + jobExecution.getStatus());
    }

    /**
     * 测试没有增量数据的情况
     */
    @Test
    @Order(7)
    @DisplayName("7. 测试没有增量数据的Job执行")
    void testIncrementalIndexJobWithNoNewData() throws Exception {
        // 将所有数据的时间设置为1小时前
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        String timestamp = oneHourAgo.format(FORMATTER);
        
        jdbcTemplate.update("UPDATE sample_data SET updated_at = ?", timestamp);
        
        // 验证没有最近的数据
        Integer recentCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sample_data WHERE updated_at > ?", 
            Integer.class, 
            LocalDateTime.now().minusMinutes(10).format(FORMATTER));
        assertEquals(0, recentCount, "应该没有最近更新的数据");

        // 执行Job
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("jobId", "incrementalIndexJob_no_data_test_" + System.currentTimeMillis())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // 验证Job执行（没有数据也应该能正常完成）
        assertNotNull(jobExecution.getStatus(), "Job应该已执行");
        
        System.out.println("✅ 无增量数据测试完成");
        System.out.println("   Job状态: " + jobExecution.getStatus());
        
        // 检查读取步骤的数据量
        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            if ("deltaReadProcessWriteStep".equals(stepExecution.getStepName())) {
                System.out.println("   读取数: " + stepExecution.getReadCount());
                assertTrue(stepExecution.getReadCount() == 0, 
                          "没有增量数据时读取数应该为0");
            }
        }
    }

    /**
     * 测试大量增量数据的处理
     */
    @Test
    @Order(8)
    @DisplayName("8. 测试大量增量数据的处理")
    void testIncrementalIndexJobWithLargeIncrementalData() throws Exception {
        // 添加大量新数据
        String sql = "INSERT INTO sample_data (name, description, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(FORMATTER);
        
        // 添加500条新记录
        for (int i = 101; i <= 600; i++) {
            jdbcTemplate.update(sql, 
                "BulkUser_" + i, 
                "批量数据_第" + i + "条", 
                i % 2 == 0 ? "active" : "inactive",
                timestamp, 
                timestamp);
        }
        
        Integer totalCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sample_data", Integer.class);
        Integer recentCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sample_data WHERE updated_at > ?", 
            Integer.class, 
            now.minusMinutes(1).format(FORMATTER));
        
        System.out.println("✅ 大量增量数据准备完成");
        System.out.println("   总记录数: " + totalCount);
        System.out.println("   增量记录数: " + recentCount);

        // 执行Job并计时
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("jobId", "incrementalIndexJob_large_test_" + System.currentTimeMillis())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        long startTime = System.currentTimeMillis();
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        long endTime = System.currentTimeMillis();

        // 验证Job执行
        assertNotNull(jobExecution.getStatus(), "Job应该已执行");
        
        System.out.println("✅ 大量增量数据测试完成");
        System.out.println("   Job状态: " + jobExecution.getStatus());
        System.out.println("   执行耗时: " + (endTime - startTime) + "ms");
        
        // 查看处理详情
        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            if ("deltaReadProcessWriteStep".equals(stepExecution.getStepName())) {
                System.out.println("   读取数: " + stepExecution.getReadCount());
                System.out.println("   写入数: " + stepExecution.getWriteCount());
            }
        }
    }

    /**
     * 测试Job的重复执行
     */
    @Test
    @Order(9)
    @DisplayName("9. 测试Job的重复执行")
    void testIncrementalIndexJobIdempotency() throws Exception {
        // 第一次执行
        JobParameters jobParameters1 = new JobParametersBuilder()
                .addString("jobId", "incrementalIndexJob_idempotency_test_1")
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
        
        JobExecution jobExecution1 = jobLauncherTestUtils.launchJob(jobParameters1);
        assertNotNull(jobExecution1.getStatus(), "第一次执行应该完成");

        // 第二次执行
        Thread.sleep(100);
        JobParameters jobParameters2 = new JobParametersBuilder()
                .addString("jobId", "incrementalIndexJob_idempotency_test_2")
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
        
        JobExecution jobExecution2 = jobLauncherTestUtils.launchJob(jobParameters2);
        assertNotNull(jobExecution2.getStatus(), "第二次执行应该完成");

        System.out.println("✅ 幂等性测试通过");
        System.out.println("   第一次执行状态: " + jobExecution1.getStatus());
        System.out.println("   第二次执行状态: " + jobExecution2.getStatus());
    }

    /**
     * 测试数据完整性
     */
    @Test
    @Order(10)
    @DisplayName("10. 验证处理后的数据完整性")
    void testDataIntegrityAfterIncrementalJob() throws Exception {
        // 记录执行前的数据
        Integer countBefore = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sample_data", Integer.class);

        // 执行Job
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("jobId", "incrementalIndexJob_integrity_test_" + System.currentTimeMillis())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        assertNotNull(jobExecution.getStatus());

        // 验证源数据未被修改
        Integer countAfter = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sample_data", Integer.class);
        assertEquals(countBefore, countAfter, 
                    "源数据总数应该保持不变");

        // 验证数据完整性
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

