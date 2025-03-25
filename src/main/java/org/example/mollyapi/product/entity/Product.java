package org.example.mollyapi.product.entity;

import com.github.f4b6a3.tsid.TsidCreator;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.example.mollyapi.common.entity.Base;
import org.example.mollyapi.common.exception.CustomException;
import org.example.mollyapi.common.exception.error.impl.ProductError;
import org.example.mollyapi.product.dto.ProductAndThumbnailDto;
import org.example.mollyapi.product.dto.UploadFile;
import org.example.mollyapi.user.entity.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.example.mollyapi.common.exception.error.impl.ProductError.*;

@Slf4j
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SqlResultSetMapping(
        name = "ProductAndThumbnailDtoMapping",
        classes = @ConstructorResult(
                targetClass = ProductAndThumbnailDto.class,
                columns = {
                        @ColumnResult(name = "product_id", type = Long.class),
                        @ColumnResult(name = "category_id", type = Long.class),
                        @ColumnResult(name = "brand_name", type = String.class),
                        @ColumnResult(name = "product_name", type = String.class),
                        @ColumnResult(name = "price", type = Long.class),
                        @ColumnResult(name = "thumbnail_url", type = String.class),
                        @ColumnResult(name = "thumbnail_filename", type = String.class),
                        @ColumnResult(name = "user_id", type = Long.class),
                        @ColumnResult(name = "view_count", type = Long.class),
                        @ColumnResult(name = "purchase_count", type = Long.class),
                        @ColumnResult(name = "created_at", type = LocalDateTime.class)
                }
        )
)
public class Product extends Base {

    @Id
    @Column(name = "product_id")
    Long id;

    @ManyToOne
    @JoinColumn(name = "category_id")
    Category category;
    String brandName;
    String productName;
    Long price;
    @Column(length = 500)
    String description;
    Long viewCount = 0L;

    @Column(nullable = false)
    Long purchaseCount = 0L;

    @Setter
    String thumbnailUrl;
    @Setter
    String thumbnailFilename;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    List<ProductItem> items = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "user_id")
    User user;

    @Builder
    public Product(
            Category category,
            String brandName,
            String productName,
            Long price,
            String description,
            User user,
            String thumbnailUrl,
            String thumbnailFilename
    ) {
        this.id = TsidCreator.getTsid().toLong();
        this.category = category;
        this.brandName = brandName;
        this.productName = productName;
        this.price = price;
        this.description = description;
        this.user = user;
        this.viewCount = 0L;
        this.purchaseCount = 0L;
        this.thumbnailUrl = thumbnailUrl;
        this.thumbnailFilename = thumbnailFilename;
    }

    public void increaseViewCount() {
        this.viewCount++;
        for (ProductItem item : items) {
            item.viewCount++;
        }
    }

    public void increasePurchaseCount() {
        this.purchaseCount = Optional.ofNullable(this.purchaseCount).orElse(0L) + 1;
    }

    public void decreasePurchaseCount() {
        this.purchaseCount = Math.max(0, Optional.ofNullable(this.purchaseCount).orElse(0L) - 1);
    }

    public void addImage(ProductImage productImage) {
        images.add(productImage);
    }

    public void addItem(ProductItem productItem) {
        items.add(productItem);
    }

    public UploadFile getThumbnail() {
        ProductImage productImage = images.stream()
                .filter(img -> img.isRepresentative)
                .findAny()
                .orElseThrow(() -> new IllegalStateException("No product image found"));

        return UploadFile.builder()
                .storedFileName(productImage.url)
                .uploadFileName(productImage.filename)
                .build();
    }

    public List<UploadFile> getProductImages() {
        return images.stream()
                .filter(img -> img.isProductImage)
                .map(img -> UploadFile.builder()
                        .storedFileName(img.url)
                        .uploadFileName(img.filename)
                        .build())
                .toList();
    }

    public List<UploadFile> getDescriptionImages() {
        return images.stream()
                .filter(img -> img.isDescriptionImage)
                .map(img -> UploadFile.builder()
                        .storedFileName(img.url)
                        .uploadFileName(img.filename)
                        .build())
                .toList();
    }

    public Product update(
            Category category,
            String brandName,
            String productName,
            Long price,
            String description
    ) {
        updateCategory(category);
        updateBrandName(brandName);
        updateProductName(productName);
        updatePrice(price);
        updateDescription(description);
        return this;
    }

    public void updateCategory(Category category) {
        if (category == null) {
            log.debug("카테고리를 null로 업데이트하려는 시도는 무시됩니다.");
            return;
        }
        this.category = category;
    }

    public void updateBrandName(String brandName) {
        if (brandName == null) {
            log.debug("브랜드명을 null로 업데이트하려는 시도는 무시됩니다.");
            return;
        }
        this.brandName = brandName;
    }

    public void updateProductName(String productName) {
        if (productName == null) {
            log.debug("상품명을 null로 업데이트 하려는 시도는 무시됩니다.");
            return;
        }
        this.productName = productName;
    }

    public void updateDescription(String description) {
        if (description == null) {
            log.debug("상품 설명을 null로 업데이트 하려는 시도는 무시됩니다.");
            return;
        }
        this.description = description;
    }

    public void updatePrice(Long price) throws CustomException {
        if (price == null) {
            log.debug("가격을 null로 업데이트 하려는 시도는 무시됩니다.");
            return;
        }

        if (price < 0) {
            throw new CustomException(NEGATIVE_PRICE);
        }

        this.price = price;
        for (ProductItem productItem : items) {
            productItem.price = price;
        }
    }

    public void setPurchaseCount(long purchaseCount) {
        this.purchaseCount = purchaseCount;
    }
}