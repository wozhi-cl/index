package com.company.index.batch.reader;

import com.company.index.common.model.SourceRecord;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * CSV 文件读取器
 */
@Component
public class FileCsvReader {

    private final String csvPath;
    private final String[] fieldNames;

    public FileCsvReader(@Value("${index.dataSource.csvPath:./data/sample.csv}") String csvPath,
                        @Value("${index.dataSource.csvFields:id,name,email,created_at,updated_at}") String csvFields) {
        this.csvPath = csvPath;
        this.fieldNames = csvFields.split(",");
    }

    /**
     * 创建 CSV 读取器
     */
    public FlatFileItemReader<SourceRecord> createReader() {
        Resource resource = new FileSystemResource(csvPath);

        return new FlatFileItemReaderBuilder<SourceRecord>()
                .name("csvFileReader")
                .resource(resource)
                .lineMapper(createLineMapper())
                .linesToSkip(1) // 跳过标题行
                .build();
    }

    /**
     * 创建行映射器
     */
    private DefaultLineMapper<SourceRecord> createLineMapper() {
        DefaultLineMapper<SourceRecord> lineMapper = new DefaultLineMapper<>();
        
        // 设置分隔符
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setDelimiter(",");
        tokenizer.setNames(fieldNames);
        lineMapper.setLineTokenizer(tokenizer);

        // 设置字段映射器
        lineMapper.setFieldSetMapper(fieldSet -> {
            SourceRecord record = new SourceRecord();
            
            String id = fieldSet.readString("id");
            record.setId(id);
            record.setType("INSERT");
            record.setSource("CSV");
            record.setVersion(1L);
            
            // 解析时间戳
            String timeStr = fieldSet.readString("updated_at");
            LocalDateTime timestamp = parseTimestamp(timeStr);
            record.setTimestamp(timestamp);

            // 构建数据映射
            Map<String, Object> data = new HashMap<>();
            for (String fieldName : fieldNames) {
                Object value = fieldSet.readRawString(fieldName);
                data.put(fieldName, value);
            }
            record.setData(data);

            return record;
        });

        return lineMapper;
    }

    /**
     * 解析时间戳字符串
     */
    private LocalDateTime parseTimestamp(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return LocalDateTime.now();
        }
        
        try {
            // 支持多种时间格式
            if (timeStr.contains("T")) {
                return LocalDateTime.parse(timeStr);
            } else if (timeStr.contains("-") && timeStr.contains(":")) {
                return LocalDateTime.parse(timeStr.replace(" ", "T"));
            } else {
                return LocalDateTime.now();
            }
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
