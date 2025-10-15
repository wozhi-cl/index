package com.company.index.batch.partition;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 远程分片配置
 * 主控 Step 将分片任务分发给远程工作器
 */
@Configuration
public class RemotePartitionConfig {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private RangePartitioner rangePartitioner;

    @Autowired
    private HashPartitioner hashPartitioner;

    @Value("${index.parallelism.partitions:4}")
    private int partitionCount;

    @Value("${index.remote.workerUrl:http://localhost:8080}")
    private String workerUrl;

    @Value("${index.remote.timeout:30000}")
    private long timeout;

    /**
     * 远程分片 Job
     */
    @Bean
    public Job remotePartitionJob() {
        return new JobBuilder("remotePartitionJob", jobRepository)
                .start(masterStep())
                .build();
    }

    /**
     * 主控 Step
     */
    @Bean
    public Step masterStep() {
        return new StepBuilder("masterStep", jobRepository)
                .partitioner("workerStep", partitioner())
                .gridSize(partitionCount)
                .build();
    }

    /**
     * 工作器 Step
     */
    @Bean
    public Step workerStep() {
        return new StepBuilder("workerStep", jobRepository)
                .tasklet(workerTasklet(), transactionManager)
                .build();
    }

    /**
     * 分片器
     */
    @Bean
    public Partitioner partitioner() {
        // 可以根据配置选择不同的分片器
        return rangePartitioner;
    }

    /**
     * 工作器任务
     */
    @Bean
    public Tasklet workerTasklet() {
        return (contribution, chunkContext) -> {
            // 获取分片参数
            String partitionName = (String) chunkContext.getStepContext()
                    .getStepExecutionContext()
                    .get("partitionName");
            
            int partitionNumber = (Integer) chunkContext.getStepContext()
                    .getStepExecutionContext()
                    .get("partitionNumber");
            
            // 执行分片任务
            executePartitionTask(partitionName, partitionNumber);
            
            return null;
        };
    }

    /**
     * 执行分片任务
     */
    private void executePartitionTask(String partitionName, int partitionNumber) {
        try {
            System.out.println("Executing partition task: " + partitionName + " (partition: " + partitionNumber + ")");
            
            // 这里可以实现具体的分片任务逻辑
            // 例如：读取分片数据、处理、写入索引等
            
            // 模拟任务执行
            Thread.sleep(1000);
            
            System.out.println("Partition task completed: " + partitionName);
            
        } catch (Exception e) {
            throw new RuntimeException("Partition task failed: " + partitionName, e);
        }
    }

    /**
     * 创建分片任务
     */
    public void createPartitionTasks() {
        try {
            // 创建分片任务
            for (int i = 0; i < partitionCount; i++) {
                createPartitionTask(i);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create partition tasks", e);
        }
    }

    /**
     * 创建单个分片任务
     */
    private void createPartitionTask(int partitionNumber) {
        try {
            // 这里可以实现分片任务的创建逻辑
            // 例如：发送到消息队列、调用远程 API 等
            
            System.out.println("Creating partition task: " + partitionNumber);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to create partition task: " + partitionNumber, e);
        }
    }

    /**
     * 监控分片任务状态
     */
    public void monitorPartitionTasks() {
        try {
            // 这里可以实现分片任务的状态监控逻辑
            // 例如：检查任务完成状态、处理失败任务等
            
            System.out.println("Monitoring partition tasks...");
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to monitor partition tasks", e);
        }
    }
}
