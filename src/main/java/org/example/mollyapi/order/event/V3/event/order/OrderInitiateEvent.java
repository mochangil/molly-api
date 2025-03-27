package org.example.mollyapi.order.event.V3.event.order;

import org.example.mollyapi.delivery.dto.DeliveryReqDto;

public record OrderInitiateEvent(
        Long userId,
        String paymentKey,
        String tossOrderId,
        Long amount,
        String point,
        String paymentType,
        DeliveryReqDto deliveryInfo
) {
}
