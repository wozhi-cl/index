package com.company.index.batch.writer;

import com.company.index.common.model.SourceRecord;
import com.company.index.common.model.IndexDocument;
import com.company.index.common.service.RecordBuilderService;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GetQuick 批量写入器
 * 支持 SourceRecord 和 IndexDocument 写入
 */
@Component
public class GetQuickWriter implements DataWriter, ItemWriter<IndexDocument> {

    private final String baseUrl;
    private final String indexName;
    private final String username;
    private final String password;
    private final int batchSize;

    @Autowired
    private RecordBuilderService recordBuilderService;
    
    private final HttpClient httpClient;
    private long totalWritten = 0;
    private long totalErrors = 0;

    public GetQuickWriter(String baseUrl, String indexName, String username, String password, int batchSize) {
        this.baseUrl = baseUrl;
        this.indexName = indexName;
        this.username = username;
        this.password = password;
        this.batchSize = batchSize;
        
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public void write(Chunk<? extends IndexDocument> chunk) throws Exception {
        writeIndexData(chunk.getItems());
    }

    @Override
    public void writeSourceData(List<? extends SourceRecord> items) throws Exception {
        // 将 SourceRecord 转换为 IndexDocument 后写入
        List<IndexDocument> indexDocs = new ArrayList<>();
        for (SourceRecord sourceRecord : items) {
            IndexDocument indexDoc = recordBuilderService.convertToIndexDocument(sourceRecord, indexName);
            indexDocs.add(indexDoc);
        }
        writeIndexData(indexDocs);
    }

    @Override
    public void writeIndexData(List<? extends IndexDocument> items) throws Exception {
        if (items == null || items.isEmpty()) {
            return;
        }

        // 构建批量数据
        List<Map<String, Object>> documents = new ArrayList<>();
        for (IndexDocument doc : items) {
            try {
                Map<String, Object> documentData = recordBuilderService.convertIndexDocumentToMap(doc);
                documentData.put("_id", doc.getKeyValue());
                documentData.put("_operation", doc.getOperation().toString());
                documentData.put("_timestamp", doc.getTimestamp());
                documents.add(documentData);
            } catch (Exception e) {
                System.err.println("Error preparing document: " + e.getMessage());
                totalErrors++;
            }
        }

        if (!documents.isEmpty()) {
            sendBatchToGetQuick(documents);
        }
    }

    @Override
    public ItemWriter<SourceRecord> createSourceWriter() {
        return new ItemWriter<SourceRecord>() {
            @Override
            public void write(Chunk<? extends SourceRecord> chunk) throws Exception {
                writeSourceData(chunk.getItems());
            }
        };
    }

    @Override
    public ItemWriter<IndexDocument> createIndexWriter() {
        return this;
    }

    @Override
    public void finishWrite() throws Exception {
        System.out.println("========================================");
        System.out.println("GetQuick 写入完成:");
        System.out.println("  索引: " + indexName);
        System.out.println("  总写入: " + totalWritten);
        System.out.println("  总错误: " + totalErrors);
        System.out.println("========================================");
    }

    /**
     * 发送批量数据到 GetQuick
     */
    private void sendBatchToGetQuick(List<Map<String, Object>> documents) throws Exception {
        try {
            String url = baseUrl + "/api/index/" + indexName + "/batch";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("documents", documents);
            requestBody.put("batchSize", batchSize);
            
            String jsonBody = convertToJson(requestBody);
            
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
            
            // 添加认证头
            if (username != null && !username.isEmpty()) {
                String auth = username + ":" + password;
                String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes());
                requestBuilder.header("Authorization", "Basic " + encodedAuth);
            }
            
            HttpRequest request = requestBuilder.build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                totalWritten += documents.size();
                System.out.println("Successfully sent " + documents.size() + " documents to GetQuick");
                } else {
                System.err.println("GetQuick API error: " + response.statusCode() + " - " + response.body());
                totalErrors += documents.size();
            }
            
        } catch (IOException | InterruptedException e) {
            System.err.println("Error sending batch to GetQuick: " + e.getMessage());
            totalErrors += documents.size();
            throw new Exception("Failed to send batch to GetQuick", e);
        }
    }

    /**
     * 创建索引（如果不存在）
     */
    public void createIndexIfNotExists() throws Exception {
        try {
            String url = baseUrl + "/api/index/" + indexName;
            
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString("{}"));
            
            // 添加认证头
            if (username != null && !username.isEmpty()) {
                String auth = username + ":" + password;
                String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes());
                requestBuilder.header("Authorization", "Basic " + encodedAuth);
            }
            
            HttpRequest request = requestBuilder.build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                System.out.println("Created/verified index: " + indexName);
                } else {
                System.err.println("GetQuick index creation error: " + response.statusCode() + " - " + response.body());
            }
            
        } catch (IOException | InterruptedException e) {
            System.err.println("Error creating index in GetQuick: " + e.getMessage());
            throw new Exception("Failed to create index in GetQuick", e);
        }
    }

    /**
     * 删除索引
     */
    public void deleteIndex() throws Exception {
        try {
            String url = baseUrl + "/api/index/" + indexName;
            
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .DELETE();
            
            // 添加认证头
            if (username != null && !username.isEmpty()) {
                String auth = username + ":" + password;
                String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes());
                requestBuilder.header("Authorization", "Basic " + encodedAuth);
            }
            
            HttpRequest request = requestBuilder.build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                System.out.println("Deleted index: " + indexName);
            } else {
                System.err.println("GetQuick index deletion error: " + response.statusCode() + " - " + response.body());
            }
            
        } catch (IOException | InterruptedException e) {
            System.err.println("Error deleting index in GetQuick: " + e.getMessage());
            throw new Exception("Failed to delete index in GetQuick", e);
        }
    }

    /**
     * 发布索引
     */
    public void publishIndex() throws Exception {
        try {
            String url = baseUrl + "/api/index/" + indexName + "/publish";
            
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{}"));
            
            // 添加认证头
            if (username != null && !username.isEmpty()) {
                String auth = username + ":" + password;
                String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes());
                requestBuilder.header("Authorization", "Basic " + encodedAuth);
            }
            
            HttpRequest request = requestBuilder.build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                System.out.println("Published index: " + indexName);
            } else {
                System.err.println("GetQuick index publish error: " + response.statusCode() + " - " + response.body());
            }
            
        } catch (IOException | InterruptedException e) {
            System.err.println("Error publishing index in GetQuick: " + e.getMessage());
            throw new Exception("Failed to publish index in GetQuick", e);
        }
    }

    @Override
    public Object getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("indexName", indexName);
        stats.put("totalWritten", totalWritten);
        stats.put("totalErrors", totalErrors);
        return stats;
    }

    /**
     * 关闭连接
     */
    public void close() throws Exception {
        finishWrite();
    }

    /**
     * 简单的 JSON 转换（实际项目中应使用 Jackson 等库）
     */
    private String convertToJson(Map<String, Object> data) {
        // 这里应该使用 Jackson 或其他 JSON 库
        // 为了简化，这里返回一个基本的 JSON 字符串
        return "{\"documents\":" + data.get("documents") + ",\"batchSize\":" + data.get("batchSize") + "}";
    }
}