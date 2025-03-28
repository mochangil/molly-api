package org.example.mollyapi.order.event.V3.event.payment;

import org.example.mollyapi.payment.entity.Payment;

public record PaymentApprovedEventV3(
        String tossOrderId,
        String paymentKey
) {
}
