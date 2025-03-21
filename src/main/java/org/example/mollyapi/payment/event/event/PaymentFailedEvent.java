package org.example.mollyapi.payment.event.event;

import org.example.mollyapi.common.exception.error.impl.PaymentError;

public record PaymentFailedEvent(
        Long paymentId,
        PaymentError error
        ) {
}
