package com.company.index.batch.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Gauge;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 指标服务
 * 提供指标收集和更新功能
 */
@Service
public class MetricsService {

    @Autowired
    private MeterRegistry meterRegistry;

    private final AtomicLong indexHealthStatus = new AtomicLong(1);
    private final AtomicLong indexDocCount = new AtomicLong(0);

    /**
     * 记录任务执行
     */
    public void recordJobExecution(String jobType) {
        Counter.builder("index.job.execution")
                .tag("type", jobType)
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录任务成功
     */
    public void recordJobSuccess(String jobType) {
        Counter.builder("index.job.success")
                .tag("type", jobType)
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录任务失败
     */
    public void recordJobFailure(String jobType, String error) {
        Counter.builder("index.job.failure")
                .tag("type", jobType)
                .tag("error", error)
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录任务执行时间
     */
    public Timer.Sample startJobTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * 停止任务计时器
     */
    public void stopJobTimer(Timer.Sample sample, String jobType) {
        sample.stop(Timer.builder("index.job.duration")
                .tag("type", jobType)
                .register(meterRegistry));
    }

    /**
     * 记录处理记录数
     */
    public void recordProcessedRecords(int count, String jobType) {
        Counter.builder("index.records.processed")
                .tag("type", jobType)
                .register(meterRegistry)
                .increment(count);
    }

    /**
     * 记录跳过记录数
     */
    public void recordSkippedRecords(int count, String jobType) {
        Counter.builder("index.records.skipped")
                .tag("type", jobType)
                .register(meterRegistry)
                .increment(count);
    }

    /**
     * 记录错误记录数
     */
    public void recordErrorRecords(int count, String jobType, String error) {
        Counter.builder("index.records.error")
                .tag("type", jobType)
                .tag("error", error)
                .register(meterRegistry)
                .increment(count);
    }

    /**
     * 更新索引健康状态
     */
    public void updateIndexHealth(boolean healthy) {
        indexHealthStatus.set(healthy ? 1 : 0);
    }

    /**
     * 更新索引文档数量
     */
    public void updateIndexDocCount(long count) {
        indexDocCount.set(count);
    }

    /**
     * 记录分片处理时间
     */
    public Timer.Sample startPartitionTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * 停止分片计时器
     */
    public void stopPartitionTimer(Timer.Sample sample, String partitionName) {
        sample.stop(Timer.builder("index.partition.duration")
                .tag("partition", partitionName)
                .register(meterRegistry));
    }

    /**
     * 记录重试次数
     */
    public void recordRetry(String jobType, String reason) {
        Counter.builder("index.retry.count")
                .tag("type", jobType)
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录自定义指标
     */
    public void recordCustomMetric(String name, double value, String... tags) {
        AtomicReference<Double> valueRef = new AtomicReference<>(value);
        Gauge.builder("index.custom." + name, valueRef, AtomicReference::get)
                .tags(tags)
                .register(meterRegistry);
    }

    /**
     * 获取指标值
     */
    public double getMetricValue(String name) {
        return meterRegistry.get(name).gauge().value();
    }

    /**
     * 获取计数器值
     */
    public double getCounterValue(String name) {
        return meterRegistry.get(name).counter().count();
    }

    /**
     * 获取计时器值
     */
    public double getTimerValue(String name) {
        return meterRegistry.get(name).timer().totalTime(java.util.concurrent.TimeUnit.SECONDS);
    }
}
