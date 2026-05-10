package com.cafe.infrastructure.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic orderPaidTopic() {
        return TopicBuilder.name(KafkaTopics.ORDER_PAID)
                .partitions(KafkaTopics.DEFAULT_PARTITIONS)
                .replicas(KafkaTopics.DEFAULT_REPLICATION_FACTOR)
                .build();
    }

    @Bean
    public NewTopic orderPaidDltTopic() {
        return TopicBuilder.name(KafkaTopics.ORDER_PAID_DLT)
                .partitions(KafkaTopics.DEFAULT_PARTITIONS)
                .replicas(KafkaTopics.DEFAULT_REPLICATION_FACTOR)
                .build();
    }

    @Bean(name = "kafkaProducerCallbackExecutor")
    public Executor kafkaProducerCallbackExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("kafka-cb-");
        executor.initialize();
        return executor;
    }
}
