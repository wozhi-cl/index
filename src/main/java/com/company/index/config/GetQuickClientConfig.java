package com.company.index.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * GetQuick 客户端配置
 */
@Configuration
@Profile("!dev & !h2") // 开发环境和 H2 环境不配置 GetQuick
public class GetQuickClientConfig {

    @Value("${index.getquick.timeout:30000}")
    private int timeout;

    @Bean
    public RestTemplate getQuickRestTemplate() {
        // 配置 HTTP 客户端
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        // factory.setReadTimeout(timeout); // 方法不存在，注释掉
        factory.setConnectionRequestTimeout(timeout);

        return new RestTemplate(factory);
    }
}
