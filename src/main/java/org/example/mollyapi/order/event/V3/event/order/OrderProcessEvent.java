package org.example.mollyapi.order.event.V3.event.order;


import org.example.mollyapi.payment.dto.request.PaymentConfirmReqDto;

public record OrderProcessEvent(
        Long userId,
        PaymentConfirmReqDto paymentConfirmReqDto
){
}
