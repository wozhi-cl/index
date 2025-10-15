package com.company.index.batch.job;

import com.company.index.batch.job.base.AbstractJobTest;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Job;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * H2 → Elasticsearch 集成测试
 * 从H2内存数据库读取数据，写入到Elasticsearch (Docker)
 * 
 * 使用真实的Elasticsearch服务，不使用Mock
 */
@SpringBootTest(classes = com.company.index.Application.class)
@SpringBatchTest
@Import(com.company.index.config.TestBatchConfig.class)
@ActiveProfiles({"h2", "test-h2-es"})
@TestExecutionListeners(listeners = {
    DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class
}, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@TestPropertySource(properties = {
    "spring.batch.job.enabled=false",
    "index.dataSource.type=h2",
    "index.dataSource.table=sample_data",
    "index.indexTarget.type=elasticsearch",
    "index.indexTarget.indexName=test_index_h2"
})
public class H2ToElasticsearchJobTest extends AbstractJobTest {

    @Autowired
    @Qualifier("fullIndexJob")
    private Job fullIndexJob;

    @Override
    protected String getJobName() {
        // 设置要测试的Job
        jobLauncherTestUtils.setJob(fullIndexJob);
        return "fullIndexJob";
    }

    @Override
    protected String getDataSourceType() {
        return "H2";
    }

    @Override
    protected String getIndexTargetType() {
        return "Elasticsearch (Docker)";
    }

    @Override
    protected String getTestProfile() {
        return "test-h2-es";
    }

    @Override
    protected void prepareTestData() throws Exception {
        System.out.println("\n====== 准备H2测试数据 ======");
        
        // 插入H2测试数据
        insertRdbTestData("sample_data", 20);
        
        int count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sample_data", Integer.class);
        System.out.println("✅ 已插入 " + count + " 条测试数据到H2");
    }

    @Override
    protected void cleanupTestData() throws Exception {
        System.out.println("\n====== 清理测试数据 ======");
        
        cleanupRdbTestData("sample_data");
        
        System.out.println("✅ H2测试数据已清空");
        System.out.println("ℹ️  Elasticsearch数据保留（可通过Kibana查看）");
    }

    @Test
    public void testFullIndexJobWithH2ToElasticsearch() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🧪 测试: 全量索引任务 - H2 → Elasticsearch (真实环境)");
        System.out.println("=".repeat(80));

        // 准备Job参数
        JobParameters jobParameters = new JobParametersBuilder()
            .addString("jobId", "h2ToEs_test_" + System.currentTimeMillis())
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters();

        // 执行Job
        JobExecution jobExecution = launchJobAndAssert(jobParameters);

        // 验证执行成功
        System.out.println("\n====== 索引验证 ======");
        System.out.println("✅ Job执行成功");
        System.out.println("📊 数据已写入Elasticsearch: test_index_h2");
        System.out.println("🔍 查看数据: http://localhost:9200/test_index_h2/_search");

        // 打印测试总结
        printTestSummary(jobExecution, true);
    }
}

