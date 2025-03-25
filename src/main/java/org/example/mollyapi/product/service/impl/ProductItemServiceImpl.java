package org.example.mollyapi.product.service.impl;

import jakarta.transaction.Transactional;
import org.example.mollyapi.common.exception.CustomException;
import org.example.mollyapi.product.dto.ProductItemDto;
import org.example.mollyapi.product.dto.ProductItemReqDto;
import org.example.mollyapi.product.entity.Product;
import org.example.mollyapi.product.entity.ProductItem;
import org.example.mollyapi.product.repository.ProductItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static org.example.mollyapi.common.exception.error.impl.ProductItemError.NOT_EXISTS_PRODUCT;

@Service
public class ProductItemServiceImpl {

    @Autowired
    private ProductItemRepository productItemRepository;

    @Transactional
    public List<ProductItemDto> createProductItem(Product product, List<ProductItemReqDto> itemDtoList) {
        if (product == null || itemDtoList == null) {
            throw new IllegalArgumentException("product, itemDtoList는 null일 수 없습니다");
        }
        List<ProductItem> items = new ArrayList<>();

        for (ProductItemReqDto dto : itemDtoList) {
            ProductItem item = ProductItem.builder()
                    .color(dto.color())
                    .colorCode(dto.colorCode())
                    .size(dto.size())
                    .quantity(dto.quantity())
                    .product(product)
                    .build();
            items.add(item);
            product.addItem(item);
        }

        List<ProductItem> saved = productItemRepository.saveAll(items);
        return saved.stream().map(ProductItemDto::of).toList();
    }

    @Transactional
    public ProductItemDto updateProductItem(Long id, ProductItemReqDto itemDto) {
        if (id == null || itemDto == null) {
            throw new IllegalArgumentException("id와 itemDto는 null일 수 없습니다");
        }

        ProductItem productItem = productItemRepository.findById(id)
                .orElseThrow(() -> new CustomException(NOT_EXISTS_PRODUCT));

        productItem.updateQuantity(itemDto.quantity());
        return ProductItemDto.of(productItem);
    }

    public Long getItemQuantity(Long id) {
        ProductItem item = productItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품아이템 아이디입니다"));

        if (item.getQuantity() == null) {
            throw new IllegalStateException("상품 재고가 null 입니다");
        }

        return item.getQuantity();
    }
}
