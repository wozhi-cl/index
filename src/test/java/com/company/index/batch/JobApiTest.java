package com.company.index.batch;

import com.company.index.Application;
import com.company.index.common.TestDataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Job API 测试类
 * 测试 Job API 控制器的所有接口
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("h2-file")
@AutoConfigureWebMvc
public class JobApiTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private TestDataGenerator testDataGenerator;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // 准备测试数据
        testDataGenerator.createTable("orders");
        testDataGenerator.insertTestData("orders", 10);
    }

    @Test
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/api/jobs/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.message").value("Job API 服务正常"));
    }

    @Test
    void testGetJobTypes() throws Exception {
        mockMvc.perform(get("/api/jobs/types"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.jobTypes").isArray())
                .andExpect(jsonPath("$.jobTypes[0]").value("full"))
                .andExpect(jsonPath("$.jobTypes[1]").value("incremental"))
                .andExpect(jsonPath("$.descriptions").isArray())
                .andExpect(jsonPath("$.message").value("可用的 Job 类型"));
    }

    @Test
    void testStartFullIndexJob() throws Exception {
        mockMvc.perform(post("/api/jobs/full-index/start"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.jobId").exists())
                .andExpect(jsonPath("$.jobName").value("fullIndexJob"))
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.startTime").exists())
                .andExpect(jsonPath("$.message").value("全量索引 Job 启动成功"));
    }

    @Test
    void testStartIncrementalIndexJob() throws Exception {
        mockMvc.perform(post("/api/jobs/incremental-index/start"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.jobId").exists())
                .andExpect(jsonPath("$.jobName").value("incrementalIndexJob"))
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.startTime").exists())
                .andExpect(jsonPath("$.message").value("增量索引 Job 启动成功"));
    }

    @Test
    void testStartJobWithTypeFull() throws Exception {
        mockMvc.perform(post("/api/jobs/start/full"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.jobId").exists())
                .andExpect(jsonPath("$.jobName").value("全量索引"))
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.startTime").exists())
                .andExpect(jsonPath("$.message").value("全量索引 Job 启动成功"));
    }

    @Test
    void testStartJobWithTypeIncremental() throws Exception {
        mockMvc.perform(post("/api/jobs/start/incremental"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.jobId").exists())
                .andExpect(jsonPath("$.jobName").value("增量索引"))
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.startTime").exists())
                .andExpect(jsonPath("$.message").value("增量索引 Job 启动成功"));
    }

    @Test
    void testStartJobWithInvalidType() throws Exception {
        mockMvc.perform(post("/api/jobs/start/invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("不支持的 Job 类型: invalid"))
                .andExpect(jsonPath("$.message").value("支持的 Job 类型: full, incremental"));
    }

    @Test
    void testGetJobStatus() throws Exception {
        // 先启动一个 Job
        mockMvc.perform(post("/api/jobs/start/full"))
                .andExpect(status().isOk());

        // 查询 Job 状态（简化实现）
        mockMvc.perform(get("/api/jobs/status/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.jobId").value(1))
                .andExpect(jsonPath("$.message").value("Job 状态查询功能待实现"));
    }

    @Test
    void testApiEndpointsExist() throws Exception {
        // 测试所有 API 端点是否存在
        String[] endpoints = {
                "/api/jobs/health",
                "/api/jobs/types",
                "/api/jobs/full-index/start",
                "/api/jobs/incremental-index/start",
                "/api/jobs/start/full",
                "/api/jobs/start/incremental",
                "/api/jobs/status/1"
        };

        for (String endpoint : endpoints) {
            if (endpoint.contains("start")) {
                mockMvc.perform(post(endpoint))
                        .andExpect(status().isOk());
            } else {
                mockMvc.perform(get(endpoint))
                        .andExpect(status().isOk());
            }
        }
    }

    @Test
    void testApiResponseFormat() throws Exception {
        // 测试 API 响应格式的一致性
        mockMvc.perform(get("/api/jobs/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        mockMvc.perform(post("/api/jobs/start/full"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").exists())
                .andExpect(jsonPath("$.jobId").exists())
                .andExpect(jsonPath("$.jobName").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.startTime").exists())
                .andExpect(jsonPath("$.message").exists());
    }
}
