package org.example.mollyapi.cart.repository.impl;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.mollyapi.cart.dto.Response.CartInfoDto;
import org.example.mollyapi.cart.repository.CartCustomRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.example.mollyapi.cart.entity.QCart.cart;
import static org.example.mollyapi.product.entity.QProduct.product;
import static org.example.mollyapi.product.entity.QProductImage.productImage;
import static org.example.mollyapi.product.entity.QProductItem.productItem;

@RequiredArgsConstructor
public class CartCustomRepositoryImpl implements CartCustomRepository {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<CartInfoDto> getCartInfo(Long userId) {
        return jpaQueryFactory.select(
                        Projections.constructor(CartInfoDto.class,
                                cart.cartId,
                                productItem.id,
                                productItem.color,
                                productItem.size,
                                product.id,
                                product.productName,
                                product.brandName,
                                product.price,
                                productImage.url,
                                cart.quantity
                        )).from(cart)
                .innerJoin(productItem).on(cart.productItem.eq(productItem))
                .innerJoin(product).on(productItem.product.eq(product))
                .innerJoin(productImage).on(product.id.eq(productImage.product.id)
                    .and(productImage.isRepresentative.eq(Boolean.TRUE)))
                .where(cart.user.userId.eq(userId))
                .orderBy(cart.createdAt.desc())
                .fetch();
    }

    @Override
    public boolean countByUserUserId(Long userId) {
        return Boolean.TRUE.equals(jpaQueryFactory.select(
                        new CaseBuilder()
                                .when(cart.count().gt(30L))
                                .then(Boolean.TRUE)
                                .otherwise(Boolean.FALSE)
                ).from(cart)
                .where(cart.user.userId.eq(userId))
                .fetchOne());
    }

    @Override
    public List<Long> getExpiredCartId() {
        return jpaQueryFactory.select(
                    cart.cartId
                ).from(cart)
                .where(cart.createdAt.lt(LocalDateTime.now().minusDays(365)))
                .fetch();
    }
}
