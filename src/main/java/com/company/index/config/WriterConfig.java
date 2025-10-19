package com.company.index.config;

import com.company.index.batch.writer.ElasticsearchWriter;
import com.company.index.batch.writer.FileWriter;
import com.company.index.batch.writer.GetQuickWriter;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Writer 配置类
 * 解决 Writer 类的依赖注入问题
 */
@Configuration
public class WriterConfig {

    @Value("${index.indexTarget.type:file}")
    private String indexTargetType;

    @Value("${index.indexTarget.path:./output}")
    private String outputPath;

    @Value("${index.indexTarget.fileName:output.json}")
    private String fileName;

    @Value("${index.indexTarget.format:json}")
    private String format;

    @Value("${index.indexTarget.indexName:orders-index}")
    private String indexName;

    @Value("${index.indexTarget.batchSize:1000}")
    private int batchSize;

    @Value("${elasticsearch.hosts:localhost:9200}")
    private String elasticsearchHosts;

    @Value("${elasticsearch.username:}")
    private String elasticsearchUsername;

    @Value("${elasticsearch.password:}")
    private String elasticsearchPassword;

    @Value("${getquick.url:http://localhost:8080}")
    private String getQuickUrl;

    @Value("${getquick.username:}")
    private String getQuickUsername;

    @Value("${getquick.password:}")
    private String getQuickPassword;

    /**
     * FileWriter Bean
     */
    @Bean
    public FileWriter fileWriter() {
        return new FileWriter(outputPath, fileName, format);
    }

    /**
     * ElasticsearchWriter Bean
     */
    @Bean
    public ElasticsearchWriter elasticsearchWriter() {
        ElasticsearchClient client = createElasticsearchClient();
        return new ElasticsearchWriter(client, indexName, batchSize);
    }

    /**
     * GetQuickWriter Bean
     */
    @Bean
    public GetQuickWriter getQuickWriter() {
        return new GetQuickWriter(getQuickUrl, indexName, getQuickUsername, getQuickPassword, batchSize);
    }

    /**
     * 创建 Elasticsearch 客户端
     */
    private ElasticsearchClient createElasticsearchClient() {
        // 解析 hosts 配置
        String[] hosts = elasticsearchHosts.split(",");
        HttpHost[] httpHosts = new HttpHost[hosts.length];
        
        for (int i = 0; i < hosts.length; i++) {
            String[] hostPort = hosts[i].trim().split(":");
            String host = hostPort[0];
            int port = hostPort.length > 1 ? Integer.parseInt(hostPort[1]) : 9200;
            httpHosts[i] = new HttpHost(host, port, "http");
        }

        // 创建 RestClient
        RestClient restClient = RestClient.builder(httpHosts).build();
        
        // 创建 Transport
        ElasticsearchTransport transport = new RestClientTransport(
            restClient, new JacksonJsonpMapper());
        
        // 创建 ElasticsearchClient
        return new ElasticsearchClient(transport);
    }
}
