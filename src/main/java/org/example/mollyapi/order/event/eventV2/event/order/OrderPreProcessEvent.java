package org.example.mollyapi.order.event.eventV2.event.order;

import org.example.mollyapi.delivery.dto.DeliveryReqDto;

public record OrderPreProcessEvent(
        Long userId,
        String tossOrderId,
        String point
){ }
