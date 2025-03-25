package org.example.mollyapi.product.repository;

import org.example.mollyapi.product.dto.BrandSummaryDto;
import org.example.mollyapi.product.dto.ProductAndThumbnailDto;
import org.example.mollyapi.product.dto.ProductFilterCondition;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;


public interface ProductRepositoryCustom {
    Slice<BrandSummaryDto> getTotalViewGroupByBrandName(Pageable pageable);
    Slice<ProductAndThumbnailDto> findByCondition(ProductFilterCondition condition, Pageable pageable);
    Slice<ProductAndThumbnailDto> findByCondition(ProductFilterCondition condition, Pageable pageable, Long offset);
}
