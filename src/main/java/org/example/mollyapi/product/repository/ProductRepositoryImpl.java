package org.example.mollyapi.product.repository;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.example.mollyapi.product.dto.*;
import org.example.mollyapi.product.enums.OrderBy;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.List;

import static org.example.mollyapi.product.entity.QProduct.product;
import static org.example.mollyapi.product.entity.QProductImage.productImage;
import static org.springframework.util.StringUtils.hasText;

public class ProductRepositoryImpl implements ProductRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final EntityManager entityManager;

    public ProductRepositoryImpl(JPAQueryFactory queryFactory, EntityManager entityManager) {
        this.queryFactory = queryFactory;
        this.entityManager = entityManager;
    }

    @Override
    public Slice<BrandSummaryDto> getTotalViewGroupByBrandName(Pageable pageable) {
        if (pageable == null) { pageable  = Pageable.unpaged(); }

        JPAQuery<BrandSummaryDto> query = queryFactory.select(
                new QBrandSummaryDto(
                    productImage.url.max().as("brandThumbnail"),
                    product.brandName,
                    product.count(),
                    product.viewCount.sum().as("viewCount")))
            .from(productImage)
            .join(productImage.product, product)
            .on(productImage.isRepresentative.isTrue())
            .groupBy(product.brandName)
            .orderBy(product.viewCount.sum().desc());

        if (pageable.isPaged()) {
            query.offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1);
        }
        List<BrandSummaryDto> content = query.fetch();

        boolean hasNext = false;
        if (pageable.isPaged() && content.size() > pageable.getPageSize()) {
            content.remove(pageable.getPageSize());
            hasNext = true;
        }
        return new SliceImpl<>(content, pageable, hasNext);
    }

    @Override
    public Slice<ProductAndThumbnailDto> findByCondition(ProductFilterCondition condition, Pageable pageable) {
        if (pageable == null) pageable = Pageable.unpaged();

        ProductReadQuery productReadQuery = new ProductReadQuery();

        // 서브쿼리 베이스 생성
        ProductItemSubQuery subQuery = new ProductItemSubQuery(
                condition == null || condition.orderBy() == null ? OrderBy.CREATED_AT : condition.orderBy(),
                pageable.isPaged() ? pageable.getOffset() : null,
                pageable.isPaged() ? (long)pageable.getPageSize() : null);

        // 서브쿼리 조건 추가 및 toString
        String subQueryString = subQuery
                .appendConditions()
                .appendCategoryId(condition != null ? condition.categoryId() : null)
                .appendBrandName(condition != null ? condition.brandName() : null)
                .appendColorCode(condition != null ? condition.colorCode() : null)
                .appendSize(condition != null ? condition.size() : null)
                .appendPriceGoe(condition != null  ? condition.priceGoe(): null)
                .appendPriceLt(condition != null ? condition.priceLt() : null)
                .appendExcludeSoldOut(condition != null ? condition.excludeSoldOut() : null)
                .appendOrderAndLimit()
                .build();

        // 쿼리 조건 추가 및 toString
        String nativeSql = productReadQuery
                .appendJoin(subQueryString)
                .appendOrder(
                        condition == null || condition.orderBy() == null ? OrderBy.CREATED_AT : condition.orderBy()
                )
                .build();

        Query query = entityManager.createNativeQuery(nativeSql, "ProductAndThumbnailDtoMapping");

        List<ProductAndThumbnailDto> content = query.getResultList();

        boolean hasNext = false;
        if (pageable.isPaged() && content.size() > pageable.getPageSize()) {
            content.remove(pageable.getPageSize());
            hasNext = true;
        }
        return new SliceImpl<>(content, pageable, hasNext);
    }

    @Override
    public Slice<ProductAndThumbnailDto> findByCondition(ProductFilterCondition condition, Pageable pageable, Long offset) {
        if (pageable == null) pageable = Pageable.unpaged();

        ProductReadQuery productReadQuery = new ProductReadQuery();

        // 서브쿼리 베이스 생성
        ProductItemSubQuery subQuery = new ProductItemSubQuery(
                condition == null || condition.orderBy() == null ? OrderBy.CREATED_AT : condition.orderBy(),
                offset,
                pageable.isPaged() ? (long)pageable.getPageSize() : null);

        // 서브쿼리 조건 추가 및 toString
        String subQueryString = subQuery
                .appendConditions()
                .appendCategoryId(condition != null ? condition.categoryId() : null)
                .appendPriceGoe(condition != null  ? condition.priceGoe(): null)
                .appendPriceLt(condition != null ? condition.priceLt() : null)
                .appendBrandName(condition != null ? condition.brandName() : null)
                .appendColorCode(condition != null ? condition.colorCode() : null)
                .appendSize(condition != null ? condition.size() : null)
                .appendExcludeSoldOut(condition != null ? condition.excludeSoldOut() : null)
                .appendOrderAndLimit()
                .build();

        // 쿼리 조건 추가 및 toString
        String nativeSql = productReadQuery
                .appendJoin(subQueryString)
                .appendOrder(
                        condition == null || condition.orderBy() == null ? OrderBy.CREATED_AT : condition.orderBy()
                )
                .build();

        Query query = entityManager.createNativeQuery(nativeSql, "ProductAndThumbnailDtoMapping");

        List<ProductAndThumbnailDto> content = query.getResultList();

        boolean hasNext = false;
        if (pageable.isPaged() && content.size() > pageable.getPageSize()) {
            content.remove(pageable.getPageSize());
            hasNext = true;
        }
        return new SliceImpl<>(content, pageable, hasNext);
    }

}