package com.company.index.batch.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 日志服务
 * 提供结构化日志记录功能
 */
@Service
public class LoggingService {

    private static final Logger logger = LoggerFactory.getLogger(LoggingService.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * 记录任务开始
     */
    public void logJobStart(String jobName, String jobId, Map<String, Object> parameters) {
        MDC.put("jobId", jobId);
        MDC.put("stepId", "START");
        MDC.put("timestamp", LocalDateTime.now().format(formatter));
        
        logger.info("Job started: {} with parameters: {}", jobName, parameters);
    }

    /**
     * 记录任务完成
     */
    public void logJobComplete(String jobName, String jobId, String status, long duration) {
        MDC.put("jobId", jobId);
        MDC.put("stepId", "COMPLETE");
        MDC.put("timestamp", LocalDateTime.now().format(formatter));
        
        logger.info("Job completed: {} with status: {} in {}ms", jobName, status, duration);
    }

    /**
     * 记录任务失败
     */
    public void logJobFailure(String jobName, String jobId, String error, Exception exception) {
        MDC.put("jobId", jobId);
        MDC.put("stepId", "FAILURE");
        MDC.put("timestamp", LocalDateTime.now().format(formatter));
        
        logger.error("Job failed: {} with error: {}", jobName, error, exception);
    }

    /**
     * 记录步骤开始
     */
    public void logStepStart(String stepName, String stepId, Map<String, Object> parameters) {
        MDC.put("stepId", stepId);
        MDC.put("timestamp", LocalDateTime.now().format(formatter));
        
        logger.info("Step started: {} with parameters: {}", stepName, parameters);
    }

    /**
     * 记录步骤完成
     */
    public void logStepComplete(String stepName, String stepId, String status, long duration) {
        MDC.put("stepId", stepId);
        MDC.put("timestamp", LocalDateTime.now().format(formatter));
        
        logger.info("Step completed: {} with status: {} in {}ms", stepName, status, duration);
    }

    /**
     * 记录步骤失败
     */
    public void logStepFailure(String stepName, String stepId, String error, Exception exception) {
        MDC.put("stepId", stepId);
        MDC.put("timestamp", LocalDateTime.now().format(formatter));
        
        logger.error("Step failed: {} with error: {}", stepName, error, exception);
    }

    /**
     * 记录分片开始
     */
    public void logPartitionStart(String partitionName, String partitionId, Map<String, Object> parameters) {
        MDC.put("partitionId", partitionId);
        MDC.put("timestamp", LocalDateTime.now().format(formatter));
        
        logger.info("Partition started: {} with parameters: {}", partitionName, parameters);
    }

    /**
     * 记录分片完成
     */
    public void logPartitionComplete(String partitionName, String partitionId, String status, long duration) {
        MDC.put("partitionId", partitionId);
        MDC.put("timestamp", LocalDateTime.now().format(formatter));
        
        logger.info("Partition completed: {} with status: {} in {}ms", partitionName, status, duration);
    }

    /**
     * 记录分片失败
     */
    public void logPartitionFailure(String partitionName, String partitionId, String error, Exception exception) {
        MDC.put("partitionId", partitionId);
        MDC.put("timestamp", LocalDateTime.now().format(formatter));
        
        logger.error("Partition failed: {} with error: {}", partitionName, error, exception);
    }

    /**
     * 记录记录处理
     */
    public void logRecordProcessed(String recordId, String recordType, String status) {
        MDC.put("timestamp", LocalDateTime.now().format(formatter));
        
        logger.debug("Record processed: {} type: {} status: {}", recordId, recordType, status);
    }

    /**
     * 记录记录跳过
     */
    public void logRecordSkipped(String recordId, String reason) {
        MDC.put("timestamp", LocalDateTime.now().format(formatter));
        
        logger.warn("Record skipped: {} reason: {}", recordId, reason);
    }

    /**
     * 记录记录错误
     */
    public void logRecordError(String recordId, String error, Exception exception) {
        MDC.put("timestamp", LocalDateTime.now().format(formatter));
        
        logger.error("Record error: {} error: {}", recordId, error, exception);
    }

    /**
     * 记录性能指标
     */
    public void logPerformanceMetrics(String operation, long duration, int count) {
        MDC.put("timestamp", LocalDateTime.now().format(formatter));
        
        logger.info("Performance metrics: operation={} duration={}ms count={}", operation, duration, count);
    }

    /**
     * 记录健康检查
     */
    public void logHealthCheck(String component, boolean healthy, String details) {
        MDC.put("timestamp", LocalDateTime.now().format(formatter));
        
        if (healthy) {
            logger.info("Health check passed: {} details: {}", component, details);
        } else {
            logger.warn("Health check failed: {} details: {}", component, details);
        }
    }

    /**
     * 记录重试
     */
    public void logRetry(String operation, int attempt, String reason) {
        MDC.put("timestamp", LocalDateTime.now().format(formatter));
        
        logger.warn("Retry attempt: {} operation: {} reason: {}", attempt, operation, reason);
    }

    /**
     * 清理 MDC
     */
    public void clearMDC() {
        MDC.clear();
    }
}
