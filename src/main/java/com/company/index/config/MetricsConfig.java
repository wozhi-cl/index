package com.company.index.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 指标配置
 * 配置 Micrometer 指标收集
 */
@Configuration
public class MetricsConfig {

    @Autowired
    private MeterRegistry meterRegistry;

    /**
     * 任务执行计数器
     */
    @Bean
    public Counter jobExecutionCounter() {
        return Counter.builder("index.job.execution")
                .description("Number of job executions")
                .tag("type", "total")
                .register(meterRegistry);
    }

    /**
     * 任务成功计数器
     */
    @Bean
    public Counter jobSuccessCounter() {
        return Counter.builder("index.job.success")
                .description("Number of successful job executions")
                .tag("type", "success")
                .register(meterRegistry);
    }

    /**
     * 任务失败计数器
     */
    @Bean
    public Counter jobFailureCounter() {
        return Counter.builder("index.job.failure")
                .description("Number of failed job executions")
                .tag("type", "failure")
                .register(meterRegistry);
    }

    /**
     * 任务执行时间计时器
     */
    @Bean
    public Timer jobExecutionTimer() {
        return Timer.builder("index.job.duration")
                .description("Job execution duration")
                .register(meterRegistry);
    }

    /**
     * 记录处理计数器
     */
    @Bean
    public Counter recordProcessedCounter() {
        return Counter.builder("index.records.processed")
                .description("Number of records processed")
                .tag("type", "processed")
                .register(meterRegistry);
    }

    /**
     * 记录跳过计数器
     */
    @Bean
    public Counter recordSkippedCounter() {
        return Counter.builder("index.records.skipped")
                .description("Number of records skipped")
                .tag("type", "skipped")
                .register(meterRegistry);
    }

    /**
     * 记录错误计数器
     */
    @Bean
    public Counter recordErrorCounter() {
        return Counter.builder("index.records.error")
                .description("Number of records with errors")
                .tag("type", "error")
                .register(meterRegistry);
    }

    /**
     * 索引健康状态指标
     */
    @Bean
    public Gauge indexHealthGauge() {
        AtomicLong healthStatus = new AtomicLong(1);
        return Gauge.builder("index.health.status", healthStatus, AtomicLong::get)
                .description("Index health status (1=healthy, 0=unhealthy)")
                .register(meterRegistry);
    }

    /**
     * 索引文档数量指标
     */
    @Bean
    public Gauge indexDocCountGauge() {
        AtomicLong docCount = new AtomicLong(0);
        return Gauge.builder("index.docs.count", docCount, AtomicLong::get)
                .description("Number of documents in index")
                .register(meterRegistry);
    }

    /**
     * 分片处理时间计时器
     */
    @Bean
    public Timer partitionProcessingTimer() {
        return Timer.builder("index.partition.duration")
                .description("Partition processing duration")
                .register(meterRegistry);
    }

    /**
     * 重试次数计数器
     */
    @Bean
    public Counter retryCounter() {
        return Counter.builder("index.retry.count")
                .description("Number of retries")
                .tag("type", "retry")
                .register(meterRegistry);
    }

    /**
     * 自定义指标绑定器
     */
    @Bean
    public MeterBinder customMeterBinder() {
        return new MeterBinder() {
            @Override
            public void bindTo(MeterRegistry registry) {
                // 绑定自定义指标
                AtomicLong customMetric = new AtomicLong(0);
                Gauge.builder("index.custom.metric", customMetric, AtomicLong::get)
                        .description("Custom metric")
                        .register(registry);
            }
        };
    }
}
