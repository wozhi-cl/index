package com.company.index.batch.job;

import com.company.index.batch.check.CheckMetrics;
import com.company.index.batch.check.IndexCheckService;
import com.company.index.batch.retry.RetryStats;
import com.company.index.batch.retry.IndexRetryService;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Job 完成监听器
 * 输出指标与审计信息
 */
@Component
public class JobCompletionListener implements JobExecutionListener {

    @Autowired
    private IndexCheckService indexCheckService;

    @Autowired
    private IndexRetryService indexRetryService;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        LocalDateTime startTime = LocalDateTime.now();
        
        System.out.println("=== Job Started ===");
        System.out.println("Job Name: " + jobName);
        System.out.println("Start Time: " + startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        System.out.println("Job Parameters: " + jobExecution.getJobParameters());
        System.out.println("==================");
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        LocalDateTime endTime = LocalDateTime.now();
        String status = jobExecution.getStatus().toString();
        
        System.out.println("=== Job Completed ===");
        System.out.println("Job Name: " + jobName);
        System.out.println("End Time: " + endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        System.out.println("Status: " + status);
        System.out.println("Exit Code: " + jobExecution.getExitStatus().getExitCode());
        
        // 输出执行统计
        outputExecutionStats(jobExecution);
        
        // 输出检查指标
        outputCheckMetrics();
        
        // 输出重试统计
        outputRetryStats();
        
        System.out.println("====================");
    }

    /**
     * 输出执行统计
     */
    private void outputExecutionStats(JobExecution jobExecution) {
        System.out.println("--- Execution Stats ---");
        System.out.println("Read Count: " + jobExecution.getStepExecutions().stream()
                .mapToLong(step -> step.getReadCount())
                .sum());
        System.out.println("Write Count: " + jobExecution.getStepExecutions().stream()
                .mapToLong(step -> step.getWriteCount())
                .sum());
        System.out.println("Skip Count: " + jobExecution.getStepExecutions().stream()
                .mapToLong(step -> step.getSkipCount())
                .sum());
        System.out.println("Error Count: " + jobExecution.getStepExecutions().stream()
                .mapToLong(step -> step.getProcessSkipCount())
                .sum());
        System.out.println("----------------------");
    }

    /**
     * 输出检查指标
     */
    private void outputCheckMetrics() {
        try {
            CheckMetrics metrics = indexCheckService.getCheckMetrics();
            System.out.println("--- Check Metrics ---");
            System.out.println("Healthy: " + metrics.isHealthy());
            System.out.println("Doc Count: " + metrics.getDocCount());
            System.out.println("Timestamp: " + metrics.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            if (metrics.getError() != null) {
                System.out.println("Error: " + metrics.getError());
            }
            System.out.println("-------------------");
        } catch (Exception e) {
            System.out.println("Failed to get check metrics: " + e.getMessage());
        }
    }

    /**
     * 输出重试统计
     */
    private void outputRetryStats() {
        try {
            RetryStats stats = indexRetryService.getRetryStats();
            System.out.println("--- Retry Stats ---");
            System.out.println("Max Attempts: " + stats.getMaxAttempts());
            System.out.println("Initial Delay: " + stats.getInitialDelay() + "ms");
            System.out.println("Max Delay: " + stats.getMaxDelay() + "ms");
            System.out.println("Failure Rate Threshold: " + stats.getFailureRateThreshold());
            System.out.println("Timestamp: " + stats.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            System.out.println("------------------");
        } catch (Exception e) {
            System.out.println("Failed to get retry stats: " + e.getMessage());
        }
    }
}
