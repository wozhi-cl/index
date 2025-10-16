package com.company.index.batch.writer;

import com.company.index.common.model.DynamicRecord;
import com.company.index.common.service.RecordBuilderService;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 动态 Writer 工厂
 * 根据配置创建不同类型的 Writer
 */
@Component
public class DynamicWriterFactory {

    @Autowired
    private RecordBuilderService recordBuilderService;

    @Value("${index.indexTarget.type:file}")
    private String indexTargetType;

    @Value("${index.indexTarget.path:./output}")
    private String outputPath;

    @Value("${index.indexTarget.fileName:dynamic-index-output.json}")
    private String fileName;

    @Value("${index.indexTarget.indexName:order_index}")
    private String indexName;

    private FileWriter fileWriter;
    private int recordCount = 0;

    /**
     * 获取 Writer
     */
    public ItemWriter<DynamicRecord> getWriter() {
        switch (indexTargetType.toLowerCase()) {
            case "file":
                return createFileWriter();
            case "elasticsearch":
            case "es":
                // TODO: 实现 Elasticsearch Writer
                throw new UnsupportedOperationException("Elasticsearch writer not implemented yet for DynamicRecord");
            default:
                throw new IllegalArgumentException("Unsupported index target type: " + indexTargetType);
        }
    }

    /**
     * 创建文件 Writer
     */
    private ItemWriter<DynamicRecord> createFileWriter() {
        return items -> {
            try {
                if (fileWriter == null) {
                    initFileWriter();
                }

                for (DynamicRecord record : items) {
                    Map<String, Object> data = recordBuilderService.convertRecordToMap(record);
                    String json = convertToJson(data);
                    fileWriter.write(json);
                    fileWriter.write("\n");
                    recordCount++;
                }
                fileWriter.flush();

            } catch (IOException e) {
                throw new RuntimeException("写入文件失败", e);
            }
        };
    }

    /**
     * 初始化文件 Writer
     */
    private void initFileWriter() throws IOException {
        File dir = new File(outputPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String fullFileName = fileName.replace(".json", "-" + timestamp + ".json");
        File file = new File(dir, fullFileName);

        fileWriter = new FileWriter(file, false);  // 覆盖模式
        recordCount = 0;

        System.out.println("创建动态文件 Writer: " + file.getAbsolutePath());
    }

    /**
     * 简单的 JSON 转换（用于演示）
     */
    private String convertToJson(Map<String, Object> data) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!first) {
                json.append(",");
            }
            json.append("\"").append(entry.getKey()).append("\":");
            
            Object value = entry.getValue();
            if (value == null) {
                json.append("null");
            } else if (value instanceof String) {
                json.append("\"").append(value.toString().replace("\"", "\\\"")).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                json.append(value);
            } else {
                json.append("\"").append(value.toString().replace("\"", "\\\"")).append("\"");
            }
            
            first = false;
        }
        json.append("}");
        return json.toString();
    }

    /**
     * 创建索引（如果不存在）
     */
    public void createIndexIfNotExists() throws Exception {
        System.out.println("创建索引: " + indexName);
        // 对于文件类型，不需要额外操作
        if ("file".equalsIgnoreCase(indexTargetType)) {
            System.out.println("文件类型索引，无需创建");
        }
    }

    /**
     * 删除索引
     */
    public void deleteIndex() throws Exception {
        System.out.println("删除索引: " + indexName);
        
        if ("file".equalsIgnoreCase(indexTargetType)) {
            // 关闭当前文件
            if (fileWriter != null) {
                fileWriter.close();
                fileWriter = null;
            }
            System.out.println("文件 Writer 已关闭");
        }
    }

    /**
     * 完成索引
     */
    public void finishIndex() throws Exception {
        System.out.println("完成索引: " + indexName);
        
        if ("file".equalsIgnoreCase(indexTargetType)) {
            if (fileWriter != null) {
                fileWriter.flush();
                fileWriter.close();
                fileWriter = null;
            }
            System.out.println("文件已写入完成，共 " + recordCount + " 条记录");
        }
    }

    /**
     * 获取索引统计信息
     */
    public Object getIndexStats() {
        if ("file".equalsIgnoreCase(indexTargetType)) {
            return String.format("文件索引统计: 总记录数=%d, 输出路径=%s", recordCount, outputPath);
        }
        return "暂无统计信息";
    }
}

