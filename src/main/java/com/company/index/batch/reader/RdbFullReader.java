package com.company.index.batch.reader;

import com.company.index.common.model.SourceRecord;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * RDB 全量数据读取器
 * 支持分页和游标两种模式
 */
@Component
public class RdbFullReader {

    private final DataSource dataSource;
    private final String tableName;
    private final String idColumn;
    private final String timeColumn;

    public RdbFullReader(DataSource dataSource,
                        @Value("${index.dataSource.table:business_data}") String tableName,
                        @Value("${index.dataSource.idColumn:id}") String idColumn,
                        @Value("${index.dataSource.timeColumn:updated_at}") String timeColumn) {
        this.dataSource = dataSource;
        this.tableName = tableName;
        this.idColumn = idColumn;
        this.timeColumn = timeColumn;
    }

    /**
     * 创建分页读取器（推荐用于大数据量）
     */
    public JdbcPagingItemReader<SourceRecord> createPagingReader(int pageSize) {
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
                    .rowMapper(new SourceRecordRowMapper())
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to create paging reader", e);
        }
    }

    /**
     * 创建游标读取器（适用于小数据量或需要实时处理）
     */
    public JdbcCursorItemReader<SourceRecord> createCursorReader() {
        String sql = "SELECT * FROM " + tableName + " ORDER BY " + idColumn;

        return new JdbcCursorItemReaderBuilder<SourceRecord>()
                .name("rdbFullCursorReader")
                .dataSource(dataSource)
                .sql(sql)
                .rowMapper(new SourceRecordRowMapper())
                .build();
    }

    /**
     * 行映射器：将 ResultSet 转换为 SourceRecord
     */
    private class SourceRecordRowMapper implements RowMapper<SourceRecord> {
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
                "RDB",
                1L
            );
        }
    }
}
