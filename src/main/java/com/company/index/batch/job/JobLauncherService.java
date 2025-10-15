package com.company.index.batch.job;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Job 启动服务
 * 封装 Job 启动与参数构建
 */
@Service
public class JobLauncherService {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job fullIndexJob;

    @Autowired
    private Job incrementalIndexJob;

    /**
     * 启动全量索引任务
     */
    public void launchFullIndexJob() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("jobType", "FULL")
                .addString("startTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(fullIndexJob, jobParameters);
    }

    /**
     * 启动增量索引任务
     */
    public void launchIncrementalIndexJob() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("jobType", "INCREMENTAL")
                .addString("startTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(incrementalIndexJob, jobParameters);
    }

    /**
     * 启动全量索引任务（带参数）
     */
    public void launchFullIndexJob(String dataSourceType, String indexTargetType, boolean forceRebuild) throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("jobType", "FULL")
                .addString("dataSourceType", dataSourceType)
                .addString("indexTargetType", indexTargetType)
                .addString("forceRebuild", String.valueOf(forceRebuild))
                .addString("startTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(fullIndexJob, jobParameters);
    }

    /**
     * 启动增量索引任务（带参数）
     */
    public void launchIncrementalIndexJob(String dataSourceType, String indexTargetType, int timeWindowMinutes) throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("jobType", "INCREMENTAL")
                .addString("dataSourceType", dataSourceType)
                .addString("indexTargetType", indexTargetType)
                .addString("timeWindowMinutes", String.valueOf(timeWindowMinutes))
                .addString("startTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(incrementalIndexJob, jobParameters);
    }
}
