package org.example.mollyapi.order.event.V2.event.order;

public record OrderPreProcessEvent(
        Long userId,
        String tossOrderId,
        String point
){ }
