package org.example.mollyapi.product.dto;

import com.querydsl.core.annotations.QueryProjection;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.SqlResultSetMapping;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ProductAndThumbnailDto {
    Long id;
    Long categoryId;
    String brandName;
    String productName;
    Long price;
    String url;
    String filename;
    LocalDateTime createdAt;
    Long viewCount;
    Long purchaseCount;
    Long sellerId;

    @QueryProjection
    public ProductAndThumbnailDto(
            Long id,
            Long categoryId,
            String brandName,
            String productName,
            Long price,
            String url,
            String filename,
            Long sellerId,
            Long viewCount,
            Long purchaseCount,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.categoryId = categoryId;
        this.brandName = brandName;
        this.productName = productName;
        this.price = price;
        this.createdAt = createdAt;
        this.viewCount = viewCount;
        this.purchaseCount = purchaseCount;
        this.url = url;
        this.filename = filename;
        this.sellerId = sellerId;
    }
}
