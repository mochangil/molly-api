package org.example.mollyapi.order.v4.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.mollyapi.order.dto.OrderConfirmRequestDto;
import org.example.mollyapi.order.v4.dto.request.OrderPaymentResponseDto;
import org.example.mollyapi.order.v4.dto.request.OrderRequestDto;
import org.example.mollyapi.order.v4.dto.request.OrderRequestDtoList;
import org.example.mollyapi.order.v4.dto.response.OrderResponseDto;
import org.example.mollyapi.order.v4.service.OrderServiceV4;
import org.example.mollyapi.user.auth.annotation.Auth;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders/v4")
@RequiredArgsConstructor
public class OrderControllerV4 {
    private final OrderServiceV4 orderService;

    /**
     * 주문 생성
     */
    @Auth
    @PostMapping(produces = "application/json")
    @Operation(summary = "주문 생성 API", description = "사용자가 장바구니에서 또는 상품페이지에서 주문을 요청")
    public ResponseEntity<OrderResponseDto> createOrder(
            @Valid @RequestBody OrderRequestDtoList orderRequestDto,
            HttpServletRequest httpRequest) {

        Long userId = (Long) httpRequest.getAttribute("userId");
        OrderResponseDto response = orderService.createOrder(userId, orderRequestDto.orderRequests());

        return ResponseEntity.ok(response);
    }

    @Auth
    @PostMapping("/order/payment")
    public ResponseEntity<OrderPaymentResponseDto> initiateOrder(
            HttpServletRequest request,
            @Valid @RequestBody OrderConfirmRequestDto orderConfirmRequestDto){
        Long userId = (Long)request.getAttribute("userId");
        System.out.println(userId);
        orderService.initiateOrder(userId, orderConfirmRequestDto);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
