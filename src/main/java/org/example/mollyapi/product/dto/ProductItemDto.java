package org.example.mollyapi.product.dto;

import jakarta.validation.constraints.NotBlank;
import org.example.mollyapi.product.entity.Product;
import org.example.mollyapi.product.entity.ProductItem;

public record ProductItemDto(
        Long id,
        String color,
        String colorCode,
        String size,
        Long quantity
) {

    public static ProductItemDto of(ProductItem productItem) {
        return new ProductItemDto(
                productItem.getId(),
                productItem.getColor(),
                productItem.getColorCode(),
                productItem.getSize(),
                productItem.getQuantity()
        );
    }
}
