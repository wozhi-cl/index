package com.company.index.batch.reader;

import com.company.index.common.model.SourceRecord;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Reader 工厂类
 * 根据配置创建不同类型的 Reader
 */
@Component
public class ReaderFactory {

    @Autowired
    private RdbFullReader rdbFullReader;

    @Autowired
    private RdbDeltaReader rdbDeltaReader;

    @Autowired
    private FileCsvReader fileCsvReader;

    @Autowired
    private FileJsonReader fileJsonReader;

    @Value("${index.dataSource.type:h2}")
    private String dataSourceType;

    /**
     * 创建全量读取器
     */
    public ItemReader<SourceRecord> createFullReader(int pageSize) {
        switch (dataSourceType.toLowerCase()) {
            case "mysql":
            case "oracle":
            case "h2":
                return rdbFullReader.createPagingReader(pageSize);
            case "csv":
                return fileCsvReader.createReader();
            case "json":
                return fileJsonReader.createReader();
            default:
                throw new IllegalArgumentException("Unsupported data source type: " + dataSourceType);
        }
    }

    /**
     * 创建增量读取器
     */
    public ItemReader<SourceRecord> createDeltaReader(java.time.LocalDateTime startTime, 
                                                     java.time.LocalDateTime endTime, 
                                                     int overlapMinutes) {
        switch (dataSourceType.toLowerCase()) {
            case "mysql":
            case "oracle":
            case "h2":
                return rdbDeltaReader.createTimeBasedReader(startTime, endTime, overlapMinutes);
            case "csv":
            case "json":
                // 文件类型不支持增量，返回全量读取器
                return createFullReader(1000);
            default:
                throw new IllegalArgumentException("Unsupported data source type: " + dataSourceType);
        }
    }

    /**
     * 创建变更表读取器
     */
    public ItemReader<SourceRecord> createChangeTableReader(Long lastProcessedId) {
        if ("mysql".equals(dataSourceType.toLowerCase()) || 
            "oracle".equals(dataSourceType.toLowerCase()) || 
            "h2".equals(dataSourceType.toLowerCase())) {
            // 使用最近一小时的变更日志
            LocalDateTime since = LocalDateTime.now().minusHours(1);
            return rdbDeltaReader.createChangeLogReader(since);
        } else {
            throw new IllegalArgumentException("Change table reader only supports RDB data sources");
        }
    }
}
