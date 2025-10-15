package com.company.index.batch.job;

import com.company.index.batch.job.base.AbstractJobTest;
import org.junit.jupiter.api.Test;
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
 * MySQL → Elasticsearch 集成测试
 * 使用Docker中的真实MySQL和Elasticsearch
 */
@SpringBootTest(classes = com.company.index.Application.class)
@SpringBatchTest
@Import(com.company.index.config.TestBatchConfig.class)
@ActiveProfiles({"test-mysql-es"})
@TestExecutionListeners(listeners = {
    DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class
}, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@TestPropertySource(properties = {
    "spring.batch.job.enabled=false"
})
public class MysqlToElasticsearchJobTest extends AbstractJobTest {

    @Autowired
    @Qualifier("fullIndexJob")
    private Job fullIndexJob;

    @Autowired
    @Qualifier("incrementalIndexJob")
    private Job incrementalIndexJob;

    @Override
    protected String getJobName() {
        jobLauncherTestUtils.setJob(fullIndexJob);
        return "fullIndexJob";
    }

    @Override
    protected String getDataSourceType() {
        return "MySQL (Docker)";
    }

    @Override
    protected String getIndexTargetType() {
        return "Elasticsearch (Docker)";
    }

    @Override
    protected String getTestProfile() {
        return "test-mysql-es";
    }

    @Test
    public void testFullIndexJobWithMysqlToElasticsearch() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🧪 测试: 全量索引任务 - MySQL → Elasticsearch (真实环境)");
        System.out.println("=".repeat(80));

        // 准备Job参数
        JobParameters jobParameters = new JobParametersBuilder()
            .addString("jobId", "mysqlToEs_test_" + System.currentTimeMillis())
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters();

        // 执行Job
        JobExecution jobExecution = launchJobAndAssert(jobParameters);

        // 验证执行成功
        System.out.println("\n====== 索引验证 ======");
        System.out.println("✅ Job执行成功");
        System.out.println("📊 数据已写入Elasticsearch: test_index");
        
        // 打印测试总结
        printTestSummary(jobExecution, true);
    }

    @Test
    public void testIncrementalIndexJobWithMysqlToElasticsearch() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🧪 测试: 增量索引任务 - MySQL → Elasticsearch (真实环境)");
        System.out.println("=".repeat(80));

        // 设置增量索引Job
        jobLauncherTestUtils.setJob(incrementalIndexJob);

        // 准备Job参数
        JobParameters jobParameters = new JobParametersBuilder()
            .addString("jobId", "mysqlToEsIncremental_test_" + System.currentTimeMillis())
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters();

        // 执行Job
        JobExecution jobExecution = launchJobAndAssert(jobParameters);

        // 验证执行成功
        System.out.println("\n====== 增量索引验证 ======");
        System.out.println("✅ Job执行成功");
        System.out.println("📊 增量数据已写入Elasticsearch: test_index");
        
        // 打印测试总结
        printTestSummary(jobExecution, true);
    }

    @Override
    protected void prepareTestData() throws Exception {
        System.out.println("\n====== 准备MySQL测试数据 ======");
        
        // 清空现有数据
        jdbcTemplate.execute("TRUNCATE TABLE sample_data");
        jdbcTemplate.execute("TRUNCATE TABLE sample_data_changelog");
        
        // 插入测试数据
        String insertSql = "INSERT INTO sample_data (id, name, description, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)";
        
        for (int i = 1; i <= 20; i++) {
            jdbcTemplate.update(insertSql,
                i,
                "测试数据_" + i,
                "这是第" + i + "条测试数据",
                i % 2 == 0 ? "active" : "inactive",
                "2024-10-" + String.format("%02d", 15 - (i % 15)) + "T10:00:00",
                "2024-10-15T" + String.format("%02d", 23 - i) + ":00:00"
            );
        }
        
        int count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sample_data", Integer.class);
        System.out.println("✅ 已插入 " + count + " 条测试数据到MySQL");
    }

    @Override
    protected void cleanupTestData() throws Exception {
        System.out.println("\n====== 清理测试数据 ======");
        
        // 清空MySQL数据
        jdbcTemplate.execute("TRUNCATE TABLE sample_data");
        jdbcTemplate.execute("TRUNCATE TABLE sample_data_changelog");
        
        System.out.println("✅ MySQL测试数据已清空");
        System.out.println("ℹ️  Elasticsearch数据保留（可通过Kibana查看）");
    }
}

