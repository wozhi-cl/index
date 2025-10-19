package com.company.index.common.service;

import com.company.index.common.model.SourceRecord;
import com.company.index.common.model.IndexDocument;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 记录构建服务 - 使用反射自动映射字段
 */
@Service
public class RecordBuilderService {
    
    /**
     * 从 ResultSet 构建源数据记录
     * 使用反射自动映射同名字段
     * 
     * @param rs ResultSet
     * @param tableName 表名
     * @return 源记录
     */
    public SourceRecord buildSourceRecordFromResultSet(ResultSet rs, String tableName) throws SQLException {
        SourceRecord record = new SourceRecord();
        
        // 读取 ResultSet 的所有列
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        // 获取 SourceRecord 的所有字段
        Map<String, Field> fieldMap = getFieldMap(SourceRecord.class);
        
        for (int i = 1; i <= columnCount; i++) {
            String columnLabel = metaData.getColumnLabel(i);
            Object value = rs.getObject(i);
            
            // 自动映射字段
            autoMapField(record, fieldMap, columnLabel, value);
        }
        
        // 设置元数据
        setMetadata(record, tableName);
        
        return record;
    }
    
    /**
     * 从 Map 构建源数据记录
     * 使用反射自动映射同名字段
     * 
     * @param data Map 数据
     * @param tableName 表名
     * @return 源记录
     */
    public SourceRecord buildSourceRecordFromMap(Map<String, Object> data, String tableName) {
        SourceRecord record = new SourceRecord();
        
        // 获取 SourceRecord 的所有字段
        Map<String, Field> fieldMap = getFieldMap(SourceRecord.class);
        
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();
            
            // 自动映射字段
            autoMapField(record, fieldMap, fieldName, value);
        }
        
        // 设置元数据
        setMetadata(record, tableName);
        
        return record;
    }
    
    /**
     * 将源记录转换为索引文档
     * 使用反射自动映射同名字段
     * 
     * @param sourceRecord 源记录
     * @param indexName 索引名
     * @return 索引文档
     */
    public IndexDocument convertToIndexDocument(SourceRecord sourceRecord, String indexName) {
        IndexDocument doc = new IndexDocument();
        
        // 获取两个类的字段映射
        Map<String, Field> sourceFields = getFieldMap(SourceRecord.class);
        Map<String, Field> indexFields = getFieldMap(IndexDocument.class);
        
        // 自动映射同名字段
        for (Map.Entry<String, Field> entry : sourceFields.entrySet()) {
            String fieldName = entry.getKey();
            Field sourceField = entry.getValue();
            
            // 跳过元数据字段
            if (isMetadataField(fieldName)) {
                continue;
            }
            
            try {
                Object value = sourceField.get(sourceRecord);
                if (value != null) {
                    // 尝试直接映射同名字段
                    Field targetField = indexFields.get(fieldName);
                    if (targetField != null && isCompatibleType(sourceField.getType(), targetField.getType())) {
                        targetField.set(doc, value);
                        continue;
                    }
                    
                    // 特殊字段映射
                    mapSpecialFieldConversion(doc, fieldName, value);
                }
            } catch (IllegalAccessException e) {
                System.err.println("Failed to access field " + fieldName + ": " + e.getMessage());
            }
        }
        
        // 设置元数据
        doc.setOperation(IndexDocument.OperationType.valueOf(sourceRecord.getOperation().name()));
        doc.setTimestamp(sourceRecord.getTimestamp());
        doc.setSource(sourceRecord.getSource());
        doc.setVersion(sourceRecord.getVersion());
        
        return doc;
    }
    
    /**
     * 将索引文档转换为 Map
     * 使用反射自动获取所有字段
     * 
     * @param indexDocument 索引文档
     * @return Map 数据
     */
    public Map<String, Object> convertIndexDocumentToMap(IndexDocument indexDocument) {
        Map<String, Object> map = new HashMap<>();
        Map<String, Field> fieldMap = getFieldMap(IndexDocument.class);
        
        for (Map.Entry<String, Field> entry : fieldMap.entrySet()) {
            String fieldName = entry.getKey();
            Field field = entry.getValue();
            
            try {
                Object value = field.get(indexDocument);
                map.put(fieldName, value);
            } catch (IllegalAccessException e) {
                System.err.println("Failed to access field " + fieldName + ": " + e.getMessage());
            }
        }
        
        return map;
    }
    
    /**
     * 验证源记录
     * 
     * @param sourceRecord 源记录
     * @return 是否有效
     */
    public boolean validateSourceRecord(SourceRecord sourceRecord) {
        return sourceRecord != null && 
               sourceRecord.getId() != null && 
               sourceRecord.getOrderNo() != null && 
               sourceRecord.getAmount() != null;
    }
    
    /**
     * 获取类的字段映射
     * 
     * @param clazz 类
     * @return 字段名到字段对象的映射
     */
    private Map<String, Field> getFieldMap(Class<?> clazz) {
        Map<String, Field> fieldMap = new HashMap<>();
        
        // 获取所有字段（包括父类）
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            Field[] fields = currentClass.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                fieldMap.put(field.getName(), field);
            }
            currentClass = currentClass.getSuperclass();
        }
        
        return fieldMap;
    }
    
    /**
     * 自动映射字段
     * 
     * @param target 目标对象
     * @param fieldMap 字段映射
     * @param fieldName 字段名
     * @param value 值
     * @return 是否映射成功
     */
    private boolean autoMapField(Object target, Map<String, Field> fieldMap, String fieldName, Object value) {
        Field field = fieldMap.get(fieldName);
        if (field != null) {
            try {
                Object convertedValue = convertValue(value, field.getType());
                field.set(target, convertedValue);
                return true;
            } catch (Exception e) {
                System.err.println("Failed to map field " + fieldName + ": " + e.getMessage());
            }
        }
        return false;
    }
    
    /**
     * 特殊字段转换映射
     * 
     * @param doc 索引文档
     * @param fieldName 字段名
     * @param value 值
     */
    private void mapSpecialFieldConversion(IndexDocument doc, String fieldName, Object value) {
        switch (fieldName) {
            case "id":
                doc.setOrderId(convertToLong(value));
                break;
            case "createdAt":
                doc.setOrderTime(convertToLocalDateTime(value));
                break;
        }
    }
    
    /**
     * 设置元数据
     * 
     * @param record 记录
     * @param tableName 表名
     */
    private void setMetadata(SourceRecord record, String tableName) {
        record.setSource("RDB:" + tableName);
        record.setVersion(1L);
        record.setOperation(SourceRecord.OperationType.INSERT);
        if (record.getCreatedAt() != null) {
            record.setTimestamp(record.getCreatedAt());
        } else {
        record.setTimestamp(LocalDateTime.now());
        }
    }
    
    /**
     * 判断是否为元数据字段
     * 
     * @param fieldName 字段名
     * @return 是否为元数据字段
     */
    private boolean isMetadataField(String fieldName) {
        return "operation".equals(fieldName) || 
               "timestamp".equals(fieldName) || 
               "source".equals(fieldName) || 
               "version".equals(fieldName);
    }
    
    /**
     * 判断类型是否兼容
     * 
     * @param sourceType 源类型
     * @param targetType 目标类型
     * @return 是否兼容
     */
    private boolean isCompatibleType(Class<?> sourceType, Class<?> targetType) {
        return sourceType.equals(targetType) || 
               targetType.isAssignableFrom(sourceType);
    }
    
    /**
     * 转换值到指定类型
     * 
     * @param value 原值
     * @param targetType 目标类型
     * @return 转换后的值
     */
    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }
        
        if (targetType == Long.class || targetType == long.class) {
            return convertToLong(value);
        } else if (targetType == Double.class || targetType == double.class) {
            return convertToDouble(value);
        } else if (targetType == String.class) {
            return convertToString(value);
        } else if (targetType == LocalDateTime.class) {
            return convertToLocalDateTime(value);
        }
        
        return value;
    }
    
    /**
     * 转换为 Long
     */
    private Long convertToLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }
    
    /**
     * 转换为 Double
     */
    private Double convertToDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }
    
    /**
     * 转换为 String
     */
    private String convertToString(Object value) {
        return value != null ? value.toString() : null;
    }
    
    /**
     * 转换为 LocalDateTime
     */
    private LocalDateTime convertToLocalDateTime(Object value) {
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        } else if (value instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) value).toLocalDateTime();
        }
        return null;
    }
}