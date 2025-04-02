package org.example.mollyapi.order.event.V2.handler.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mollyapi.order.event.V2.EventFutureType;
import org.example.mollyapi.order.event.V2.annotation.HandleFutureEvent;
import org.example.mollyapi.order.event.V2.event.order.OrderInitiateEvent;
import org.example.mollyapi.order.service.OrderServiceImplV0;
import org.example.mollyapi.payment.event.event.PaymentApprovedEvent;
import org.example.mollyapi.payment.event.event.PaymentFailedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventHandler {

    private final OrderServiceImplV0 orderServiceImplV0;

    @EventListener
    @Async("preProcessOrderExecutor")
    @HandleFutureEvent(EventFutureType.INITIATE_ORDER)
    @Transactional
    public void handleOrderInitiateEvent(OrderInitiateEvent event) {
        System.out.println("Order Initiate Event");
        //주문 검증
        orderServiceImplV0.validateOrder(event.tossOrderId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("paymentExecutor")
    @HandleFutureEvent(EventFutureType.SUCCESS_ORDER)
    public void handlePaymentApprovedEvent(PaymentApprovedEvent event) {
        orderServiceImplV0.successOrder(event.tossOrderId(),event.tossPaymentKey());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("paymentExecutor")
    @HandleFutureEvent(EventFutureType.FAIL_ORDER)
    public void handlePaymentFailedEvent(PaymentFailedEvent event) {
//        orderServiceImplV0.failOrder();
    }
}
