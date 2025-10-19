package com.company.index.batch.writer;

import com.company.index.common.model.SourceRecord;
import com.company.index.common.model.IndexDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 文件写入器
 * 支持将源数据和索引数据写入 JSON/CSV 文件
 */
@Component
public class FileWriter implements DataWriter, ItemWriter<SourceRecord> {

    private final String outputPath;
    private final String fileName;
    private final String format; // json 或 csv
    private final ObjectMapper objectMapper;
    private BufferedWriter writer;
    private int recordCount = 0;
    private String actualFilePath;

    public FileWriter(String outputPath, String fileName, String format) {
        this.outputPath = outputPath;
        this.fileName = fileName;
        this.format = format != null ? format.toLowerCase() : "json";
        
        // 配置 ObjectMapper
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * 初始化文件写入器
     */
    private void initWriter() throws IOException {
        if (writer == null) {
            File outputDir = new File(outputPath);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            // 生成带时间戳的文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            String baseFileName = fileName.replaceAll("\\.[^.]+$", ""); // 去掉扩展名
            String extension = format.equals("csv") ? ".csv" : ".json";
            actualFilePath = outputPath + File.separator + baseFileName + "-" + timestamp + extension;

            writer = new BufferedWriter(new java.io.FileWriter(actualFilePath, false));
            
            System.out.println("========================================");
            System.out.println("初始化文件写入器:");
            System.out.println("  输出路径: " + actualFilePath);
            System.out.println("  格式: " + format.toUpperCase());
            System.out.println("========================================");

            // JSON 格式：写入数组开始标记
            if (format.equals("json")) {
                writer.write("[");
                writer.newLine();
            }
        }
    }

    @Override
    public void write(Chunk<? extends SourceRecord> chunk) throws Exception {
        initWriter();

        for (SourceRecord record : chunk) {
            if (format.equals("json")) {
                writeJsonSourceRecord(record);
            } else if (format.equals("csv")) {
                writeCsvSourceRecord(record);
            } else {
                writeJsonSourceRecord(record); // 默认 JSON
            }
            recordCount++;
        }
    }

    @Override
    public void writeSourceData(List<? extends SourceRecord> items) throws Exception {
        initWriter();
        
        for (SourceRecord record : items) {
            if (format.equals("json")) {
                writeJsonSourceRecord(record);
            } else if (format.equals("csv")) {
                writeCsvSourceRecord(record);
            } else {
                writeJsonSourceRecord(record);
            }
            recordCount++;
        }
    }

    @Override
    public void writeIndexData(List<? extends IndexDocument> items) throws Exception {
        initWriter();
        
        for (IndexDocument doc : items) {
            if (format.equals("json")) {
                writeJsonIndexDocument(doc);
            } else if (format.equals("csv")) {
                writeCsvIndexDocument(doc);
            } else {
                writeJsonIndexDocument(doc);
            }
            recordCount++;
        }
    }

    @Override
    public ItemWriter<SourceRecord> createSourceWriter() {
        return this;
    }

    @Override
    public ItemWriter<IndexDocument> createIndexWriter() {
        return new ItemWriter<IndexDocument>() {
            @Override
            public void write(Chunk<? extends IndexDocument> chunk) throws Exception {
                writeIndexData(chunk.getItems());
            }
        };
    }

    /**
     * 写入 JSON 格式源记录
     */
    private void writeJsonSourceRecord(SourceRecord record) throws IOException {
        if (recordCount > 0) {
            writer.write(",");
            writer.newLine();
        }

        // 构建输出对象
        Map<String, Object> output = new HashMap<>();
        output.put("_id", record.getKeyValue());
        output.put("_type", record.getOperation().toString());
        output.put("_timestamp", record.getTimestamp());
        output.put("id", record.getId());
        output.put("order_no", record.getOrderNo());
        output.put("amount", record.getAmount());
        output.put("user_id", record.getUserId());
        output.put("status", record.getStatus());
        output.put("created_at", record.getCreatedAt());
        output.put("updated_at", record.getUpdatedAt());
        output.put("user_name", record.getUserName());
        output.put("user_phone", record.getUserPhone());

        String json = objectMapper.writeValueAsString(output);
        writer.write("  " + json);
    }

    /**
     * 写入 CSV 格式源记录
     */
    private void writeCsvSourceRecord(SourceRecord record) throws IOException {
        // 第一行写入表头
        if (recordCount == 0) {
            List<String> headers = new ArrayList<>();
            headers.add("_id");
            headers.add("_type");
            headers.add("_timestamp");
            headers.add("id");
            headers.add("order_no");
            headers.add("amount");
            headers.add("user_id");
            headers.add("status");
            headers.add("created_at");
            headers.add("updated_at");
            headers.add("user_name");
            headers.add("user_phone");
            writer.write(String.join(",", headers));
            writer.newLine();
        }

        // 写入数据行
        List<String> values = new ArrayList<>();
        values.add(escapeCsv(String.valueOf(record.getKeyValue())));
        values.add(escapeCsv(record.getOperation() != null ? record.getOperation().toString() : ""));
        values.add(escapeCsv(record.getTimestamp() != null ? record.getTimestamp().toString() : ""));
        values.add(escapeCsv(record.getId() != null ? record.getId().toString() : ""));
        values.add(escapeCsv(record.getOrderNo()));
        values.add(escapeCsv(record.getAmount() != null ? record.getAmount().toString() : ""));
        values.add(escapeCsv(record.getUserId() != null ? record.getUserId().toString() : ""));
        values.add(escapeCsv(record.getStatus()));
        values.add(escapeCsv(record.getCreatedAt() != null ? record.getCreatedAt().toString() : ""));
        values.add(escapeCsv(record.getUpdatedAt() != null ? record.getUpdatedAt().toString() : ""));
        values.add(escapeCsv(record.getUserName()));
        values.add(escapeCsv(record.getUserPhone()));
        
        writer.write(String.join(",", values));
        writer.newLine();
    }

    /**
     * 写入 JSON 格式索引文档
     */
    private void writeJsonIndexDocument(IndexDocument doc) throws IOException {
        if (recordCount > 0) {
            writer.write(",");
            writer.newLine();
        }

        // 构建输出对象
        Map<String, Object> output = new HashMap<>();
        output.put("_id", doc.getKeyValue());
        output.put("_type", doc.getOperation().toString());
        output.put("_timestamp", doc.getTimestamp());
        output.put("order_id", doc.getOrderId());
        output.put("order_no", doc.getOrderNo());
        output.put("amount", doc.getAmount());
        output.put("user_name", doc.getUserName());
        output.put("order_time", doc.getOrderTime());
        output.put("updated_at", doc.getUpdatedAt());

        String json = objectMapper.writeValueAsString(output);
        writer.write("  " + json);
    }

    /**
     * 写入 CSV 格式索引文档
     */
    private void writeCsvIndexDocument(IndexDocument doc) throws IOException {
        // 第一行写入表头
        if (recordCount == 0) {
            List<String> headers = new ArrayList<>();
            headers.add("_id");
            headers.add("_type");
            headers.add("_timestamp");
            headers.add("order_id");
            headers.add("order_no");
            headers.add("amount");
            headers.add("user_name");
            headers.add("order_time");
            headers.add("updated_at");
            writer.write(String.join(",", headers));
            writer.newLine();
        }

        // 写入数据行
        List<String> values = new ArrayList<>();
        values.add(escapeCsv(String.valueOf(doc.getKeyValue())));
        values.add(escapeCsv(doc.getOperation() != null ? doc.getOperation().toString() : ""));
        values.add(escapeCsv(doc.getTimestamp() != null ? doc.getTimestamp().toString() : ""));
        values.add(escapeCsv(doc.getOrderId() != null ? doc.getOrderId().toString() : ""));
        values.add(escapeCsv(doc.getOrderNo()));
        values.add(escapeCsv(doc.getAmount() != null ? doc.getAmount().toString() : ""));
        values.add(escapeCsv(doc.getUserName()));
        values.add(escapeCsv(doc.getOrderTime() != null ? doc.getOrderTime().toString() : ""));
        values.add(escapeCsv(doc.getUpdatedAt() != null ? doc.getUpdatedAt().toString() : ""));
        
        writer.write(String.join(",", values));
        writer.newLine();
    }

    /**
     * CSV 字段转义
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * 标准化数据（处理特殊类型）
     */
    private Map<String, Object> normalizeData(Map<String, Object> data) {
        if (data == null) {
            return new HashMap<>();
        }

        Map<String, Object> normalized = new HashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Object value = entry.getValue();
            normalized.put(entry.getKey(), normalizeValue(value));
        }
        return normalized;
    }

    /**
     * 标准化单个值
     */
    private Object normalizeValue(Object value) {
        if (value == null) {
            return null;
        }

        String className = value.getClass().getName();

        // 处理 Oracle 特殊类型
        if (className.startsWith("oracle.sql.")) {
            try {
                if (className.equals("oracle.sql.TIMESTAMP")) {
                    java.lang.reflect.Method method = value.getClass().getMethod("timestampValue");
                    java.sql.Timestamp ts = (java.sql.Timestamp) method.invoke(value);
                    return ts.toLocalDateTime().toString();
                } else if (className.equals("oracle.sql.DATE")) {
                    java.lang.reflect.Method method = value.getClass().getMethod("dateValue");
                    java.sql.Date date = (java.sql.Date) method.invoke(value);
                    return date.toLocalDate().toString();
                } else if (className.equals("oracle.sql.CLOB")) {
                    java.lang.reflect.Method method = value.getClass().getMethod("stringValue");
                    return method.invoke(value);
                } else if (className.equals("oracle.sql.NUMBER")) {
                    java.lang.reflect.Method method = value.getClass().getMethod("doubleValue");
                    return method.invoke(value);
                }
            } catch (Exception e) {
                System.err.println("Failed to normalize Oracle type: " + className + ", error: " + e.getMessage());
                return value.toString();
            }
        }

        // 处理嵌套 Map
        if (value instanceof Map) {
            return normalizeData((Map<String, Object>) value);
        }

        // 处理 List
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            List<Object> normalizedList = new ArrayList<>();
            for (Object item : list) {
                normalizedList.add(normalizeValue(item));
            }
            return normalizedList;
        }

        return value;
    }

    /**
     * 完成写入（关闭文件）
     */
    public void finishWrite() throws IOException {
        if (writer != null) {
            // JSON 格式：写入数组结束标记
            if (format.equals("json")) {
                writer.newLine();
                writer.write("]");
            }
            
            writer.flush();
            writer.close();
            writer = null;

            System.out.println("======================================");
            System.out.println("文件写入完成！");
            System.out.println("  文件路径: " + actualFilePath);
            System.out.println("  记录数: " + recordCount);
            System.out.println("  格式: " + format.toUpperCase());
            System.out.println("======================================");
        }
    }

    @Override
    public void deleteIndex() throws Exception {
        // 文件写入器不需要删除索引操作
        System.out.println("文件写入器：无需删除索引操作");
    }

    @Override
    public Object getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("indexName", fileName);
        stats.put("totalWritten", recordCount);
        stats.put("totalErrors", 0);
        return stats;
    }

    /**
     * 获取实际文件路径
     */
    public String getActualFilePath() {
        return actualFilePath;
    }
}

