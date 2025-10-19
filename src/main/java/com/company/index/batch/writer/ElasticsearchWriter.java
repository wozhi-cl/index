package com.company.index.batch.writer;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.indices.RefreshRequest;
import com.company.index.common.model.SourceRecord;
import com.company.index.common.model.IndexDocument;
import com.company.index.common.service.RecordBuilderService;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch 批量写入器
 * 支持 SourceRecord 和 IndexDocument 写入
 */
@Component
public class ElasticsearchWriter implements DataWriter, ItemWriter<IndexDocument> {

    private final ElasticsearchClient elasticsearchClient;
    private final String indexName;
    private final int batchSize;
    
    @Autowired
    private RecordBuilderService recordBuilderService;
    
    private long totalWritten = 0;
    private long totalErrors = 0;

    public ElasticsearchWriter(ElasticsearchClient elasticsearchClient, String indexName, int batchSize) {
        this.elasticsearchClient = elasticsearchClient;
        this.indexName = indexName;
        this.batchSize = batchSize;
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

        List<BulkOperation> operations = new ArrayList<>();
        
        for (IndexDocument doc : items) {
            try {
                // 构建文档数据
                Map<String, Object> documentData = recordBuilderService.convertIndexDocumentToMap(doc);
                
                // 根据操作类型创建不同的操作
                BulkOperation operation;
                switch (doc.getOperation()) {
                    case INSERT:
                    case UPDATE:
                        operation = BulkOperation.of(op -> op
                            .index(idx -> idx
                                .index(indexName)
                                .id(doc.getKeyValue())
                                .document(documentData)
                            )
                        );
                        break;
                    case DELETE:
                        operation = BulkOperation.of(op -> op
                            .delete(del -> del
                                .index(indexName)
                                .id(doc.getKeyValue())
                            )
                        );
                        break;
                    default:
                        continue;
                }
                
                operations.add(operation);
                
            } catch (Exception e) {
                System.err.println("Error preparing document for bulk operation: " + e.getMessage());
                totalErrors++;
            }
        }

        if (!operations.isEmpty()) {
            executeBulkOperation(operations);
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
        // 刷新索引
        RefreshRequest refreshRequest = RefreshRequest.of(r -> r.index(indexName));
        elasticsearchClient.indices().refresh(refreshRequest);
        
        System.out.println("========================================");
        System.out.println("Elasticsearch 写入完成:");
        System.out.println("  索引: " + indexName);
        System.out.println("  总写入: " + totalWritten);
        System.out.println("  总错误: " + totalErrors);
        System.out.println("========================================");
    }

    /**
     * 执行批量操作
     */
    private void executeBulkOperation(List<BulkOperation> operations) throws Exception {
        BulkRequest bulkRequest = BulkRequest.of(b -> b
            .index(indexName)
            .operations(operations)
        );

        BulkResponse bulkResponse = elasticsearchClient.bulk(bulkRequest);
        
        if (bulkResponse.errors()) {
            System.err.println("Bulk operation had errors:");
            for (BulkResponseItem item : bulkResponse.items()) {
                if (item.error() != null) {
                    System.err.println("Error: " + item.error().reason());
                    totalErrors++;
                } else {
                    totalWritten++;
                }
            }
        } else {
            totalWritten += operations.size();
            System.out.println("Successfully wrote " + operations.size() + " documents to " + indexName);
        }
    }

    /**
     * 创建索引（如果不存在）
     */
    public void createIndexIfNotExists() throws Exception {
        try {
            boolean exists = elasticsearchClient.indices().exists(e -> e.index(indexName)).value();
            if (!exists) {
                elasticsearchClient.indices().create(c -> c.index(indexName));
                System.out.println("Created index: " + indexName);
            } else {
                System.out.println("Index already exists: " + indexName);
            }
        } catch (Exception e) {
            System.err.println("Error creating index: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 删除索引
     */
    public void deleteIndex() throws Exception {
        try {
            boolean exists = elasticsearchClient.indices().exists(e -> e.index(indexName)).value();
            if (exists) {
                elasticsearchClient.indices().delete(d -> d.index(indexName));
                System.out.println("Deleted index: " + indexName);
            } else {
                System.out.println("Index does not exist: " + indexName);
            }
        } catch (Exception e) {
            System.err.println("Error deleting index: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 完成索引
     */
    public void finishIndex() throws Exception {
        finishWrite();
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
     * 获取索引统计信息
     */
    public Map<String, Object> getIndexStats() {
        return (Map<String, Object>) getStats();
    }
}