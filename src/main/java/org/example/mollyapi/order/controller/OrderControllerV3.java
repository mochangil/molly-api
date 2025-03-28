package org.example.mollyapi.order.controller;


import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mollyapi.order.dto.OrderConfirmRequestDto;
import org.example.mollyapi.order.service.OrderProcessServiceV3;
import org.example.mollyapi.payment.dto.response.PaymentResDto;
import org.example.mollyapi.user.auth.annotation.Auth;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/orders3")
@RequiredArgsConstructor
public class OrderControllerV3 {
    private final OrderProcessServiceV3 orderProcessServiceV3;

    private final OrderProcessServiceV3 orderProcessService;

    /**
     * 주문 결제 요청
     */
    @Auth
    @PostMapping("/{orderId}/payment")
    @Operation(summary = "주문 결제 요청 API", description = "주문에 대한 결제 요청 및 성공/실패 처리")
    public ResponseEntity<PaymentResDto> processPayment(
            HttpServletRequest request,
            @Valid @RequestBody OrderConfirmRequestDto orderConfirmRequestDto) {

        Long userId = (Long) request.getAttribute("userId");

        PaymentResDto response = orderProcessServiceV3.initiateOrder(
                userId,
                orderConfirmRequestDto.paymentKey(),
                orderConfirmRequestDto.tossOrderId(),
                orderConfirmRequestDto.amount(),
                orderConfirmRequestDto.point(),
                orderConfirmRequestDto.paymentType(),
                orderConfirmRequestDto.delivery()
        );

        return ResponseEntity.ok(response);
    }

}
