package com.company.index.batch.reader;

import com.company.index.common.model.SourceRecord;
import com.company.index.common.util.GenericRowMapper;
import com.company.index.common.util.SqlBuilder;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.util.Map;

/**
 * 智能 JOIN 读取器示例
 * 演示如何使用 GenericRowMapper 和 SqlBuilder 处理多表 JOIN 和大量字段
 * 
 * 使用场景：
 * 1. 两个表各有 100+ 个字段
 * 2. 需要 JOIN 查询并合并数据
 * 3. 不想手动处理每个字段
 */
@Component
public class SmartJoinReader {

    private final DataSource dataSource;
    private final String table1Name;
    private final String table2Name;
    private final String joinColumn;

    public SmartJoinReader(
            DataSource dataSource,
            @Value("${index.dataSource.table1:MAIN_TABLE}") String table1Name,
            @Value("${index.dataSource.table2:DETAIL_TABLE}") String table2Name,
            @Value("${index.dataSource.joinColumn:ID}") String joinColumn) {
        this.dataSource = dataSource;
        this.table1Name = table1Name;
        this.table2Name = table2Name;
        this.joinColumn = joinColumn;
    }

    /**
     * 创建智能 JOIN 读取器
     * 
     * 特点：
     * 1. 自动处理所有字段
     * 2. 只需指定特殊字段的处理逻辑
     * 3. 支持字段名映射
     */
    public JdbcCursorItemReader<SourceRecord> createSmartReader() {
        
        // 1. 构建 SQL（智能 JOIN）
        String sql = buildSmartJoinSql();
        
        // 2. 创建 RowMapper（智能映射）
        GenericRowMapper<SourceRecord> rowMapper = createSmartRowMapper();
        
        // 3. 创建 Reader
        return new JdbcCursorItemReaderBuilder<SourceRecord>()
                .name("smartJoinReader")
                .dataSource(dataSource)
                .sql(sql)
                .rowMapper(rowMapper)
                .build();
    }

    /**
     * 构建智能 JOIN SQL
     */
    private String buildSmartJoinSql() {
        return SqlBuilder.create()
                .from(table1Name, "t1")
                .leftJoin(table2Name, "t2", "t1." + joinColumn + " = t2." + joinColumn)
                
                // 选择主表的关键字段
                .selectFields("t1",
                        joinColumn,
                        "NAME",
                        "STATUS",
                        "CREATED_AT",
                        "UPDATED_AT"
                        // 如果有更多字段，继续添加
                        // 或者使用配置文件读取字段列表
                )
                
                // 选择详情表的字段（自动添加前缀避免冲突）
                .selectFieldsWithPrefix("t2",
                        "DESCRIPTION",
                        "CATEGORY",
                        "TAGS",
                        "METADATA"
                        // 更多字段...
                )
                
                .where("t1.STATUS = 'ACTIVE'")
                .orderBy("t1.UPDATED_AT DESC")
                .build();
    }

    /**
     * 创建智能 RowMapper
     */
    private GenericRowMapper<SourceRecord> createSmartRowMapper() {
        return new GenericRowMapper.Builder<>(SourceRecord.class)
                // 启用自动下划线转驼峰
                .autoUnderscoreToCamelCase(true)
                
                // 处理日期时间字段
                .addDateTimeHandler("CREATED_AT")
                .addDateTimeHandler("UPDATED_AT")
                
                // 处理 CLOB 字段（如果有）
                .addClobHandler("LARGE_TEXT")
                
                // 字段名映射（如果 Java 字段名与数据库不同）
                .addFieldMapping(joinColumn, "id")
                .addFieldMapping("NAME", "name")
                
                // 特殊处理：将所有原始数据放入 data Map
                .addSpecialHandler("_ALL_DATA_", (rs, name) -> {
                    return extractAllData(rs);
                })
                
                .build();
    }

    /**
     * 提取所有数据到 Map
     * 用于保存完整的数据记录
     */
    private Map<String, Object> extractAllData(ResultSet rs) {
        try {
            Map<String, Object> data = new HashMap<>();
            ResultSetMetaData metaData = rs.getMetaData();
            
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                String columnName = metaData.getColumnLabel(i);
                Object value = rs.getObject(i);
                
                // 转换特殊类型
                if (value instanceof java.sql.Timestamp) {
                    value = ((java.sql.Timestamp) value).toLocalDateTime();
                } else if (value instanceof java.sql.Clob) {
                    java.sql.Clob clob = (java.sql.Clob) value;
                    value = clob.getSubString(1, (int) clob.length());
                }
                
                data.put(columnName, value);
            }
            
            return data;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract all data", e);
        }
    }

    /**
     * 创建简单的 Reader（只查询一个表）
     */
    public JdbcCursorItemReader<SourceRecord> createSimpleReader() {
        String sql = SqlBuilder.simpleSelect(
                table1Name,
                "STATUS = 'ACTIVE'",
                "UPDATED_AT DESC"
        );

        GenericRowMapper<SourceRecord> rowMapper = new GenericRowMapper.Builder<>(SourceRecord.class)
                .autoUnderscoreToCamelCase(true)
                .build();

        return new JdbcCursorItemReaderBuilder<SourceRecord>()
                .name("simpleReader")
                .dataSource(dataSource)
                .sql(sql)
                .rowMapper(rowMapper)
                .build();
    }

    /**
     * 创建复杂的多表 JOIN Reader
     * 
     * 示例：3 表 JOIN
     */
    public JdbcCursorItemReader<SourceRecord> createComplexJoinReader() {
        
        String sql = SqlBuilder.create()
                .from("USER_INFO", "u")
                .leftJoin("USER_DETAIL", "d", "u.USER_ID = d.USER_ID")
                .leftJoin("USER_STATUS", "s", "u.USER_ID = s.USER_ID")
                
                // 从不同表选择字段
                .selectFields("u", "USER_ID", "USER_NAME", "EMAIL")
                .selectFieldsWithPrefix("d", "ADDRESS", "PHONE", "COMPANY")
                .selectFieldsWithPrefix("s", "STATUS", "LAST_LOGIN", "LOGIN_COUNT")
                
                .where("u.DELETED = 0")
                .and("s.STATUS = 'ACTIVE'")
                .orderBy("u.CREATED_AT DESC")
                .limit(1000)
                .build();

        GenericRowMapper<SourceRecord> rowMapper = new GenericRowMapper.Builder<>(SourceRecord.class)
                .autoUnderscoreToCamelCase(true)
                
                // 处理来自不同表的同名字段
                .addFieldMapping("d_ADDRESS", "address")
                .addFieldMapping("s_STATUS", "status")
                
                .addDateTimeHandler("LAST_LOGIN")
                
                .build();

        return new JdbcCursorItemReaderBuilder<SourceRecord>()
                .name("complexJoinReader")
                .dataSource(dataSource)
                .sql(sql)
                .rowMapper(rowMapper)
                .build();
    }
}

