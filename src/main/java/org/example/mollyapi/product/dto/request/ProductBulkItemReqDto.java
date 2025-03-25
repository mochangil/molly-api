package org.example.mollyapi.product.dto.request;

import com.github.f4b6a3.tsid.TsidCreator;
import lombok.Builder;
import lombok.Getter;
import org.example.mollyapi.product.entity.Product;
import org.example.mollyapi.product.entity.ProductItem;

@Getter
public class ProductBulkItemReqDto {

    private final long productId;

    private final String color;

    private final String colorCode;

    private final long quantity;

    private final String size;

    private final long itemId;

    @Builder
    public ProductBulkItemReqDto(long itemId, long productId, String color, String colorCode,
        long quantity, String size) {
        this.itemId = itemId;
        this.productId = productId;
        this.color = color;
        this.colorCode = colorCode;
        this.quantity = quantity;
        this.size = size;
    }

    public static ProductBulkItemReqDto createBulkProductItemReqDto(long itemId,
        long productBulkReqDtoId,
        String color,
        String colorCode,
        long quantity,
        String size) {
        return ProductBulkItemReqDto.builder()
            .itemId(itemId)
            .productId(productBulkReqDtoId)
            .color(color)
            .colorCode(colorCode)
            .quantity(quantity)
            .size(size)
            .build();
    }
}
