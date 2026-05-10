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
            String message = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, key, message)
                    .whenCompleteAsync((result, exception) -> {
                        if (exception != null) {
                            log.error("Failed to publish order event. topic={}, key={}", topic, key, exception);
                            return;
                        }
                        log.debug("Published order event. topic={}, key={}, offset={}",
                                topic,
                                key,
                                result.getRecordMetadata().offset()
                        );
                    }, callbackExecutor);
        } catch (Exception exception) {
            log.error("Failed to serialize order event. topic={}, key={}", topic, key, exception);
        }
    }
}
