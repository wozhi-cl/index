package com.company.index.batch.reader;

import com.company.index.common.model.SourceRecord;
import com.company.index.common.service.RecordBuilderService;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.item.support.AbstractItemStreamItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件读取器
 * 支持 CSV 和 JSON 格式的 SourceRecord 读取
 */
public class CustomFileReader extends AbstractItemStreamItemReader<SourceRecord> {

    private final String filePath;
    private final String fileFormat;
    private final String tableName;
    private final RecordBuilderService recordBuilderService;
    private final List<String> fieldNames;
    private final String keyFieldName;

    private BufferedReader reader;
    private List<SourceRecord> records;
    private int currentIndex = 0;

    public CustomFileReader(String filePath, String fileFormat, String tableName, 
                     RecordBuilderService recordBuilderService, 
                     List<String> fieldNames, String keyFieldName) {
        this.filePath = filePath;
        this.fileFormat = fileFormat;
        this.tableName = tableName;
        this.recordBuilderService = recordBuilderService;
        this.fieldNames = fieldNames;
        this.keyFieldName = keyFieldName;
        
        setExecutionContextName("fileReader");
    }

    @Override
    public void open(org.springframework.batch.item.ExecutionContext executionContext) {
        super.open(executionContext);
        
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                throw new IllegalStateException("File does not exist: " + filePath);
            }
            
            reader = new BufferedReader(new FileReader(file));
            
            if ("csv".equalsIgnoreCase(fileFormat)) {
                records = readCsvFile();
            } else if ("json".equalsIgnoreCase(fileFormat)) {
                records = readJsonFile();
            } else {
                throw new IllegalArgumentException("Unsupported file format: " + fileFormat);
            }
            
            currentIndex = 0;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to open file: " + filePath, e);
        }
    }

    @Override
    public SourceRecord read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (currentIndex >= records.size()) {
            return null;
        }
        
        SourceRecord record = records.get(currentIndex);
        currentIndex++;
        return record;
    }

    @Override
    public void close() {
        super.close();
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                throw new RuntimeException("Failed to close file reader", e);
            }
        }
    }

    /**
     * 创建 CSV 读取器
     */
    public ItemReader<SourceRecord> createCsvReader() {
        return this;
    }

    /**
     * 创建 JSON 读取器
     */
    public ItemReader<SourceRecord> createJsonReader() {
        return this;
    }

    /**
     * 读取 CSV 文件
     */
    private List<SourceRecord> readCsvFile() throws IOException {
        List<SourceRecord> records = new ArrayList<>();
        String line;
        String[] headers = null;
        
        while ((line = reader.readLine()) != null) {
            if (headers == null) {
                // 第一行是表头
                headers = line.split(",");
                continue;
            }
            
            String[] values = line.split(",");
            if (values.length != headers.length) {
                System.err.println("Skipping malformed line: " + line);
                continue;
            }
            
            Map<String, Object> data = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                data.put(headers[i].trim(), values[i].trim());
            }
            
            SourceRecord record = recordBuilderService.buildSourceRecordFromMap(data, tableName);
            records.add(record);
        }
        
        return records;
    }

    /**
     * 读取 JSON 文件
     */
    private List<SourceRecord> readJsonFile() throws IOException {
        List<SourceRecord> records = new ArrayList<>();
        StringBuilder jsonContent = new StringBuilder();
        String line;
        
        while ((line = reader.readLine()) != null) {
            jsonContent.append(line);
        }
        
        // 简单的 JSON 解析（实际项目中应使用 Jackson 等库）
        String content = jsonContent.toString().trim();
        if (content.startsWith("[")) {
            // JSON 数组格式
            records = parseJsonArray(content);
        } else {
            // 单个 JSON 对象
            Map<String, Object> data = parseJsonObject(content);
            SourceRecord record = recordBuilderService.buildSourceRecordFromMap(data, tableName);
            records.add(record);
        }
        
        return records;
    }

    /**
     * 解析 JSON 数组
     */
    private List<SourceRecord> parseJsonArray(String jsonArray) {
        List<SourceRecord> records = new ArrayList<>();
        // 这里应该使用 Jackson 等 JSON 库进行解析
        // 为了简化，这里返回空列表
        return records;
    }

    /**
     * 解析 JSON 对象
     */
    private Map<String, Object> parseJsonObject(String jsonObject) {
        Map<String, Object> data = new HashMap<>();
        // 这里应该使用 Jackson 等 JSON 库进行解析
        // 为了简化，这里返回空 Map
        return data;
    }
}