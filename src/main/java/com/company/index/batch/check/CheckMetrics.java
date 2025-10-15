package com.company.index.batch.check;

import java.time.LocalDateTime;

/**
 * 检查指标数据模型
 */
public class CheckMetrics {
    private boolean healthy;
    private Long docCount;
    private Object indexStats;
    private LocalDateTime timestamp;
    private String error;

    public CheckMetrics() {}

    public CheckMetrics(boolean healthy, Long docCount, Object indexStats, LocalDateTime timestamp) {
        this.healthy = healthy;
        this.docCount = docCount;
        this.indexStats = indexStats;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public boolean isHealthy() {
        return healthy;
    }

    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }

    public Long getDocCount() {
        return docCount;
    }

    public void setDocCount(Long docCount) {
        this.docCount = docCount;
    }

    public Object getIndexStats() {
        return indexStats;
    }

    public void setIndexStats(Object indexStats) {
        this.indexStats = indexStats;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "CheckMetrics{" +
                "healthy=" + healthy +
                ", docCount=" + docCount +
                ", timestamp=" + timestamp +
                ", error='" + error + '\'' +
                '}';
    }
}
