package org.example.mollyapi.product.entity;

import org.example.mollyapi.common.exception.CustomException;
import org.example.mollyapi.common.exception.error.CustomError;
import org.example.mollyapi.common.exception.error.impl.ProductError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.mollyapi.common.exception.error.impl.ProductError.NEGATIVE_PRICE;
import static org.junit.jupiter.api.Assertions.*;

class ProductTest {

    @Test
    void increaseViewCount() {
        // given
        Product product = createProduct();
        Long beforeCount = product.getViewCount();

        // when
        product.increaseViewCount();

        // then
        assertThat(beforeCount).isEqualTo(0);
        assertThat(product.getViewCount()).isEqualTo(1);
    }

    @Test
    void increasePurchaseCount() {
        // given
        Product product = createProduct();
        Long beforeCount = product.getPurchaseCount();

        // when
        product.increasePurchaseCount();

        // then
        assertThat(beforeCount).isEqualTo(0);
        assertThat(product.getPurchaseCount()).isEqualTo(1);
    }

    @Test
    void decreasePurchaseCount() {
        // given
        Product product = createProduct();
        product.increasePurchaseCount();
        Long beforeCount = product.getPurchaseCount();

        // when
        product.decreasePurchaseCount();

        // then
        assertThat(beforeCount).isEqualTo(1);
        assertThat(product.getPurchaseCount()).isEqualTo(0);
    }

    @Test
    void addImage() {
        // given
        Product product = createProduct();
        ProductImage productImage = new ProductImage();

        // when
        product.addImage(productImage);
        List<ProductImage> images = product.getImages();

        // then
        assertThat(images).hasSize(1);
    }

    @Test
    void addItem() {
        // given
        Product product = createProduct();
        ProductItem productItem = new ProductItem();

        // when
        product.addItem(productItem);
        List<ProductItem> items = product.getItems();

        // then
        assertThat(items).hasSize(1);
    }

    @Test
    void setThumbnailUrl() {
        // given
        Product product = createProduct();
        String thumbnailUrl = "http://example.com";

        // when
        product.setThumbnailUrl(thumbnailUrl);

        // then
        assertThat(product.getThumbnailUrl()).isEqualTo(thumbnailUrl);
    }

    @Test
    void setThumbnailFilename() {
        // given
        Product product = createProduct();
        String thumbnailFilename = "http://example.com";

        // when
        product.setThumbnailUrl(thumbnailFilename);

        // then
        assertThat(product.getThumbnailUrl()).isEqualTo(thumbnailFilename);
    }

    @DisplayName("카테고리를 업데이트 한다")
    @Test
    void updateCategory() {
        // given
        Category category1 = Category.builder()
                .categoryName("category1")
                .build();
        Category category2 = Category.builder()
                .categoryName("category2")
                .build();
        Product product = createProduct(category1);
        Category before = product.getCategory();

        // when
        product.updateCategory(category2);

        // then
        assertThat(before).isEqualTo(category1);
        assertThat(product.getCategory()).isEqualTo(category2);
    }

    @DisplayName("카테고리를 null로 업데이트를 시도하면 무시한다")
    @Test
    void updateCategory_null() {
        // given
        Category category1 = Category.builder()
                .categoryName("category1")
                .build();
        Product product = createProduct(category1);
        Category before = product.getCategory();

        // when
        product.updateCategory(null);

        // then
        assertThat(product.getCategory()).isEqualTo(before);
    }


    @DisplayName("브랜드먕을 업데이트 한다")
    @Test
    void updateBrandName() {
        // given
        Product product = createProduct();
        String beforeBrandName = product.getBrandName();
        String afterBrandName = "afterBrandName";

        // when
        product.updateBrandName(afterBrandName);

        // then
        assertThat(beforeBrandName).isNotEqualTo(afterBrandName);
        assertThat(product.getBrandName()).isEqualTo(afterBrandName);
    }

    @DisplayName("브랜드명을 null로 업데이트 하려하면 무시한다")
    @Test
    void updateBrandName_null() {
        // given
        Product product = createProduct();
        String beforeBrandName = product.getBrandName();

        // when
        product.updateBrandName(null);

        // then
        assertThat(product.getBrandName()).isEqualTo(beforeBrandName);
    }

    @DisplayName("상품명을 업데이트 한다")
    @Test
    void updateProductName() {
        // given
        Product product = createProduct();
        String beforeProductName = product.getProductName();
        String afterProductName = "afterProductName";

        // when
        product.updateBrandName(afterProductName);

        // then
        assertThat(beforeProductName).isNotEqualTo(afterProductName);
        assertThat(product.getBrandName()).isEqualTo(afterProductName);
    }

    @DisplayName("상품명을 null로 업데이트 하려하면 무시한다")
    @Test
    void updateProductName_null() {
        // given
        Product product = createProduct();
        String beforeProductName = product.getProductName();

        // when
        product.updateProductName(null);

        // then
        assertThat(product.getProductName()).isEqualTo(beforeProductName);
    }

    @DisplayName("가격을 업데이트 한다")
    @Test
    void updatePrice() {
        // given
        Product product = createProduct();
        Long beforePrice = product.getPrice();

        // when
        product.updatePrice(1L);

        // then
        assertThat(beforePrice).isNotEqualTo(1L);
        assertThat(product.getPrice()).isEqualTo(1L);
    }

    @DisplayName("가격을 null로 업데이트 하려하면 무시한다")
    @Test
    void updatePrice_null() {
        // given
        Product product = createProduct();
        Long beforePrice = product.getPrice();

        // when
        product.updatePrice(null);

        // then
        assertThat(product.getPrice()).isEqualTo(beforePrice);
    }

    @DisplayName("가격을 음수로 업데이트 하려하면 예외를 던진다")
    @Test
    void updatePrice_negative() {
        // given
        Product product = createProduct();
        Long beforePrice = product.getPrice();

        // when, then
        CustomException exception;
        exception = assertThrows(CustomException.class, () -> product.updatePrice(-1L));
        assertThat(exception.getMessage()).isEqualTo(NEGATIVE_PRICE.getMessage());
    }

    @DisplayName("상품 설명을 업데이트 한다")
    @Test
    void updateDescription() {
        // given
        Product product = createProduct();
        String beforeDescription = product.getDescription();
        String afterDescription = "afterDescription";

        // when
        product.updateDescription(afterDescription);

        // then
        assertThat(beforeDescription).isNotEqualTo(afterDescription);
        assertThat(product.getDescription()).isEqualTo(afterDescription);
    }

    @DisplayName("상품 설명을 null로 업데이트하려 하면 무시한다")
    @Test
    void updateDescription_null() {
        // given
        Product product = createProduct();
        String beforeDescription = product.getDescription();

        // when
        product.updateDescription(null);

        // then
        assertThat(product.getDescription()).isEqualTo(beforeDescription);
    }


    private static Product createProduct() {
        return Product.builder()
                .brandName("defaultBrandName")
                .productName("defaultProductName")
                .price(1000L)
                .description("defaultDescription")
                .build();
    }

    private static Product createProduct(Category category) {
        return Product.builder()
                .category(category)
                .build();
    }

}