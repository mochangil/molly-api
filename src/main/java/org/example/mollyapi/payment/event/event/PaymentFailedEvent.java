package org.example.mollyapi.payment.event.event;

import org.example.mollyapi.common.exception.error.impl.PaymentError;

public record PaymentFailedEvent(
        Long paymentId,
        String paymentType,
        Long amount,
        String paymentStatus,
        String tossOrderId,
        String tossPaymentKey,
        PaymentError error
        ) {
}
