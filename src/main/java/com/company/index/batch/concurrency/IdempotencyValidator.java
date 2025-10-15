package com.company.index.batch.concurrency;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 幂等性验证器
 * 确保任务重复执行不会产生副作用
 */
@Component
public class IdempotencyValidator {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${index.idempotency.validation.enabled:true}")
    private boolean validationEnabled;

    @Value("${index.idempotency.validation.timeout:3600}")
    private int validationTimeout;

    // 本地幂等性缓存
    private final Map<String, IdempotencyRecord> idempotencyCache = new ConcurrentHashMap<>();

    /**
     * 验证操作是否幂等
     */
    public boolean isIdempotent(String operationId, String operationType, String instanceId) {
        if (!validationEnabled) {
            return true;
        }

        // 检查本地缓存
        IdempotencyRecord localRecord = idempotencyCache.get(operationId);
        if (localRecord != null && localRecord.isCompleted()) {
            System.out.println("Operation " + operationId + " already completed locally");
            return true;
        }

        // 检查数据库记录
        if (jdbcTemplate != null) {
            return checkDatabaseIdempotency(operationId, operationType);
        }

        return true;
    }

    /**
     * 记录操作开始
     */
    public void recordOperationStart(String operationId, String operationType, String instanceId) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setOperationId(operationId);
        record.setOperationType(operationType);
        record.setInstanceId(instanceId);
        record.setStartTime(LocalDateTime.now());
        record.setStatus(OperationStatus.IN_PROGRESS);

        idempotencyCache.put(operationId, record);

        // 记录到数据库
        if (jdbcTemplate != null) {
            recordDatabaseOperation(operationId, operationType, instanceId, OperationStatus.IN_PROGRESS);
        }

        System.out.println("Recorded operation start: " + operationId + " (type: " + operationType + ")");
    }

    /**
     * 记录操作完成
     */
    public void recordOperationComplete(String operationId, String instanceId) {
        IdempotencyRecord record = idempotencyCache.get(operationId);
        if (record != null && record.getInstanceId().equals(instanceId)) {
            record.setStatus(OperationStatus.COMPLETED);
            record.setEndTime(LocalDateTime.now());
        }

        // 更新数据库记录
        if (jdbcTemplate != null) {
            updateDatabaseOperation(operationId, instanceId, OperationStatus.COMPLETED);
        }

        System.out.println("Recorded operation completion: " + operationId);
    }

    /**
     * 记录操作失败
     */
    public void recordOperationFailure(String operationId, String instanceId, String error) {
        IdempotencyRecord record = idempotencyCache.get(operationId);
        if (record != null && record.getInstanceId().equals(instanceId)) {
            record.setStatus(OperationStatus.FAILED);
            record.setEndTime(LocalDateTime.now());
            record.setError(error);
        }

        // 更新数据库记录
        if (jdbcTemplate != null) {
            updateDatabaseOperation(operationId, instanceId, OperationStatus.FAILED);
        }

        System.out.println("Recorded operation failure: " + operationId + " - " + error);
    }

    /**
     * 检查数据库幂等性
     */
    private boolean checkDatabaseIdempotency(String operationId, String operationType) {
        try {
            String sql = "SELECT status FROM idempotency_records WHERE operation_id = ? AND operation_type = ? AND status = 'COMPLETED'";
            String status = jdbcTemplate.queryForObject(sql, String.class, operationId, operationType);
            return status != null && "COMPLETED".equals(status);
        } catch (Exception e) {
            // 如果查询失败，假设操作可以执行
            return false;
        }
    }

    /**
     * 记录数据库操作
     */
    private void recordDatabaseOperation(String operationId, String operationType, String instanceId, OperationStatus status) {
        try {
            String sql = "INSERT INTO idempotency_records (operation_id, operation_type, instance_id, status, start_time) VALUES (?, ?, ?, ?, NOW()) " +
                        "ON DUPLICATE KEY UPDATE status = VALUES(status), start_time = NOW()";
            jdbcTemplate.update(sql, operationId, operationType, instanceId, status.name());
        } catch (Exception e) {
            System.err.println("Failed to record database operation: " + e.getMessage());
        }
    }

    /**
     * 更新数据库操作
     */
    private void updateDatabaseOperation(String operationId, String instanceId, OperationStatus status) {
        try {
            String sql = "UPDATE idempotency_records SET status = ?, end_time = NOW() WHERE operation_id = ? AND instance_id = ?";
            jdbcTemplate.update(sql, status.name(), operationId, instanceId);
        } catch (Exception e) {
            System.err.println("Failed to update database operation: " + e.getMessage());
        }
    }

    /**
     * 清理过期记录
     */
    public void cleanupExpiredRecords() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
        
        // 清理本地缓存
        idempotencyCache.entrySet().removeIf(entry -> {
            IdempotencyRecord record = entry.getValue();
            return record.getEndTime() != null && record.getEndTime().isBefore(cutoffTime);
        });

        // 清理数据库记录
        if (jdbcTemplate != null) {
            try {
                String sql = "DELETE FROM idempotency_records WHERE end_time < ?";
                jdbcTemplate.update(sql, cutoffTime);
            } catch (Exception e) {
                System.err.println("Failed to cleanup database records: " + e.getMessage());
            }
        }
    }

    /**
     * 获取操作状态
     */
    public OperationStatus getOperationStatus(String operationId) {
        IdempotencyRecord record = idempotencyCache.get(operationId);
        if (record == null) {
            return OperationStatus.UNKNOWN;
        }
        return record.getStatus();
    }

    /**
     * 幂等性记录模型
     */
    public static class IdempotencyRecord {
        private String operationId;
        private String operationType;
        private String instanceId;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private OperationStatus status;
        private String error;

        // Getters and Setters
        public String getOperationId() { return operationId; }
        public void setOperationId(String operationId) { this.operationId = operationId; }

        public String getOperationType() { return operationType; }
        public void setOperationType(String operationType) { this.operationType = operationType; }

        public String getInstanceId() { return instanceId; }
        public void setInstanceId(String instanceId) { this.instanceId = instanceId; }

        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

        public OperationStatus getStatus() { return status; }
        public void setStatus(OperationStatus status) { this.status = status; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }

        public boolean isCompleted() {
            return status == OperationStatus.COMPLETED;
        }
    }

    /**
     * 操作状态枚举
     */
    public enum OperationStatus {
        UNKNOWN,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }
}
