package org.example.mollyapi.order.event.V2.event.order;

public record OrderPostProcessEvent(
        String tossOrderId
) implements BaseEvent {
}
