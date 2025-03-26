package org.example.mollyapi.product.dto;

import jakarta.validation.constraints.NotBlank;

public record ProductItemReqDto(
        @NotBlank String color,
        @NotBlank String colorCode,
        @NotBlank String size,
        @NotBlank Long quantity
) {

    public static ProductItemReqDto of(ProductItemDto itemDto) {
        return new ProductItemReqDto(
                itemDto.color(),
                itemDto.colorCode(),
                itemDto.size(),
                itemDto.quantity()
        );
    }
}
