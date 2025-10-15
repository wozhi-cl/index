package com.company.index.batch.writer;

import com.company.index.common.model.SourceRecord;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Writer 工厂类
 * 根据配置创建不同类型的 Writer
 */
@Component
public class WriterFactory {

    @Autowired
    private ElasticsearchWriter elasticsearchWriter;

    @Autowired(required = false)
    private GetQuickWriter getQuickWriter;

    @Autowired(required = false)
    private IndexFileWriter fileWriter;

    @Value("${index.indexTarget.type:file}")
    private String indexTargetType;

    /**
     * 创建索引写入器
     */
    public ItemWriter<SourceRecord> createWriter() {
        switch (indexTargetType.toLowerCase()) {
            case "elasticsearch":
            case "es":
                return elasticsearchWriter;
            case "getquick":
            case "gq":
                if (getQuickWriter == null) {
                    throw new UnsupportedOperationException("GetQuick writer not available in current profile");
                }
                return getQuickWriter;
            case "file":
                if (fileWriter == null) {
                    throw new UnsupportedOperationException("File writer not available in current profile");
                }
                return fileWriter;
            default:
                throw new IllegalArgumentException("Unsupported index target type: " + indexTargetType);
        }
    }

    /**
     * 创建索引（如果不存在）
     */
    public void createIndexIfNotExists() throws Exception {
        switch (indexTargetType.toLowerCase()) {
            case "elasticsearch":
            case "es":
                elasticsearchWriter.createIndexIfNotExists();
                break;
            case "getquick":
            case "gq":
                if (getQuickWriter == null) {
                    throw new UnsupportedOperationException("GetQuick writer not available in current profile");
                }
                getQuickWriter.createIndexIfNotExists();
                break;
            default:
                throw new IllegalArgumentException("Unsupported index target type: " + indexTargetType);
        }
    }

    /**
     * 删除索引
     */
    public void deleteIndex() throws Exception {
        switch (indexTargetType.toLowerCase()) {
            case "elasticsearch":
            case "es":
                elasticsearchWriter.deleteIndex();
                break;
            case "getquick":
            case "gq":
                if (getQuickWriter == null) {
                    throw new UnsupportedOperationException("GetQuick writer not available in current profile");
                }
                getQuickWriter.deleteIndex();
                break;
            default:
                throw new IllegalArgumentException("Unsupported index target type: " + indexTargetType);
        }
    }

    /**
     * 获取索引统计信息
     */
    public Object getIndexStats() throws Exception {
        switch (indexTargetType.toLowerCase()) {
            case "elasticsearch":
            case "es":
                return elasticsearchWriter.getIndexStats();
            case "getquick":
            case "gq":
                if (getQuickWriter == null) {
                    throw new UnsupportedOperationException("GetQuick writer not available in current profile");
                }
                return getQuickWriter.getIndexStats();
            default:
                throw new IllegalArgumentException("Unsupported index target type: " + indexTargetType);
        }
    }

    /**
     * 健康检查
     */
    public boolean isHealthy() {
        switch (indexTargetType.toLowerCase()) {
            case "elasticsearch":
            case "es":
                try {
                    elasticsearchWriter.getIndexStats();
                    return true;
                } catch (Exception e) {
                    return false;
                }
            case "getquick":
            case "gq":
                if (getQuickWriter == null) {
                    return false;
                }
                return getQuickWriter.isHealthy();
            default:
                return false;
        }
    }
}
