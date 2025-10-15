package com.company.index.batch.job.base;

import com.company.index.Application;
import com.company.index.config.TestBatchConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Job测试基类
 * 提供通用的测试方法和工具
 * 
 * 注意：子类需要添加以下注解：
 * - @SpringBootTest(classes = Application.class)
 * - @SpringBatchTest
 * - @Import(TestBatchConfig.class)
 * - @ActiveProfiles({...})
 */
public abstract class AbstractJobTest {

    @Autowired
    protected JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    /**
     * 获取要测试的Job名称（子类覆盖）
     * 例如: "fullIndexJob" 或 "incrementalIndexJob"
     */
    protected abstract String getJobName();

    /**
     * 获取数据源类型（子类覆盖）
     */
    protected abstract String getDataSourceType();

    /**
     * 获取索引目标类型（子类覆盖）
     */
    protected abstract String getIndexTargetType();

    /**
     * 获取测试Profile（子类覆盖）
     */
    protected abstract String getTestProfile();

    /**
     * 准备测试数据（子类实现）
     */
    protected abstract void prepareTestData() throws Exception;

    /**
     * 清理测试数据（子类实现）
     */
    protected abstract void cleanupTestData() throws Exception;

    @BeforeEach
    public void setUp() throws Exception {
        // 首先设置要测试的Job（必须在其他操作之前）
        String jobName = getJobName();
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🧪 测试Job: " + jobName);
        System.out.println("🧪 测试组合: " + getDataSourceType() + " → " + getIndexTargetType());
        System.out.println("📋 测试Profile: " + getTestProfile());
        System.out.println("=".repeat(60));
        
        prepareTestData();
    }

    @AfterEach
    public void tearDown() throws Exception {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🧹 清理测试数据");
        System.out.println("=".repeat(60));
        
        cleanupTestData();
    }

    /**
     * 执行Job并验证结果
     */
    protected JobExecution launchJobAndAssert(JobParameters jobParameters) throws Exception {
        System.out.println("\n====== 准备执行Job ======");
        System.out.println("Job参数: " + jobParameters);
        
        // 执行Job
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        
        System.out.println("\n====== Job执行返回 ======");
        System.out.println("JobExecution ID: " + jobExecution.getId());
        System.out.println("Job名称: " + jobExecution.getJobInstance().getJobName());
        System.out.println("Job状态: " + jobExecution.getStatus());
        System.out.println("退出状态: " + jobExecution.getExitStatus());
        
        // 计算执行时间
        if (jobExecution.getEndTime() != null && jobExecution.getStartTime() != null) {
            Duration duration = Duration.between(jobExecution.getStartTime(), jobExecution.getEndTime());
            System.out.println("执行耗时: " + duration.toMillis() + "ms");
        }
        
        // 打印Step详情
        System.out.println("\n====== Job执行详情 ======");
        jobExecution.getStepExecutions().forEach(step -> {
            System.out.println("\nStep名称: " + step.getStepName());
            System.out.println("  状态: " + step.getStatus());
            System.out.println("  读取数: " + step.getReadCount());
            System.out.println("  写入数: " + step.getWriteCount());
            System.out.println("  跳过数: " + step.getSkipCount());
            System.out.println("  提交数: " + step.getCommitCount());
            if (step.getEndTime() != null && step.getStartTime() != null) {
                Duration stepDuration = Duration.between(step.getStartTime(), step.getEndTime());
                System.out.println("  耗时: " + stepDuration.toMillis() + "ms");
            }
        });
        
        // 打印错误信息（如果有）
        if (jobExecution.getStatus() != BatchStatus.COMPLETED) {
            System.err.println("\n⚠️  警告：Job执行失败！");
            System.err.println("Job状态: " + jobExecution.getStatus());
            System.err.println("退出消息: " + jobExecution.getExitStatus().getExitDescription());
            
            for (Throwable e : jobExecution.getAllFailureExceptions()) {
                System.err.println("异常: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // 断言Job完成
        assertTrue(
            jobExecution.getStatus() == BatchStatus.COMPLETED || 
            jobExecution.getStatus() == BatchStatus.STOPPED,
            "Job应该成功完成或正常停止，实际状态: " + jobExecution.getStatus() + 
            ", 退出消息: " + jobExecution.getExitStatus().getExitDescription()
        );
        
        return jobExecution;
    }

    /**
     * 在关系型数据库中插入测试数据
     */
    protected void insertRdbTestData(String tableName, int count) {
        System.out.println("\n====== @BeforeEach 开始 ==========");
        System.out.println("准备插入测试数据到表: " + tableName);
        
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 1; i <= count; i++) {
            jdbcTemplate.update(
                "INSERT INTO " + tableName + " (id, name, description, status, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?)",
                i,
                "测试数据_" + i,
                "这是第" + i + "条测试数据",
                i % 2 == 0 ? "active" : "inactive",
                now.minusDays(i),
                now.minusHours(i)
            );
        }
        
        System.out.println("✅ 测试数据初始化完成，共插入 " + count + " 条记录");
        
        // 验证插入
        Integer actualCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + tableName, 
            Integer.class
        );
        System.out.println("数据库实际记录数: " + actualCount);
    }

    /**
     * 清理关系型数据库测试数据
     */
    protected void cleanupRdbTestData(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + tableName, 
            Integer.class
        );
        System.out.println("清理前记录数: " + count);
        
        jdbcTemplate.execute("DELETE FROM " + tableName);
        
        System.out.println("✅ 测试数据已清空");
    }

    /**
     * 验证索引结果（通用方法）
     */
    protected void verifyIndexResult(int expectedRecordCount) {
        // 子类可以覆盖此方法来实现具体的验证逻辑
        System.out.println("\n====== 验证索引结果 ======");
        System.out.println("预期记录数: " + expectedRecordCount);
    }

    /**
     * 打印测试总结
     */
    protected void printTestSummary(JobExecution jobExecution, boolean success) {
        System.out.println("\n" + "=".repeat(60));
        if (success) {
            System.out.println("✅ 测试通过！");
        } else {
            System.out.println("❌ 测试失败！");
        }
        System.out.println("数据源: " + getDataSourceType());
        System.out.println("索引目标: " + getIndexTargetType());
        System.out.println("Job状态: " + jobExecution.getStatus());
        System.out.println("=".repeat(60) + "\n");
    }
}

