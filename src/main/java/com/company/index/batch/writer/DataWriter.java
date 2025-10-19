package com.company.index.batch.writer;

import com.company.index.common.model.SourceRecord;
import com.company.index.common.model.IndexDocument;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

/**
 * 数据写入器接口
 * 提供源数据和索引数据的写入方法
 */
public interface DataWriter {
    
    /**
     * 写入源数据
     * 
     * @param items 源数据列表
     * @throws Exception 写入异常
     */
    void writeSourceData(List<? extends SourceRecord> items) throws Exception;
    
    /**
     * 写入索引数据
     * 
     * @param items 索引数据列表
     * @throws Exception 写入异常
     */
    void writeIndexData(List<? extends IndexDocument> items) throws Exception;
    
    /**
     * 创建源数据写入器
     * 
     * @return 源数据写入器
     */
    ItemWriter<SourceRecord> createSourceWriter();
    
    /**
     * 创建索引数据写入器
     * 
     * @return 索引数据写入器
     */
    ItemWriter<IndexDocument> createIndexWriter();
    
    /**
     * 完成写入操作
     * 
     * @throws Exception 完成异常
     */
    void finishWrite() throws Exception;
    
    /**
     * 删除索引
     * 
     * @throws Exception 删除异常
     */
    void deleteIndex() throws Exception;
    
    /**
     * 获取统计信息
     * 
     * @return 统计信息
     */
    Object getStats();
}
