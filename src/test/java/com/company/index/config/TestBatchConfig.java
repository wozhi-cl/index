package com.company.index.config;

import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;

/**
 * 测试环境的Batch配置
 * 覆盖默认的异步JobLauncher，使用同步执行器
 */
@TestConfiguration
public class TestBatchConfig {

    /**
     * 测试用的同步JobLauncher
     * 使用@Primary确保在测试时优先使用这个Bean
     */
    @Bean
    @Primary
    public JobLauncher syncJobLauncher(JobRepository jobRepository) throws Exception {
        TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();
        launcher.setJobRepository(jobRepository);
        // 使用同步执行器，确保launchJob()方法等待Job完成后再返回
        launcher.setTaskExecutor(new SyncTaskExecutor());
        launcher.afterPropertiesSet();
        return launcher;
    }

    /**
     * 提供 JobLauncherTestUtils Bean 用于测试
     */
    @Bean
    public JobLauncherTestUtils jobLauncherTestUtils(JobLauncher jobLauncher, JobRepository jobRepository) {
        JobLauncherTestUtils utils = new JobLauncherTestUtils();
        utils.setJobLauncher(jobLauncher);
        utils.setJobRepository(jobRepository);
        return utils;
    }
}

