package com.cafe.infrastructure.kafka;

import com.cafe.domain.order.event.OrderCanceledEvent;
import com.cafe.domain.order.event.OrderPaidEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.Executor;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderKafkaProducer {
    // 주문 도메인 이벤트를 JSON 문자열로 직렬화해 Kafka에 발행한다.

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper objectMapper;
    @Qualifier("kafkaProducerCallbackExecutor")
    private final Executor callbackExecutor;

    public void sendOrderPaid(OrderPaidEvent event) {
        send(KafkaTopics.ORDER_PAID, event.orderNumber(), event);
    }

    public void sendOrderCanceled(OrderCanceledEvent event) {
        send(KafkaTopics.ORDER_CANCELED, event.orderNumber(), event);
    }

    private void send(String topic, String key, Object payload) {
        try {
            // Spring Kafka JsonSerializer 대신 명시적으로 JSON 문자열을 만들어 Jackson 타입 힌트 이슈를 줄인다.
            String message = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, key, message)
                    .whenCompleteAsync((result, exception) -> {
                        if (exception != null) {
                            log.error("Failed to publish order event. topic={}, key={}", topic, key, exception);
                            return;
                        }
                        // 성공 로그는 이벤트가 많아질 수 있어 debug 레벨로 둔다.
                        log.debug("Published order event. topic={}, key={}, offset={}",
                                topic,
                                key,
                                result.getRecordMetadata().offset()
                        );
                    }, callbackExecutor);
        } catch (Exception exception) {
            // 직렬화 실패는 Kafka 전송 전 단계에서 발생하므로 별도로 로그를 남긴다.
            log.error("Failed to serialize order event. topic={}, key={}", topic, key, exception);
        }
    }
}
