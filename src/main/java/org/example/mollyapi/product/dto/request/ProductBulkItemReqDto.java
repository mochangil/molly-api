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
    public ProductBulkItemReqDto(Long productId, String color, String colorCode, Long quantity, String size) {
        this.itemId = TsidCreator.getTsid().toLong();
        this.productId = productId;
        this.color = color;
        this.colorCode = colorCode;
        this.quantity = quantity;
        this.size = size;
    }

    public ProductItem toProductItem(Product product) {

        return ProductItem.builder()
                .product(product)
                .color(this.color)
                .colorCode(this.colorCode)
                .quantity(this.quantity)
                .size(this.size)
                .build();
    }

    public static ProductBulkItemReqDto createBulkProductItemReqDto(Long productBulkReqDtoId,
                                                             String color,
                                                             String colorCode,
                                                             long quantity,
                                                             String size) {
        return ProductBulkItemReqDto.builder()
                .productId(productBulkReqDtoId)
                .color(color)
                .colorCode(colorCode)
                .quantity(quantity)
                .size(size)
                .build();
    }
}
