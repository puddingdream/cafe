package com.cafe.infrastructure.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class KafkaConfig {
    // 주문 이벤트 토픽과 producer callback executor를 등록한다.

    @Bean
    public NewTopic orderPaidTopic() {
        // 주문 완료 이벤트를 발행하는 실제 사용 토픽이다.
        return TopicBuilder.name(KafkaTopics.ORDER_PAID)
                .partitions(KafkaTopics.DEFAULT_PARTITIONS)
                .replicas(KafkaTopics.DEFAULT_REPLICATION_FACTOR)
                .build();
    }

    @Bean
    public NewTopic orderCanceledTopic() {
        // 주문 취소 이벤트를 발행하는 실제 사용 토픽이다.
        return TopicBuilder.name(KafkaTopics.ORDER_CANCELED)
                .partitions(KafkaTopics.DEFAULT_PARTITIONS)
                .replicas(KafkaTopics.DEFAULT_REPLICATION_FACTOR)
                .build();
    }

    @Bean
    public NewTopic orderPaidDltTopic() {
        // 현재는 확장용으로만 생성해 둔 DLT 토픽이다.
        return TopicBuilder.name(KafkaTopics.ORDER_PAID_DLT)
                .partitions(KafkaTopics.DEFAULT_PARTITIONS)
                .replicas(KafkaTopics.DEFAULT_REPLICATION_FACTOR)
                .build();
    }

    @Bean
    public NewTopic orderCanceledDltTopic() {
        return TopicBuilder.name(KafkaTopics.ORDER_CANCELED_DLT)
                .partitions(KafkaTopics.DEFAULT_PARTITIONS)
                .replicas(KafkaTopics.DEFAULT_REPLICATION_FACTOR)
                .build();
    }

    @Bean(name = "kafkaProducerCallbackExecutor")
    public Executor kafkaProducerCallbackExecutor() {
        // Kafka send 결과 콜백을 별도 스레드에서 처리해 요청 흐름을 막지 않는다.
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("kafka-cb-");
        executor.initialize();
        return executor;
    }
}
