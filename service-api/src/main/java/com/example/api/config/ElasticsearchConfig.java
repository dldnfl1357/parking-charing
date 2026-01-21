package com.example.api.config;

import org.apache.http.HttpHost;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;

/**
 * ElasticSearch 클라이언트 설정
 * - 커넥션 풀 최적화
 * - 타임아웃 설정
 */
@Configuration
public class ElasticsearchConfig {

    @Bean
    public RestClient restClient() {
        RestClientBuilder builder = RestClient.builder(new HttpHost("localhost", 9200, "http"));

        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            // IO 스레드 수 증가
            httpClientBuilder.setDefaultIOReactorConfig(
                    IOReactorConfig.custom()
                            .setIoThreadCount(Runtime.getRuntime().availableProcessors() * 2)
                            .build()
            );
            // 커넥션 풀 크기 증가
            httpClientBuilder.setMaxConnTotal(200);
            httpClientBuilder.setMaxConnPerRoute(100);
            return httpClientBuilder;
        });

        builder.setRequestConfigCallback(requestConfigBuilder ->
                requestConfigBuilder
                        .setConnectTimeout(5000)
                        .setSocketTimeout(30000)
        );

        return builder.build();
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(RestClient restClient) {
        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }

    @Bean
    public ElasticsearchOperations elasticsearchOperations(ElasticsearchClient elasticsearchClient) {
        return new ElasticsearchTemplate(elasticsearchClient);
    }
}
