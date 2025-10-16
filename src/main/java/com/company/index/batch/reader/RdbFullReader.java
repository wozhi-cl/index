package com.company.index.batch.reader;

import com.company.index.common.model.SourceRecord;
import com.company.index.common.service.FieldMappingService;
import com.company.index.common.util.DynamicSqlBuilder;
import com.company.index.config.FieldMappingConfig.*;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RDB 全量数据读取器
 * 使用字段映射配置动态生成 SQL 和处理数据
 */
@Component
public class RdbFullReader {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private FieldMappingService fieldMappingService;

    @Value("${index.dataSource.table:sample_data}")
    private String tableName;

    @Value("${index.dataSource.idColumn:id}")
    private String idColumn;

    @Value("${index.dataSource.timeColumn:updated_at}")
    private String timeColumn;

    /**
     * 创建分页读取器（推荐用于大数据量）
     * 使用字段映射配置动态生成 SQL
     */
    public JdbcPagingItemReader<SourceRecord> createPagingReader(int pageSize) {
        try {
            // 获取字段映射配置
            List<FieldConfig> mainFields = fieldMappingService.getMainFields(tableName);
            List<JoinConfig> joins = fieldMappingService.getJoinConfigs(tableName);
            FieldConfig keyField = fieldMappingService.getKeyField(tableName);
            
            if (mainFields == null || mainFields.isEmpty()) {
                System.err.println("警告：表 " + tableName + " 未配置字段映射，使用默认 SELECT *");
                return createDefaultPagingReader(pageSize);
            }

            // 构建 SELECT 子句
            String selectClause = buildSelectClause(mainFields, joins);
            
            // 构建 FROM 子句
            String fromClause = buildFromClause(tableName, joins);
            
            // 使用主键作为排序键
            String sortKey = keyField != null ? keyField.getName() : idColumn;

            System.out.println("========================================");
            System.out.println("创建分页读取器:");
            System.out.println("  表名: " + tableName);
            System.out.println("  字段数: " + mainFields.size());
            System.out.println("  关联数: " + (joins != null ? joins.size() : 0));
            System.out.println("  排序键: " + sortKey);
            System.out.println("========================================");

            SqlPagingQueryProviderFactoryBean queryProvider = new SqlPagingQueryProviderFactoryBean();
            queryProvider.setDataSource(dataSource);
            queryProvider.setSelectClause(selectClause);
            queryProvider.setFromClause(fromClause);
            queryProvider.setSortKey(sortKey);

            PagingQueryProvider pagingQueryProvider = queryProvider.getObject();

            return new JdbcPagingItemReaderBuilder<SourceRecord>()
                    .name("rdbFullPagingReader")
                    .dataSource(dataSource)
                    .queryProvider(pagingQueryProvider)
                    .pageSize(pageSize)
                    .rowMapper(new DynamicSourceRecordRowMapper(mainFields, joins))
                    .build();

        } catch (Exception e) {
            System.err.println("创建分页读取器失败，使用默认配置: " + e.getMessage());
            return createDefaultPagingReader(pageSize);
        }
    }

    /**
     * 创建游标读取器（适用于小数据量或需要实时处理）
     * 使用字段映射配置动态生成 SQL
     */
    public JdbcCursorItemReader<SourceRecord> createCursorReader() {
        try {
            // 获取字段映射配置
            List<FieldConfig> mainFields = fieldMappingService.getMainFields(tableName);
            List<JoinConfig> joins = fieldMappingService.getJoinConfigs(tableName);
            FieldConfig keyField = fieldMappingService.getKeyField(tableName);

            if (mainFields == null || mainFields.isEmpty()) {
                System.err.println("警告：表 " + tableName + " 未配置字段映射，使用默认 SELECT *");
                return createDefaultCursorReader();
            }

            // 使用 DynamicSqlBuilder 生成完整 SQL
            String sql = DynamicSqlBuilder.buildFullQuerySql(tableName, mainFields, joins, keyField);

            System.out.println("========================================");
            System.out.println("创建游标读取器:");
            System.out.println("  生成的 SQL: " + sql);
            System.out.println("========================================");

            return new JdbcCursorItemReaderBuilder<SourceRecord>()
                    .name("rdbFullCursorReader")
                    .dataSource(dataSource)
                    .sql(sql)
                    .rowMapper(new DynamicSourceRecordRowMapper(mainFields, joins))
                    .build();

        } catch (Exception e) {
            System.err.println("创建游标读取器失败，使用默认配置: " + e.getMessage());
            return createDefaultCursorReader();
        }
    }

    /**
     * 构建 SELECT 子句
     */
    private String buildSelectClause(List<FieldConfig> mainFields, List<JoinConfig> joins) {
        StringBuilder select = new StringBuilder("SELECT ");
        
        // 添加主表字段
        for (int i = 0; i < mainFields.size(); i++) {
            if (i > 0) select.append(", ");
            FieldConfig field = mainFields.get(i);
            
            if (field.getTransform() != null && !field.getTransform().trim().isEmpty()) {
                select.append(field.getTransform());
            } else {
                select.append(tableName).append(".").append(field.getName());
            }
            
            if (field.getAlias() != null && !field.getAlias().trim().isEmpty()) {
                select.append(" AS ").append(field.getAlias());
            }
        }
        
        // 添加关联表字段
        if (joins != null && !joins.isEmpty()) {
            for (JoinConfig join : joins) {
                if (join.getFields() != null) {
                    for (FieldConfig field : join.getFields()) {
                        select.append(", ");
                        if (field.getTransform() != null && !field.getTransform().trim().isEmpty()) {
                            select.append(field.getTransform());
                        } else {
                            select.append(join.getTable()).append(".").append(field.getName());
                        }
                        if (field.getAlias() != null && !field.getAlias().trim().isEmpty()) {
                            select.append(" AS ").append(field.getAlias());
                        }
                    }
                }
            }
        }
        
        return select.toString();
    }

    /**
     * 构建 FROM 子句（包含 JOIN）
     */
    private String buildFromClause(String tableName, List<JoinConfig> joins) {
        StringBuilder from = new StringBuilder("FROM ").append(tableName);
        
        if (joins != null && !joins.isEmpty()) {
            for (JoinConfig join : joins) {
                String joinType = join.getType() != null ? join.getType() : "LEFT";
                from.append(" ").append(joinType).append(" JOIN ").append(join.getTable());
                from.append(" ON ").append(tableName).append(".").append(join.getOnField());
                from.append(" = ").append(join.getTable()).append(".id");
            }
        }
        
        return from.toString();
    }

    /**
     * 创建默认分页读取器（未配置字段映射时使用）
     */
    private JdbcPagingItemReader<SourceRecord> createDefaultPagingReader(int pageSize) {
        try {
            SqlPagingQueryProviderFactoryBean queryProvider = new SqlPagingQueryProviderFactoryBean();
            queryProvider.setDataSource(dataSource);
            queryProvider.setSelectClause("SELECT *");
            queryProvider.setFromClause("FROM " + tableName);
            queryProvider.setSortKey(idColumn);

            PagingQueryProvider pagingQueryProvider = queryProvider.getObject();

            return new JdbcPagingItemReaderBuilder<SourceRecord>()
                    .name("rdbFullPagingReader")
                    .dataSource(dataSource)
                    .queryProvider(pagingQueryProvider)
                    .pageSize(pageSize)
                    .rowMapper(new DefaultSourceRecordRowMapper())
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to create default paging reader", e);
        }
    }

    /**
     * 创建默认游标读取器（未配置字段映射时使用）
     */
    private JdbcCursorItemReader<SourceRecord> createDefaultCursorReader() {
        String sql = "SELECT * FROM " + tableName + " ORDER BY " + idColumn;

        return new JdbcCursorItemReaderBuilder<SourceRecord>()
                .name("rdbFullCursorReader")
                .dataSource(dataSource)
                .sql(sql)
                .rowMapper(new DefaultSourceRecordRowMapper())
                .build();
    }

    /**
     * 动态行映射器：根据字段配置映射数据
     */
    private class DynamicSourceRecordRowMapper implements RowMapper<SourceRecord> {
        private final List<FieldConfig> mainFields;
        private final List<JoinConfig> joins;

        public DynamicSourceRecordRowMapper(List<FieldConfig> mainFields, List<JoinConfig> joins) {
            this.mainFields = mainFields;
            this.joins = joins;
        }

        @Override
        public SourceRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            // 获取主键值
            FieldConfig keyField = mainFields.stream()
                    .filter(f -> f.getIsKey() != null && f.getIsKey())
                    .findFirst()
                    .orElse(mainFields.get(0));
            
            String id = String.valueOf(rs.getObject(keyField.getEffectiveName()));
            
            // 获取时间戳
            LocalDateTime timestamp = LocalDateTime.now();
            try {
                Object timeValue = rs.getObject(timeColumn);
                if (timeValue != null) {
                    if (timeValue instanceof java.sql.Timestamp) {
                        timestamp = ((java.sql.Timestamp) timeValue).toLocalDateTime();
                    } else if (timeValue instanceof LocalDateTime) {
                        timestamp = (LocalDateTime) timeValue;
                    }
                }
            } catch (Exception e) {
                // 时间字段不存在或类型不匹配，使用默认值
            }

            // 构建数据映射
            Map<String, Object> data = new HashMap<>();
            
            // 添加主表字段
            for (FieldConfig field : mainFields) {
                String fieldName = field.getEffectiveName();
                try {
                    Object value = rs.getObject(fieldName);
                    data.put(fieldName, value);
                } catch (SQLException e) {
                    System.err.println("读取字段 " + fieldName + " 失败: " + e.getMessage());
                }
            }
            
            // 添加关联表字段
            if (joins != null && !joins.isEmpty()) {
                for (JoinConfig join : joins) {
                    if (join.getFields() != null) {
                        for (FieldConfig field : join.getFields()) {
                            String fieldName = field.getEffectiveName();
                            try {
                                Object value = rs.getObject(fieldName);
                                data.put(fieldName, value);
                            } catch (SQLException e) {
                                System.err.println("读取关联字段 " + fieldName + " 失败: " + e.getMessage());
                            }
                        }
                    }
                }
            }

            return new SourceRecord(
                id,
                "INSERT", // 全量读取默认为 INSERT 类型
                timestamp,
                data,
                "RDB:" + tableName,
                1L
            );
        }
    }

    /**
     * 默认行映射器：未配置字段映射时使用
     */
    private class DefaultSourceRecordRowMapper implements RowMapper<SourceRecord> {
        @Override
        public SourceRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            String id = String.valueOf(rs.getLong(idColumn));
            LocalDateTime timestamp = rs.getTimestamp(timeColumn) != null 
                ? rs.getTimestamp(timeColumn).toLocalDateTime() 
                : LocalDateTime.now();

            // 构建数据映射
            Map<String, Object> data = new HashMap<>();
            int columnCount = rs.getMetaData().getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = rs.getMetaData().getColumnName(i);
                Object value = rs.getObject(i);
                data.put(columnName, value);
            }

            return new SourceRecord(
                id,
                "INSERT", // 全量读取默认为 INSERT 类型
                timestamp,
                data,
                "RDB:" + tableName,
                1L
            );
        }
    }
}
