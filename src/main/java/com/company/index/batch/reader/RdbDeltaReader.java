package com.company.index.batch.reader;

import com.company.index.common.model.SourceRecord;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
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
 * RDB 增量数据读取器
 * 支持基于时间戳和变更表的增量读取
 */
@Component
public class RdbDeltaReader {

    private final DataSource dataSource;
    private final String tableName;
    private final String deltaTableName;
    private final String idColumn;
    private final String timeColumn;

    public RdbDeltaReader(DataSource dataSource,
                          @Value("${index.dataSource.table:business_data}") String tableName,
                          @Value("${index.dataSource.deltaTable:business_data_changelog}") String deltaTableName,
                          @Value("${index.dataSource.idColumn:id}") String idColumn,
                          @Value("${index.dataSource.timeColumn:updated_at}") String timeColumn) {
        this.dataSource = dataSource;
        this.tableName = tableName;
        this.deltaTableName = deltaTableName;
        this.idColumn = idColumn;
        this.timeColumn = timeColumn;
    }

    /**
     * 创建基于时间戳的增量读取器
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param overlapMinutes 重叠窗口（分钟），用于处理迟到数据
     */
    public JdbcCursorItemReader<SourceRecord> createTimeBasedReader(LocalDateTime startTime, 
                                                                   LocalDateTime endTime, 
                                                                   int overlapMinutes) {
        // 计算重叠窗口的开始时间
        LocalDateTime actualStartTime = startTime.minusMinutes(overlapMinutes);
        
        String sql = String.format(
            "SELECT %s, %s, * FROM %s WHERE %s >= ? AND %s <= ? ORDER BY %s",
            idColumn, timeColumn, tableName, timeColumn, timeColumn, timeColumn
        );

        return new JdbcCursorItemReaderBuilder<SourceRecord>()
                .name("rdbDeltaTimeReader")
                .dataSource(dataSource)
                .sql(sql)
                .preparedStatementSetter(ps -> {
                    ps.setTimestamp(1, java.sql.Timestamp.valueOf(actualStartTime));
                    ps.setTimestamp(2, java.sql.Timestamp.valueOf(endTime));
                })
                .rowMapper(new DeltaRecordRowMapper())
                .build();
    }

    /**
     * 创建基于变更表的增量读取器
     * @param lastProcessedId 最后处理的记录ID
     */
    public JdbcCursorItemReader<SourceRecord> createChangeTableReader(Long lastProcessedId) {
        String sql = String.format(
            "SELECT %s, %s, change_type, * FROM %s WHERE id > ? ORDER BY id",
            idColumn, timeColumn, deltaTableName
        );

        return new JdbcCursorItemReaderBuilder<SourceRecord>()
                .name("rdbDeltaChangeTableReader")
                .dataSource(dataSource)
                .sql(sql)
                .preparedStatementSetter(ps -> ps.setLong(1, lastProcessedId))
                .rowMapper(new ChangeTableRowMapper())
                .build();
    }

    /**
     * 创建混合增量读取器（主表 + 变更表）
     * @param startTime 开始时间
     * @param endTime 结束时间
     */
    public JdbcCursorItemReader<SourceRecord> createHybridReader(LocalDateTime startTime, 
                                                               LocalDateTime endTime) {
        String sql = String.format(
            "SELECT t.%s, t.%s, 'UPDATE' as change_type, t.* " +
            "FROM %s t " +
            "WHERE t.%s >= ? AND t.%s <= ? " +
            "UNION ALL " +
            "SELECT c.%s, c.%s, c.change_type, c.* " +
            "FROM %s c " +
            "WHERE c.%s >= ? AND c.%s <= ? " +
            "ORDER BY %s",
            idColumn, timeColumn, tableName, timeColumn, timeColumn,
            idColumn, timeColumn, deltaTableName, timeColumn, timeColumn,
            timeColumn
        );

        return new JdbcCursorItemReaderBuilder<SourceRecord>()
                .name("rdbDeltaHybridReader")
                .dataSource(dataSource)
                .sql(sql)
                .preparedStatementSetter(ps -> {
                    ps.setTimestamp(1, java.sql.Timestamp.valueOf(startTime));
                    ps.setTimestamp(2, java.sql.Timestamp.valueOf(endTime));
                    ps.setTimestamp(3, java.sql.Timestamp.valueOf(startTime));
                    ps.setTimestamp(4, java.sql.Timestamp.valueOf(endTime));
                })
                .rowMapper(new ChangeTableRowMapper())
                .build();
    }

    /**
     * 基于时间戳的行映射器
     */
    private class DeltaRecordRowMapper implements RowMapper<SourceRecord> {
        @Override
        public SourceRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            String id = rs.getString(idColumn);
            LocalDateTime timestamp = rs.getTimestamp(timeColumn) != null 
                ? rs.getTimestamp(timeColumn).toLocalDateTime() 
                : LocalDateTime.now();

            Map<String, Object> data = new HashMap<>();
            int columnCount = rs.getMetaData().getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = rs.getMetaData().getColumnName(i);
                Object value = rs.getObject(i);
                data.put(columnName, value);
            }

            return new SourceRecord(
                id,
                "UPDATE", // 增量读取默认为 UPDATE 类型
                timestamp,
                data,
                "RDB",
                1L
            );
        }
    }

    /**
     * 基于变更表的行映射器
     */
    private class ChangeTableRowMapper implements RowMapper<SourceRecord> {
        @Override
        public SourceRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            String id = rs.getString(idColumn);
            LocalDateTime timestamp = rs.getTimestamp(timeColumn) != null 
                ? rs.getTimestamp(timeColumn).toLocalDateTime() 
                : LocalDateTime.now();
            String changeType = rs.getString("change_type");

            Map<String, Object> data = new HashMap<>();
            int columnCount = rs.getMetaData().getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = rs.getMetaData().getColumnName(i);
                Object value = rs.getObject(i);
                data.put(columnName, value);
            }

            return new SourceRecord(
                id,
                changeType != null ? changeType : "UPDATE",
                timestamp,
                data,
                "RDB",
                1L
            );
        }
    }
}
