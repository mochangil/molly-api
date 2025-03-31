package org.example.mollyapi.product.dto.request;

import lombok.Builder;
import lombok.Getter;

@Getter
public class ProductBulkItemReqDto {

    private final long productId;

    private final String color;

    private final String colorCode;

    private final long quantity;

    private final String size;

    private final long itemId;

    private final long viewCount;

    private final long purchaseCount;

    private final long categoryId;

    private final long price;

    private final String brandName;

    @Builder
    public ProductBulkItemReqDto(long productId, String color, String colorCode, long quantity,
        String size, long itemId, long viewCount, long purchaseCount, long categoryId, long price,
        String brandName) {
        this.productId = productId;
        this.color = color;
        this.colorCode = colorCode;
        this.quantity = quantity;
        this.size = size;
        this.itemId = itemId;
        this.viewCount = viewCount;
        this.purchaseCount = purchaseCount;
        this.categoryId = categoryId;
        this.price = price;
        this.brandName = brandName;
    }

    public static ProductBulkItemReqDto createBulkProductItemReqDto(long itemId,
        long productBulkReqDtoId,
        String color,
        String colorCode,
        long quantity,
        String size,
        long viewCount,
        long purchaseCount,
        long categoryId,
        long price,
        String brandName) {
        return ProductBulkItemReqDto.builder()
            .itemId(itemId)
            .productId(productBulkReqDtoId)
            .color(color)
            .colorCode(colorCode)
            .quantity(quantity)
            .size(size)
            .viewCount(viewCount)
            .purchaseCount(purchaseCount)
            .categoryId(categoryId)
            .price(price)
            .brandName(brandName)
            .build();
    }
}
