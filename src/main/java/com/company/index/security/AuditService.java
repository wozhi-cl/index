package com.company.index.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 审计服务
 * 记录任务触发与执行结果
 */
@Service
public class AuditService {

    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    // 内存中的审计记录（生产环境应使用持久化存储）
    private final Map<String, AuditRecord> auditRecords = new ConcurrentHashMap<>();

    /**
     * 记录任务触发
     */
    public void recordJobTrigger(String jobType, String userId, String clientIp, Map<String, Object> parameters) {
        String auditId = generateAuditId();
        AuditRecord record = new AuditRecord();
        record.setAuditId(auditId);
        record.setEventType("JOB_TRIGGER");
        record.setJobType(jobType);
        record.setUserId(userId);
        record.setClientIp(clientIp);
        record.setParameters(parameters);
        record.setTimestamp(LocalDateTime.now());
        record.setStatus("INITIATED");
        
        auditRecords.put(auditId, record);
        
        // 记录审计日志
        MDC.put("auditId", auditId);
        MDC.put("userId", userId);
        MDC.put("clientIp", clientIp);
        
        auditLogger.info("Job triggered: {} by user: {} from IP: {} with parameters: {}", 
                        jobType, userId, clientIp, parameters);
        
        MDC.clear();
    }

    /**
     * 记录任务执行结果
     */
    public void recordJobExecution(String auditId, String status, String message, long duration) {
        AuditRecord record = auditRecords.get(auditId);
        if (record != null) {
            record.setStatus(status);
            record.setMessage(message);
            record.setDuration(duration);
            record.setCompletedAt(LocalDateTime.now());
            
            // 记录审计日志
            MDC.put("auditId", auditId);
            MDC.put("userId", record.getUserId());
            MDC.put("clientIp", record.getClientIp());
            
            auditLogger.info("Job execution completed: {} status: {} duration: {}ms message: {}", 
                            record.getJobType(), status, duration, message);
            
            MDC.clear();
        }
    }

    /**
     * 记录任务失败
     */
    public void recordJobFailure(String auditId, String error, Exception exception) {
        AuditRecord record = auditRecords.get(auditId);
        if (record != null) {
            record.setStatus("FAILED");
            record.setMessage(error);
            record.setError(exception.getMessage());
            record.setCompletedAt(LocalDateTime.now());
            
            // 记录审计日志
            MDC.put("auditId", auditId);
            MDC.put("userId", record.getUserId());
            MDC.put("clientIp", record.getClientIp());
            
            auditLogger.error("Job execution failed: {} error: {}", record.getJobType(), error, exception);
            
            MDC.clear();
        }
    }

    /**
     * 记录权限检查
     */
    public void recordPermissionCheck(String userId, String resource, String action, boolean granted) {
        String auditId = generateAuditId();
        
        MDC.put("auditId", auditId);
        MDC.put("userId", userId);
        
        auditLogger.info("Permission check: user: {} resource: {} action: {} granted: {}", 
                        userId, resource, action, granted);
        
        MDC.clear();
    }

    /**
     * 记录数据访问
     */
    public void recordDataAccess(String userId, String dataSource, String operation, int recordCount) {
        String auditId = generateAuditId();
        
        MDC.put("auditId", auditId);
        MDC.put("userId", userId);
        
        auditLogger.info("Data access: user: {} source: {} operation: {} records: {}", 
                        userId, dataSource, operation, recordCount);
        
        MDC.clear();
    }

    /**
     * 记录索引操作
     */
    public void recordIndexOperation(String userId, String indexTarget, String operation, int recordCount) {
        String auditId = generateAuditId();
        
        MDC.put("auditId", auditId);
        MDC.put("userId", userId);
        
        auditLogger.info("Index operation: user: {} target: {} operation: {} records: {}", 
                        userId, indexTarget, operation, recordCount);
        
        MDC.clear();
    }

    /**
     * 记录系统配置变更
     */
    public void recordConfigChange(String userId, String configKey, String oldValue, String newValue) {
        String auditId = generateAuditId();
        
        MDC.put("auditId", auditId);
        MDC.put("userId", userId);
        
        auditLogger.info("Config change: user: {} key: {} old: {} new: {}", 
                        userId, configKey, oldValue, newValue);
        
        MDC.clear();
    }

    /**
     * 获取审计记录
     */
    public AuditRecord getAuditRecord(String auditId) {
        return auditRecords.get(auditId);
    }

    /**
     * 获取用户审计记录
     */
    public Map<String, AuditRecord> getUserAuditRecords(String userId) {
        return auditRecords.entrySet().stream()
                .filter(entry -> entry.getValue().getUserId().equals(userId))
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue
                ));
    }

    /**
     * 生成审计 ID
     */
    private String generateAuditId() {
        return "AUDIT-" + System.currentTimeMillis() + "-" + Thread.currentThread().getId();
    }

    /**
     * 审计记录模型
     */
    public static class AuditRecord {
        private String auditId;
        private String eventType;
        private String jobType;
        private String userId;
        private String clientIp;
        private Map<String, Object> parameters;
        private LocalDateTime timestamp;
        private String status;
        private String message;
        private String error;
        private long duration;
        private LocalDateTime completedAt;

        // Getters and Setters
        public String getAuditId() { return auditId; }
        public void setAuditId(String auditId) { this.auditId = auditId; }

        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }

        public String getJobType() { return jobType; }
        public void setJobType(String jobType) { this.jobType = jobType; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getClientIp() { return clientIp; }
        public void setClientIp(String clientIp) { this.clientIp = clientIp; }

        public Map<String, Object> getParameters() { return parameters; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }

        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }

        public LocalDateTime getCompletedAt() { return completedAt; }
        public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    }
}
