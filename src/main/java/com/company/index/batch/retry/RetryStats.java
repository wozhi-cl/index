package com.company.index.batch.retry;

import java.time.LocalDateTime;

/**
 * 重试统计信息
 */
public class RetryStats {
    private int maxAttempts;
    private long initialDelay;
    private long maxDelay;
    private double failureRateThreshold;
    private LocalDateTime timestamp;

    public RetryStats() {}

    public RetryStats(int maxAttempts, long initialDelay, long maxDelay, double failureRateThreshold, LocalDateTime timestamp) {
        this.maxAttempts = maxAttempts;
        this.initialDelay = initialDelay;
        this.maxDelay = maxDelay;
        this.failureRateThreshold = failureRateThreshold;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public long getInitialDelay() {
        return initialDelay;
    }

    public void setInitialDelay(long initialDelay) {
        this.initialDelay = initialDelay;
    }

    public long getMaxDelay() {
        return maxDelay;
    }

    public void setMaxDelay(long maxDelay) {
        this.maxDelay = maxDelay;
    }

    public double getFailureRateThreshold() {
        return failureRateThreshold;
    }

    public void setFailureRateThreshold(double failureRateThreshold) {
        this.failureRateThreshold = failureRateThreshold;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "RetryStats{" +
                "maxAttempts=" + maxAttempts +
                ", initialDelay=" + initialDelay +
                ", maxDelay=" + maxDelay +
                ", failureRateThreshold=" + failureRateThreshold +
                ", timestamp=" + timestamp +
                '}';
    }
}
