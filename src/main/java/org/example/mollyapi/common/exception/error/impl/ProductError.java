package org.example.mollyapi.common.exception.error.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.mollyapi.common.exception.error.CustomError;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ProductError implements CustomError {
    NOT_EXISTS_PRODUCT(HttpStatus.BAD_REQUEST, "해당 상품이 존재하지 않습니다."),
    NEGATIVE_PRICE(HttpStatus.BAD_REQUEST, "가격은 0보다 작을 수 없습니다."),

    //problem registering bulk products.
    ;

    private final HttpStatus status;
    private final String message;
}
