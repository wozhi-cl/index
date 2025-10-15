package com.company.index.batch.job;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Job测试配置类
 * 提供测试所需的Bean配置
 */
@Configuration
@EnableBatchProcessing
@EnableAutoConfiguration
public class JobTestConfiguration {

    /**
     * 配置JobLauncherTestUtils
     * 用于测试时启动Job
     */
    @Bean
    public JobLauncherTestUtils jobLauncherTestUtils() {
        return new JobLauncherTestUtils();
    }
}

