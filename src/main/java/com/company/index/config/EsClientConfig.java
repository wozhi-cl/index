package com.company.index.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Elasticsearch 客户端配置
 */
@Configuration
public class EsClientConfig {

    @Value("${index.indexTarget.url:http://localhost:9200}")
    private String esUrl;

    @Value("${index.indexTarget.username:}")
    private String username;

    @Value("${index.indexTarget.password:}")
    private String password;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        // 解析 URL
        HttpHost host = parseHttpHost(esUrl);
        
        // 创建 RestClient 构建器
        RestClientBuilder builder = RestClient.builder(host);
        
        // 配置认证（如果提供了用户名和密码）
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(username, password)
            );
            
            builder.setHttpClientConfigCallback(httpClientBuilder -> 
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
            );
        }
        
        // 创建 RestClient
        RestClient restClient = builder.build();
        
        // 创建 Jackson ObjectMapper 并配置 JSR310 支持
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // 创建传输层
        ElasticsearchTransport transport = new RestClientTransport(
            restClient,
            new JacksonJsonpMapper(objectMapper)
        );
        
        // 创建客户端
        return new ElasticsearchClient(transport);
    }

    /**
     * 解析 HTTP Host
     */
    private HttpHost parseHttpHost(String url) {
        try {
            if (url.startsWith("http://")) {
                url = url.substring(7);
            } else if (url.startsWith("https://")) {
                url = url.substring(8);
            }
            
            String[] parts = url.split(":");
            String host = parts[0];
            int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9200;
            
            return new HttpHost(host, port, "http");
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Elasticsearch URL: " + url, e);
        }
    }
}
