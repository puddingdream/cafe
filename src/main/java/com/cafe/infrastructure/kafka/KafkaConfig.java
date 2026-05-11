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
    // 현재 Kafka는 주문 완료/취소 이벤트를 Redis 인기 메뉴 ZSET read model에 반영하는 용도로 사용한다.

    @Bean
    public NewTopic orderPaidTopic() {
        // 주문 완료 이벤트를 발행하는 실제 사용 토픽이다.
        // consumer는 이 이벤트를 받아 메뉴별 판매 수량 점수를 증가시킨다.
        return TopicBuilder.name(KafkaTopics.ORDER_PAID)
                .partitions(KafkaTopics.DEFAULT_PARTITIONS)
                .replicas(KafkaTopics.DEFAULT_REPLICATION_FACTOR)
                .build();
    }

    @Bean
    public NewTopic orderCanceledTopic() {
        // 주문 취소 이벤트를 발행하는 실제 사용 토픽이다.
        // consumer는 이 이벤트를 받아 원 주문일자의 메뉴별 판매 수량 점수를 감소시킨다.
        return TopicBuilder.name(KafkaTopics.ORDER_CANCELED)
                .partitions(KafkaTopics.DEFAULT_PARTITIONS)
                .replicas(KafkaTopics.DEFAULT_REPLICATION_FACTOR)
                .build();
    }

    @Bean
    public NewTopic orderPaidDltTopic() {
        // 현재는 확장용으로만 생성해 둔 DLT 토픽이다.
        // DefaultErrorHandler/DLT 라우팅을 붙이면 처리 실패 이벤트를 별도 토픽에 보관할 수 있다.
        return TopicBuilder.name(KafkaTopics.ORDER_PAID_DLT)
                .partitions(KafkaTopics.DEFAULT_PARTITIONS)
                .replicas(KafkaTopics.DEFAULT_REPLICATION_FACTOR)
                .build();
    }

    @Bean
    public NewTopic orderCanceledDltTopic() {
        // 주문 취소 이벤트 처리 실패를 분리해 둘 수 있는 DLT 토픽이다. 현재 consumer에는 아직 DLT recoverer를 연결하지 않았다.
        return TopicBuilder.name(KafkaTopics.ORDER_CANCELED_DLT)
                .partitions(KafkaTopics.DEFAULT_PARTITIONS)
                .replicas(KafkaTopics.DEFAULT_REPLICATION_FACTOR)
                .build();
    }

    @Bean(name = "kafkaProducerCallbackExecutor")
    public Executor kafkaProducerCallbackExecutor() {
        // Kafka send 결과 콜백을 별도 스레드에서 처리해 요청 흐름을 막지 않는다.
        // callback은 성공/실패 로그 기록 목적이고, 주문 트랜잭션의 성공 여부를 다시 바꾸지는 않는다.
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("kafka-cb-");
        executor.initialize();
        return executor;
    }
}
