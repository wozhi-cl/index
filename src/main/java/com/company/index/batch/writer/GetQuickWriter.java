package com.company.index.batch.writer;

import com.company.index.common.model.IndexDocument;
import com.company.index.common.model.SourceRecord;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GetQuick Search 批量写入器
 * 支持批量写、删除操作
 */
@Component
@Profile("!dev & !h2") // 开发环境和 H2 环境不启用 GetQuick
public class GetQuickWriter implements ItemWriter<SourceRecord> {

    private final RestTemplate restTemplate;
    private final String gqUrl;
    private final String username;
    private final String password;
    private final String indexName;
    private final int batchSize;

    public GetQuickWriter(RestTemplate restTemplate,
                         @Value("${index.getquick.url:http://localhost:8080}") String gqUrl,
                         @Value("${index.getquick.username:admin}") String username,
                         @Value("${index.getquick.password:admin123}") String password,
                         @Value("${index.getquick.indexName:default_gq_index}") String indexName,
                         @Value("${index.getquick.batchSize:1000}") int batchSize) {
        this.restTemplate = restTemplate;
        this.gqUrl = gqUrl;
        this.username = username;
        this.password = password;
        this.indexName = indexName;
        this.batchSize = batchSize;
    }

    @Override
    public void write(Chunk<? extends SourceRecord> chunk) throws Exception {
        List<Map<String, Object>> batchOperations = new ArrayList<>();

        for (SourceRecord record : chunk) {
            Map<String, Object> operation = createOperation(record);
            batchOperations.add(operation);

            // 批量提交
            if (batchOperations.size() >= batchSize) {
                executeBatchOperations(batchOperations);
                batchOperations.clear();
            }
        }

        // 执行剩余操作
        if (!batchOperations.isEmpty()) {
            executeBatchOperations(batchOperations);
        }
    }

    /**
     * 根据记录类型创建操作
     */
    private Map<String, Object> createOperation(SourceRecord record) {
        String type = record.getType();
        String id = record.getId();

        Map<String, Object> operation = new HashMap<>();
        operation.put("id", id);
        operation.put("type", type);
        operation.put("index", indexName);

        switch (type.toUpperCase()) {
            case "INSERT":
            case "UPDATE":
                operation.put("action", "index");
                operation.put("document", convertToIndexDocument(record));
                break;
            case "DELETE":
                operation.put("action", "delete");
                break;
            default:
                // 默认为 Upsert 操作
                operation.put("action", "upsert");
                operation.put("document", convertToIndexDocument(record));
                break;
        }

        return operation;
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
     * 执行批量操作
     */
    private void executeBatchOperations(List<Map<String, Object>> operations) throws Exception {
        try {
            // 构建批量请求
            Map<String, Object> batchRequest = new HashMap<>();
            batchRequest.put("operations", operations);
            batchRequest.put("index", indexName);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBasicAuth(username, password);

            // 创建请求实体
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(batchRequest, headers);

            // 发送批量请求
            String url = gqUrl + "/api/batch";
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                Map.class
            );

            // 检查响应状态
            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null && responseBody.containsKey("errors")) {
                    List<Map<String, Object>> errors = (List<Map<String, Object>>) responseBody.get("errors");
                    if (errors != null && !errors.isEmpty()) {
                        throw new RuntimeException("GetQuick batch operation failed with errors: " + errors);
                    }
                }
            } else {
                throw new RuntimeException("GetQuick batch operation failed with status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to execute GetQuick batch operations", e);
        }
    }

    /**
     * 创建索引（如果不存在）
     */
    public void createIndexIfNotExists() throws Exception {
        try {
            // 检查索引是否存在
            String url = gqUrl + "/api/index/" + indexName;
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(username, password);
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                // 索引已存在
                return;
            }

        } catch (Exception e) {
            // 索引不存在，创建索引
            createIndex();
        }
    }

    /**
     * 创建索引
     */
    private void createIndex() throws Exception {
        Map<String, Object> indexConfig = new HashMap<>();
        indexConfig.put("name", indexName);
        indexConfig.put("shards", 1);
        indexConfig.put("replicas", 0);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(username, password);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(indexConfig, headers);

        String url = gqUrl + "/api/index";
        ResponseEntity<Map> response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            requestEntity,
            Map.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to create GetQuick index: " + response.getStatusCode());
        }
    }

    /**
     * 删除索引
     */
    public void deleteIndex() throws Exception {
        try {
            String url = gqUrl + "/api/index/" + indexName;
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(username, password);
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                requestEntity,
                Map.class
            );
        } catch (Exception e) {
            // 索引可能不存在，忽略错误
        }
    }

    /**
     * 获取索引统计信息
     */
    public Map<String, Object> getIndexStats() throws Exception {
        String url = gqUrl + "/api/index/" + indexName + "/stats";
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(username, password);
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            requestEntity,
            Map.class
        );

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            throw new RuntimeException("Failed to get GetQuick index stats: " + response.getStatusCode());
        }
    }

    /**
     * 健康检查
     */
    public boolean isHealthy() {
        try {
            String url = gqUrl + "/api/health";
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(username, password);
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                Map.class
            );

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}
