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
 * MySQL 数据库专用读取器
 * 针对 MySQL 数据库的 SQL 语法和特性优化
 */
@Component
public class MySQLReader implements DataReader {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RecordBuilderService recordBuilderService;

    @Value("${index.dataSource.table:orders}")
    private String tableName;

    @Value("${index.incremental.overlapMinutes:5}")
    private int overlapMinutes;

    /**
     * 创建 MySQL 全量分页读取器
     */
    public ItemReader<SourceRecord> createFullReader(int pageSize) {
        try {
            // 构建固定的 SQL 查询
            String selectClause = buildMySQLSelectClause();
            String fromClause = buildMySQLFromClause();
            String sortKey = "orders.id";

            System.out.println("========================================");
            System.out.println("创建 MySQL 全量分页读取器:");
            System.out.println("  表名: " + tableName);
            System.out.println("  排序键: " + sortKey);
            System.out.println("========================================");

            // 创建分页查询提供器
            SqlPagingQueryProviderFactoryBean queryProviderFactory = new SqlPagingQueryProviderFactoryBean();
            queryProviderFactory.setDataSource(dataSource);
            queryProviderFactory.setSelectClause(selectClause);
            queryProviderFactory.setFromClause(fromClause);
            queryProviderFactory.setSortKey(sortKey);

            PagingQueryProvider queryProvider = queryProviderFactory.getObject();

            // 创建分页读取器
            return new JdbcPagingItemReaderBuilder<SourceRecord>()
                    .name("mysqlFullPagingReader")
                    .dataSource(dataSource)
                    .queryProvider(queryProvider)
                    .pageSize(pageSize)
                    .rowMapper(new MySQLRecordRowMapper())
                    .build();

        } catch (Exception e) {
            System.err.println("创建 MySQL 全量读取器失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create MySQL full reader", e);
        }
    }

    /**
     * 创建 MySQL 增量游标读取器
     */
    public JdbcCursorItemReader<SourceRecord> createDeltaReader(LocalDateTime startTime, LocalDateTime endTime) {
        try {
            // 构建增量查询 SQL
            String sql = buildMySQLDeltaQuery(startTime, endTime);

            System.out.println("========================================");
            System.out.println("创建 MySQL 增量游标读取器:");
            System.out.println("  表名: " + tableName);
            System.out.println("  开始时间: " + startTime);
            System.out.println("  结束时间: " + endTime);
            System.out.println("========================================");

            return new JdbcCursorItemReaderBuilder<SourceRecord>()
                    .name("mysqlDeltaCursorReader")
                    .dataSource(dataSource)
                    .sql(sql)
                    .rowMapper(new MySQLRecordRowMapper())
                    .build();

        } catch (Exception e) {
            System.err.println("创建 MySQL 增量读取器失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create MySQL delta reader", e);
        }
    }

    /**
     * MySQL 记录行映射器
     */
    private class MySQLRecordRowMapper implements RowMapper<SourceRecord> {
        @Override
        public SourceRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            return recordBuilderService.buildSourceRecordFromResultSet(rs, tableName);
        }
    }

    /**
     * 构建 MySQL 全量查询的 SELECT 子句
     */
    private String buildMySQLSelectClause() {
        return "orders.id, orders.order_no, orders.amount, orders.user_id, orders.status, " +
               "orders.created_at, orders.updated_at, users.name as user_name, users.phone as user_phone";
    }

    /**
     * 构建 MySQL 全量查询的 FROM 子句
     */
    private String buildMySQLFromClause() {
        return "orders LEFT JOIN users ON orders.user_id = users.id";
    }

    /**
     * 构建 MySQL 增量查询 SQL
     */
    private String buildMySQLDeltaQuery(LocalDateTime startTime, LocalDateTime endTime) {
        return "SELECT orders.id, orders.order_no, orders.amount, orders.user_id, orders.status, " +
               "orders.created_at, orders.updated_at, users.name as user_name, users.phone as user_phone " +
               "FROM orders LEFT JOIN users ON orders.user_id = users.id " +
               "WHERE orders.updated_at >= ? AND orders.updated_at <= ? " +
               "ORDER BY orders.id";
    }
}