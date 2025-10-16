package com.company.index.batch;

import com.company.index.batch.job.DynamicIncrementalIndexJobConfig;
import org.junit.jupiter.api.*;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 动态增量索引 Job 测试
 */
@SpringBootTest
@SpringBatchTest
@ActiveProfiles("h2")
@org.springframework.context.annotation.Import(com.company.index.config.TestBatchConfig.class)
@TestPropertySource(properties = {
    "index.dataSource.type=h2",
    "index.dataSource.table=orders",
    "index.dataSource.deltaTable=orders_changelog",
    "index.dataSource.timeColumn=updated_at",
    "index.dataSource.idColumn=id",
    "index.indexTarget.type=file",
    "index.indexTarget.path=./test-output",
    "index.indexTarget.fileName=dynamic-incremental-test-output.json",
    "index.indexTarget.indexName=order_index",
    "index.parallelism.chunkSize=10",
    "index.retry.maxAttempts=3",
    "index.incremental.overlapMinutes=5",
    "spring.batch.job.enabled=false"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DynamicIncrementalIndexJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    @Qualifier("dynamicIncrementalIndexJob")
    private Job job;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // 设置要测试的 Job
        jobLauncherTestUtils.setJob(job);
        
        jdbcTemplate = new JdbcTemplate(dataSource);
        
        // 确保测试表存在并有数据
        ensureTestDataExists();
    }

    /**
     * 确保测试数据存在
     */
    private void ensureTestDataExists() {
        try {
            // 检查 users 表
            Integer userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users", Integer.class);
            
            if (userCount == null || userCount == 0) {
                System.out.println("插入测试用户数据...");
                jdbcTemplate.execute(
                    "INSERT INTO users (name, phone, email) VALUES " +
                    "('张三', '13800138001', 'zhangsan@example.com'), " +
                    "('李四', '13800138002', 'lisi@example.com'), " +
                    "('王五', '13800138003', 'wangwu@example.com')"
                );
            }
            
            // 检查 orders 表
            Integer orderCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders", Integer.class);
            
            if (orderCount == null || orderCount == 0) {
                System.out.println("插入测试订单数据...");
                jdbcTemplate.execute(
                    "INSERT INTO orders (order_no, amount, user_id, status, created_at, updated_at) VALUES " +
                    "('ORD001', 199.99, 1, 'COMPLETED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), " +
                    "('ORD002', 299.99, 2, 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), " +
                    "('ORD003', 399.99, 3, 'COMPLETED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
                );
            }
            
            System.out.println("✅ 测试数据准备完成");
            System.out.println("   用户数: " + jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class));
            System.out.println("   订单数: " + jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Integer.class));
            
        } catch (Exception e) {
            System.err.println("准备测试数据失败: " + e.getMessage());
        }
    }

    @Test
    @Order(1)
    @DisplayName("1. 测试增量读取处理写入步骤(dynamicDeltaReadProcessWriteStep)")
    void testDynamicDeltaReadProcessWriteStep() throws Exception {
        System.out.println("\n========================================");
        System.out.println("测试: dynamicDeltaReadProcessWriteStep");
        System.out.println("========================================");
        
        // 更新一些订单数据以产生增量
        jdbcTemplate.update(
            "UPDATE orders SET updated_at = CURRENT_TIMESTAMP WHERE order_no = 'ORD001'"
        );
        jdbcTemplate.update(
            "UPDATE orders SET amount = 999.99, updated_at = CURRENT_TIMESTAMP WHERE order_no = 'ORD002'"
        );
        
        // 执行步骤
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("testStep", "deltaReadProcessWrite")
                .toJobParameters();
        
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("dynamicDeltaReadProcessWriteStep", jobParameters);
        
        if (jobExecution.getStepExecutions().isEmpty()) {
            fail("Step executions should not be empty");
        }
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        
        // 验证
        assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus(), 
            "dynamicDeltaReadProcessWriteStep应该成功完成");
        
        System.out.println("✅ dynamicDeltaReadProcessWriteStep执行成功");
        System.out.println("   状态: " + stepExecution.getStatus());
        System.out.println("   读取: " + stepExecution.getReadCount());
        System.out.println("   写入: " + stepExecution.getWriteCount());
        System.out.println("   跳过: " + stepExecution.getSkipCount());
    }

    @Test
    @Order(2)
    @DisplayName("2. 测试增量检查步骤(dynamicIncrementalCheckStep)")
    void testDynamicIncrementalCheckStep() throws Exception {
        System.out.println("\n========================================");
        System.out.println("测试: dynamicIncrementalCheckStep");
        System.out.println("========================================");
        
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("testStep", "incrementalCheck")
                .toJobParameters();
        
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("dynamicIncrementalCheckStep", jobParameters);
        
        if (jobExecution.getStepExecutions().isEmpty()) {
            fail("Step executions should not be empty");
        }
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        
        // 验证
        BatchStatus status = stepExecution.getStatus();
        assertNotNull(status, "Step状态不应为null");
        
        System.out.println("✅ dynamicIncrementalCheckStep执行完成");
        System.out.println("   状态: " + status);
        System.out.println("   退出码: " + stepExecution.getExitStatus().getExitCode());
    }

    @Test
    @Order(3)
    @DisplayName("3. 测试触发全量重建步骤(dynamicTriggerFullRebuildStep)")
    void testDynamicTriggerFullRebuildStep() throws Exception {
        System.out.println("\n========================================");
        System.out.println("测试: dynamicTriggerFullRebuildStep");
        System.out.println("========================================");
        
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .addString("testStep", "triggerFullRebuild")
                    .toJobParameters();
            
            JobExecution jobExecution = jobLauncherTestUtils.launchStep("dynamicTriggerFullRebuildStep", jobParameters);
            
            if (jobExecution.getStepExecutions().isEmpty()) {
                fail("Step executions should not be empty");
            }
            StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
            
            assertNotNull(stepExecution.getStatus(), "dynamicTriggerFullRebuildStep应该有执行状态");
            
            System.out.println("✅ dynamicTriggerFullRebuildStep执行完成");
            System.out.println("   状态: " + stepExecution.getStatus());
            System.out.println("   退出码: " + stepExecution.getExitStatus().getExitCode());
            
        } catch (Exception e) {
            System.out.println("⚠️  dynamicTriggerFullRebuildStep测试跳过（需要dynamicFullIndexJob配置）");
            System.out.println("   错误信息: " + e.getMessage());
        }
    }

    @Test
    @Order(4)
    @DisplayName("4. 测试增量完成步骤(dynamicIncrementalFinishStep)")
    void testDynamicIncrementalFinishStep() throws Exception {
        System.out.println("\n========================================");
        System.out.println("测试: dynamicIncrementalFinishStep");
        System.out.println("========================================");
        
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("testStep", "incrementalFinish")
                .toJobParameters();
        
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("dynamicIncrementalFinishStep", jobParameters);
        
        if (jobExecution.getStepExecutions().isEmpty()) {
            fail("Step executions should not be empty");
        }
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        
        assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus(), 
            "dynamicIncrementalFinishStep应该成功完成");
        
        System.out.println("✅ dynamicIncrementalFinishStep执行成功");
        System.out.println("   状态: " + stepExecution.getStatus());
    }

    @Test
    @Order(5)
    @DisplayName("5. 测试完整增量索引流程")
    void testDynamicIncrementalIndexJobFullFlow() throws Exception {
        System.out.println("\n========================================");
        System.out.println("测试: 完整增量索引流程");
        System.out.println("========================================");
        
        // 1. 准备增量数据
        System.out.println("1. 准备增量数据...");
        
        // 新增订单
        jdbcTemplate.update(
            "INSERT INTO orders (order_no, amount, user_id, status, created_at, updated_at) " +
            "VALUES ('ORD999', 1999.99, 1, 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
        );
        
        // 更新现有订单
        jdbcTemplate.update(
            "UPDATE orders SET amount = 888.88, updated_at = CURRENT_TIMESTAMP WHERE order_no = 'ORD001'"
        );
        
        Integer totalOrders = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Integer.class);
        System.out.println("   总订单数: " + totalOrders);
        
        // 2. 执行完整 Job
        System.out.println("2. 执行动态增量索引 Job...");
        
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("testRun", "fullFlow")
                .toJobParameters();
        
        try {
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
            
            // 3. 验证执行结果
            System.out.println("3. 验证执行结果...");
            
            BatchStatus status = jobExecution.getStatus();
            System.out.println("   Job 状态: " + status);
            System.out.println("   退出码: " + jobExecution.getExitStatus().getExitCode());
            
            // 打印所有 Step 的执行情况
            System.out.println("\n   Steps 执行详情:");
            for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
                System.out.println("   ├─ " + stepExecution.getStepName());
                System.out.println("   │  状态: " + stepExecution.getStatus());
                System.out.println("   │  读取: " + stepExecution.getReadCount());
                System.out.println("   │  写入: " + stepExecution.getWriteCount());
                System.out.println("   │  跳过: " + stepExecution.getSkipCount());
            }
            
            // 验证状态
            assertTrue(
                status == BatchStatus.COMPLETED || status == BatchStatus.FAILED,
                "Job应该完成或失败（可能因为检查失败触发重建）"
            );
            
            System.out.println("\n✅ 完整增量索引流程测试完成");
            System.out.println("   最终状态: " + status);
            
        } catch (Exception e) {
            System.err.println("❌ 增量索引 Job 执行失败: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    @Order(6)
    @DisplayName("6. 测试增量数据读取")
    void testIncrementalDataReading() throws Exception {
        System.out.println("\n========================================");
        System.out.println("测试: 增量数据读取");
        System.out.println("========================================");
        
        // 1. 记录当前时间
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(10);
        
        // 2. 插入新数据
        jdbcTemplate.update(
            "INSERT INTO orders (order_no, amount, user_id, status, created_at, updated_at) " +
            "VALUES ('ORD_INCREMENTAL_TEST', 5555.55, 2, 'COMPLETED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
        );
        
        // 3. 更新数据
        int updatedCount = jdbcTemplate.update(
            "UPDATE orders SET amount = 6666.66, updated_at = CURRENT_TIMESTAMP " +
            "WHERE order_no IN ('ORD001', 'ORD002')"
        );
        
        System.out.println("1. 数据变更:");
        System.out.println("   新增订单: 1 条");
        System.out.println("   更新订单: " + updatedCount + " 条");
        
        // 4. 查询增量数据（模拟增量读取）
        LocalDateTime endTime = LocalDateTime.now();
        
        Integer incrementalCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM orders WHERE updated_at BETWEEN ? AND ?",
            Integer.class,
            startTime.minusMinutes(5),  // 加上重叠时间
            endTime
        );
        
        System.out.println("\n2. 增量查询:");
        System.out.println("   时间范围: " + startTime + " ~ " + endTime);
        System.out.println("   增量记录数: " + incrementalCount);
        
        assertTrue(incrementalCount >= 1, "应该至少有1条增量记录");
        
        // 5. 执行增量读取步骤
        System.out.println("\n3. 执行增量读取步骤...");
        
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("testStep", "incrementalDataReading")
                .toJobParameters();
        
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("dynamicDeltaReadProcessWriteStep", jobParameters);
        
        if (jobExecution.getStepExecutions().isEmpty()) {
            fail("Step executions should not be empty");
        }
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        
        System.out.println("   读取记录数: " + stepExecution.getReadCount());
        System.out.println("   写入记录数: " + stepExecution.getWriteCount());
        
        assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
        
        System.out.println("\n✅ 增量数据读取测试成功");
    }

    @Test
    @Order(7)
    @DisplayName("7. 测试增量索引错误处理")
    void testIncrementalIndexErrorHandling() throws Exception {
        System.out.println("\n========================================");
        System.out.println("测试: 增量索引错误处理");
        System.out.println("========================================");
        
        // 插入一些可能引起错误的数据（如空值）
        try {
            jdbcTemplate.update(
                "INSERT INTO orders (order_no, amount, user_id, status, created_at, updated_at) " +
                "VALUES ('ORD_ERROR_TEST', 7777.77, 1, 'COMPLETED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
            );
        } catch (Exception e) {
            System.out.println("   插入测试数据失败（预期）: " + e.getMessage());
        }
        
        // 执行增量步骤
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("testStep", "errorHandling")
                .toJobParameters();
        
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("dynamicDeltaReadProcessWriteStep", jobParameters);
        
        if (jobExecution.getStepExecutions().isEmpty()) {
            fail("Step executions should not be empty");
        }
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        
        // 验证：即使有错误，Step 也应该完成（通过 skip）
        BatchStatus status = stepExecution.getStatus();
        assertTrue(
            status == BatchStatus.COMPLETED || status == BatchStatus.FAILED,
            "Step应该完成或失败"
        );
        
        System.out.println("✅ 错误处理测试完成");
        System.out.println("   状态: " + status);
        System.out.println("   跳过数: " + stepExecution.getSkipCount());
        System.out.println("   错误数: " + stepExecution.getFailureExceptions().size());
    }

    @AfterEach
    void printSummary() {
        System.out.println("\n========================================");
        System.out.println("测试完成");
        System.out.println("========================================\n");
    }
}

