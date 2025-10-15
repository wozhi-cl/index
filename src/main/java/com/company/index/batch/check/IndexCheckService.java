package com.company.index.batch.check;

import com.company.index.batch.writer.WriterFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 索引完整性检查服务
 */
@Service
public class IndexCheckService {

    @Autowired
    private WriterFactory writerFactory;

    @Value("${index.check.thresholds.errorRate:0.01}")
    private double errorRateThreshold;

    @Value("${index.check.thresholds.sampleSize:1000}")
    private int sampleSize;

    /**
     * 检查索引完整性
     */
    public boolean checkIndexIntegrity() {
        try {
            // 获取索引统计信息
            Object stats = writerFactory.getIndexStats();
            
            // 检查索引是否可访问
            if (!writerFactory.isHealthy()) {
                return false;
            }

            // 检查记录数量（如果支持）
            if (stats instanceof Map) {
                Map<String, Object> statsMap = (Map<String, Object>) stats;
                Long docCount = extractDocCount(statsMap);
                
                if (docCount != null && docCount > 0) {
                    // 执行采样检查
                    return performSamplingCheck(docCount);
                }
            }

            // 如果无法获取统计信息，执行基本健康检查
            return performBasicHealthCheck();

        } catch (Exception e) {
            throw new RuntimeException("Index integrity check failed", e);
        }
    }

    /**
     * 提取文档数量
     */
    private Long extractDocCount(Map<String, Object> statsMap) {
        try {
            // 尝试从不同路径提取文档数量
            if (statsMap.containsKey("count")) {
                return ((Number) statsMap.get("count")).longValue();
            }
            
            if (statsMap.containsKey("total")) {
                Object total = statsMap.get("total");
                if (total instanceof Map) {
                    Map<String, Object> totalMap = (Map<String, Object>) total;
                    if (totalMap.containsKey("docs")) {
                        Object docs = totalMap.get("docs");
                        if (docs instanceof Map) {
                            Map<String, Object> docsMap = (Map<String, Object>) docs;
                            if (docsMap.containsKey("count")) {
                                return ((Number) docsMap.get("count")).longValue();
                            }
                        }
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 执行采样检查
     */
    private boolean performSamplingCheck(Long docCount) {
        try {
            // 计算采样大小
            int actualSampleSize = Math.min(sampleSize, docCount.intValue());
            
            // 这里可以实现更复杂的采样检查逻辑
            // 例如：随机采样、查询验证、数据一致性检查等
            
            // 简化版本：检查索引是否可访问
            return writerFactory.isHealthy();
            
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 执行基本健康检查
     */
    private boolean performBasicHealthCheck() {
        try {
            // 检查索引是否可访问
            return writerFactory.isHealthy();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取检查指标
     */
    public CheckMetrics getCheckMetrics() {
        CheckMetrics metrics = new CheckMetrics();
        
        try {
            Object stats = writerFactory.getIndexStats();
            metrics.setIndexStats(stats);
            metrics.setHealthy(writerFactory.isHealthy());
            metrics.setTimestamp(java.time.LocalDateTime.now());
            
            if (stats instanceof Map) {
                Map<String, Object> statsMap = (Map<String, Object>) stats;
                Long docCount = extractDocCount(statsMap);
                metrics.setDocCount(docCount);
            }
            
        } catch (Exception e) {
            metrics.setHealthy(false);
            metrics.setError(e.getMessage());
        }
        
        return metrics;
    }
}
