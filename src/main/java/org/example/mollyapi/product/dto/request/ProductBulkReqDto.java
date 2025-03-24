package org.example.mollyapi.product.dto.request;

import com.github.f4b6a3.tsid.TsidCreator;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.mollyapi.product.entity.Category;
import org.example.mollyapi.product.entity.Product;
import org.example.mollyapi.user.entity.User;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductBulkReqDto {

    private Long id;

    private String productName;

    private String description;

    private Long price;

    private String brandName;

    private Long categoryId;

    @Builder
    public ProductBulkReqDto(String productName, String description, Long price, String brandName, long categoryId) {
        this.id = TsidCreator.getTsid().toLong();
        this.productName = productName;
        this.description = description;
        this.price = price;
        this.brandName = brandName;
        this.categoryId = categoryId;
    }

    public Product toProduct(User user, Category category) {
        return Product.builder()
                .id(this.id)
                .productName(this.productName)
                .description(this.description)
                .brandName(this.brandName)
                .category(category)
                .user(user)
                .price(this.price)
                .build();
    }
}
