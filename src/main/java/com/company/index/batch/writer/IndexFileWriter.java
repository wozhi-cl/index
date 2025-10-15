package com.company.index.batch.writer;

import com.company.index.common.model.IndexDocument;
import com.company.index.common.model.SourceRecord;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 文件写入器（H2 模式使用）
 * 将数据写入 JSON 文件
 */
@Component
public class IndexFileWriter implements ItemWriter<SourceRecord> {

    private final String outputPath;
    private final String fileName;

    public IndexFileWriter(@Value("${index.indexTarget.path:./output}") String outputPath,
                     @Value("${index.indexTarget.fileName:index-output.json}") String fileName) {
        this.outputPath = outputPath;
        this.fileName = fileName;
    }

    @Override
    public void write(Chunk<? extends SourceRecord> chunk) throws Exception {
        // 确保输出目录存在
        Path outputDir = Paths.get(outputPath);
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        // 生成带时间戳的文件名
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String fullFileName = fileName.replace(".json", "-" + timestamp + ".json");
        Path outputFile = outputDir.resolve(fullFileName);

        // 写入文件
        try (FileWriter writer = new FileWriter(outputFile.toFile())) {
            writer.write("[\n");
            
            int index = 0;
            for (SourceRecord record : chunk) {
                IndexDocument document = convertToIndexDocument(record);
                
                writer.write("  {\n");
                writer.write("    \"id\": \"" + document.getId() + "\",\n");
                writer.write("    \"type\": \"" + document.getType() + "\",\n");
                writer.write("    \"timestamp\": \"" + document.getTimestamp() + "\",\n");
                writer.write("    \"source\": \"" + document.getSource() + "\",\n");
                writer.write("    \"version\": " + document.getVersion() + ",\n");
                writer.write("    \"data\": " + convertDataToJson(document.getData()) + "\n");
                
                if (index < chunk.size() - 1) {
                    writer.write("  },\n");
                } else {
                    writer.write("  }\n");
                }
                index++;
            }
            
            writer.write("]\n");
        }

        System.out.println("数据已写入文件: " + outputFile);
    }

    /**
     * 转换 SourceRecord 为 IndexDocument
     */
    private IndexDocument convertToIndexDocument(SourceRecord record) {
        IndexDocument document = new IndexDocument();
        document.setId(record.getId());
        document.setType(record.getType());
        document.setTimestamp(record.getTimestamp());
        document.setSource(record.getSource());
        document.setVersion(record.getVersion());
        document.setData(record.getData());
        return document;
    }

    /**
     * 将数据映射转换为 JSON 字符串
     */
    private String convertDataToJson(java.util.Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return "{}";
        }

        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        
        for (java.util.Map.Entry<String, Object> entry : data.entrySet()) {
            if (!first) {
                json.append(", ");
            }
            first = false;
            
            json.append("\"").append(entry.getKey()).append("\": ");
            
            Object value = entry.getValue();
            if (value == null) {
                json.append("null");
            } else if (value instanceof String) {
                json.append("\"").append(value).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                json.append(value);
            } else {
                json.append("\"").append(value.toString()).append("\"");
            }
        }
        
        json.append("}");
        return json.toString();
    }
}
