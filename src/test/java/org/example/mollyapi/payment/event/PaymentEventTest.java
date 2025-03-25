package org.example.mollyapi.payment.event;

import org.example.mollyapi.common.exception.error.impl.PaymentError;
import org.example.mollyapi.order.event.handler.order.OrderEventHandler;
import org.example.mollyapi.payment.event.event.PaymentApprovedEvent;
import org.example.mollyapi.payment.event.event.PaymentFailedEvent;
import org.example.mollyapi.payment.event.handler.PaymentEventHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.Mockito.verify;

@SpringBootTest
public class PaymentEventTest {

    @Autowired
    private ApplicationEventPublisher publisher;

    @MockBean
    private PaymentEventHandler paymentEventHandler;

    @MockBean
    private OrderEventHandler orderEventHandler;

    @DisplayName("PaymentApprove 이벤트 전파를 확인합니다")
    @Test
    void testPaymentApproveEvent() {

        //when
        PaymentApprovedEvent event = new PaymentApprovedEvent("ord-123","pay-123");
        publisher.publishEvent(event);

        //then
        verify(paymentEventHandler).handlePaymentApprovedEvent(event);
        verify(orderEventHandler).handlePaymentApprovedEvent(event);
    }

    @DisplayName("PaymentFail 이벤트 전파를 확인합니다")
    @Test
    void testPaymentFailedEvent() {

        //when
        PaymentFailedEvent event = new PaymentFailedEvent(1L, PaymentError.PAYMENT_FAILED);
        publisher.publishEvent(event);

        //then
        verify(paymentEventHandler).handlePaymentFailedEvent(event);
    }
}
