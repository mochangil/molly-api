package org.example.mollyapi.order.event.V3.event.delivery;

import org.example.mollyapi.delivery.entity.Delivery;

public record DeliveryCreatedEvent (
        Delivery delivery
){
}
