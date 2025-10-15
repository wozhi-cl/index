package com.company.index.api.dto;

import java.time.LocalDateTime;

/**
 * 任务请求参数
 */
public class JobRequest {
    private String jobType; // FULL, INCREMENTAL
    private String dataSourceType; // mysql, oracle, csv, json
    private String indexTargetType; // elasticsearch, getquick, file
    private boolean forceRebuild;
    private int timeWindowMinutes;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public JobRequest() {}

    public JobRequest(String jobType, String dataSourceType, String indexTargetType) {
        this.jobType = jobType;
        this.dataSourceType = dataSourceType;
        this.indexTargetType = indexTargetType;
    }

    // Getters and Setters
    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getDataSourceType() {
        return dataSourceType;
    }

    public void setDataSourceType(String dataSourceType) {
        this.dataSourceType = dataSourceType;
    }

    public String getIndexTargetType() {
        return indexTargetType;
    }

    public void setIndexTargetType(String indexTargetType) {
        this.indexTargetType = indexTargetType;
    }

    public boolean isForceRebuild() {
        return forceRebuild;
    }

    public void setForceRebuild(boolean forceRebuild) {
        this.forceRebuild = forceRebuild;
    }

    public int getTimeWindowMinutes() {
        return timeWindowMinutes;
    }

    public void setTimeWindowMinutes(int timeWindowMinutes) {
        this.timeWindowMinutes = timeWindowMinutes;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    @Override
    public String toString() {
        return "JobRequest{" +
                "jobType='" + jobType + '\'' +
                ", dataSourceType='" + dataSourceType + '\'' +
                ", indexTargetType='" + indexTargetType + '\'' +
                ", forceRebuild=" + forceRebuild +
                ", timeWindowMinutes=" + timeWindowMinutes +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}
