package org.example.mollyapi.cart.repository;

import org.example.mollyapi.cart.dto.Response.CartInfoDto;

import java.util.List;

public interface CartCustomRepository {
    List<CartInfoDto> getCartInfo(Long userId);
    boolean countByUserUserId(Long userId);
    List<Long> getExpiredCartId();
}
