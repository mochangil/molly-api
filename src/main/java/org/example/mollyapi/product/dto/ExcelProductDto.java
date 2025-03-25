package org.example.mollyapi.product.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExcelProductDto {

    String productName;

    String description;

    String brandName;

    String color;

    String colorCode;

    String size;

    String rowNumber;

    long categoryId;

    long price;

    long quantity;

    long itemId;

    @Builder
    public ExcelProductDto(long itemId, String productName, String description, String brandName, String color,
        String colorCode, String size, String rowNumber, long categoryId, long price,
        long quantity) {
        this.itemId = itemId;
        this.productName = productName;
        this.description = description;
        this.brandName = brandName;
        this.color = color;
        this.colorCode = colorCode;
        this.size = size;
        this.rowNumber = rowNumber;
        this.categoryId = categoryId;
        this.price = price;
        this.quantity = quantity;
    }


}
