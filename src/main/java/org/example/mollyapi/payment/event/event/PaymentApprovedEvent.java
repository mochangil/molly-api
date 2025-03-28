package org.example.mollyapi.payment.event.event;

public record PaymentApprovedEvent(
        Long paymentId,
        String paymentType,
        Long amount,
        String paymentStatus,
        String tossOrderId,
        String tossPaymentKey

) {
}
