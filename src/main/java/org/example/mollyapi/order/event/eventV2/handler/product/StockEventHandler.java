package org.example.mollyapi.order.event.eventV2.handler.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mollyapi.common.exception.CustomException;
import org.example.mollyapi.common.exception.error.impl.PaymentError;
import org.example.mollyapi.order.entity.Order;
import org.example.mollyapi.order.entity.OrderDetail;
import org.example.mollyapi.order.event.eventV2.event.order.OrderPreProcessEvent;
import org.example.mollyapi.order.repository.OrderRepository;
import org.example.mollyapi.product.entity.ProductItem;
import org.example.mollyapi.product.repository.ProductItemRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockEventHandler {

    private final OrderRepository orderRepository;
    private final ProductItemRepository productItemRepository;

    @EventListener
    public void handleOrderPreProcessingEvent(OrderPreProcessEvent event) {

        Order order = orderRepository.findByTossOrderId(event.tossOrderId())
                        .orElseThrow(()-> new CustomException(PaymentError.ORDER_NOT_FOUND));

        for (OrderDetail detail : order.getOrderDetails()) {
            ProductItem productItem = productItemRepository.findByIdWithLock(detail.getProductItem().getId())
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. itemId=" + detail.getProductItem().getId()));

            productItem.decreaseStock(detail.getQuantity());
        }
    }
}
