package com.company.index.api.dto;

import java.time.LocalDateTime;

/**
 * 任务响应结果
 */
public class JobResponse {
    private String jobInstanceId;
    private String jobExecutionId;
    private String status;
    private String message;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long duration;

    public JobResponse() {}

    public JobResponse(String jobInstanceId, String jobExecutionId, String status, String message) {
        this.jobInstanceId = jobInstanceId;
        this.jobExecutionId = jobExecutionId;
        this.status = status;
        this.message = message;
        this.startTime = LocalDateTime.now();
    }

    // Getters and Setters
    public String getJobInstanceId() {
        return jobInstanceId;
    }

    public void setJobInstanceId(String jobInstanceId) {
        this.jobInstanceId = jobInstanceId;
    }

    public String getJobExecutionId() {
        return jobExecutionId;
    }

    public void setJobExecutionId(String jobExecutionId) {
        this.jobExecutionId = jobExecutionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    @Override
    public String toString() {
        return "JobResponse{" +
                "jobInstanceId='" + jobInstanceId + '\'' +
                ", jobExecutionId='" + jobExecutionId + '\'' +
                ", status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", duration=" + duration +
                '}';
    }
}
