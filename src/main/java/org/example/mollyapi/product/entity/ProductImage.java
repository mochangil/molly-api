package org.example.mollyapi.product.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.mollyapi.common.entity.Base;
import org.example.mollyapi.common.enums.ImageType;
import org.example.mollyapi.product.dto.UploadFile;
import org.example.mollyapi.product.enums.ProductImageType;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductImage extends Base {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    Long id;
    String url;
    String filename;
    Boolean isProductImage;
    Boolean isRepresentative;
    Boolean isDescriptionImage;
    Long imageIndex;

    @Setter
    @ManyToOne
    @JoinColumn(name = "product_id")
    Product product;

    @Builder
    ProductImage(UploadFile uploadFile,
                 ProductImageType type,
                 Long imageIndex,
                 Product product
    ) {
        this.url = uploadFile.getStoredFileName();
        this.filename = uploadFile.getUploadFileName();
        this.isProductImage = type.equals(ProductImageType.PRODUCT);
        this.isRepresentative = type.equals(ProductImageType.THUMBNAIL);
        this.isDescriptionImage = type.equals(ProductImageType.DESCRIPTION);
        this.imageIndex = imageIndex;
        this.product = product;
    }

    public static ProductImage create(Product product, UploadFile uploadFile, ProductImageType type) {
        return ProductImage.builder()
                .uploadFile(uploadFile)
                .type(type)
                .imageIndex(0L)
                .product(product).build();
    }

    public static List<ProductImage> create(Product product, List<UploadFile> uploadFile, ProductImageType type) {
        List<ProductImage> productImages = new ArrayList<>();

        for (int i = 0; i < uploadFile.size(); i++) {
            ProductImage productImage = ProductImage.builder()
                    .uploadFile(uploadFile.get(i))
                    .type(type)
                    .imageIndex((long) i)
                    .product(product).build();
            productImages.add(productImage);
        }
        return productImages;
    }
}
