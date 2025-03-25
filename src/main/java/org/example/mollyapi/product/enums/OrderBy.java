package org.example.mollyapi.product.enums;

import lombok.Getter;

@Getter
public enum OrderBy {
    CREATED_AT("createAt", "created_at", "DESC"),
    VIEW_COUNT("viewCount","view_count", "DESC"),
    PURCHASE_COUNT("purchaseCount","purchase_count", "DESC"),
    PRICE_DESC("priceDesc","price","DESC"),
    PRICE_ASC("priceAsc","price","ASC");

    private final String value;
    private final String columnName;
    private final String direction;

    OrderBy(String value, String columnName, String direction) {
        this.value = value;
        this.columnName = columnName;
        this.direction = direction;
    }
}
