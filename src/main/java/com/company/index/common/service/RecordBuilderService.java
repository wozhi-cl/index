package com.company.index.common.service;

import com.company.index.common.model.DynamicRecord;
import com.company.index.config.FieldMappingConfig.FieldConfig;
import com.company.index.config.FieldMappingConfig.IndexFieldConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 记录构建服务 - 根据配置构建动态记录
 */
@Service
public class RecordBuilderService {
    
    @Autowired
    private FieldMappingService fieldMappingService;
    
    /**
     * 从 ResultSet 构建源数据记录
     * 
     * @param rs ResultSet
     * @param tableName 表名
     * @return 动态源记录
     */
    public DynamicRecord buildSourceRecordFromResultSet(ResultSet rs, String tableName) throws SQLException {
        DynamicRecord record = DynamicRecord.createSourceRecord(tableName);
        
        // 获取字段配置
        List<FieldConfig> fieldConfigs = fieldMappingService.getMainFields(tableName);
        FieldConfig keyField = fieldMappingService.getKeyField(tableName);
        FieldConfig timeField = fieldMappingService.getTimeColumn(tableName);
        
        // 读取 ResultSet 的所有列
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        Map<String, Object> fields = new HashMap<>();
        Map<String, String> types = new HashMap<>();
        
        for (int i = 1; i <= columnCount; i++) {
            String columnLabel = metaData.getColumnLabel(i);  // 使用别名
            Object value = rs.getObject(i);
            String typeName = metaData.getColumnTypeName(i);
            
            fields.put(columnLabel, value);
            types.put(columnLabel, typeName);
        }
        
        // 设置字段数据
        record.setFieldsWithTypes(fields, types);
        
        // 设置元数据
        record.setSource("RDB:" + tableName);
        record.setVersion(1L);
        record.setOperation(DynamicRecord.OperationType.INSERT);  // 全量读取默认为 INSERT
        
        // 设置时间戳
        if (timeField != null) {
            Object tsValue = fields.get(timeField.getAlias() != null ? timeField.getAlias() : timeField.getName());
            if (tsValue instanceof java.sql.Timestamp) {
                record.setTimestamp(((java.sql.Timestamp) tsValue).toLocalDateTime());
            } else if (tsValue instanceof LocalDateTime) {
                record.setTimestamp((LocalDateTime) tsValue);
            } else {
                record.setTimestamp(LocalDateTime.now());
            }
        } else {
            record.setTimestamp(LocalDateTime.now());
        }
        
        return record;
    }
    
    /**
     * 从源记录转换为索引记录
     * 
     * @param sourceRecord 源记录
     * @param indexName 索引名称
     * @return 索引记录
     */
    public DynamicRecord convertToIndexRecord(DynamicRecord sourceRecord, String indexName) {
        // 获取索引字段配置
        List<IndexFieldConfig> indexFieldConfigs = fieldMappingService.getIndexFields(indexName);
        
        // 创建索引记录
        DynamicRecord indexRecord = DynamicRecord.createIndexRecord(indexName);
        indexRecord.setOperation(sourceRecord.getOperation());
        indexRecord.setTimestamp(sourceRecord.getTimestamp());
        indexRecord.setSource(sourceRecord.getSource());
        indexRecord.setVersion(sourceRecord.getVersion());
        
        // 根据索引字段配置映射字段
        Map<String, Object> indexFields = new HashMap<>();
        Map<String, String> indexTypes = new HashMap<>();
        
        for (IndexFieldConfig indexFieldConfig : indexFieldConfigs) {
            String indexFieldName = indexFieldConfig.getName();
            String sourceFieldName = indexFieldConfig.getSourceField();
            String indexFieldType = indexFieldConfig.getType();
            
            // 从源记录获取字段值
            Object value = sourceRecord.getField(sourceFieldName);
            
            if (value != null) {
                indexFields.put(indexFieldName, value);
                indexTypes.put(indexFieldName, indexFieldType);
            }
        }
        
        indexRecord.setFieldsWithTypes(indexFields, indexTypes);
        
        return indexRecord;
    }
    
    /**
     * 从 Map 构建源记录（用于文件读取等场景）
     * 
     * @param data 数据 Map
     * @param tableName 表名
     * @return 源记录
     */
    public DynamicRecord buildSourceRecordFromMap(Map<String, Object> data, String tableName) {
        DynamicRecord record = DynamicRecord.createSourceRecord(tableName);
        record.setFields(data);
        record.setSource("FILE:" + tableName);
        record.setVersion(1L);
        record.setTimestamp(LocalDateTime.now());
        return record;
    }
    
    /**
     * 将动态记录转换为 Map（用于写入）
     * 
     * @param record 动态记录
     * @return Map 数据
     */
    public Map<String, Object> convertRecordToMap(DynamicRecord record) {
        Map<String, Object> result = new HashMap<>(record.getFields());
        
        // 添加元数据字段
        result.put("_id", record.getKeyValue());
        result.put("_operation", record.getOperation().name());
        result.put("_timestamp", record.getTimestamp());
        result.put("_source", record.getSource());
        result.put("_version", record.getVersion());
        
        return result;
    }
    
    /**
     * 验证记录字段完整性
     * 
     * @param record 动态记录
     * @param tableName 表名
     * @return 是否有效
     */
    public boolean validateRecord(DynamicRecord record, String tableName) {
        List<FieldConfig> fieldConfigs = fieldMappingService.getMainFields(tableName);
        
        for (FieldConfig fieldConfig : fieldConfigs) {
            // 检查必填字段
            if (fieldConfig.getRequired() != null && fieldConfig.getRequired()) {
                String fieldName = fieldConfig.getAlias() != null ? fieldConfig.getAlias() : fieldConfig.getName();
                if (!record.hasField(fieldName) || record.getField(fieldName) == null) {
                    System.err.println("缺少必填字段: " + fieldName);
                    return false;
                }
            }
        }
        
        return true;
    }
}

