package org.example.mollyapi.order.event.V3.event.payment;

import org.example.mollyapi.common.exception.error.impl.PaymentError;

public record PaymentFailedEventV3(
        Long paymentId,
        PaymentError error
) {
}
