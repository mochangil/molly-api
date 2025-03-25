package org.example.mollyapi.order.event.handler.cart;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mollyapi.cart.repository.CartRepository;
import org.example.mollyapi.order.entity.Order;
import org.example.mollyapi.order.entity.OrderDetail;
import org.example.mollyapi.order.event.event.order.OrderProcessEvent;
import org.example.mollyapi.order.repository.OrderRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class CartEventHandler {
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;

    @EventListener
    @Transactional
    public void handleOrderProcessEvent(OrderProcessEvent event) {
        Order order = orderRepository.findByTossOrderId(event.tossOrderId())
                .orElseThrow(() -> new IllegalArgumentException("validateBeforePayment: 일치하는 주문이 없습니다."));
        for (OrderDetail orderDetail : order.getOrderDetails()) {
            if (orderDetail.getCartId() != null) {
                cartRepository.deleteById(orderDetail.getCartId());
            }
        }
    }

}
