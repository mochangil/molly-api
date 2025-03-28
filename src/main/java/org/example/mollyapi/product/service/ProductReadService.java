package org.example.mollyapi.product.service;

import lombok.RequiredArgsConstructor;
import org.example.mollyapi.product.dto.BrandSummaryDto;
import org.example.mollyapi.product.dto.ProductFilterCondition;
import org.example.mollyapi.product.dto.response.ProductResDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.Optional;

public interface ProductReadService {
    Slice<ProductResDto> getAllProducts(ProductFilterCondition condition, Pageable pageable, Long offsetId);

    Optional<ProductResDto> getProductById(Long id);
}
