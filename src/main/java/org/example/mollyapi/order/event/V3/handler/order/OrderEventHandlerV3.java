package org.example.mollyapi.order.event.V3.handler.order;

import lombok.RequiredArgsConstructor;
import org.example.mollyapi.common.exception.CustomException;
import org.example.mollyapi.common.exception.error.impl.PaymentError;
import org.example.mollyapi.order.entity.Order;
import org.example.mollyapi.order.event.V3.event.order.OrderInitiateEvent;
import org.example.mollyapi.order.event.V3.event.payment.PaymentApprovedEventV3;
import org.example.mollyapi.order.repository.OrderRepository;
import org.example.mollyapi.order.service.OrderProcessServiceV3;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OrderEventHandlerV3 {

    private final OrderProcessServiceV3 orderProcessServiceV3;
    private final OrderRepository orderRepository;

    @EventListener
    @Async
    @Transactional
    public void handleOrderInitiateEvent(OrderInitiateEvent event) {

        Order order = orderRepository.findByTossOrderId(event.tossOrderId())
                .orElseThrow(() -> new CustomException(PaymentError.ORDER_NOT_FOUND));

        //주문 검증
        orderProcessServiceV3.validateOrder(event, order);
        //주문 정보 추가
        orderProcessServiceV3.addOrderInfo(event, order);

    }

    @EventListener
    @Async
    @Transactional
    public void handlePaymentApprovedEvent(PaymentApprovedEventV3 event) {
        orderProcessServiceV3.successOrder(event.tossOrderId(),event.paymentKey());
    }
}
