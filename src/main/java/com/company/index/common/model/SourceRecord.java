package com.company.index.common.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 源数据记录 - 固定的数据结构
 * 用于从数据库读取的原始数据
 */
public class SourceRecord {
    
    /**
     * 操作类型：INSERT、UPDATE、DELETE
     */
    public enum OperationType {
        INSERT,
        UPDATE,
        DELETE
    }
    
    // 主键字段
    private Long id;
    
    // 业务字段
    private String orderNo;
    private Double amount;
    private Long userId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 关联字段（来自 JOIN）
    private String userName;
    private String userPhone;
    
    // 元数据字段
    private OperationType operation;
    private LocalDateTime timestamp;
    private String source;
    private Long version;
    
    // 默认构造函数
    public SourceRecord() {
        this.timestamp = LocalDateTime.now();
        this.version = 1L;
        this.operation = OperationType.INSERT;
    }
    
    // 完整构造函数
    public SourceRecord(Long id, String orderNo, Double amount, Long userId, 
                       String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this();
        this.id = id;
        this.orderNo = orderNo;
        this.amount = amount;
        this.userId = userId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    // ===== Getters and Setters =====
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
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
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public String getUserPhone() {
        return userPhone;
    }
    
    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
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
        SourceRecord that = (SourceRecord) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(orderNo, that.orderNo) &&
                Objects.equals(amount, that.amount) &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(status, that.status) &&
                Objects.equals(createdAt, that.createdAt) &&
                Objects.equals(updatedAt, that.updatedAt) &&
                Objects.equals(userName, that.userName) &&
                Objects.equals(userPhone, that.userPhone) &&
                operation == that.operation &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(source, that.source) &&
                Objects.equals(version, that.version);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, orderNo, amount, userId, status, createdAt, updatedAt, 
                           userName, userPhone, operation, timestamp, source, version);
    }
    
    @Override
    public String toString() {
        return "SourceRecord{" +
                "id=" + id +
                ", orderNo='" + orderNo + '\'' +
                ", amount=" + amount +
                ", userId=" + userId +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", userName='" + userName + '\'' +
                ", userPhone='" + userPhone + '\'' +
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
        return id != null ? String.valueOf(id) : null;
    }
    
    /**
     * 转换为索引文档
     */
    public IndexDocument toIndexDocument() {
        IndexDocument doc = new IndexDocument();
        doc.setOrderId(this.id);
        doc.setOrderNo(this.orderNo);
        doc.setAmount(this.amount);
        doc.setUserName(this.userName);
        doc.setOrderTime(this.createdAt);
        doc.setUpdatedAt(this.updatedAt);
        doc.setOperation(IndexDocument.OperationType.valueOf(this.operation.name()));
        doc.setTimestamp(this.timestamp);
        doc.setSource(this.source);
        doc.setVersion(this.version);
        return doc;
    }
}
