package org.example.mollyapi.cart.service;

import org.example.mollyapi.cart.dto.Request.AddCartReqDto;
import org.example.mollyapi.cart.dto.Request.UpdateCartReqDto;
import org.example.mollyapi.cart.dto.Response.CartInfoResDto;
import java.util.List;

public interface CartService {
    void addCart(AddCartReqDto addCartReqDto, Long userId);
    List<CartInfoResDto> getCartDetail(Long userId);
    void updateItemOption(UpdateCartReqDto updateCartReqDto, Long userId);
    void deleteCartItem(List<Long> cartList, Long userId);
}
