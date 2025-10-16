package com.company.index.common.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 动态数据记录 - 基于配置的通用数据模型
 * 替代固定的 SourceRecord 和 IndexDocument
 */
public class DynamicRecord {
    
    /**
     * 记录类型：SOURCE（源数据）、INDEX（索引数据）
     */
    public enum RecordType {
        SOURCE,  // 源数据记录
        INDEX    // 索引数据记录
    }
    
    /**
     * 操作类型：INSERT、UPDATE、DELETE
     */
    public enum OperationType {
        INSERT,
        UPDATE,
        DELETE
    }
    
    // 元数据字段
    private RecordType recordType;           // 记录类型
    private String tableName;                // 表名或索引名
    private OperationType operation;         // 操作类型
    private LocalDateTime timestamp;         // 时间戳
    private String source;                   // 数据来源
    private Long version;                    // 版本号
    
    // 动态字段数据（根据配置）
    private Map<String, Object> fields;      // 所有字段数据
    private Map<String, String> fieldTypes;  // 字段类型映射
    
    // 默认构造函数
    public DynamicRecord() {
        this.fields = new HashMap<>();
        this.fieldTypes = new HashMap<>();
    }
    
    // 完整构造函数
    public DynamicRecord(RecordType recordType, String tableName, OperationType operation) {
        this();
        this.recordType = recordType;
        this.tableName = tableName;
        this.operation = operation;
        this.timestamp = LocalDateTime.now();
        this.version = 1L;
    }
    
    // ===== 字段操作方法 =====
    
    /**
     * 设置字段值
     */
    public void setField(String fieldName, Object value) {
        this.fields.put(fieldName, value);
    }
    
    /**
     * 设置字段值和类型
     */
    public void setField(String fieldName, Object value, String type) {
        this.fields.put(fieldName, value);
        this.fieldTypes.put(fieldName, type);
    }
    
    /**
     * 获取字段值
     */
    public Object getField(String fieldName) {
        return this.fields.get(fieldName);
    }
    
    /**
     * 获取字段值（带类型转换）
     */
    @SuppressWarnings("unchecked")
    public <T> T getField(String fieldName, Class<T> type) {
        Object value = this.fields.get(fieldName);
        if (value == null) {
            return null;
        }
        return (T) value;
    }
    
    /**
     * 获取字段类型
     */
    public String getFieldType(String fieldName) {
        return this.fieldTypes.get(fieldName);
    }
    
    /**
     * 检查是否有某个字段
     */
    public boolean hasField(String fieldName) {
        return this.fields.containsKey(fieldName);
    }
    
    /**
     * 获取主键字段值（通常是 id 或配置的 key 字段）
     */
    public String getKeyValue() {
        // 尝试常见的主键字段名
        Object id = fields.get("id");
        if (id != null) {
            return String.valueOf(id);
        }
        
        Object orderId = fields.get("order_id");
        if (orderId != null) {
            return String.valueOf(orderId);
        }
        
        // 返回第一个字段值作为降级方案
        return fields.isEmpty() ? null : String.valueOf(fields.values().iterator().next());
    }
    
    /**
     * 批量设置字段和类型
     */
    public void setFieldsWithTypes(Map<String, Object> fields, Map<String, String> types) {
        if (fields != null) {
            this.fields.putAll(fields);
        }
        if (types != null) {
            this.fieldTypes.putAll(types);
        }
    }
    
    // ===== Getters and Setters =====
    
    public RecordType getRecordType() {
        return recordType;
    }
    
    public void setRecordType(RecordType recordType) {
        this.recordType = recordType;
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
    
    public OperationType getOperation() {
        return operation;
    }
    
    public void setOperation(OperationType operation) {
        this.operation = operation;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public Long getVersion() {
        return version;
    }
    
    public void setVersion(Long version) {
        this.version = version;
    }
    
    public Map<String, Object> getFields() {
        return fields;
    }
    
    public void setFields(Map<String, Object> fields) {
        this.fields = fields;
    }
    
    public Map<String, String> getFieldTypes() {
        return fieldTypes;
    }
    
    public void setFieldTypes(Map<String, String> fieldTypes) {
        this.fieldTypes = fieldTypes;
    }
    
    // ===== equals, hashCode, toString =====
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DynamicRecord that = (DynamicRecord) o;
        return recordType == that.recordType &&
                Objects.equals(tableName, that.tableName) &&
                operation == that.operation &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(source, that.source) &&
                Objects.equals(version, that.version) &&
                Objects.equals(fields, that.fields);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(recordType, tableName, operation, timestamp, source, version, fields);
    }
    
    @Override
    public String toString() {
        return "DynamicRecord{" +
                "recordType=" + recordType +
                ", tableName='" + tableName + '\'' +
                ", operation=" + operation +
                ", timestamp=" + timestamp +
                ", source='" + source + '\'' +
                ", version=" + version +
                ", fields=" + fields +
                ", fieldTypes=" + fieldTypes +
                '}';
    }
    
    // ===== 便利工厂方法 =====
    
    /**
     * 创建源数据记录
     */
    public static DynamicRecord createSourceRecord(String tableName) {
        return new DynamicRecord(RecordType.SOURCE, tableName, OperationType.INSERT);
    }
    
    /**
     * 创建索引数据记录
     */
    public static DynamicRecord createIndexRecord(String indexName) {
        return new DynamicRecord(RecordType.INDEX, indexName, OperationType.INSERT);
    }
    
    /**
     * 从源记录转换为索引记录
     */
    public DynamicRecord toIndexRecord(String indexName) {
        DynamicRecord indexRecord = new DynamicRecord(RecordType.INDEX, indexName, this.operation);
        indexRecord.setTimestamp(this.timestamp);
        indexRecord.setSource(this.source);
        indexRecord.setVersion(this.version);
        indexRecord.setFields(new HashMap<>(this.fields));  // 复制字段
        indexRecord.setFieldTypes(new HashMap<>(this.fieldTypes));  // 复制类型
        return indexRecord;
    }
}

