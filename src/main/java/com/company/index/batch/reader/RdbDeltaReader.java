package com.company.index.batch.reader;

import com.company.index.common.model.SourceRecord;
import com.company.index.common.service.FieldMappingService;
import com.company.index.common.util.DynamicSqlBuilder;
import com.company.index.config.FieldMappingConfig.*;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
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
 * RDB 增量数据读取器
 * 使用字段映射配置动态生成 SQL 和处理数据
 */
@Component
public class RdbDeltaReader {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private FieldMappingService fieldMappingService;

    @Value("${index.dataSource.table:sample_data}")
    private String tableName;

    @Value("${index.dataSource.deltaTable:sample_data_changelog}")
    private String deltaTableName;

    @Value("${index.dataSource.idColumn:id}")
    private String idColumn;

    @Value("${index.dataSource.timeColumn:updated_at}")
    private String timeColumn;

    /**
     * 创建基于时间戳的增量读取器
     * 使用字段映射配置动态生成 SQL
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param overlapMinutes 重叠窗口（分钟），用于处理迟到数据
     */
    public JdbcCursorItemReader<SourceRecord> createTimeBasedReader(LocalDateTime startTime, 
                                                                   LocalDateTime endTime, 
                                                                   int overlapMinutes) {
        try {
            // 获取字段映射配置
            List<FieldConfig> mainFields = fieldMappingService.getMainFields(tableName);
            List<JoinConfig> joins = fieldMappingService.getJoinConfigs(tableName);
            FieldConfig keyField = fieldMappingService.getKeyField(tableName);

            if (mainFields == null || mainFields.isEmpty()) {
                System.err.println("警告：表 " + tableName + " 未配置字段映射，使用默认 SELECT *");
                return createDefaultTimeBasedReader(startTime, endTime, overlapMinutes);
            }

            // 计算重叠窗口的开始时间
            LocalDateTime actualStartTime = startTime.minusMinutes(overlapMinutes);

            // 使用 DynamicSqlBuilder 生成 SQL
            String sql = DynamicSqlBuilder.buildDeltaQuerySql(
                tableName, 
                mainFields, 
                joins, 
                timeColumn, 
                keyField
            );

            System.out.println("========================================");
            System.out.println("创建增量读取器（时间范围）:");
            System.out.println("  表名: " + tableName);
            System.out.println("  时间范围: " + actualStartTime + " ~ " + endTime);
            System.out.println("  重叠窗口: " + overlapMinutes + " 分钟");
            System.out.println("  生成的 SQL: " + sql);
            System.out.println("========================================");

            return new JdbcCursorItemReaderBuilder<SourceRecord>()
                    .name("rdbDeltaTimeReader")
                    .dataSource(dataSource)
                    .sql(sql)
                    .preparedStatementSetter(ps -> {
                        ps.setObject(1, actualStartTime);
                        ps.setObject(2, endTime);
                    })
                    .rowMapper(new DynamicDeltaRecordRowMapper(mainFields, joins))
                    .build();

        } catch (Exception e) {
            System.err.println("创建增量读取器失败，使用默认配置: " + e.getMessage());
            return createDefaultTimeBasedReader(startTime, endTime, overlapMinutes);
        }
    }

    /**
     * 创建基于变更表的增量读取器
     * （保留原有功能，可选实现）
     */
    public JdbcCursorItemReader<SourceRecord> createChangeLogReader(LocalDateTime since) {
        // 这里可以实现基于变更日志表的读取
        // 暂时使用默认实现
        return createDefaultChangeLogReader(since);
    }

    /**
     * 创建默认时间范围读取器（未配置字段映射时使用）
     */
    private JdbcCursorItemReader<SourceRecord> createDefaultTimeBasedReader(
            LocalDateTime startTime, LocalDateTime endTime, int overlapMinutes) {
        
        LocalDateTime actualStartTime = startTime.minusMinutes(overlapMinutes);
        
        String sql = String.format(
            "SELECT * FROM %s WHERE %s >= ? AND %s <= ? ORDER BY %s",
            tableName, timeColumn, timeColumn, timeColumn
        );

        return new JdbcCursorItemReaderBuilder<SourceRecord>()
                .name("rdbDeltaTimeReader")
                .dataSource(dataSource)
                .sql(sql)
                .preparedStatementSetter(ps -> {
                    ps.setObject(1, actualStartTime);
                    ps.setObject(2, endTime);
                })
                .rowMapper(new DefaultDeltaRecordRowMapper())
                .build();
    }

    /**
     * 创建默认变更日志读取器
     */
    private JdbcCursorItemReader<SourceRecord> createDefaultChangeLogReader(LocalDateTime since) {
        String sql = String.format(
            "SELECT * FROM %s WHERE %s >= ? ORDER BY %s",
            deltaTableName, timeColumn, timeColumn
        );

        return new JdbcCursorItemReaderBuilder<SourceRecord>()
                .name("rdbDeltaChangeLogReader")
                .dataSource(dataSource)
                .sql(sql)
                .preparedStatementSetter(ps -> ps.setObject(1, since))
                .rowMapper(new DefaultDeltaRecordRowMapper())
                .build();
    }

    /**
     * 动态增量行映射器
     */
    private class DynamicDeltaRecordRowMapper implements RowMapper<SourceRecord> {
        private final List<FieldConfig> mainFields;
        private final List<JoinConfig> joins;

        public DynamicDeltaRecordRowMapper(List<FieldConfig> mainFields, List<JoinConfig> joins) {
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
                "UPDATE", // 增量读取默认为 UPDATE 类型
                timestamp,
                data,
                "RDB:" + tableName + ":DELTA",
                1L
            );
        }
    }

    /**
     * 默认增量行映射器
     */
    private class DefaultDeltaRecordRowMapper implements RowMapper<SourceRecord> {
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
                "UPDATE", // 增量读取默认为 UPDATE 类型
                timestamp,
                data,
                "RDB:" + tableName + ":DELTA",
                1L
            );
        }
    }
}
