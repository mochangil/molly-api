package org.example.mollyapi.product.entity;

import com.github.f4b6a3.tsid.TsidCreator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.mollyapi.common.entity.Base;
import org.example.mollyapi.product.dto.UploadFile;
import org.example.mollyapi.user.entity.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    List<ProductItem> items = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "user_id")
    User user;

    @Builder
    public Product(
            Long id,
            Category category,
            String brandName,
            String productName,
            Long price,
            String description,
            User user
    ) {
        this.id = TsidCreator.getTsid().toLong();;
        this.category = category;
        this.brandName = brandName;
        this.productName = productName;
        this.price = price;
        this.description = description;
        this.user = user;
        this.viewCount = 0L;
        this.purchaseCount = 0L;
    }

    public void increaseViewCount() {
        this.viewCount++;
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
        this.category = category;
        this.brandName = brandName;
        this.productName = productName;
        this.price = price;
        this.description = description;
        return this;
    }

    public void setPurchaseCount(long purchaseCount) {
        this.purchaseCount = purchaseCount;
    }
}