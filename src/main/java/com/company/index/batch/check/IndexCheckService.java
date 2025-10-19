package com.company.index.batch.check;

import com.company.index.batch.writer.DataWriter;
import com.company.index.batch.writer.FileWriter;
import com.company.index.batch.writer.ElasticsearchWriter;
import com.company.index.batch.writer.GetQuickWriter;
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
    private FileWriter fileWriter;

    @Autowired
    private ElasticsearchWriter elasticsearchWriter;

    @Autowired
    private GetQuickWriter getQuickWriter;

    @Value("${index.check.thresholds.errorRate:0.01}")
    private double errorRateThreshold;

    @Value("${index.check.thresholds.sampleSize:1000}")
    private int sampleSize;

    @Value("${index.indexTarget.type:file}")
    private String indexTargetType;

    /**
     * 检查索引完整性
     */
    public boolean checkIndexIntegrity() {
        try {
            // 获取索引统计信息
            Map<String, Object> stats = getIndexStats();
            
            if (stats == null) {
                System.err.println("无法获取索引统计信息");
                return false;
            }
            
            // 检查错误率
            Object totalWrittenObj = stats.get("totalWritten");
            Object totalErrorsObj = stats.get("totalErrors");
            
            if (totalWrittenObj == null || totalErrorsObj == null) {
                System.err.println("统计信息不完整");
                return false;
            }
            
            long totalWritten = ((Number) totalWrittenObj).longValue();
            long totalErrors = ((Number) totalErrorsObj).longValue();
            
            if (totalWritten == 0) {
                System.out.println("没有数据写入，跳过检查");
                return true;
            }
            
            double errorRate = (double) totalErrors / totalWritten;
            
            System.out.println("========================================");
            System.out.println("索引完整性检查结果:");
            System.out.println("  索引类型: " + indexTargetType);
            System.out.println("  总写入: " + totalWritten);
            System.out.println("  总错误: " + totalErrors);
            System.out.println("  错误率: " + String.format("%.4f", errorRate));
            System.out.println("  阈值: " + String.format("%.4f", errorRateThreshold));
            System.out.println("========================================");
            
            boolean isHealthy = errorRate <= errorRateThreshold;
            
            if (isHealthy) {
                System.out.println("✓ 索引完整性检查通过");
            } else {
                System.err.println("✗ 索引完整性检查失败：错误率超过阈值");
            }
            
            return isHealthy;
            
        } catch (Exception e) {
            System.err.println("索引完整性检查异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取索引统计信息
     */
    public Map<String, Object> getIndexStats() {
        DataWriter writer = getDataWriter();
        if (writer == null) {
            return null;
        }
        
        try {
            if (writer instanceof FileWriter) {
                return (Map<String, Object>) ((FileWriter) writer).getStats();
            } else if (writer instanceof ElasticsearchWriter) {
                return (Map<String, Object>) ((ElasticsearchWriter) writer).getStats();
            } else if (writer instanceof GetQuickWriter) {
                return (Map<String, Object>) ((GetQuickWriter) writer).getStats();
            }
        } catch (Exception e) {
            System.err.println("获取统计信息失败: " + e.getMessage());
        }
        
        return null;
    }

    /**
     * 根据配置获取数据写入器
     */
    private DataWriter getDataWriter() {
        switch (indexTargetType.toLowerCase()) {
            case "file":
                return fileWriter;
            case "elasticsearch":
            case "es":
                return elasticsearchWriter;
            case "getquick":
            case "gq":
                return getQuickWriter;
            default:
                System.err.println("不支持的索引目标类型: " + indexTargetType);
                return null;
        }
    }

    /**
     * 检查索引健康状态
     */
    public boolean isIndexHealthy() {
        try {
            DataWriter writer = getDataWriter();
            if (writer == null) {
                return false;
            }
            
            // 这里可以添加更复杂的健康检查逻辑
            // 比如检查索引是否可访问、数据是否一致等
            
            return true;
            
        } catch (Exception e) {
            System.err.println("索引健康检查异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取检查报告
     */
    public String getCheckReport() {
        StringBuilder report = new StringBuilder();
        
        report.append("索引完整性检查报告\n");
        report.append("==================\n");
        report.append("索引类型: ").append(indexTargetType).append("\n");
        
        Map<String, Object> stats = getIndexStats();
        if (stats != null) {
            report.append("总写入: ").append(stats.get("totalWritten")).append("\n");
            report.append("总错误: ").append(stats.get("totalErrors")).append("\n");
            
            Object totalWrittenObj = stats.get("totalWritten");
            Object totalErrorsObj = stats.get("totalErrors");
            
            if (totalWrittenObj != null && totalErrorsObj != null) {
                long totalWritten = ((Number) totalWrittenObj).longValue();
                long totalErrors = ((Number) totalErrorsObj).longValue();
                
                if (totalWritten > 0) {
                    double errorRate = (double) totalErrors / totalWritten;
                    report.append("错误率: ").append(String.format("%.4f", errorRate)).append("\n");
                    report.append("阈值: ").append(String.format("%.4f", errorRateThreshold)).append("\n");
                    report.append("状态: ").append(errorRate <= errorRateThreshold ? "通过" : "失败").append("\n");
                }
            }
        } else {
            report.append("无法获取统计信息\n");
        }
        
        report.append("健康状态: ").append(isIndexHealthy() ? "健康" : "异常").append("\n");
        
        return report.toString();
    }
}