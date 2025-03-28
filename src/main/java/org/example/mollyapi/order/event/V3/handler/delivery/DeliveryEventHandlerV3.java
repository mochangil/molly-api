package org.example.mollyapi.order.event.V3.handler.delivery;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mollyapi.common.exception.CustomException;
import org.example.mollyapi.common.exception.error.impl.PaymentError;
import org.example.mollyapi.delivery.entity.Delivery;
import org.example.mollyapi.delivery.repository.DeliveryRepository;
import org.example.mollyapi.order.entity.Order;
import org.example.mollyapi.order.event.V3.event.order.OrderInitiateEvent;
import org.example.mollyapi.order.repository.OrderRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryEventHandlerV3 {
    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;

    @EventListener
    @Async
    @Transactional
    public void handleOrderInitiateEvent(OrderInitiateEvent event) {
        Order order = orderRepository.findByTossOrderId(event.tossOrderId())
                        .orElseThrow(() -> new CustomException(PaymentError.ORDER_NOT_FOUND));

        Delivery delivery = Delivery.from(event.deliveryInfo(),order.getId());
        deliveryRepository.save(delivery);

        //추후 ddd라면 order 도메인에 이벤트 발행
//        order.setDelivery(delivery);
//        orderRepository.save(order);
    }


}
