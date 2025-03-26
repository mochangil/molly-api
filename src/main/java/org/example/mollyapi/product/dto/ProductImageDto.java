package org.example.mollyapi.product.dto;

import org.example.mollyapi.product.entity.ProductImage;
import org.example.mollyapi.product.enums.ProductImageType;

public record ProductImageDto(
        String url,
        String filename,
        ProductImageType type) {
    public static ProductImageDto of(ProductImage productImage) {
        ProductImageType type;

        if (productImage.getIsRepresentative()) {
            type = ProductImageType.THUMBNAIL;
        } else if (productImage.getIsProductImage()) {
            type = ProductImageType.PRODUCT;
        } else {
            type = ProductImageType.DESCRIPTION;
        }

        return new ProductImageDto(
                productImage.getUrl(),
                productImage.getFilename(),
                type
        );
    }
}
