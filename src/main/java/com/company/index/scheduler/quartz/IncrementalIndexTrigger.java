package com.company.index.scheduler.quartz;

import com.company.index.batch.job.JobLauncherService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 增量索引触发器
 * 每5分钟执行一次
 */
@Component
public class IncrementalIndexTrigger implements Job {

    @Autowired
    private JobLauncherService jobLauncherService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            System.out.println("Starting incremental index job at: " + java.time.LocalDateTime.now());
            jobLauncherService.launchIncrementalIndexJob();
            System.out.println("Incremental index job completed at: " + java.time.LocalDateTime.now());
        } catch (Exception e) {
            System.err.println("Incremental index job failed: " + e.getMessage());
            throw new JobExecutionException("Incremental index job execution failed", e);
        }
    }
}
