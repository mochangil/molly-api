package org.example.mollyapi.payment.event.event;

public record PaymentApprovedEvent(
        String tossOrderId,
        String paymentKey
) {
}
