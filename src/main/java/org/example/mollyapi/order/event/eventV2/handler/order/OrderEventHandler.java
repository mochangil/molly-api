package org.example.mollyapi.order.event.eventV2.handler.order;

import lombok.RequiredArgsConstructor;
import org.example.mollyapi.order.event.eventV2.event.order.OrderInitiateEvent;
import org.example.mollyapi.order.service.OrderServiceImplV2;
import org.example.mollyapi.payment.event.event.PaymentApprovedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventHandler {

    private final OrderServiceImplV2 orderServiceImplV2;

    @EventListener
    public void handleOrderInitiateEvent(OrderInitiateEvent event) {
        System.out.println("Order Initiate Event");
        //주문 검증
        orderServiceImplV2.validateOrder(event.tossOrderId());
    }

    @EventListener
    public void handlePaymentApprovedEvent(PaymentApprovedEvent event) {
        orderServiceImplV2.successOrder(event.tossOrderId(),event.paymentKey());
    }
}
