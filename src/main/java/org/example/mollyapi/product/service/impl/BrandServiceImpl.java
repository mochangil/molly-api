package org.example.mollyapi.product.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.mollyapi.product.dto.BrandSummaryDto;
import org.example.mollyapi.product.repository.ProductRepository;
import org.example.mollyapi.product.service.BrandService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final ProductRepository productRepository;

    @Override
    public Slice<BrandSummaryDto> getPopularBrand(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            int defaultPageNumber = 0;
            int defaultPageSize = 10;
            pageable = PageRequest.of(defaultPageNumber, defaultPageSize);
        }
        return productRepository.getTotalViewGroupByBrandName(pageable);
    }
}
