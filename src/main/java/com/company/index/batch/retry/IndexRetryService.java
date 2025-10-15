package com.company.index.batch.retry;

import com.company.index.batch.check.IndexCheckService;
import com.company.index.batch.writer.WriterFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 索引重试与恢复服务
 */
@Service
public class IndexRetryService {

    @Autowired
    private IndexCheckService indexCheckService;

    @Autowired
    private WriterFactory writerFactory;

    @Value("${index.retry.backoff.initial:1000}")
    private long initialDelay;

    @Value("${index.retry.backoff.max:30000}")
    private long maxDelay;

    @Value("${index.retry.backoff.attempts:3}")
    private int maxAttempts;

    @Value("${index.retry.threshold.failureRate:0.1}")
    private double failureRateThreshold;

    /**
     * 执行重试逻辑
     */
    public boolean executeWithRetry(Runnable operation) {
        int attempts = 0;
        long delay = initialDelay;

        while (attempts < maxAttempts) {
            try {
                operation.run();
                return true;
            } catch (Exception e) {
                attempts++;
                if (attempts >= maxAttempts) {
                    throw new RuntimeException("Operation failed after " + maxAttempts + " attempts", e);
                }

                // 指数退避
                try {
                    TimeUnit.MILLISECONDS.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }

                delay = Math.min(delay * 2, maxDelay);
            }
        }

        return false;
    }

    /**
     * 检查是否需要重建
     */
    public boolean shouldRebuild() {
        try {
            // 检查索引健康状态
            if (!indexCheckService.checkIndexIntegrity()) {
                return true;
            }

            // 检查失败率
            double failureRate = calculateFailureRate();
            if (failureRate > failureRateThreshold) {
                return true;
            }

            return false;
        } catch (Exception e) {
            // 如果检查失败，认为需要重建
            return true;
        }
    }

    /**
     * 计算失败率
     */
    private double calculateFailureRate() {
        // 这里可以实现更复杂的失败率计算逻辑
        // 例如：基于历史数据、错误日志等
        // 简化版本：返回 0
        return 0.0;
    }

    /**
     * 执行重建策略
     */
    public void executeRebuildStrategy() {
        try {
            // 检查是否需要重建
            if (shouldRebuild()) {
                // 执行全量重建
                executeFullRebuild();
            } else {
                // 执行增量重建
                executeIncrementalRebuild();
            }
        } catch (Exception e) {
            throw new RuntimeException("Rebuild strategy execution failed", e);
        }
    }

    /**
     * 执行全量重建
     */
    private void executeFullRebuild() {
        try {
            // 删除现有索引
            writerFactory.deleteIndex();

            // 重新创建索引
            writerFactory.createIndexIfNotExists();

            // 这里可以触发全量索引任务
            // 例如：通过 JobLauncher 启动全量索引 Job

        } catch (Exception e) {
            throw new RuntimeException("Full rebuild failed", e);
        }
    }

    /**
     * 执行增量重建
     */
    private void executeIncrementalRebuild() {
        try {
            // 这里可以实现增量重建逻辑
            // 例如：重新处理最近的数据、修复损坏的记录等

        } catch (Exception e) {
            throw new RuntimeException("Incremental rebuild failed", e);
        }
    }

    /**
     * 获取重试统计信息
     */
    public RetryStats getRetryStats() {
        RetryStats stats = new RetryStats();
        stats.setMaxAttempts(maxAttempts);
        stats.setInitialDelay(initialDelay);
        stats.setMaxDelay(maxDelay);
        stats.setFailureRateThreshold(failureRateThreshold);
        stats.setTimestamp(LocalDateTime.now());
        return stats;
    }
}
