package com.cafe.infrastructure.kafka;

import com.cafe.domain.order.event.OrderCanceledEvent;
import com.cafe.domain.order.event.OrderPaidEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class OrderEventKafkaPublisher {
    // Spring 도메인 이벤트를 트랜잭션 커밋 이후 Kafka 메시지로 전달하는 어댑터다.

    private final OrderKafkaProducer orderKafkaProducer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(OrderPaidEvent event) {
        // 주문 트랜잭션이 성공적으로 커밋된 뒤에만 Kafka로 발행한다.
        orderKafkaProducer.sendOrderPaid(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(OrderCanceledEvent event) {
        // 취소 이벤트도 커밋 이후에만 발행해 롤백된 주문이 랭킹에 반영되지 않게 한다.
        orderKafkaProducer.sendOrderCanceled(event);
    }
}
