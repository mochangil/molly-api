package org.example.mollyapi.common.exception.error.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.mollyapi.common.exception.error.CustomError;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CartError implements CustomError {

    MAX_CART(HttpStatus.BAD_REQUEST, "장바구니 최대 수량을 초과했습니다."),
    EMPTY_CART(HttpStatus.NO_CONTENT, "장바구니가 비었습니다."),
    FAIL_UPDATE(HttpStatus.INTERNAL_SERVER_ERROR, "변경 사항 업데이트에 실패했습니다."),
    FAIL_DELETE(HttpStatus.INTERNAL_SERVER_ERROR, "장바구니 삭제에 실패했습니다."),
    EXIST_CART(HttpStatus.CONFLICT, "해당 상품이 이미 장바구니에 존재합니다."),
    NOT_EXIST_CART(HttpStatus.BAD_REQUEST, "요청 하신 내역에 문제가 발생했습니다.")
    ;

    private final HttpStatus status;
    private final String message;

}
