package com.company.index.batch.support;

import com.company.index.batch.writer.WriterFactory;
import com.company.index.common.model.SourceRecord;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * ItemWriter 工厂与装配
 */
@Component
public class ItemWriters {

    @Autowired
    private WriterFactory writerFactory;

    /**
     * 创建索引写入器
     */
    public ItemWriter<SourceRecord> createIndexWriter() {
        return writerFactory.createWriter();
    }

    /**
     * 创建索引（如果不存在）
     */
    public void createIndexIfNotExists() throws Exception {
        writerFactory.createIndexIfNotExists();
    }

    /**
     * 删除索引
     */
    public void deleteIndex() throws Exception {
        writerFactory.deleteIndex();
    }

    /**
     * 获取索引统计信息
     */
    public Object getIndexStats() throws Exception {
        return writerFactory.getIndexStats();
    }

    /**
     * 健康检查
     */
    public boolean isHealthy() {
        return writerFactory.isHealthy();
    }
}
