package com.company.index.batch.partition;

import com.company.index.common.util.HashingUtil;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 哈希分片器
 * 基于稳定哈希分配数据
 */
@Component
public class HashPartitioner implements Partitioner {

    @Value("${index.parallelism.partitions:4}")
    private int partitionCount;

    @Value("${index.partition.hashColumn:id}")
    private String hashColumn;

    @Value("${index.partition.hashFunction:MD5}")
    private String hashFunction;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> partitions = new HashMap<>();
        
        int actualPartitions = Math.min(partitionCount, gridSize);
        
        for (int i = 0; i < actualPartitions; i++) {
            ExecutionContext context = new ExecutionContext();
            
            // 设置分片参数
            context.putString("partitionName", "hash-partition-" + i);
            context.putInt("partitionNumber", i);
            context.putString("hashColumn", hashColumn);
            context.putString("hashFunction", hashFunction);
            context.putInt("partitionCount", actualPartitions);
            context.putString("sqlCondition", buildHashSqlCondition(hashColumn, i, actualPartitions));
            
            partitions.put("hash-partition-" + i, context);
        }
        
        return partitions;
    }

    /**
     * 构建哈希 SQL 条件
     */
    private String buildHashSqlCondition(String column, int partitionNumber, int totalPartitions) {
        // 使用模运算进行哈希分片
        return "MOD(ABS(CRC32(" + column + ")), " + totalPartitions + ") = " + partitionNumber;
    }

    /**
     * 基于哈希值分片
     */
    public Map<String, ExecutionContext> partitionByHashValue(int gridSize, 
                                                           String hashColumn,
                                                           String hashValue) {
        Map<String, ExecutionContext> partitions = new HashMap<>();
        
        int actualPartitions = Math.min(partitionCount, gridSize);
        
        // 计算哈希值
        int hash = HashingUtil.calculateHash(hashValue, hashFunction);
        int partitionNumber = Math.abs(hash) % actualPartitions;
        
        ExecutionContext context = new ExecutionContext();
        context.putString("partitionName", "hash-partition-" + partitionNumber);
        context.putInt("partitionNumber", partitionNumber);
        context.putString("hashColumn", hashColumn);
        context.putString("hashValue", hashValue);
        context.putInt("hash", hash);
        context.putString("sqlCondition", buildHashValueSqlCondition(hashColumn, hashValue, actualPartitions));
        
        partitions.put("hash-partition-" + partitionNumber, context);
        
        return partitions;
    }

    /**
     * 构建哈希值 SQL 条件
     */
    private String buildHashValueSqlCondition(String column, String hashValue, int totalPartitions) {
        int hash = HashingUtil.calculateHash(hashValue, hashFunction);
        int partitionNumber = Math.abs(hash) % totalPartitions;
        
        return "MOD(ABS(CRC32(" + column + ")), " + totalPartitions + ") = " + partitionNumber;
    }
}
