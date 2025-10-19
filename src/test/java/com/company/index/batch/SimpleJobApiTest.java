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
 * 简化的 Job API 测试类
 * 只测试 API 接口，不涉及 Spring Batch Job 执行
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("h2-file")
@AutoConfigureWebMvc
public class SimpleJobApiTest {

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
        testDataGenerator.insertTestData("orders", 5);
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
    void testStartJobWithInvalidType() throws Exception {
        mockMvc.perform(post("/api/jobs/start/invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("不支持的 Job 类型: invalid"))
                .andExpect(jsonPath("$.message").value("支持的 Job 类型: full, incremental"));
    }

    @Test
    void testApiEndpointsExist() throws Exception {
        // 测试所有 API 端点是否存在
        String[] endpoints = {
                "/api/jobs/health",
                "/api/jobs/types"
        };

        for (String endpoint : endpoints) {
            mockMvc.perform(get(endpoint))
                    .andExpect(status().isOk());
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
    }
}
