package org.example.mollyapi.order.controller;


import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mollyapi.order.dto.OrderConfirmRequestDto;
import org.example.mollyapi.order.dto.OrderCreateRequestDto;
import org.example.mollyapi.order.dto.OrderResponseDto;
import org.example.mollyapi.order.entity.Order;
import org.example.mollyapi.order.service.OrderServiceImplV2;
import org.example.mollyapi.order.repository.OrderRepository;
import org.example.mollyapi.payment.dto.response.PaymentResDto;
import org.example.mollyapi.user.auth.annotation.Auth;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/orders2")
@RequiredArgsConstructor
public class OrderControllerV2 {
    private final OrderServiceImplV2 orderServiceImplV2;
    private final OrderRepository orderRepository;

    /**
     * 주문 생성
     */
    @Auth
    @PostMapping(produces = "application/json")
    @Operation(summary = "주문 생성 API", description = "사용자가 장바구니에서 또는 상품페이지에서 주문을 요청")
    public ResponseEntity<OrderResponseDto> createOrder(
            @Valid @RequestBody OrderCreateRequestDto orderRequestDto,
            HttpServletRequest httpRequest) {

        Long userId = (Long) httpRequest.getAttribute("userId");

        OrderResponseDto response = orderServiceImplV2.createOrder(userId, orderRequestDto.orderRequests());

        return ResponseEntity.ok(response);
    }

    /**
     * 주문 철회 요청
     */
    @Auth
    @PostMapping("/{orderId}/withdraw")
    @Operation(summary = "주문 철회 요청 API", description = "주문 ID를 받아 주문 철회 요청")
    public ResponseEntity<String> withdrawOrder(@PathVariable Long orderId) {
        log.info("주문 철회 요청: orderId={}", orderId);
        orderServiceImplV2.withdrawOrder(orderId);
        return ResponseEntity.ok("주문 철회가 완료되었습니다.");
    }

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

        PaymentResDto response = orderServiceImplV2.processOrder(
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

    /**
     * 결제 실패 후 사용자 선택 API
     * (결제가 실패했을 때 "네"를 선택하면 결제를 다시 시도하고, "아니오"를 선택하면 주문 실패 처리)
     */
    @Auth
    @PostMapping("/{orderId}/fail-payment")
    @Operation(summary = "결제 실패 후 사용자 선택 API", description = "사용자가 결제 실패 후 다시 시도할지 여부를 선택")
    public ResponseEntity<String> handleFailedPayment(
            HttpServletRequest request,
            @PathVariable Long orderId,
            @RequestParam("retry") boolean retry) { // true면 재시도, false면 주문 실패 처리

        Long userId = (Long) request.getAttribute("userId");

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("해당 주문을 찾을 수 없습니다. orderId=" + orderId));
        String tossOrderId = order.getTossOrderId(); // 주문의 tossOrderId 가져오기

        if (retry) {
            // "네" 선택 시 결제 재시도
            try {
                orderServiceImplV2.processOrder(userId, null, null, null, null, null, null);
            } catch (Exception e) {
                log.error("수동 결제 재시도 실패! -> failOrder 실행: orderId={}", orderId);
                orderServiceImplV2.failOrder(tossOrderId); // 실패하면 즉시 failOrder 실행(강제)
                return ResponseEntity.ok("결제 재시도 실패로 주문이 자동 실패 처리되었습니다.");
            }
            return ResponseEntity.ok("결제를 다시 시도합니다.");
        } else {
            // "아니오" 선택 시 주문 즉시 실패 처리
            orderServiceImplV2.failOrder(tossOrderId);
            return ResponseEntity.ok("주문이 실패 처리되었습니다.");
        }
    }

    /**
     * 반품 확인 API
     */
    @Auth
    @PostMapping("/{orderId}/return-confirm")
    @Operation(summary = "반품 확인 API", description = "반품 도착 확인, 이후 자동 환불 진행 (관리자용)")
    public ResponseEntity<String> confirmReturn(@PathVariable Long orderId) {
        log.info("반품 확인 요청: orderId={}", orderId);
        orderServiceImplV2.handleReturnArrived(orderId);
        return ResponseEntity.ok("반품이 확인되어 환불 처리가 시작됩니다.");
    }


}
