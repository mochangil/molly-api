package org.example.mollyapi.order.service;

import org.example.mollyapi.order.dto.OrderHistoryResponseDto;
import org.example.mollyapi.order.dto.OrderRequestDto;
import org.example.mollyapi.order.dto.OrderResponseDto;
import org.example.mollyapi.payment.dto.response.PaymentResDto;
import org.example.mollyapi.delivery.dto.DeliveryReqDto;
import org.example.mollyapi.order.entity.Order;
import org.example.mollyapi.order.entity.OrderDetail;
import org.example.mollyapi.order.repository.OrderDetailRepository;
import org.example.mollyapi.order.repository.OrderRepository;
import org.example.mollyapi.order.type.CancelStatus;
import org.example.mollyapi.order.type.OrderStatus;
import org.example.mollyapi.payment.repository.PaymentRepository;
import org.example.mollyapi.product.entity.ProductItem;
import org.example.mollyapi.product.repository.ProductItemRepository;
import org.example.mollyapi.product.repository.ProductRepository;
import org.example.mollyapi.review.repository.ReviewRepository;
import org.example.mollyapi.user.entity.User;
import org.example.mollyapi.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface OrderService {
    OrderHistoryResponseDto getUserOrders(Long userId);
    OrderResponseDto getOrderDetails(Long orderId);
    OrderResponseDto createOrder(Long userId, List<OrderRequestDto> orderRequests);
    String cancelOrder(Long orderId);
    void expireOrder(Long orderId);
    PaymentResDto processPayment(Long userId, String paymentKey, String tossOrderId, Long amount, String point, String paymentType, DeliveryReqDto deliveryInfo);
//    PaymentResDto processPaymentTest(Long userId, String paymentKey, String tossOrderId, Long amount, String point, String paymentType, DeliveryReqDto deliveryInfo, String status);
    void withdrawOrder(Long orderId);
}