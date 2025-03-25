package org.example.mollyapi.product.entity;

import com.github.f4b6a3.tsid.TsidCreator;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.example.mollyapi.common.entity.Base;
import org.example.mollyapi.common.exception.CustomException;
import org.example.mollyapi.common.exception.error.impl.ProductItemError;
import org.example.mollyapi.order.entity.OrderDetail;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.List;

import static org.example.mollyapi.common.exception.error.impl.ProductItemError.*;

@Slf4j
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductItem {

        @Id
        @Column(name = "item_id")
        Long id;

        String color;
        String colorCode;
        String size;
        Long quantity;

        @Setter
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "product_id")
        Product product;

        Long price;
        Long viewCount;
        Long purchaseCount;
        @Column(updatable = false)
        private LocalDateTime createdAt;

        @LastModifiedDate
        private LocalDateTime updatedAt;

//        @Version
//        @Column(nullable = false) // NOT NULL 설정
//        private Integer version = 0;  // 기본값 0 설정

        @Builder
        public ProductItem(
                String color,
                String colorCode,
                String size,
                Long quantity,
                Product product) {
                this.id = TsidCreator.getTsid().toLong();
                this.color = color;
                this.colorCode = colorCode;
                this.size = size;
                this.quantity = quantity;
                this.product = product;
                this.price = product.getPrice();
                this.viewCount = product.getViewCount();
                this.purchaseCount = product.getPurchaseCount();
                this.createdAt = product.getCreatedAt();
        }

        public void updateQuantity(Long quantity) {
                if (quantity < 0) {
                        throw new CustomException(NEGATIVE_STOCK);
                }
                this.quantity = quantity;
        }



        public void decreaseStock(Long quantityToDecrease) {
                if (this.quantity < quantityToDecrease) {
                        throw new IllegalArgumentException("재고 부족: 현재 수량=" + this.quantity + ", 요청 수량=" + quantityToDecrease);
                }
                this.quantity -= quantityToDecrease;
                this.product.increasePurchaseCount();
                this.purchaseCount = this.product.getPurchaseCount();
        }

        public void restoreStock(Long quantityToRestore) {
                log.info("재고 복구 시작: 상품 ID={}, 현재 재고={}, 복구 수량={}", this.id, this.quantity, quantityToRestore);

                if (this.product == null) {
                        throw new IllegalStateException("재고 복구 실패: Product가 null입니다. itemId=" + this.id);
                }

                if (this.product.getPurchaseCount() == null) {
                        log.warn("purchaseCount가 null이므로 0으로 초기화합니다. productId={}", this.product.getId());
                        this.product.setPurchaseCount(0L);
                }

                this.quantity += quantityToRestore;
                this.product.decreasePurchaseCount();
                this.purchaseCount = this.product.getPurchaseCount();

                log.info("재고 복구 완료: 상품 ID={}, 최종 재고={}", this.id, this.quantity);
        }

//        /**
//         * 재고 차감 (낙관적 락 적용)
//         */
//        public void decreaseStock(Long quantityToDecrease) {
//                if (this.quantity < quantityToDecrease) {
//                        throw new IllegalArgumentException("재고 부족: 현재 수량=" + this.quantity + ", 요청 수량=" + quantityToDecrease);
//                }
//
//                this.quantity -= quantityToDecrease;
//                this.product.increasePurchaseCount();
//
//                log.info("재고 차감 완료: 상품 ID={}, 남은 재고={}", this.id, this.quantity);
//        }
//
//        /**
//         * 재고 복구 (낙관적 락 적용)
//         */
//        public void restoreStock(Long quantityToRestore) {
//                log.info("재고 복구 시작: 상품 ID={}, 현재 재고={}, 복구 수량={}", this.id, this.quantity, quantityToRestore);
//
//                if (this.product == null) {
//                        throw new IllegalStateException("재고 복구 실패: Product가 null입니다. itemId=" + this.id);
//                }
//
//                if (this.product.getPurchaseCount() == null) {
//                        log.warn("purchaseCount가 null이므로 0으로 초기화합니다. productId={}", this.product.getId());
//                        this.product.setPurchaseCount(0L);
//                }
//
//                this.quantity += quantityToRestore;
//                this.product.decreasePurchaseCount();
//
//                log.info("재고 복구 완료: 상품 ID={}, 최종 재고={}", this.id, this.quantity);
//        }
}
