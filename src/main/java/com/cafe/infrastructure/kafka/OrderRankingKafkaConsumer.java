package com.cafe.infrastructure.kafka;

import com.cafe.domain.menu.support.PopularMenuRankingService;
import com.cafe.domain.order.event.OrderCanceledEvent;
import com.cafe.domain.order.event.OrderPaidEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderRankingKafkaConsumer {
    // Kafka 주문 이벤트를 소비해 Redis 인기 메뉴 read model을 갱신한다.

    private final JsonMapper objectMapper;
    private final PopularMenuRankingService popularMenuRankingService;

    @KafkaListener(topics = KafkaTopics.ORDER_PAID)
    public void consumeOrderPaid(String message, Acknowledgment acknowledgment) throws Exception {
        // 메시지 처리가 끝난 뒤 수동 ack를 호출해 offset commit 시점을 명확히 한다.
        OrderPaidEvent event = objectMapper.readValue(message, OrderPaidEvent.class);
        popularMenuRankingService.increase(event);
        acknowledgment.acknowledge();
        log.debug("Applied order paid ranking event. orderNumber={}", event.orderNumber());
    }

    @KafkaListener(topics = KafkaTopics.ORDER_CANCELED)
    public void consumeOrderCanceled(String message, Acknowledgment acknowledgment) throws Exception {
        // 취소 이벤트는 원 주문일자의 ZSET 점수를 감소시킨다.
        OrderCanceledEvent event = objectMapper.readValue(message, OrderCanceledEvent.class);
        popularMenuRankingService.decrease(event);
        acknowledgment.acknowledge();
        log.debug("Applied order canceled ranking event. orderNumber={}", event.orderNumber());
    }
}
