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
        
        // 转换数据映射
        Map<String, Object> data = record.getData();
        document.setData(data);
        
        return document;
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
        return stats;
    }
}
