package com.company.index.batch.reader;

import com.company.index.common.model.DynamicRecord;
import com.company.index.common.service.FieldMappingService;
import com.company.index.common.service.RecordBuilderService;
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
import java.util.List;

/**
 * 动态 RDB 全量数据读取器
 * 使用 DynamicRecord 和字段映射配置
 */
@Component
public class DynamicRdbFullReader {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private FieldMappingService fieldMappingService;

    @Autowired
    private RecordBuilderService recordBuilderService;

    @Value("${index.dataSource.table:orders}")
    private String tableName;

    /**
     * 创建分页读取器（推荐用于大数据量）
     */
    public JdbcPagingItemReader<DynamicRecord> createPagingReader(int pageSize) {
        try {
            // 获取字段映射配置
            List<FieldConfig> mainFields = fieldMappingService.getMainFields(tableName);
            List<JoinConfig> joins = fieldMappingService.getJoinConfigs(tableName);
            FieldConfig keyField = fieldMappingService.getKeyField(tableName);
            
            if (mainFields == null || mainFields.isEmpty()) {
                throw new IllegalStateException("表 " + tableName + " 未配置字段映射");
            }

            // 使用 DynamicSqlBuilder 构建完整 SQL
            String fullSql = DynamicSqlBuilder.buildFullQuerySql(tableName, mainFields, joins, keyField);
            
            // 提取 SELECT 和 FROM 子句
            String selectClause = extractSelectClause(fullSql);
            String fromClause = extractFromClause(fullSql);
            
            // 使用主键的实际列名作为排序键
            // 注意：当有 JOIN 时，必须包含表名前缀以避免歧义
            String sortKeyColumnName = keyField != null ? keyField.getName() : "id";
            String sortKey = (joins != null && !joins.isEmpty()) ? 
                tableName + "." + sortKeyColumnName : sortKeyColumnName;

            System.out.println("========================================");
            System.out.println("创建动态分页读取器:");
            System.out.println("  表名: " + tableName);
            System.out.println("  字段数: " + mainFields.size());
            System.out.println("  关联数: " + (joins != null ? joins.size() : 0));
            System.out.println("  排序键: " + sortKey);
            System.out.println("  完整SQL: " + fullSql);
            System.out.println("========================================");

            // 创建分页查询提供器
            SqlPagingQueryProviderFactoryBean queryProviderFactory = new SqlPagingQueryProviderFactoryBean();
            queryProviderFactory.setDataSource(dataSource);
            queryProviderFactory.setSelectClause(selectClause);
            queryProviderFactory.setFromClause(fromClause);
            queryProviderFactory.setSortKey(sortKey);
            
            PagingQueryProvider queryProvider = queryProviderFactory.getObject();

            // 创建分页读取器
            return new JdbcPagingItemReaderBuilder<DynamicRecord>()
                    .name("dynamicRdbFullPagingReader")
                    .dataSource(dataSource)
                    .queryProvider(queryProvider)
                    .pageSize(pageSize)
                    .rowMapper(new DynamicRecordRowMapper())
                    .build();

        } catch (Exception e) {
            System.err.println("创建动态分页读取器失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create dynamic paging reader", e);
        }
    }

    /**
     * 创建游标读取器（适用于小数据量）
     */
    public JdbcCursorItemReader<DynamicRecord> createCursorReader() {
        try {
            // 获取字段映射配置
            List<FieldConfig> mainFields = fieldMappingService.getMainFields(tableName);
            List<JoinConfig> joins = fieldMappingService.getJoinConfigs(tableName);
            FieldConfig keyField = fieldMappingService.getKeyField(tableName);
            
            if (mainFields == null || mainFields.isEmpty()) {
                throw new IllegalStateException("表 " + tableName + " 未配置字段映射");
            }

            // 使用 DynamicSqlBuilder 构建完整 SQL
            String fullSql = DynamicSqlBuilder.buildFullQuerySql(tableName, mainFields, joins, keyField);

            System.out.println("========================================");
            System.out.println("创建动态游标读取器:");
            System.out.println("  表名: " + tableName);
            System.out.println("  字段数: " + mainFields.size());
            System.out.println("  关联数: " + (joins != null ? joins.size() : 0));
            System.out.println("  SQL: " + fullSql);
            System.out.println("========================================");

            // 创建游标读取器
            return new JdbcCursorItemReaderBuilder<DynamicRecord>()
                    .name("dynamicRdbFullCursorReader")
                    .dataSource(dataSource)
                    .sql(fullSql)
                    .rowMapper(new DynamicRecordRowMapper())
                    .build();

        } catch (Exception e) {
            System.err.println("创建动态游标读取器失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create dynamic cursor reader", e);
        }
    }

    /**
     * 动态记录行映射器
     */
    private class DynamicRecordRowMapper implements RowMapper<DynamicRecord> {
        @Override
        public DynamicRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            return recordBuilderService.buildSourceRecordFromResultSet(rs, tableName);
        }
    }

    // ===== 辅助方法 =====

    /**
     * 从完整 SQL 中提取 SELECT 子句
     */
    private String extractSelectClause(String sql) {
        int fromIndex = sql.toUpperCase().indexOf(" FROM ");
        if (fromIndex == -1) {
            throw new IllegalArgumentException("Invalid SQL: missing FROM clause");
        }
        return sql.substring(0, fromIndex);
    }

    /**
     * 从完整 SQL 中提取 FROM 子句
     */
    private String extractFromClause(String sql) {
        int fromIndex = sql.toUpperCase().indexOf(" FROM ");
        int whereIndex = sql.toUpperCase().indexOf(" WHERE ", fromIndex);
        int orderByIndex = sql.toUpperCase().indexOf(" ORDER BY ", fromIndex);

        if (whereIndex != -1) {
            return sql.substring(fromIndex, whereIndex);
        } else if (orderByIndex != -1) {
            return sql.substring(fromIndex, orderByIndex);
        } else {
            return sql.substring(fromIndex);
        }
    }
}

