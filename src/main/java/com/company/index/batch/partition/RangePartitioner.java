package com.company.index.batch.partition;

import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 范围分片器
 * 按主键范围或时间范围分片
 */
@Component
public class RangePartitioner implements Partitioner {

    @Value("${index.parallelism.partitions:4}")
    private int partitionCount;

    @Value("${index.partition.rangeColumn:id}")
    private String rangeColumn;

    @Value("${index.partition.minValue:1}")
    private long minValue;

    @Value("${index.partition.maxValue:1000000}")
    private long maxValue;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> partitions = new HashMap<>();
        
        // 使用配置的分片数或网格大小
        int actualPartitions = Math.min(partitionCount, gridSize);
        
        // 计算每个分片的范围
        long rangeSize = (maxValue - minValue) / actualPartitions;
        
        for (int i = 0; i < actualPartitions; i++) {
            ExecutionContext context = new ExecutionContext();
            
            // 计算分片范围
            long startValue = minValue + (i * rangeSize);
            long endValue = (i == actualPartitions - 1) ? maxValue : startValue + rangeSize - 1;
            
            // 设置分片参数
            context.putString("partitionName", "partition-" + i);
            context.putInt("partitionNumber", i);
            context.putString("rangeColumn", rangeColumn);
            context.putLong("startValue", startValue);
            context.putLong("endValue", endValue);
            context.putString("sqlCondition", buildSqlCondition(rangeColumn, startValue, endValue));
            
            partitions.put("partition-" + i, context);
        }
        
        return partitions;
    }

    /**
     * 构建 SQL 条件
     */
    private String buildSqlCondition(String column, long startValue, long endValue) {
        return column + " >= " + startValue + " AND " + column + " <= " + endValue;
    }

    /**
     * 按时间范围分片
     */
    public Map<String, ExecutionContext> partitionByTimeRange(int gridSize, 
                                                             String timeColumn,
                                                             String startTime, 
                                                             String endTime) {
        Map<String, ExecutionContext> partitions = new HashMap<>();
        
        int actualPartitions = Math.min(partitionCount, gridSize);
        
        for (int i = 0; i < actualPartitions; i++) {
            ExecutionContext context = new ExecutionContext();
            
            // 这里可以实现更复杂的时间分片逻辑
            // 例如：按小时、天、周等分片
            context.putString("partitionName", "time-partition-" + i);
            context.putInt("partitionNumber", i);
            context.putString("timeColumn", timeColumn);
            context.putString("startTime", startTime);
            context.putString("endTime", endTime);
            context.putString("sqlCondition", buildTimeSqlCondition(timeColumn, startTime, endTime));
            
            partitions.put("time-partition-" + i, context);
        }
        
        return partitions;
    }

    /**
     * 构建时间 SQL 条件
     */
    private String buildTimeSqlCondition(String timeColumn, String startTime, String endTime) {
        return timeColumn + " >= '" + startTime + "' AND " + timeColumn + " <= '" + endTime + "'";
    }
}
