package com.company.index.batch.writer;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.DeleteOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.elasticsearch.core.bulk.UpdateOperation;
// import co.elastic.clients.elasticsearch.core.bulk.UpdateOperationVariant;
import com.company.index.common.model.IndexDocument;
import com.company.index.common.model.SourceRecord;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch 批量写入器
 * 支持批量写、Upsert、删除操作
 */
@Component
public class ElasticsearchWriter implements ItemWriter<SourceRecord> {

    private final ElasticsearchClient elasticsearchClient;
    private final String indexName;
    private final int batchSize;

    @Autowired
    public ElasticsearchWriter(ElasticsearchClient elasticsearchClient,
                              @Value("${index.indexTarget.indexName:default_index}") String indexName,
                              @Value("${index.indexTarget.batchSize:1000}") int batchSize) {
        this.elasticsearchClient = elasticsearchClient;
        this.indexName = indexName;
        this.batchSize = batchSize;
    }

    @Override
    public void write(Chunk<? extends SourceRecord> chunk) throws Exception {
        List<BulkOperation> operations = new ArrayList<>();

        for (SourceRecord record : chunk) {
            BulkOperation operation = createBulkOperation(record);
            operations.add(operation);

            // 批量提交
            if (operations.size() >= batchSize) {
                executeBulkOperations(operations);
                operations.clear();
            }
        }

        // 执行剩余操作
        if (!operations.isEmpty()) {
            executeBulkOperations(operations);
        }
    }

    /**
     * 根据记录类型创建批量操作
     */
    private BulkOperation createBulkOperation(SourceRecord record) {
        String type = record.getType();
        String id = record.getId();

        switch (type.toUpperCase()) {
            case "INSERT":
                return createIndexOperation(record);
            case "UPDATE":
                return createUpdateOperation(record);
            case "DELETE":
                return createDeleteOperation(id);
            default:
                // 默认为 Upsert 操作
                return createUpsertOperation(record);
        }
    }

    /**
     * 创建索引操作
     */
    private BulkOperation createIndexOperation(SourceRecord record) {
        IndexDocument document = convertToIndexDocument(record);
        
        IndexOperation<IndexDocument> indexOp = IndexOperation.of(i -> i
            .index(indexName)
            .id(record.getId())
            .document(document)
        );

        return BulkOperation.of(b -> b.index(indexOp));
    }

    /**
     * 创建更新操作（简化为索引操作）
     */
    private BulkOperation createUpdateOperation(SourceRecord record) {
        IndexDocument document = convertToIndexDocument(record);
        
        IndexOperation<IndexDocument> indexOp = IndexOperation.of(i -> i
            .index(indexName)
            .id(record.getId())
            .document(document)
        );

        return BulkOperation.of(b -> b.index(indexOp));
    }

    /**
     * 创建删除操作
     */
    private BulkOperation createDeleteOperation(String id) {
        DeleteOperation deleteOp = DeleteOperation.of(d -> d
            .index(indexName)
            .id(id)
        );

        return BulkOperation.of(b -> b.delete(deleteOp));
    }

    /**
     * 创建 Upsert 操作（简化为索引操作）
     */
    private BulkOperation createUpsertOperation(SourceRecord record) {
        IndexDocument document = convertToIndexDocument(record);
        
        IndexOperation<IndexDocument> indexOp = IndexOperation.of(i -> i
            .index(indexName)
            .id(record.getId())
            .document(document)
        );

        return BulkOperation.of(b -> b.index(indexOp));
    }

    /**
     * 执行批量操作
     */
    private void executeBulkOperations(List<BulkOperation> operations) throws Exception {
        BulkRequest bulkRequest = BulkRequest.of(b -> b.operations(operations));
        
        try {
            elasticsearchClient.bulk(bulkRequest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute bulk operations", e);
        }
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
        
        // 转换数据映射，处理 Oracle 特殊类型
        Map<String, Object> data = record.getData();
        Map<String, Object> normalizedData = normalizeData(data);
        document.setData(normalizedData);
        
        return document;
    }
    
    /**
     * 规范化数据，将 Oracle 特殊类型转换为标准 Java 类型
     */
    private Map<String, Object> normalizeData(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        
        Map<String, Object> normalized = new HashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // 处理 Oracle 特殊类型
            Object normalizedValue = normalizeValue(value);
            normalized.put(key, normalizedValue);
        }
        
        return normalized;
    }
    
    /**
     * 规范化单个值
     */
    private Object normalizeValue(Object value) {
        if (value == null) {
            return null;
        }
        
        // 处理 Oracle TIMESTAMP 类型
        if (value instanceof oracle.sql.TIMESTAMP) {
            try {
                oracle.sql.TIMESTAMP timestamp = (oracle.sql.TIMESTAMP) value;
                return timestamp.timestampValue(); // 转换为 java.sql.Timestamp
            } catch (Exception e) {
                return null;
            }
        }
        
        // 处理 Oracle DATE 类型
        if (value instanceof oracle.sql.DATE) {
            try {
                oracle.sql.DATE date = (oracle.sql.DATE) value;
                return date.timestampValue(); // 转换为 java.sql.Timestamp
            } catch (Exception e) {
                return null;
            }
        }
        
        // 处理 Oracle CLOB 类型
        if (value instanceof oracle.sql.CLOB) {
            try {
                oracle.sql.CLOB clob = (oracle.sql.CLOB) value;
                return clob.getSubString(1, (int) clob.length());
            } catch (Exception e) {
                return null;
            }
        }
        
        // 处理 Oracle BLOB 类型（转换为 Base64 字符串）
        if (value instanceof oracle.sql.BLOB) {
            try {
                oracle.sql.BLOB blob = (oracle.sql.BLOB) value;
                byte[] bytes = blob.getBytes(1, (int) blob.length());
                return java.util.Base64.getEncoder().encodeToString(bytes);
            } catch (Exception e) {
                return null;
            }
        }
        
        // 处理 Oracle NUMBER 类型
        if (value instanceof oracle.sql.NUMBER) {
            try {
                oracle.sql.NUMBER number = (oracle.sql.NUMBER) value;
                return number.bigDecimalValue();
            } catch (Exception e) {
                return null;
            }
        }
        
        // 处理嵌套 Map
        if (value instanceof Map) {
            return normalizeData((Map<String, Object>) value);
        }
        
        // 处理 List
        if (value instanceof List) {
            List<Object> list = (List<Object>) value;
            List<Object> normalizedList = new ArrayList<>();
            for (Object item : list) {
                normalizedList.add(normalizeValue(item));
            }
            return normalizedList;
        }
        
        // 其他类型直接返回
        return value;
    }

    /**
     * 创建索引（如果不存在）
     */
    public void createIndexIfNotExists() throws Exception {
        try {
            elasticsearchClient.indices().get(g -> g.index(indexName));
        } catch (Exception e) {
            // 索引不存在，创建索引
            elasticsearchClient.indices().create(c -> c
                .index(indexName)
                .settings(s -> s
                    .numberOfShards("1")
                    .numberOfReplicas("0")
                )
                .mappings(m -> m
                    .properties("id", p -> p.keyword(k -> k))
                    .properties("type", p -> p.keyword(k -> k))
                    .properties("timestamp", p -> p.date(d -> d.format("yyyy-MM-dd'T'HH:mm:ss")))
                    .properties("source", p -> p.keyword(k -> k))
                    .properties("version", p -> p.long_(l -> l))
                )
            );
        }
    }

    /**
     * 删除索引
     */
    public void deleteIndex() throws Exception {
        try {
            elasticsearchClient.indices().delete(d -> d.index(indexName));
        } catch (Exception e) {
            // 索引可能不存在，忽略错误
        }
    }

    /**
     * 获取索引统计信息
     */
    public Map<String, Object> getIndexStats() throws Exception {
        long docCount = elasticsearchClient.indices().stats(s -> s.index(indexName))
            .indices()
            .get(indexName)
            .total()
            .docs()
            .count();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("documentCount", docCount);
        stats.put("indexName", indexName);
        return stats;
    }
    
    /**
     * 索引结束处理
     * 1. 刷新索引，确保所有数据可见
     * 2. 强制合并段（可选，提升查询性能）
     * 3. 如果需要蓝绿部署，可以在这里切换别名
     * 
     * 注意：当前简化实现，只刷新索引
     * 生产环境如需蓝绿部署，可以实现别名切换逻辑
     */
    public void finishIndex() throws Exception {
        try {
            // 检查索引是否存在
            boolean exists = elasticsearchClient.indices().exists(e -> e.index(indexName)).value();
            
            if (!exists) {
                System.out.println("索引 " + indexName + " 不存在，跳过结束处理");
                return;
            }
            
            System.out.println("开始 Elasticsearch 索引结束处理: " + indexName);
            
            // 1. 刷新索引，确保所有数据可见
            elasticsearchClient.indices().refresh(r -> r.index(indexName));
            System.out.println("  ✓ 索引已刷新，所有数据已可见");
            
            // 2. 可选：强制合并段（生产环境建议在低峰期执行）
            // elasticsearchClient.indices().forcemerge(f -> f
            //     .index(indexName)
            //     .maxNumSegments(1)
            // );
            // System.out.println("  ✓ 索引段已合并");
            
            // 3. 可选：如果使用蓝绿部署，在这里切换别名
            // 示例代码（需要在创建索引时使用带版本号的名称）：
            //
            // String aliasName = "my_index";  // 应用使用的别名
            // String newIndexName = indexName;  // 当前新建的索引（如：my_index_v20251016）
            //
            // // 原子性切换别名
            // elasticsearchClient.indices().updateAliases(u -> u
            //     .actions(a -> a
            //         .add(add -> add.index(newIndexName).alias(aliasName))
            //     )
            // );
            // System.out.println("  ✓ 别名已切换到新索引");
            //
            // // 删除旧索引（保留最近N个版本）
            // var allIndices = elasticsearchClient.indices().get(g -> g.index(aliasName + "_v*"));
            // // ... 删除逻辑
            
            System.out.println("Elasticsearch 索引结束处理完成: " + indexName);
            
        } catch (Exception e) {
            throw new RuntimeException("Elasticsearch 索引结束处理失败", e);
        }
    }
}
