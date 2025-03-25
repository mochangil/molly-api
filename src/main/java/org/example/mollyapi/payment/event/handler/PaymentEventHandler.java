package org.example.mollyapi.payment.event.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mollyapi.order.event.event.order.OrderProcessEvent;
import org.example.mollyapi.payment.event.event.PaymentApprovedEvent;
import org.example.mollyapi.payment.event.event.PaymentFailedEvent;
import org.example.mollyapi.payment.service.PaymentService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventHandler {
    private final PaymentService paymentService;

    @EventListener
    public void handleOrderProcessEvent(OrderProcessEvent event) {
        paymentService.processPayment(event.userId(), event.paymentConfirmReqDto());
    }

    @EventListener
    public void handlePaymentApprovedEvent(PaymentApprovedEvent event) {
        paymentService.approvePayment(event.paymentKey());
    }

    @EventListener
    public void handlePaymentFailedEvent(PaymentFailedEvent event) {
        paymentService.failPayment(event.paymentId(),event.error());
    }
}
