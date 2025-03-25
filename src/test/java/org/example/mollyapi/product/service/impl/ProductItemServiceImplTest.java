package org.example.mollyapi.product.service.impl;

import jakarta.transaction.Transactional;
import org.example.mollyapi.common.exception.CustomException;
import org.example.mollyapi.common.exception.error.impl.ProductItemError;
import org.example.mollyapi.product.dto.ProductItemDto;
import org.example.mollyapi.product.dto.ProductItemReqDto;
import org.example.mollyapi.product.entity.Product;
import org.example.mollyapi.product.repository.CategoryRepository;
import org.example.mollyapi.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.example.mollyapi.common.exception.error.impl.ProductItemError.NEGATIVE_STOCK;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ProductItemServiceImplTest {

    @Autowired
    private ProductItemServiceImpl productItemService;

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    @DisplayName("상품 아이템을 생성하고 상품에 추가한다")
    @Test
    @Transactional
    void createProductItem() {
        // given
        Product product = createMockProduct();
        List<ProductItemReqDto> request = List.of(
                createMockItemDto("RED", "#111111", "S", 10L),
                createMockItemDto("BLUE", "#222222", "M", 11L),
                createMockItemDto("GREEN", "#333333", "XXL", 12L)
        );

        // when
        List<ProductItemDto> result = productItemService.createProductItem(product, request);

        // then
        assertThat(result).hasSize(3)
                .extracting("color", "colorCode", "size", "quantity", "product")
                .containsExactly(
                        tuple("RED", "#111111", "S", 10L, product),
                        tuple("BLUE", "#222222", "M", 11L, product),
                        tuple("GREEN", "#333333", "XXL", 12L, product)
                );
        assertThat(product.getItems()).hasSize(3)
                .extracting("color", "colorCode", "size", "quantity", "product")
                .containsExactly(
                        tuple("RED", "#111111", "S", 10L, product),
                        tuple("BLUE", "#222222", "M", 11L, product),
                        tuple("GREEN", "#333333", "XXL", 12L, product)
                );
    }

    @DisplayName("상품 아이템 목록이 null이면 예외를 던진다")
    @Test
    void createProductItem_nullProductItem() {
        // given
        List<ProductItemReqDto> request = List.of(
                createMockItemDto("RED", "#111111", "S", 10L),
                createMockItemDto("BLUE", "#222222", "M", 11L),
                createMockItemDto("GREEN", "#333333", "XXL", 12L)
        );

        // when, then
        assertThrows(IllegalArgumentException.class,
                () -> productItemService.createProductItem(null, request));
    }

    @DisplayName("상품이 null이면 예외를 던진다")
    @Test
    void createProductItem_nullProduct() {
        // given
        Product product = createMockProduct();

        // when, then
        assertThrows(IllegalArgumentException.class,
                () -> productItemService.createProductItem(product, null));
    }

    @DisplayName("상품의 재고를 수정할 수 있다")
    @Test
    @Transactional
    void updateProductItem() {
        // given
        Product product = createMockProduct();
        List<ProductItemReqDto> request = List.of(
                createMockItemDto("BLUE", "#222222", "M", 11L)
        );
        List<ProductItemDto> item = productItemService.createProductItem(product, request);
        List<ProductItemReqDto> updateDto = item.stream().map((r) -> new ProductItemReqDto(
                r.color(), r.colorCode(), r.size(), r.quantity() - 10L
        )).toList();

        // when
        ProductItemDto result = productItemService.updateProductItem(item.get(0).id(), updateDto.get(0));

        // then
        assertThat(result)
                .extracting("color", "colorCode", "size", "quantity", "product")
                .containsExactly("BLUE", "#222222", "M", 1L, product);
    }

    @DisplayName("상품의 재고를 0 미만으로 수정하면 예외를 던진다")
    @Test
    @Transactional
    void updateProductItem_negativeQuantity() {
        // given
        Product product = createMockProduct();
        List<ProductItemReqDto> request = List.of(
                createMockItemDto("BLUE", "#222222", "M", 11L)
        );
        List<ProductItemDto> item = productItemService.createProductItem(product, request);
        List<ProductItemReqDto> updateDto = item.stream().map((r) -> new ProductItemReqDto(
                r.color(), r.colorCode(), r.size(), -1L
        )).toList();

        // when, then
        CustomException exception = assertThrows(CustomException.class,
                () -> productItemService.updateProductItem(item.get(0).id(), updateDto.get(0)));
        assertThat(exception.getMessage()).isEqualTo(NEGATIVE_STOCK.getMessage());
    }

    @DisplayName("상품 아이템 아이디로 재고를 조회한다")
    @Test
    public void getItemQuantity() throws Exception {
        //given
        Product product = createMockProduct();
        List<ProductItemReqDto> request = List.of(
                createMockItemDto("BLUE", "#222222", "M", 11L)
        );
        List<ProductItemDto> item = productItemService.createProductItem(product, request);

        //when
        Long itemQuantity = productItemService.getItemQuantity(item.get(0).id());

        //then
        assertThat(itemQuantity).isEqualTo(11L);
       }


    private Product createMockProduct() {
        Product product = Product.builder().build();
        return productRepository.save(product);

    }

    private ProductItemReqDto createMockItemDto(String color,
                                                String colorCode,
                                                String size,
                                                Long quantity) {
        return new ProductItemReqDto(
                color,
                colorCode,
                size,
                quantity
        );
    }
}