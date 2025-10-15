package com.company.index.scheduler.lock;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 带分布式锁的 Job 启动器
 * 确保同一时间只有一个实例执行任务
 */
@Component
public class LockAwareJobLauncher {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private DistributedLockService distributedLockService;

    @Value("${index.distributed.lock.timeout:300}")
    private int lockTimeout;

    @Value("${index.distributed.lock.retry.max:3}")
    private int maxRetries;

    @Value("${index.distributed.lock.retry.interval:5000}")
    private int retryInterval;

    /**
     * 启动带锁的任务
     */
    public void launchJobWithLock(Job job, JobParameters jobParameters) throws Exception {
        String lockName = job.getName() + "-lock";
        String instanceId = generateInstanceId();

        // 尝试获取锁
        boolean lockAcquired = distributedLockService.tryAcquireLock(
            lockName, instanceId, maxRetries, retryInterval);

        if (!lockAcquired) {
            System.out.println("Failed to acquire lock for job: " + job.getName());
            return;
        }

        try {
            System.out.println("Acquired lock for job: " + job.getName() + " (instance: " + instanceId + ")");
            
            // 执行任务
            jobLauncher.run(job, jobParameters);
            
            System.out.println("Job completed: " + job.getName());
        } finally {
            // 释放锁
            distributedLockService.releaseLock(lockName, instanceId);
            System.out.println("Released lock for job: " + job.getName());
        }
    }

    /**
     * 启动全量索引任务（带锁）
     */
    public void launchFullIndexJobWithLock(Job fullIndexJob) throws Exception {
        JobParameters jobParameters = new org.springframework.batch.core.JobParametersBuilder()
                .addString("jobType", "FULL")
                .addString("startTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        launchJobWithLock(fullIndexJob, jobParameters);
    }

    /**
     * 启动增量索引任务（带锁）
     */
    public void launchIncrementalIndexJobWithLock(Job incrementalIndexJob) throws Exception {
        JobParameters jobParameters = new org.springframework.batch.core.JobParametersBuilder()
                .addString("jobType", "INCREMENTAL")
                .addString("startTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        launchJobWithLock(incrementalIndexJob, jobParameters);
    }

    /**
     * 检查任务是否正在运行
     */
    public boolean isJobRunning(String jobName) {
        String lockName = jobName + "-lock";
        return distributedLockService.isLocked(lockName);
    }

    /**
     * 获取任务锁信息
     */
    public DistributedLockService.LockInfo getJobLockInfo(String jobName) {
        String lockName = jobName + "-lock";
        return distributedLockService.getLockInfo(lockName);
    }

    /**
     * 强制释放任务锁（紧急情况使用）
     */
    public void forceReleaseJobLock(String jobName) {
        String lockName = jobName + "-lock";
        // 这里可以实现强制释放逻辑，但需要谨慎使用
        System.out.println("Force releasing lock for job: " + jobName);
    }

    /**
     * 生成实例 ID
     */
    private String generateInstanceId() {
        return System.getenv("HOSTNAME") + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
