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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MySQL → File 集成测试
 * 使用Docker中的真实MySQL
 */
@SpringBootTest(classes = com.company.index.Application.class)
@SpringBatchTest
@Import(com.company.index.config.TestBatchConfig.class)
@ActiveProfiles({"test-mysql-file"})
@TestExecutionListeners(listeners = {
    DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class
}, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@TestPropertySource(properties = {
    "spring.batch.job.enabled=false",
    "index.retry.maxAttempts=3"
})
public class MysqlToFileJobTest extends AbstractJobTest {

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
        return "MySQL (Docker)";
    }

    @Override
    protected String getIndexTargetType() {
        return "File";
    }

    @Override
    protected String getTestProfile() {
        return "test-mysql-file";
    }

    @Test
    public void testFullIndexJobWithMysqlToFile() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🧪 测试: 全量索引任务 - MySQL → File");
        System.out.println("=".repeat(80));

        // 准备Job参数
        JobParameters jobParameters = new JobParametersBuilder()
            .addString("jobId", "mysqlToFile_test_" + System.currentTimeMillis())
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters();

        // 执行Job
        JobExecution jobExecution = launchJobAndAssert(jobParameters);

        // 验证输出文件（文件名会带时间戳）
        File outputDir = new File("./target/test-output");
        assertTrue(outputDir.exists(), "输出目录应该存在");
        
        // 查找生成的文件（带时间戳）
        File[] outputFiles = outputDir.listFiles((dir, name) -> 
            name.startsWith("mysql-to-file-output-") && name.endsWith(".json"));
        assertNotNull(outputFiles, "应该能列出输出文件");
        assertTrue(outputFiles.length > 0, "应该至少有一个输出文件");
        
        // 取最新的文件
        File outputFile = outputFiles[outputFiles.length - 1];
        
        // 读取并验证输出内容
        String content = Files.readString(Paths.get(outputFile.getAbsolutePath()));
        assertFalse(content.isEmpty(), "输出文件不应为空");
        
        System.out.println("\n====== 输出文件验证 ======");
        System.out.println("输出目录: " + outputDir.getAbsolutePath());
        System.out.println("文件数量: " + outputFiles.length);
        System.out.println("文件名: " + outputFile.getName());
        System.out.println("文件大小: " + outputFile.length() + " 字节");
        System.out.println("内容预览: " + content.substring(0, Math.min(200, content.length())) + "...");

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
    }
}

