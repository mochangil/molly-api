package org.example.mollyapi.order.event.V3.handler.user;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mollyapi.common.exception.CustomException;
import org.example.mollyapi.common.exception.error.impl.PaymentError;
import org.example.mollyapi.common.exception.error.impl.UserError;
import org.example.mollyapi.order.entity.Order;
import org.example.mollyapi.order.event.V3.event.order.OrderPreProcessEvent;
import org.example.mollyapi.order.repository.OrderRepository;
import org.example.mollyapi.payment.util.AESUtil;
import org.example.mollyapi.user.entity.User;
import org.example.mollyapi.user.repository.UserRepository;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PointEventHandlerV3 {
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @EventListener
    @Async
    @Transactional
    public void handleOrderPreProcessEvent(OrderPreProcessEvent event) {
        Order order = orderRepository.findByTossOrderId(event.tossOrderId())
                .orElseThrow(() -> new CustomException(PaymentError.ORDER_NOT_FOUND));
        User user = userRepository.findById(order.getUser().getUserId())
                .orElseThrow(() -> new CustomException(UserError.NOT_EXISTS_USER));

        int pointUsage = 0;
        validateUserPoint(user, pointUsage);
        user.updatePoint(pointUsage);
        userRepository.save(user);
    }

    private void validateUserPoint(User user, Integer requiredPoint) {
        if (user.getPoint() < requiredPoint) {
            throw new IllegalArgumentException("사용자 포인트가 부족합니다.");
        }
    }
}
