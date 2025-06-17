package org.example.mollyapi.common.exception.error.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.mollyapi.common.exception.error.CustomError;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum OrderError implements CustomError {

    INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "재고가 부족하여 결제를 진행할 수 없습니다."),
    INSUFFICIENT_POINT(HttpStatus.BAD_REQUEST, "사용자 포인트가 부족하여 결제를 진행할 수 없습니다."),
    ORDER_WITHDRAW_REFUND_FAIL(HttpStatus.BAD_REQUEST, "포인트 환불 중 오류가 발생했습니다. 관리자에게 문의하세요."),
    PAYMENT_RETRY_REQUIRED(HttpStatus.PAYMENT_REQUIRED, "결제가 실패했습니다. 다시 시도하시겠습니까? (API: /orders/{orderId}/fail-payment)"),
    INVALID_ORDER(HttpStatus.BAD_REQUEST, "유효하지 않은 결제 요청입니다."),
    NOT_EXIST_ORDER(HttpStatus.NOT_FOUND, "해당 주문을 찾을 수 없습니다."),
    EXPIRED_ORDER(HttpStatus.BAD_REQUEST, "이미 만료된 주문입니다.");

    private final HttpStatus status;
    private final String message;
}
