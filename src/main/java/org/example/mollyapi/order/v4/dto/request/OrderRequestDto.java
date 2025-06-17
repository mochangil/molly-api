package org.example.mollyapi.order.v4.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record OrderRequestDto(
        Long cartId,
        @NotNull Long itemId,
        @NotNull Long quantity
) {
    public static org.example.mollyapi.order.dto.OrderRequestDto from(Long cartId, Long itemId, Long quantity) {
        return new org.example.mollyapi.order.dto.OrderRequestDto(cartId, itemId, quantity);
    }
}