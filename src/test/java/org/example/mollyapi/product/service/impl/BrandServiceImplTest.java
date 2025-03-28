package org.example.mollyapi.product.service.impl;

import org.example.mollyapi.product.dto.BrandSummaryDto;
import org.example.mollyapi.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static reactor.core.publisher.Mono.when;

@SpringBootTest
class BrandServiceImplTest {

    @Autowired
    private BrandServiceImpl brandService;

    @MockBean
    private ProductRepository productRepository;

    @DisplayName("포함된 상품의 조회수 합을 기준으로 인기브랜드를 반환한다")
    @Test
    public void getPopularBrand() throws Exception {
        //given
        List<BrandSummaryDto> content = List.of(
                new BrandSummaryDto("url", "brandName", 100L, 1201L)
        );

        doReturn(new SliceImpl<>(content, Pageable.unpaged(), false))
                .when(productRepository).getTotalViewGroupByBrandName(any(Pageable.class));
        PageRequest pageRequest = PageRequest.of(0, 10);

        //when
        List<BrandSummaryDto> result = brandService.getPopularBrand(pageRequest).getContent();
        //then
        assertThat(result).hasSize(1)
                .extracting("brandThumbnailUrl", "brandName", "totalProductCount", "viewCount")
                .containsExactly(
                        tuple("url", "brandName", 100L, 1201L)
                );
       }

    @DisplayName("인기 브랜드 조회시 pageable이 null이면 기본값으로 설정된다")
    @Test
    public void getPopularBrand_pageableNull() throws Exception {
        //given
        List<BrandSummaryDto> content = List.of(
                new BrandSummaryDto("url", "brandName", 100L, 1201L)
        );

        // 기대되는 호출
        doReturn(new SliceImpl<>(content, Pageable.unpaged(), false))
                .when(productRepository).getTotalViewGroupByBrandName(PageRequest.of(0, 10));

        // 잘 못된 호출
        doReturn(new SliceImpl<>(List.of(), Pageable.unpaged(), false))
                .when(productRepository).getTotalViewGroupByBrandName(null);

        //when
        List<BrandSummaryDto> result = brandService.getPopularBrand(null).getContent();

        //then
        assertThat(result).hasSize(1)
                .extracting("brandThumbnailUrl", "brandName", "totalProductCount", "viewCount")
                .containsExactly(
                        tuple("url", "brandName", 100L, 1201L)
                );
    }



}