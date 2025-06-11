package org.example.mollyapi.order.v4.dto.request;

import org.example.mollyapi.address.dto.AddressResponseDto;
import org.example.mollyapi.order.dto.OrderDetailResponseDto;
import org.example.mollyapi.order.dto.OrderResponseDto;
import org.example.mollyapi.order.entity.Order;
import org.example.mollyapi.order.entity.OrderDetail;
import org.example.mollyapi.order.type.OrderStatus;
import org.example.mollyapi.payment.dto.response.PaymentInfoResDto;

import java.util.List;

public record OrderPaymentResponseDto (
        Long orderId,
        String tossOrderId,
        Long totalAmount,
        OrderStatus status,
        Integer userPoint,
        AddressResponseDto defaultAddress,
        PaymentInfoResDto payment,
        List<OrderDetailResponseDto> orderDetails
){
    public static OrderResponseDto from(Order order, List<OrderDetail> orderDetails, Integer userPoint, AddressResponseDto defaultAddress) {
        return new OrderResponseDto(
                order.getId(),
                order.getTossOrderId(),
                order.getTotalAmount(),
                order.getStatus(),
                userPoint,
                defaultAddress,
                (order.getPayments().isEmpty()) ? null : PaymentInfoResDto.from(order.getPayments().get(0)),
                orderDetails.stream().map(OrderDetailResponseDto::from).toList()
        );
    }
}
