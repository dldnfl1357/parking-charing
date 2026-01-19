package com.example.collection.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${kafka.topic.facility}")
    private String facilityTopic;

    @Bean
    public NewTopic facilityTopic() {
        return TopicBuilder.name(facilityTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
