package com.company.index.batch.reader;

import com.company.index.common.model.DynamicRecord;
import com.company.index.common.service.FieldMappingService;
import com.company.index.common.service.RecordBuilderService;
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
import java.util.List;

/**
 * 动态 RDB 增量数据读取器
 * 使用 DynamicRecord 和字段映射配置
 */
@Component
public class DynamicRdbDeltaReader {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private FieldMappingService fieldMappingService;

    @Autowired
    private RecordBuilderService recordBuilderService;

    @Value("${index.dataSource.table:orders}")
    private String tableName;

    @Value("${index.incremental.overlapMinutes:5}")
    private int overlapMinutes;

    /**
     * 创建增量游标读取器
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 游标读取器
     */
    public JdbcCursorItemReader<DynamicRecord> createDeltaReader(LocalDateTime startTime, LocalDateTime endTime) {
        try {
            // 获取字段映射配置
            List<FieldConfig> mainFields = fieldMappingService.getMainFields(tableName);
            List<JoinConfig> joins = fieldMappingService.getJoinConfigs(tableName);
            FieldConfig keyField = fieldMappingService.getKeyField(tableName);
            FieldConfig timeField = fieldMappingService.getTimeColumn(tableName);
            
            if (mainFields == null || mainFields.isEmpty()) {
                throw new IllegalStateException("表 " + tableName + " 未配置字段映射");
            }
            
            if (timeField == null) {
                throw new IllegalStateException("表 " + tableName + " 未配置时间字段，无法执行增量索引");
            }

            // 添加时间重叠以防遗漏
            LocalDateTime adjustedStartTime = startTime.minusMinutes(overlapMinutes);
            
            // 使用 DynamicSqlBuilder 构建增量查询 SQL
            String deltaSql = DynamicSqlBuilder.buildDeltaQuerySql(
                tableName, mainFields, joins, timeField.getName(), keyField);

            System.out.println("========================================");
            System.out.println("创建动态增量读取器:");
            System.out.println("  表名: " + tableName);
            System.out.println("  时间字段: " + timeField.getName());
            System.out.println("  时间范围: " + adjustedStartTime + " ~ " + endTime);
            System.out.println("  SQL: " + deltaSql);
            System.out.println("========================================");

            // 创建游标读取器（带参数）
            JdbcCursorItemReader<DynamicRecord> reader = new JdbcCursorItemReaderBuilder<DynamicRecord>()
                    .name("dynamicRdbDeltaCursorReader")
                    .dataSource(dataSource)
                    .sql(deltaSql)
                    .rowMapper(new DynamicRecordRowMapper())
                    .build();
            
            // 设置参数
            reader.setPreparedStatementSetter(ps -> {
                ps.setObject(1, adjustedStartTime);
                ps.setObject(2, endTime);
            });

            return reader;

        } catch (Exception e) {
            System.err.println("创建动态增量读取器失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create dynamic delta reader", e);
        }
    }

    /**
     * 动态记录行映射器
     */
    private class DynamicRecordRowMapper implements RowMapper<DynamicRecord> {
        @Override
        public DynamicRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            DynamicRecord record = recordBuilderService.buildSourceRecordFromResultSet(rs, tableName);
            // 增量读取标记为 UPDATE 操作
            record.setOperation(DynamicRecord.OperationType.UPDATE);
            return record;
        }
    }
}

