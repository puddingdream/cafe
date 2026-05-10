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

    private final OrderKafkaProducer orderKafkaProducer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(OrderPaidEvent event) {
        orderKafkaProducer.sendOrderPaid(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(OrderCanceledEvent event) {
        orderKafkaProducer.sendOrderCanceled(event);
    }
}
