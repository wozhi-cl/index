package com.company.index.common.util;

import com.company.index.common.model.SourceRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能工具使用示例测试
 * 演示如何使用 GenericRowMapper、SqlBuilder 和 TestDataGenerator
 */
public class SmartToolsExampleTest {

    /**
     * 示例 1: SqlBuilder 基础用法
     */
    @Test
    public void testSqlBuilderBasic() {
        System.out.println("=== 示例 1: 简单 SELECT ===");
        
        String sql = SqlBuilder.create()
                .from("USER_INFO", "u")
                .select("u.USER_ID", "u.USER_NAME", "u.EMAIL")
                .where("u.STATUS = 'ACTIVE'")
                .orderBy("u.CREATED_AT DESC")
                .build();
        
        System.out.println(sql);
        System.out.println();
    }

    /**
     * 示例 2: SqlBuilder JOIN 查询
     */
    @Test
    public void testSqlBuilderJoin() {
        System.out.println("=== 示例 2: JOIN 查询 ===");
        
        String sql = SqlBuilder.create()
                .from("USER_INFO", "u")
                .leftJoin("USER_DETAIL", "d", "u.USER_ID = d.USER_ID")
                
                // 主表字段
                .selectFields("u", 
                        "USER_ID", "USER_NAME", "EMAIL", "PHONE")
                
                // 详情表字段（带前缀）
                .selectFieldsWithPrefix("d",
                        "ADDRESS", "CITY", "COMPANY", "POSITION")
                
                .where("u.STATUS = 'ACTIVE'")
                .and("u.CREATED_AT > CURRENT_DATE - INTERVAL 7 DAY")
                .orderBy("u.CREATED_AT DESC", "u.USER_NAME ASC")
                .limit(100)
                .build();
        
        System.out.println(sql);
        System.out.println();
    }

    /**
     * 示例 3: 使用便捷方法
     */
    @Test
    public void testSqlBuilderConvenience() {
        System.out.println("=== 示例 3: 便捷方法 ===");
        
        String sql = SqlBuilder.twoTableJoin(
                "ORDER_INFO", "o",
                "ORDER_DETAIL", "d",
                "o.ORDER_ID = d.ORDER_ID",
                new String[]{"ORDER_ID", "ORDER_NO", "TOTAL_AMOUNT", "STATUS"},
                new String[]{"PRODUCT_NAME", "QUANTITY", "PRICE"}
        );
        
        System.out.println(sql);
        System.out.println();
    }

    /**
     * 示例 4: GenericRowMapper 基础用法
     */
    @Test
    public void testGenericRowMapperBasic() {
        System.out.println("=== 示例 4: GenericRowMapper 基础用法 ===");
        
        GenericRowMapper<SourceRecord> rowMapper = new GenericRowMapper.Builder<>(SourceRecord.class)
                .autoUnderscoreToCamelCase(true)  // 启用自动转换
                .build();
        
        System.out.println("RowMapper 创建成功，支持自动字段映射");
        System.out.println("数据库字段 USER_NAME 会自动映射到 Java 字段 userName");
        System.out.println();
    }

    /**
     * 示例 5: GenericRowMapper 高级用法
     */
    @Test
    public void testGenericRowMapperAdvanced() {
        System.out.println("=== 示例 5: GenericRowMapper 高级用法 ===");
        
        GenericRowMapper<SourceRecord> rowMapper = new GenericRowMapper.Builder<>(SourceRecord.class)
                .autoUnderscoreToCamelCase(true)
                
                // 字段名映射
                .addFieldMapping("USER_ID", "id")
                .addFieldMapping("USER_NAME", "name")
                
                // 日期时间处理
                .addDateTimeHandler("CREATED_AT")
                .addDateTimeHandler("UPDATED_AT")
                
                // CLOB 处理
                .addClobHandler("LARGE_TEXT")
                
                // 自定义处理
                .addSpecialHandler("COMPLEX_FIELD", (rs, name) -> {
                    try {
                        String value = rs.getString(name);
                        // 自定义处理逻辑
                        return value != null ? value.toUpperCase() : null;
                    } catch (Exception e) {
                        return null;
                    }
                })
                
                .build();
        
        System.out.println("RowMapper 创建成功，包含特殊字段处理器");
        System.out.println("- 字段映射: USER_ID -> id");
        System.out.println("- 日期处理: CREATED_AT, UPDATED_AT");
        System.out.println("- CLOB处理: LARGE_TEXT");
        System.out.println("- 自定义处理: COMPLEX_FIELD");
        System.out.println();
    }

    /**
     * 示例 6: TestDataGenerator 基础用法
     */
    @Test
    public void testTestDataGeneratorBasic() {
        System.out.println("=== 示例 6: TestDataGenerator 基础用法 ===");
        
        TestDataGenerator generator = TestDataGenerator.forClass(SourceRecord.class)
                .excludeField("id")  // ID 由数据库生成
                .build();
        
        // 生成单个对象
        SourceRecord record = generator.generateOne();
        System.out.println("生成的测试对象: " + record);
        System.out.println();
        
        // 生成多个对象
        List<SourceRecord> records = generator.generateList(5);
        System.out.println("生成了 " + records.size() + " 个测试对象");
        System.out.println();
    }

    /**
     * 示例 7: TestDataGenerator 生成 SQL
     */
    @Test
    public void testTestDataGeneratorSql() {
        System.out.println("=== 示例 7: 生成 INSERT SQL ===");
        
        TestDataGenerator generator = TestDataGenerator.forClass(SourceRecord.class)
                .excludeField("id")
                .withCustomGenerator("type", () -> "INSERT")
                .withCustomGenerator("source", () -> "TEST")
                .build();
        
        // 生成单条 SQL
        String insertSql = generator.generateInsertSql("SOURCE_RECORD");
        System.out.println("单条 INSERT SQL:");
        System.out.println(insertSql);
        System.out.println();
        
        // 生成多条 SQL
        List<String> insertSqls = generator.generateInsertSqlList("SOURCE_RECORD", 3);
        System.out.println("生成了 " + insertSqls.size() + " 条 INSERT SQL:");
        insertSqls.forEach(sql -> System.out.println(sql));
        System.out.println();
    }

    /**
     * 示例 8: 自定义字段生成器
     */
    @Test
    public void testCustomGenerator() {
        System.out.println("=== 示例 8: 自定义字段生成器 ===");
        
        TestDataGenerator generator = TestDataGenerator.forClass(SourceRecord.class)
                .excludeField("id")
                
                // 自定义 type 字段
                .withCustomGenerator("type", () -> {
                    String[] types = {"INSERT", "UPDATE", "DELETE"};
                    return types[(int) (Math.random() * types.length)];
                })
                
                // 自定义 timestamp 字段
                .withCustomGenerator("timestamp", () -> {
                    return LocalDateTime.now().minusDays((long) (Math.random() * 30));
                })
                
                // 自定义 data 字段
                .withCustomGenerator("data", () -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("key1", "value1");
                    data.put("key2", Math.random() * 100);
                    data.put("key3", LocalDateTime.now());
                    return data;
                })
                
                .build();
        
        SourceRecord record = generator.generateOne();
        System.out.println("自定义生成的对象: " + record);
        System.out.println("Type: " + record.getType());
        System.out.println("Timestamp: " + record.getTimestamp());
        System.out.println("Data: " + record.getData());
        System.out.println();
    }

    /**
     * 示例 9: 完整的工作流
     */
    @Test
    public void testCompleteWorkflow() {
        System.out.println("=== 示例 9: 完整工作流 ===");
        
        // 步骤 1: 构建 SQL
        System.out.println("步骤 1: 构建 SQL");
        String sql = SqlBuilder.create()
                .from("USER_INFO", "u")
                .leftJoin("USER_DETAIL", "d", "u.USER_ID = d.USER_ID")
                .selectFields("u", "USER_ID", "USER_NAME", "EMAIL")
                .selectFieldsWithPrefix("d", "ADDRESS", "PHONE")
                .where("u.STATUS = 'ACTIVE'")
                .build();
        System.out.println(sql);
        System.out.println();
        
        // 步骤 2: 创建 RowMapper
        System.out.println("步骤 2: 创建 RowMapper");
        GenericRowMapper<SourceRecord> rowMapper = new GenericRowMapper.Builder<>(SourceRecord.class)
                .autoUnderscoreToCamelCase(true)
                .addFieldMapping("USER_ID", "id")
                .addDateTimeHandler("CREATED_AT")
                .build();
        System.out.println("RowMapper 已创建");
        System.out.println();
        
        // 步骤 3: 生成测试数据
        System.out.println("步骤 3: 生成测试数据");
        TestDataGenerator generator = TestDataGenerator.forClass(SourceRecord.class)
                .excludeField("id")
                .build();
        
        List<String> testSqls = generator.generateInsertSqlList("USER_INFO", 3);
        System.out.println("生成了 " + testSqls.size() + " 条测试数据 SQL:");
        testSqls.forEach(System.out::println);
        System.out.println();
        
        System.out.println("工作流完成！");
    }
}

