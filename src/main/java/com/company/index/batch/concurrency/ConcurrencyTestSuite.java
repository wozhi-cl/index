package com.company.index.batch.concurrency;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 并发测试套件
 * 验证多节点互斥与幂等性
 */
@Component
public class ConcurrencyTestSuite {

    @Autowired
    private ConcurrencyValidator concurrencyValidator;

    @Autowired
    private IdempotencyValidator idempotencyValidator;

    /**
     * 测试并发执行互斥
     */
    public void testConcurrentExecution() {
        System.out.println("=== 测试并发执行互斥 ===");
        
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    String jobName = "test-job";
                    String instanceId = "instance-" + threadId;
                    
                    if (concurrencyValidator.canExecute(jobName, instanceId)) {
                        concurrencyValidator.recordJobStart(jobName, instanceId);
                        
                        // 模拟任务执行
                        Thread.sleep(1000);
                        
                        concurrencyValidator.recordJobComplete(jobName, instanceId);
                        successCount.incrementAndGet();
                        System.out.println("Thread " + threadId + " executed successfully");
                    } else {
                        failureCount.incrementAndGet();
                        System.out.println("Thread " + threadId + " was blocked");
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.out.println("Thread " + threadId + " failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        executor.shutdown();
        
        System.out.println("并发测试结果:");
        System.out.println("成功执行: " + successCount.get());
        System.out.println("被阻塞: " + failureCount.get());
        System.out.println("预期: 只有1个成功，其他被阻塞");
    }

    /**
     * 测试幂等性
     */
    public void testIdempotency() {
        System.out.println("=== 测试幂等性 ===");
        
        String operationId = "test-operation-" + System.currentTimeMillis();
        String operationType = "INDEX_UPDATE";
        String instanceId = "instance-1";

        // 第一次执行
        System.out.println("第一次执行操作: " + operationId);
        if (idempotencyValidator.isIdempotent(operationId, operationType, instanceId)) {
            idempotencyValidator.recordOperationStart(operationId, operationType, instanceId);
            
            // 模拟操作执行
            try {
                Thread.sleep(500);
                idempotencyValidator.recordOperationComplete(operationId, instanceId);
                System.out.println("第一次执行完成");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 第二次执行（应该被识别为幂等）
        System.out.println("第二次执行操作: " + operationId);
        if (idempotencyValidator.isIdempotent(operationId, operationType, instanceId)) {
            System.out.println("第二次执行被允许（不应该发生）");
        } else {
            System.out.println("第二次执行被阻止（正确行为）");
        }

        // 检查操作状态
        IdempotencyValidator.OperationStatus status = idempotencyValidator.getOperationStatus(operationId);
        System.out.println("操作状态: " + status);
    }

    /**
     * 测试节点漂移场景
     */
    public void testNodeFailover() {
        System.out.println("=== 测试节点漂移场景 ===");
        
        String jobName = "failover-test-job";
        String instanceId1 = "node-1";
        String instanceId2 = "node-2";

        // 节点1开始执行任务
        System.out.println("节点1开始执行任务");
        if (concurrencyValidator.canExecute(jobName, instanceId1)) {
            concurrencyValidator.recordJobStart(jobName, instanceId1);
            System.out.println("节点1任务开始执行");
        }

        // 模拟节点1故障，节点2尝试接管
        System.out.println("模拟节点1故障，节点2尝试接管");
        if (concurrencyValidator.canExecute(jobName, instanceId2)) {
            System.out.println("节点2可以执行任务（节点1已故障）");
            concurrencyValidator.recordJobStart(jobName, instanceId2);
            concurrencyValidator.recordJobComplete(jobName, instanceId2);
        } else {
            System.out.println("节点2被阻止执行（节点1仍在运行）");
        }

        // 清理测试数据
        concurrencyValidator.cleanupExpiredRecords();
    }

    /**
     * 测试重启场景
     */
    public void testRestartScenario() {
        System.out.println("=== 测试重启场景 ===");
        
        String operationId = "restart-test-operation";
        String operationType = "DATA_PROCESSING";
        String instanceId = "instance-1";

        // 记录操作开始
        System.out.println("记录操作开始");
        idempotencyValidator.recordOperationStart(operationId, operationType, instanceId);

        // 模拟应用重启
        System.out.println("模拟应用重启");
        
        // 重启后检查操作状态
        IdempotencyValidator.OperationStatus status = idempotencyValidator.getOperationStatus(operationId);
        System.out.println("重启后操作状态: " + status);

        // 尝试重新执行相同操作
        if (idempotencyValidator.isIdempotent(operationId, operationType, instanceId)) {
            System.out.println("重启后可以重新执行操作");
        } else {
            System.out.println("重启后操作被识别为已完成");
        }
    }

    /**
     * 运行所有测试
     */
    public void runAllTests() {
        System.out.println("开始并发和幂等性测试...");
        
        testConcurrentExecution();
        System.out.println();
        
        testIdempotency();
        System.out.println();
        
        testNodeFailover();
        System.out.println();
        
        testRestartScenario();
        System.out.println();
        
        System.out.println("所有测试完成！");
    }
}
