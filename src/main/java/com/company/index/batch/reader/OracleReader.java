package com.company.index.batch.reader;

import com.company.index.common.model.SourceRecord;
import com.company.index.common.service.RecordBuilderService;
import org.springframework.batch.item.ItemReader;
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

/**
 * Oracle 数据库专用读取器
 * 针对 Oracle 数据库的 SQL 语法和特性优化
 * 注意：Oracle 使用大写表名，支持 ROWNUM 分页查询
 */
@Component
public class OracleReader implements DataReader {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RecordBuilderService recordBuilderService;

    @Value("${index.dataSource.table:ORDERS}")
    private String tableName;

    @Value("${index.incremental.overlapMinutes:5}")
    private int overlapMinutes;

    /**
     * 创建 Oracle 全量分页读取器
     * 使用 Oracle 特有的 ROWNUM 分页
     */
    public ItemReader<SourceRecord> createFullReader(int pageSize) {
        try {
            // 构建 Oracle 分页查询
            String selectClause = buildOracleSelectClause();
            String fromClause = buildOracleFromClause();
            String sortKey = "ORDERS.ID";

            System.out.println("========================================");
            System.out.println("创建 Oracle 全量分页读取器:");
            System.out.println("  表名: " + tableName);
            System.out.println("  排序键: " + sortKey);
            System.out.println("  分页大小: " + pageSize);
            System.out.println("========================================");

            // 创建 Oracle 分页查询提供器
            SqlPagingQueryProviderFactoryBean queryProviderFactory = new SqlPagingQueryProviderFactoryBean();
            queryProviderFactory.setDataSource(dataSource);
            queryProviderFactory.setSelectClause(selectClause);
            queryProviderFactory.setFromClause(fromClause);
            queryProviderFactory.setSortKey(sortKey);
            queryProviderFactory.setDatabaseType("Oracle");

            PagingQueryProvider queryProvider = queryProviderFactory.getObject();

            // 创建分页读取器
            return new JdbcPagingItemReaderBuilder<SourceRecord>()
                    .name("oracleFullPagingReader")
                    .dataSource(dataSource)
                    .queryProvider(queryProvider)
                    .pageSize(pageSize)
                    .rowMapper(new OracleRecordRowMapper())
                    .build();

        } catch (Exception e) {
            System.err.println("创建 Oracle 全量读取器失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create Oracle full reader", e);
        }
    }

    /**
     * 创建 Oracle 增量游标读取器
     */
    public JdbcCursorItemReader<SourceRecord> createDeltaReader(LocalDateTime startTime, LocalDateTime endTime) {
        try {
            // 构建增量查询 SQL
            String sql = buildOracleDeltaQuery(startTime, endTime);

            System.out.println("========================================");
            System.out.println("创建 Oracle 增量游标读取器:");
            System.out.println("  表名: " + tableName);
            System.out.println("  开始时间: " + startTime);
            System.out.println("  结束时间: " + endTime);
            System.out.println("========================================");

            return new JdbcCursorItemReaderBuilder<SourceRecord>()
                    .name("oracleDeltaCursorReader")
                    .dataSource(dataSource)
                    .sql(sql)
                    .rowMapper(new OracleRecordRowMapper())
                    .build();

        } catch (Exception e) {
            System.err.println("创建 Oracle 增量读取器失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create Oracle delta reader", e);
        }
    }

    /**
     * Oracle 记录行映射器
     */
    private class OracleRecordRowMapper implements RowMapper<SourceRecord> {
        @Override
        public SourceRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            return recordBuilderService.buildSourceRecordFromResultSet(rs, tableName);
        }
    }

    /**
     * 构建 Oracle 全量查询的 SELECT 子句
     */
    private String buildOracleSelectClause() {
        return "ORDERS.ID, ORDERS.ORDER_NO, ORDERS.AMOUNT, ORDERS.USER_ID, ORDERS.STATUS, " +
               "ORDERS.CREATED_AT, ORDERS.UPDATED_AT, USERS.NAME as USER_NAME, USERS.PHONE as USER_PHONE";
    }

    /**
     * 构建 Oracle 全量查询的 FROM 子句
     */
    private String buildOracleFromClause() {
        return "ORDERS LEFT JOIN USERS ON ORDERS.USER_ID = USERS.ID";
    }

    /**
     * 构建 Oracle 增量查询 SQL
     */
    private String buildOracleDeltaQuery(LocalDateTime startTime, LocalDateTime endTime) {
        return "SELECT ORDERS.ID, ORDERS.ORDER_NO, ORDERS.AMOUNT, ORDERS.USER_ID, ORDERS.STATUS, " +
               "ORDERS.CREATED_AT, ORDERS.UPDATED_AT, USERS.NAME as USER_NAME, USERS.PHONE as USER_PHONE " +
               "FROM ORDERS LEFT JOIN USERS ON ORDERS.USER_ID = USERS.ID " +
               "WHERE ORDERS.UPDATED_AT >= ? AND ORDERS.UPDATED_AT <= ? " +
               "ORDER BY ORDERS.ID";
    }
}