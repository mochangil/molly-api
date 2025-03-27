package org.example.mollyapi.order.event.V3.handler.payment;


import lombok.RequiredArgsConstructor;
import org.example.mollyapi.order.event.V3.event.order.OrderProcessEvent;
import org.example.mollyapi.order.event.V3.event.payment.PaymentApprovedEventV3;
import org.example.mollyapi.order.event.V3.event.payment.PaymentFailedEventV3;
import org.example.mollyapi.payment.service.PaymentService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class PaymentEventHandlerV3 {
    private final PaymentService paymentService;

    @EventListener
    @Async
    @Transactional
    public void handleOrderProcessEvent(OrderProcessEvent event) {
        System.out.println("Order process event: " + event);
        paymentService.processPayment(event.userId(), event.paymentConfirmReqDto());
    }

    @EventListener
    @Async
    @Transactional
    public void handlePaymentApprovedEvent(PaymentApprovedEventV3 event) {
        paymentService.approvePayment(event.paymentKey());
    }

    @EventListener
    @Async
    @Transactional
    public void handlePaymentFailedEvent(PaymentFailedEventV3 event) {
        paymentService.failPayment(event.paymentId(),event.error());
    }
}