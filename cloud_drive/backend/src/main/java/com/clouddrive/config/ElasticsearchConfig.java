package com.clouddrive.config;

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
 * Elasticsearch配置类
 */
@Configuration
public class ElasticsearchConfig {

    @Value("${elasticsearch.host}")
    private String host;

    @Value("${elasticsearch.port}")
    private int port;

    /**
     * 创建Elasticsearch客户端
     */
    @Bean
    public ElasticsearchClient elasticsearchClient() {
        // 创建低级客户端
        RestClient restClient = RestClient.builder(
                new HttpHost(host, port, "http")
        ).build();
        
        // 使用Jackson映射器创建传输层
        ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper());
        
        // 创建API客户端
        return new ElasticsearchClient(transport);
    }
} 