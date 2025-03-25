package org.example.mollyapi.order.event.event.order;


import org.example.mollyapi.delivery.dto.DeliveryReqDto;
import org.example.mollyapi.payment.dto.request.PaymentConfirmReqDto;

public record OrderProcessEvent(

        Long userId,
        String tossOrderId,
        DeliveryReqDto deliveryInfo,
        PaymentConfirmReqDto paymentConfirmReqDto
){
}
