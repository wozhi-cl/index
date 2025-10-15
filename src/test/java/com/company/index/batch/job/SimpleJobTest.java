package com.company.index.batch.job;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import com.company.index.config.TestBatchConfig;

/**
 * 简化的测试类 - 用于调试BeforeEach问题
 */
@SpringBootTest
@ActiveProfiles("h2")
@Import(TestBatchConfig.class)  // 使用同步JobLauncher
class SimpleJobTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        System.out.println("====================================");
        System.out.println("@BeforeEach 正在执行！");
        System.out.println("====================================");
        
        try {
            // 测试数据库连接
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sample_data", Integer.class);
            System.out.println("✅ 数据库连接成功，当前记录数: " + count);
            
            // 清空数据
            jdbcTemplate.execute("DELETE FROM sample_data");
            System.out.println("✅ 数据已清空");
            
            // 插入测试数据
            String sql = "INSERT INTO sample_data (name, email, data_value) VALUES (?, ?, ?)";
            jdbcTemplate.update(sql, "TestUser1", "test1@test.com", 100);
            jdbcTemplate.update(sql, "TestUser2", "test2@test.com", 200);
            jdbcTemplate.update(sql, "TestUser3", "test3@test.com", 300);
            
            count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sample_data", Integer.class);
            System.out.println("✅ 测试数据插入成功，共 " + count + " 条记录");
            
        } catch (Exception e) {
            System.err.println("❌ BeforeEach执行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @AfterEach
    void tearDown() {
        System.out.println("====================================");
        System.out.println("@AfterEach 正在执行！");
        System.out.println("====================================");
    }

    @Test
    @DisplayName("测试1: 验证BeforeEach是否执行")
    void test1() {
        System.out.println("====================================");
        System.out.println("测试1 正在执行");
        System.out.println("====================================");
        
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sample_data", Integer.class);
        System.out.println("当前数据库记录数: " + count);
        
        Assertions.assertEquals(3, count, "应该有3条测试数据");
        System.out.println("✅ 测试1 通过");
    }

    @Test
    @DisplayName("测试2: 再次验证BeforeEach")
    void test2() {
        System.out.println("====================================");
        System.out.println("测试2 正在执行");
        System.out.println("====================================");
        
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sample_data", Integer.class);
        System.out.println("当前数据库记录数: " + count);
        
        Assertions.assertEquals(3, count, "应该有3条测试数据（每个测试前都会执行BeforeEach）");
        System.out.println("✅ 测试2 通过");
    }
}

