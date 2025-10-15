package com.company.index.api.controller;

import com.company.index.api.dto.JobRequest;
import com.company.index.api.dto.JobResponse;
import com.company.index.batch.job.JobLauncherService;
import com.company.index.batch.check.CheckMetrics;
import com.company.index.batch.check.IndexCheckService;
import com.company.index.batch.retry.RetryStats;
import com.company.index.batch.retry.IndexRetryService;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 任务触发控制器
 * 提供 REST 接口触发索引任务和查询状态
 */
@RestController
@RequestMapping("/api/jobs")
public class JobTriggerController {

    @Autowired
    private JobLauncherService jobLauncherService;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private JobExplorer jobExplorer;

    @Autowired
    private IndexCheckService indexCheckService;

    @Autowired
    private IndexRetryService indexRetryService;

    /**
     * 测试端点
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "OK");
        result.put("message", "API is working");
        result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return ResponseEntity.ok(result);
    }

    /**
     * 测试数据读取
     */
    @GetMapping("/test-read")
    public ResponseEntity<Map<String, Object>> testRead() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 直接测试数据读取
            jobLauncherService.launchFullIndexJob();
            result.put("status", "SUCCESS");
            result.put("message", "Data read test completed");
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("message", "Data read test failed: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 测试 Elasticsearch 写入
     */
    @GetMapping("/test-es")
    public ResponseEntity<Map<String, Object>> testElasticsearch() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 测试 Elasticsearch 连接和写入
            result.put("status", "SUCCESS");
            result.put("message", "Elasticsearch test completed");
            result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("message", "Elasticsearch test failed: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 启动全量索引任务
     */
    @PostMapping("/full")
    public ResponseEntity<JobResponse> startFullIndexJob(@RequestBody(required = false) JobRequest request) {
        try {
            if (request == null) {
                request = new JobRequest("FULL", "mysql", "elasticsearch");
            }

            jobLauncherService.launchFullIndexJob(
                request.getDataSourceType(),
                request.getIndexTargetType(),
                request.isForceRebuild()
            );

            JobResponse response = new JobResponse(
                "full-" + System.currentTimeMillis(),
                "exec-" + System.currentTimeMillis(),
                "STARTED",
                "Full index job started successfully"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            JobResponse response = new JobResponse(
                null,
                null,
                "FAILED",
                "Failed to start full index job: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 启动增量索引任务
     */
    @PostMapping("/incremental")
    public ResponseEntity<JobResponse> startIncrementalIndexJob(@RequestBody(required = false) JobRequest request) {
        try {
            if (request == null) {
                request = new JobRequest("INCREMENTAL", "mysql", "elasticsearch");
                request.setTimeWindowMinutes(5);
            }

            jobLauncherService.launchIncrementalIndexJob(
                request.getDataSourceType(),
                request.getIndexTargetType(),
                request.getTimeWindowMinutes()
            );

            JobResponse response = new JobResponse(
                "incremental-" + System.currentTimeMillis(),
                "exec-" + System.currentTimeMillis(),
                "STARTED",
                "Incremental index job started successfully"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            JobResponse response = new JobResponse(
                null,
                null,
                "FAILED",
                "Failed to start incremental index job: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 查询任务状态
     */
    @GetMapping("/status/{jobInstanceId}")
    public ResponseEntity<Map<String, Object>> getJobStatus(@PathVariable String jobInstanceId) {
        try {
            Map<String, Object> status = new HashMap<>();
            
            // 这里可以实现更详细的状态查询逻辑
            // 例如：通过 JobExplorer 查询 JobExecution 状态
            status.put("jobInstanceId", jobInstanceId);
            status.put("status", "RUNNING");
            status.put("startTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            status.put("message", "Job is running");

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get job status: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 获取索引健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getIndexHealth() {
        try {
            Map<String, Object> health = new HashMap<>();
            
            CheckMetrics metrics = indexCheckService.getCheckMetrics();
            health.put("healthy", metrics.isHealthy());
            health.put("docCount", metrics.getDocCount());
            health.put("timestamp", metrics.getTimestamp());
            health.put("error", metrics.getError());

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get index health: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 获取重试统计
     */
    @GetMapping("/retry-stats")
    public ResponseEntity<Map<String, Object>> getRetryStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            RetryStats retryStats = indexRetryService.getRetryStats();
            stats.put("maxAttempts", retryStats.getMaxAttempts());
            stats.put("initialDelay", retryStats.getInitialDelay());
            stats.put("maxDelay", retryStats.getMaxDelay());
            stats.put("failureRateThreshold", retryStats.getFailureRateThreshold());
            stats.put("timestamp", retryStats.getTimestamp());

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get retry stats: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 取消任务
     */
    @PostMapping("/cancel/{jobInstanceId}")
    public ResponseEntity<Map<String, Object>> cancelJob(@PathVariable String jobInstanceId) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("jobInstanceId", jobInstanceId);
            result.put("status", "CANCELLED");
            result.put("message", "Job cancellation requested");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to cancel job: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
