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
    // RDB 주문 테이블을 매번 집계하지 않고, 주문 이벤트를 누적해 빠른 인기 메뉴 조회용 ZSET을 만든다.

    private final JsonMapper objectMapper;
    private final PopularMenuRankingService popularMenuRankingService;

    @KafkaListener(topics = KafkaTopics.ORDER_PAID)
    public void consumeOrderPaid(String message, Acknowledgment acknowledgment) throws Exception {
        // 메시지 처리가 끝난 뒤 수동 ack를 호출해 offset commit 시점을 명확히 한다.
        // 역직렬화 또는 Redis 갱신 중 예외가 나면 ack가 호출되지 않아 Kafka가 재처리할 수 있다.
        OrderPaidEvent event = objectMapper.readValue(message, OrderPaidEvent.class);
        popularMenuRankingService.increase(event);
        acknowledgment.acknowledge();
        log.debug("Applied order paid ranking event. orderNumber={}", event.orderNumber());
    }

    @KafkaListener(topics = KafkaTopics.ORDER_CANCELED)
    public void consumeOrderCanceled(String message, Acknowledgment acknowledgment) throws Exception {
        // 취소 이벤트는 원 주문일자의 ZSET 점수를 감소시킨다.
        // 점수가 0 이하가 되면 PopularMenuRankingService가 ZSET member를 제거한다.
        OrderCanceledEvent event = objectMapper.readValue(message, OrderCanceledEvent.class);
        popularMenuRankingService.decrease(event);
        acknowledgment.acknowledge();
        log.debug("Applied order canceled ranking event. orderNumber={}", event.orderNumber());
    }
}
