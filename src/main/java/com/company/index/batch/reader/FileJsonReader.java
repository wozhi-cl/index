package com.company.index.batch.reader;

import com.company.index.common.model.SourceRecord;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.item.file.transform.DefaultFieldSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * JSON 文件读取器
 */
@Component
public class FileJsonReader {

    private final String jsonPath;
    private final ObjectMapper objectMapper;

    public FileJsonReader(@Value("${index.dataSource.jsonPath:./data/sample.json}") String jsonPath) {
        this.jsonPath = jsonPath;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 创建 JSON 读取器
     */
    public FlatFileItemReader<SourceRecord> createReader() {
        Resource resource = new FileSystemResource(jsonPath);

        return new FlatFileItemReaderBuilder<SourceRecord>()
                .name("jsonFileReader")
                .resource(resource)
                .lineMapper(createLineMapper())
                .build();
    }

    /**
     * 创建行映射器
     */
    private DefaultLineMapper<SourceRecord> createLineMapper() {
        DefaultLineMapper<SourceRecord> lineMapper = new DefaultLineMapper<>();
        
        // 设置行标记器（每行一个 JSON 对象）
        lineMapper.setLineTokenizer(new LineTokenizer() {
            @Override
            public FieldSet tokenize(String line) {
                return new DefaultFieldSet(new String[]{line});
            }
        });

        // 设置字段映射器
        lineMapper.setFieldSetMapper(fieldSet -> {
            String jsonLine = fieldSet.readString(0);
            return parseJsonLine(jsonLine);
        });

        return lineMapper;
    }

    /**
     * 解析 JSON 行
     */
    private SourceRecord parseJsonLine(String jsonLine) {
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonLine);
            
            SourceRecord record = new SourceRecord();
            
            // 提取 ID
            String id = jsonNode.has("id") ? jsonNode.get("id").asText() : 
                       jsonNode.has("_id") ? jsonNode.get("_id").asText() : 
                       String.valueOf(System.currentTimeMillis());
            record.setId(id);
            
            // 提取类型
            String type = jsonNode.has("type") ? jsonNode.get("type").asText() : "INSERT";
            record.setType(type);
            
            // 提取时间戳
            LocalDateTime timestamp = extractTimestamp(jsonNode);
            record.setTimestamp(timestamp);
            
            // 设置源和版本
            record.setSource("JSON");
            record.setVersion(1L);
            
            // 构建数据映射
            Map<String, Object> data = new HashMap<>();
            jsonNode.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                data.put(key, convertJsonValue(value));
            });
            record.setData(data);
            
            return record;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON line: " + jsonLine, e);
        }
    }

    /**
     * 提取时间戳
     */
    private LocalDateTime extractTimestamp(JsonNode jsonNode) {
        String[] timeFields = {"updated_at", "updatedAt", "timestamp", "created_at", "createdAt", "date"};
        
        for (String field : timeFields) {
            if (jsonNode.has(field)) {
                String timeStr = jsonNode.get(field).asText();
                return parseTimestamp(timeStr);
            }
        }
        
        return LocalDateTime.now();
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

    /**
     * 转换 JSON 值
     */
    private Object convertJsonValue(JsonNode jsonNode) {
        if (jsonNode.isTextual()) {
            return jsonNode.asText();
        } else if (jsonNode.isNumber()) {
            return jsonNode.asDouble();
        } else if (jsonNode.isBoolean()) {
            return jsonNode.asBoolean();
        } else if (jsonNode.isNull()) {
            return null;
        } else if (jsonNode.isArray()) {
            return jsonNode.toString();
        } else if (jsonNode.isObject()) {
            return jsonNode.toString();
        } else {
            return jsonNode.asText();
        }
    }
}
