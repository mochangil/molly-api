package org.example.mollyapi.order.dto;

import org.example.mollyapi.order.entity.Order;
import org.example.mollyapi.order.type.OrderStatus;
import org.example.mollyapi.payment.entity.Payment;
import org.example.mollyapi.payment.repository.PaymentRepository;
import org.example.mollyapi.review.repository.ReviewRepository;
import org.springframework.data.domain.PageRequest;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public record OrderHistoryResponseDto(
        Long userId,
        List<OrderSummaryDto> orders
) {
    public OrderHistoryResponseDto(Long userId, List<Order> orders, PaymentRepository paymentRepository, ReviewRepository reviewRepository) {
        this(userId, orders.stream()
                .map(order -> OrderSummaryDto.from(order, paymentRepository, reviewRepository))
                .collect(Collectors.toList()));
    }

    public record OrderSummaryDto(
            String tossOrderId,
            OrderStatus orderStatus,
            String orderedAt,
            Long paymentAmount,
            String deliveryStatus,
            List<OrderDetailWithReviewResponseDto> orderDetails
    ) {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        public static OrderSummaryDto from(Order order, PaymentRepository paymentRepository, ReviewRepository reviewRepository) {
            List<Payment> payments = paymentRepository.findLatestPaymentByOrderId(order.getId(), PageRequest.of(0, 1));
            Long paymentAmount = payments.isEmpty() ? 0L : payments.get(0).getAmount();
            return new OrderSummaryDto(
                    order.getTossOrderId(),
                    order.getStatus(),
                    order.getOrderedAt() != null ? order.getOrderedAt().format(FORMATTER) : null,
                    paymentAmount,
                    order.getDelivery() != null ? order.getDelivery().getStatus().name() : null,
                    order.getOrderDetails().stream()
                            .map(orderDetail -> OrderDetailWithReviewResponseDto.from(order.getUser().getUserId(), orderDetail, reviewRepository))
                            .collect(Collectors.toList()));
        }
    }
}