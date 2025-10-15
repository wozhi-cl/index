package com.company.index.scheduler.quartz;

import com.company.index.batch.job.JobLauncherService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 全量索引触发器
 * 每周日凌晨3点执行
 */
@Component
public class FullIndexTrigger implements Job {

    @Autowired
    private JobLauncherService jobLauncherService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            System.out.println("Starting full index job at: " + java.time.LocalDateTime.now());
            jobLauncherService.launchFullIndexJob();
            System.out.println("Full index job completed at: " + java.time.LocalDateTime.now());
        } catch (Exception e) {
            System.err.println("Full index job failed: " + e.getMessage());
            throw new JobExecutionException("Full index job execution failed", e);
        }
    }
}
