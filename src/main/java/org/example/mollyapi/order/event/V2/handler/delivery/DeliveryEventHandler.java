package org.example.mollyapi.order.event.V2.handler.delivery;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mollyapi.common.exception.CustomException;
import org.example.mollyapi.common.exception.error.impl.PaymentError;
import org.example.mollyapi.delivery.entity.Delivery;
import org.example.mollyapi.delivery.repository.DeliveryRepository;
import org.example.mollyapi.order.entity.Order;
import org.example.mollyapi.order.event.V2.event.order.OrderProcessEvent;
import org.example.mollyapi.order.repository.OrderRepository;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryEventHandler {
    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;

    @EventListener
    @Async("processOrderExecutor")
    @Transactional
    public void handleOrderProcessEvent(OrderProcessEvent event) {

        Order order = orderRepository.findByTossOrderId(event.tossOrderId())
                        .orElseThrow(() -> new CustomException(PaymentError.ORDER_NOT_FOUND));

        event.deliveryInfo().validate();
        order.setDelivery(Delivery.from(event.deliveryInfo(),order.getId()));
    }


}
