package com.company.index.batch.observability;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Grafana 看板模板
 * 提供预定义的看板配置
 */
@Component
public class GrafanaDashboardTemplate {

    /**
     * 获取任务执行看板配置
     */
    public Map<String, Object> getJobExecutionDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        
        // 看板基本信息
        dashboard.put("title", "Index Job Execution Dashboard");
        dashboard.put("description", "Monitor index job execution metrics");
        dashboard.put("refresh", "30s");
        
        // 面板配置
        Map<String, Object> panels = new HashMap<>();
        
        // 任务执行计数器
        Map<String, Object> jobExecutionPanel = new HashMap<>();
        jobExecutionPanel.put("title", "Job Execution Count");
        jobExecutionPanel.put("type", "stat");
        jobExecutionPanel.put("targets", new Object[]{
            Map.of("expr", "sum(rate(index_job_execution_total[5m]))", "legendFormat", "Executions/sec")
        });
        panels.put("jobExecution", jobExecutionPanel);
        
        // 任务成功率
        Map<String, Object> jobSuccessRatePanel = new HashMap<>();
        jobSuccessRatePanel.put("title", "Job Success Rate");
        jobSuccessRatePanel.put("type", "stat");
        jobSuccessRatePanel.put("targets", new Object[]{
            Map.of("expr", "sum(rate(index_job_success_total[5m])) / sum(rate(index_job_execution_total[5m])) * 100", 
                   "legendFormat", "Success Rate %")
        });
        panels.put("jobSuccessRate", jobSuccessRatePanel);
        
        // 任务执行时间
        Map<String, Object> jobDurationPanel = new HashMap<>();
        jobDurationPanel.put("title", "Job Execution Duration");
        jobDurationPanel.put("type", "graph");
        jobDurationPanel.put("targets", new Object[]{
            Map.of("expr", "histogram_quantile(0.95, rate(index_job_duration_seconds_bucket[5m]))", 
                   "legendFormat", "95th percentile"),
            Map.of("expr", "histogram_quantile(0.50, rate(index_job_duration_seconds_bucket[5m]))", 
                   "legendFormat", "50th percentile")
        });
        panels.put("jobDuration", jobDurationPanel);
        
        // 记录处理速率
        Map<String, Object> recordProcessingPanel = new HashMap<>();
        recordProcessingPanel.put("title", "Record Processing Rate");
        recordProcessingPanel.put("type", "graph");
        recordProcessingPanel.put("targets", new Object[]{
            Map.of("expr", "sum(rate(index_records_processed_total[5m]))", "legendFormat", "Processed/sec"),
            Map.of("expr", "sum(rate(index_records_skipped_total[5m]))", "legendFormat", "Skipped/sec"),
            Map.of("expr", "sum(rate(index_records_error_total[5m]))", "legendFormat", "Errors/sec")
        });
        panels.put("recordProcessing", recordProcessingPanel);
        
        // 索引健康状态
        Map<String, Object> indexHealthPanel = new HashMap<>();
        indexHealthPanel.put("title", "Index Health Status");
        indexHealthPanel.put("type", "stat");
        indexHealthPanel.put("targets", new Object[]{
            Map.of("expr", "index_health_status", "legendFormat", "Health Status")
        });
        panels.put("indexHealth", indexHealthPanel);
        
        // 索引文档数量
        Map<String, Object> indexDocCountPanel = new HashMap<>();
        indexDocCountPanel.put("title", "Index Document Count");
        indexDocCountPanel.put("type", "graph");
        indexDocCountPanel.put("targets", new Object[]{
            Map.of("expr", "index_docs_count", "legendFormat", "Document Count")
        });
        panels.put("indexDocCount", indexDocCountPanel);
        
        // 分片处理时间
        Map<String, Object> partitionProcessingPanel = new HashMap<>();
        partitionProcessingPanel.put("title", "Partition Processing Duration");
        partitionProcessingPanel.put("type", "graph");
        partitionProcessingPanel.put("targets", new Object[]{
            Map.of("expr", "histogram_quantile(0.95, rate(index_partition_duration_seconds_bucket[5m]))", 
                   "legendFormat", "95th percentile"),
            Map.of("expr", "histogram_quantile(0.50, rate(index_partition_duration_seconds_bucket[5m]))", 
                   "legendFormat", "50th percentile")
        });
        panels.put("partitionProcessing", partitionProcessingPanel);
        
        // 重试次数
        Map<String, Object> retryCountPanel = new HashMap<>();
        retryCountPanel.put("title", "Retry Count");
        retryCountPanel.put("type", "graph");
        retryCountPanel.put("targets", new Object[]{
            Map.of("expr", "sum(rate(index_retry_count_total[5m]))", "legendFormat", "Retries/sec")
        });
        panels.put("retryCount", retryCountPanel);
        
        dashboard.put("panels", panels);
        
        return dashboard;
    }

    /**
     * 获取系统资源看板配置
     */
    public Map<String, Object> getSystemResourceDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        
        dashboard.put("title", "System Resource Dashboard");
        dashboard.put("description", "Monitor system resource usage");
        dashboard.put("refresh", "30s");
        
        Map<String, Object> panels = new HashMap<>();
        
        // CPU 使用率
        Map<String, Object> cpuPanel = new HashMap<>();
        cpuPanel.put("title", "CPU Usage");
        cpuPanel.put("type", "graph");
        cpuPanel.put("targets", new Object[]{
            Map.of("expr", "system_cpu_usage", "legendFormat", "CPU Usage %")
        });
        panels.put("cpu", cpuPanel);
        
        // 内存使用率
        Map<String, Object> memoryPanel = new HashMap<>();
        memoryPanel.put("title", "Memory Usage");
        memoryPanel.put("type", "graph");
        memoryPanel.put("targets", new Object[]{
            Map.of("expr", "jvm_memory_used_bytes / jvm_memory_max_bytes * 100", "legendFormat", "Memory Usage %")
        });
        panels.put("memory", memoryPanel);
        
        // 线程数
        Map<String, Object> threadPanel = new HashMap<>();
        threadPanel.put("title", "Thread Count");
        threadPanel.put("type", "graph");
        threadPanel.put("targets", new Object[]{
            Map.of("expr", "jvm_threads_live_threads", "legendFormat", "Live Threads")
        });
        panels.put("threads", threadPanel);
        
        // GC 时间
        Map<String, Object> gcPanel = new HashMap<>();
        gcPanel.put("title", "GC Time");
        gcPanel.put("type", "graph");
        gcPanel.put("targets", new Object[]{
            Map.of("expr", "jvm_gc_pause_seconds", "legendFormat", "GC Pause Time")
        });
        panels.put("gc", gcPanel);
        
        dashboard.put("panels", panels);
        
        return dashboard;
    }

    /**
     * 获取告警规则配置
     */
    public Map<String, Object> getAlertRules() {
        Map<String, Object> rules = new HashMap<>();
        
        // 任务失败告警
        Map<String, Object> jobFailureAlert = new HashMap<>();
        jobFailureAlert.put("name", "Job Failure Alert");
        jobFailureAlert.put("condition", "sum(rate(index_job_failure_total[5m])) > 0.1");
        jobFailureAlert.put("message", "Job failure rate is too high");
        jobFailureAlert.put("severity", "warning");
        
        // 索引健康告警
        Map<String, Object> indexHealthAlert = new HashMap<>();
        indexHealthAlert.put("name", "Index Health Alert");
        indexHealthAlert.put("condition", "index_health_status == 0");
        indexHealthAlert.put("message", "Index health check failed");
        indexHealthAlert.put("severity", "critical");
        
        // 记录错误告警
        Map<String, Object> recordErrorAlert = new HashMap<>();
        recordErrorAlert.put("name", "Record Error Alert");
        recordErrorAlert.put("condition", "sum(rate(index_records_error_total[5m])) > 10");
        recordErrorAlert.put("message", "Record error rate is too high");
        recordErrorAlert.put("severity", "warning");
        
        rules.put("jobFailure", jobFailureAlert);
        rules.put("indexHealth", indexHealthAlert);
        rules.put("recordError", recordErrorAlert);
        
        return rules;
    }
}
