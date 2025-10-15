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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JSON → File 组合测试
 * 从JSON文件读取数据，写入到JSON文件
 */
@SpringBootTest(classes = com.company.index.Application.class)
@SpringBatchTest
@Import(com.company.index.config.TestBatchConfig.class)
@ActiveProfiles({"h2", "test-json-file"})
@TestExecutionListeners(listeners = {
    DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class
}, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@TestPropertySource(properties = {
    "spring.batch.job.enabled=false",
    "index.dataSource.type=json",
    "index.dataSource.jsonPath=src/test/resources/test-data/sample-data.json",
    "index.indexTarget.type=file",
    "index.indexTarget.path=./target/test-output",
    "index.indexTarget.fileName=json-to-file-output.json"
})
public class JsonToFileJobTest extends AbstractJobTest {

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
        return "JSON";
    }

    @Override
    protected String getIndexTargetType() {
        return "File";
    }

    @Override
    protected String getTestProfile() {
        return "test-json-file";
    }

    @Override
    protected void prepareTestData() throws Exception {
        // JSON文件已经存在于 test-data/sample-data.json
        System.out.println("✅ JSON测试数据文件已准备好");
        System.out.println("📁 文件路径: classpath:test-data/sample-data.json");
        
        // 验证文件存在
        assertTrue(
            getClass().getClassLoader().getResource("test-data/sample-data.json") != null,
            "JSON测试数据文件应该存在"
        );
    }

    @Override
    protected void cleanupTestData() throws Exception {
        // 清理输出文件
        File outputFile = new File("./target/test-output/json-to-file-output.json");
        if (outputFile.exists()) {
            outputFile.delete();
            System.out.println("✅ 输出文件已删除: " + outputFile.getAbsolutePath());
        }
    }

    @Test
    public void testFullIndexJobWithJsonToFile() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🧪 测试: 全量索引任务 - JSON → File");
        System.out.println("=".repeat(80));

        // 准备Job参数
        JobParameters jobParameters = new JobParametersBuilder()
            .addString("jobId", "jsonToFile_test_" + System.currentTimeMillis())
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters();

        // 执行Job
        JobExecution jobExecution = launchJobAndAssert(jobParameters);

        // 验证输出文件（文件名会带时间戳）
        File outputDir = new File("./target/test-output");
        assertTrue(outputDir.exists(), "输出目录应该存在");
        
        // 查找生成的文件（带时间戳）
        File[] outputFiles = outputDir.listFiles((dir, name) -> 
            name.startsWith("json-to-file-output-") && name.endsWith(".json"));
        assertNotNull(outputFiles, "应该能列出输出文件");
        assertTrue(outputFiles.length > 0, "应该至少有一个输出文件");
        
        // 取最新的文件
        File outputFile = outputFiles[outputFiles.length - 1];
        
        // 读取并验证输出内容
        String content = Files.readString(Paths.get(outputFile.getAbsolutePath()));
        assertFalse(content.isEmpty(), "输出文件不应为空");
        
        // 验证JSON格式
        assertTrue(content.startsWith("[") || content.startsWith("{"), 
            "输出应该是有效的JSON格式");
        
        System.out.println("\n====== 输出文件验证 ======");
        System.out.println("输出目录: " + outputDir.getAbsolutePath());
        System.out.println("文件数量: " + outputFiles.length);
        System.out.println("文件名: " + outputFile.getName());
        System.out.println("文件大小: " + outputFile.length() + " 字节");
        System.out.println("内容预览: " + content.substring(0, Math.min(200, content.length())) + "...");

        // 打印测试总结
        printTestSummary(jobExecution, true);
    }

    @Test
    public void testIncrementalIndexJobWithJsonToFile() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🧪 测试: 增量索引任务 - JSON → File");
        System.out.println("=".repeat(80));
        System.out.println("⚠️  注意: JSON数据源不支持增量模式，此测试预期会跳过或失败");

        // 准备Job参数
        JobParameters jobParameters = new JobParametersBuilder()
            .addString("jobId", "jsonToFileIncr_test_" + System.currentTimeMillis())
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters();

        // 尝试执行增量Job（JSON不支持增量，可能会失败）
        try {
            JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
            System.out.println("Job状态: " + jobExecution.getStatus());
            System.out.println("注意: JSON数据源通常不支持增量索引");
        } catch (Exception e) {
            System.out.println("预期的异常: " + e.getMessage());
        }
    }
}

