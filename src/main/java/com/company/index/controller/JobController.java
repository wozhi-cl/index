package com.company.index.controller;

import com.company.index.batch.job.FullIndexJobConfig;
import com.company.index.batch.job.IncrementalIndexJobConfig;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Job API 控制器
 * 提供 Job 的启动、停止、状态查询等 API
 */
@RestController
@RequestMapping("/api/jobs")
public class JobController {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("fullIndexJob")
    private Job fullIndexJob;

    @Autowired
    @Qualifier("incrementalIndexJob")
    private Job incrementalIndexJob;

    /**
     * 启动全量索引 Job
     */
    @PostMapping("/full-index/start")
    public ResponseEntity<Map<String, Object>> startFullIndexJob() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .addString("jobType", "FULL_INDEX")
                    .toJobParameters();

            JobExecution jobExecution = jobLauncher.run(fullIndexJob, jobParameters);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("jobId", jobExecution.getId());
            response.put("jobName", jobExecution.getJobInstance().getJobName());
            response.put("status", jobExecution.getStatus().toString());
            response.put("startTime", jobExecution.getStartTime());
            response.put("message", "全量索引 Job 启动成功");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("message", "全量索引 Job 启动失败");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 启动增量索引 Job
     */
    @PostMapping("/incremental-index/start")
    public ResponseEntity<Map<String, Object>> startIncrementalIndexJob() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .addString("jobType", "INCREMENTAL_INDEX")
                    .toJobParameters();

            JobExecution jobExecution = jobLauncher.run(incrementalIndexJob, jobParameters);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("jobId", jobExecution.getId());
            response.put("jobName", jobExecution.getJobInstance().getJobName());
            response.put("status", jobExecution.getStatus().toString());
            response.put("startTime", jobExecution.getStartTime());
            response.put("message", "增量索引 Job 启动成功");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("message", "增量索引 Job 启动失败");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 启动指定类型的索引 Job
     */
    @PostMapping("/start/{jobType}")
    public ResponseEntity<Map<String, Object>> startJob(@PathVariable String jobType) {
        try {
            Job job;
            String jobName;
            
            switch (jobType.toLowerCase()) {
                case "full":
                case "full-index":
                    job = fullIndexJob;
                    jobName = "全量索引";
                    break;
                case "incremental":
                case "incremental-index":
                    job = incrementalIndexJob;
                    jobName = "增量索引";
                    break;
                default:
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("error", "不支持的 Job 类型: " + jobType);
                    response.put("message", "支持的 Job 类型: full, incremental");
                    return ResponseEntity.badRequest().body(response);
            }

            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .addString("jobType", jobType.toUpperCase())
                    .toJobParameters();

            JobExecution jobExecution = jobLauncher.run(job, jobParameters);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("jobId", jobExecution.getId());
            response.put("jobName", jobName);
            response.put("status", jobExecution.getStatus().toString());
            response.put("startTime", jobExecution.getStartTime());
            response.put("message", jobName + " Job 启动成功");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("message", "Job 启动失败");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 获取 Job 状态
     */
    @GetMapping("/status/{jobId}")
    public ResponseEntity<Map<String, Object>> getJobStatus(@PathVariable Long jobId) {
        try {
            // 这里需要 JobExplorer 来查询 Job 状态
            // 简化实现，返回基本信息
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("jobId", jobId);
            response.put("message", "Job 状态查询功能待实现");
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("message", "查询 Job 状态失败");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 获取所有可用的 Job 类型
     */
    @GetMapping("/types")
    public ResponseEntity<Map<String, Object>> getJobTypes() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("jobTypes", new String[]{"full", "incremental"});
        response.put("descriptions", new String[]{
            "全量索引 - 重建整个索引",
            "增量索引 - 只处理变更的数据"
        });
        response.put("message", "可用的 Job 类型");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("message", "Job API 服务正常");
        
        return ResponseEntity.ok(response);
    }
}
