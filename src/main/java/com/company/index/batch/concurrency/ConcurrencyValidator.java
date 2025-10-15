package com.company.index.batch.concurrency;

import com.company.index.scheduler.lock.DistributedLockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 并发验证器
 * 确保多节点环境下的任务互斥执行
 */
@Component
public class ConcurrencyValidator {

    @Autowired
    private DistributedLockService distributedLockService;

    @Value("${index.concurrency.validation.enabled:true}")
    private boolean validationEnabled;

    @Value("${index.concurrency.validation.timeout:300}")
    private int validationTimeout;

    // 本地执行记录缓存
    private final Map<String, ExecutionRecord> executionCache = new ConcurrentHashMap<>();

    /**
     * 验证任务是否可以执行
     */
    public boolean canExecute(String jobName, String instanceId) {
        if (!validationEnabled) {
            return true;
        }

        String lockName = jobName + "-execution-lock";
        
        // 检查分布式锁
        if (distributedLockService.isLocked(lockName)) {
            DistributedLockService.LockInfo lockInfo = distributedLockService.getLockInfo(lockName);
            if (lockInfo != null && !lockInfo.getInstanceId().equals(instanceId)) {
                System.out.println("Job " + jobName + " is already running on instance: " + lockInfo.getInstanceId());
                return false;
            }
        }

        // 检查本地缓存
        ExecutionRecord localRecord = executionCache.get(jobName);
        if (localRecord != null && localRecord.isRunning() && !localRecord.getInstanceId().equals(instanceId)) {
            System.out.println("Job " + jobName + " is already running locally on instance: " + localRecord.getInstanceId());
            return false;
        }

        return true;
    }

    /**
     * 记录任务开始执行
     */
    public void recordJobStart(String jobName, String instanceId) {
        ExecutionRecord record = new ExecutionRecord();
        record.setJobName(jobName);
        record.setInstanceId(instanceId);
        record.setStartTime(LocalDateTime.now());
        record.setRunning(true);

        executionCache.put(jobName, record);
        System.out.println("Recorded job start: " + jobName + " (instance: " + instanceId + ")");
    }

    /**
     * 记录任务完成
     */
    public void recordJobComplete(String jobName, String instanceId) {
        ExecutionRecord record = executionCache.get(jobName);
        if (record != null && record.getInstanceId().equals(instanceId)) {
            record.setRunning(false);
            record.setEndTime(LocalDateTime.now());
            System.out.println("Recorded job completion: " + jobName + " (instance: " + instanceId + ")");
        }
    }

    /**
     * 记录任务失败
     */
    public void recordJobFailure(String jobName, String instanceId, String error) {
        ExecutionRecord record = executionCache.get(jobName);
        if (record != null && record.getInstanceId().equals(instanceId)) {
            record.setRunning(false);
            record.setEndTime(LocalDateTime.now());
            record.setError(error);
            System.out.println("Recorded job failure: " + jobName + " (instance: " + instanceId + ") - " + error);
        }
    }

    /**
     * 清理过期记录
     */
    public void cleanupExpiredRecords() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(1);
        
        executionCache.entrySet().removeIf(entry -> {
            ExecutionRecord record = entry.getValue();
            return record.getEndTime() != null && record.getEndTime().isBefore(cutoffTime);
        });
    }

    /**
     * 获取执行状态
     */
    public ExecutionStatus getExecutionStatus(String jobName) {
        ExecutionRecord record = executionCache.get(jobName);
        if (record == null) {
            return ExecutionStatus.NOT_RUNNING;
        }

        if (record.isRunning()) {
            return ExecutionStatus.RUNNING;
        } else if (record.getError() != null) {
            return ExecutionStatus.FAILED;
        } else {
            return ExecutionStatus.COMPLETED;
        }
    }

    /**
     * 获取执行记录
     */
    public ExecutionRecord getExecutionRecord(String jobName) {
        return executionCache.get(jobName);
    }

    /**
     * 执行记录模型
     */
    public static class ExecutionRecord {
        private String jobName;
        private String instanceId;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private boolean running;
        private String error;

        // Getters and Setters
        public String getJobName() { return jobName; }
        public void setJobName(String jobName) { this.jobName = jobName; }

        public String getInstanceId() { return instanceId; }
        public void setInstanceId(String instanceId) { this.instanceId = instanceId; }

        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

        public boolean isRunning() { return running; }
        public void setRunning(boolean running) { this.running = running; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }

    /**
     * 执行状态枚举
     */
    public enum ExecutionStatus {
        NOT_RUNNING,
        RUNNING,
        COMPLETED,
        FAILED
    }
}
