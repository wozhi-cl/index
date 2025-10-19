package com.company.index.batch.reader;

import com.company.index.common.model.SourceRecord;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.JdbcCursorItemReader;

import java.time.LocalDateTime;

/**
 * 数据读取器接口
 * 提供全量和增量读取方法
 */
public interface DataReader {
    
    /**
     * 创建全量读取器
     * 
     * @param pageSize 分页大小
     * @return 全量读取器
     */
    ItemReader<SourceRecord> createFullReader(int pageSize);
    
    /**
     * 创建增量读取器
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 增量读取器
     */
    JdbcCursorItemReader<SourceRecord> createDeltaReader(LocalDateTime startTime, LocalDateTime endTime);
}
