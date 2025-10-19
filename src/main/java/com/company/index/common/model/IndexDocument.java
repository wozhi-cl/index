package com.company.index.common.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 索引文档 - 固定的数据结构
 * 用于写入到 Elasticsearch 或其他搜索引擎的数据
 */
public class IndexDocument {
    
    /**
     * 操作类型：INSERT、UPDATE、DELETE
     */
    public enum OperationType {
        INSERT,
        UPDATE,
        DELETE
    }
    
    // 主键字段
    private Long orderId;
    
    // 业务字段
    private String orderNo;
    private Double amount;
    private String userName;
    private LocalDateTime orderTime;
    private LocalDateTime updatedAt;
    
    // 元数据字段
    private OperationType operation;
    private LocalDateTime timestamp;
    private String source;
    private Long version;
    
    // 默认构造函数
    public IndexDocument() {
        this.timestamp = LocalDateTime.now();
        this.version = 1L;
        this.operation = OperationType.INSERT;
    }
    
    // 完整构造函数
    public IndexDocument(Long orderId, String orderNo, Double amount, 
                        String userName, LocalDateTime orderTime, LocalDateTime updatedAt) {
        this();
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.amount = amount;
        this.userName = userName;
        this.orderTime = orderTime;
        this.updatedAt = updatedAt;
    }
    
    // ===== Getters and Setters =====
    
    public Long getOrderId() {
        return orderId;
    }
    
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
    
    public String getOrderNo() {
        return orderNo;
    }
    
    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }
    
    public Double getAmount() {
        return amount;
    }
    
    public void setAmount(Double amount) {
        this.amount = amount;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public LocalDateTime getOrderTime() {
        return orderTime;
    }
    
    public void setOrderTime(LocalDateTime orderTime) {
        this.orderTime = orderTime;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public OperationType getOperation() {
        return operation;
    }
    
    public OperationType getOperationType() {
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
    
    // ===== equals, hashCode, toString =====
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IndexDocument that = (IndexDocument) o;
        return Objects.equals(orderId, that.orderId) &&
                Objects.equals(orderNo, that.orderNo) &&
                Objects.equals(amount, that.amount) &&
                Objects.equals(userName, that.userName) &&
                Objects.equals(orderTime, that.orderTime) &&
                Objects.equals(updatedAt, that.updatedAt) &&
                operation == that.operation &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(source, that.source) &&
                Objects.equals(version, that.version);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(orderId, orderNo, amount, userName, orderTime, updatedAt, 
                           operation, timestamp, source, version);
    }
    
    @Override
    public String toString() {
        return "IndexDocument{" +
                "orderId=" + orderId +
                ", orderNo='" + orderNo + '\'' +
                ", amount=" + amount +
                ", userName='" + userName + '\'' +
                ", orderTime=" + orderTime +
                ", updatedAt=" + updatedAt +
                ", operation=" + operation +
                ", timestamp=" + timestamp +
                ", source='" + source + '\'' +
                ", version=" + version +
                '}';
    }
    
    // ===== 便利方法 =====
    
    /**
     * 获取主键值
     */
    public String getKeyValue() {
        return orderId != null ? String.valueOf(orderId) : null;
    }
    
    /**
     * 从源记录创建索引文档
     */
    public static IndexDocument fromSourceRecord(SourceRecord sourceRecord) {
        IndexDocument doc = new IndexDocument();
        doc.setOrderId(sourceRecord.getId());
        doc.setOrderNo(sourceRecord.getOrderNo());
        doc.setAmount(sourceRecord.getAmount());
        doc.setUserName(sourceRecord.getUserName());
        doc.setOrderTime(sourceRecord.getCreatedAt());
        doc.setUpdatedAt(sourceRecord.getUpdatedAt());
        doc.setOperation(OperationType.valueOf(sourceRecord.getOperation().name()));
        doc.setTimestamp(sourceRecord.getTimestamp());
        doc.setSource(sourceRecord.getSource());
        doc.setVersion(sourceRecord.getVersion());
        return doc;
    }
}
