package com.company.index.batch.support;

import com.company.index.batch.reader.ReaderFactory;
import com.company.index.common.model.SourceRecord;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * ItemReader 工厂与装配
 */
@Component
public class ItemReaders {

    @Autowired
    private ReaderFactory readerFactory;

    @Value("${index.parallelism.chunkSize:1000}")
    private int chunkSize;

    @Value("${index.incremental.overlapMinutes:5}")
    private int overlapMinutes;

    /**
     * 创建全量读取器
     */
    public ItemReader<SourceRecord> createFullReader() {
        return readerFactory.createFullReader(chunkSize);
    }

    /**
     * 创建增量读取器
     */
    public ItemReader<SourceRecord> createIncrementalReader() {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusMinutes(5); // 默认 5 分钟窗口
        
        return readerFactory.createDeltaReader(startTime, endTime, overlapMinutes);
    }

    /**
     * 创建增量读取器（自定义时间窗口）
     */
    public ItemReader<SourceRecord> createIncrementalReader(LocalDateTime startTime, LocalDateTime endTime) {
        return readerFactory.createDeltaReader(startTime, endTime, overlapMinutes);
    }

    /**
     * 创建变更表读取器
     */
    public ItemReader<SourceRecord> createChangeTableReader(Long lastProcessedId) {
        return readerFactory.createChangeTableReader(lastProcessedId);
    }
}
