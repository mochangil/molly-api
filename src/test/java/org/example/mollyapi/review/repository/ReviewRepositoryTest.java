package org.example.mollyapi.review.repository;

import org.example.mollyapi.cart.entity.Cart;
import org.example.mollyapi.cart.repository.CartRepository;
import org.example.mollyapi.order.entity.Order;
import org.example.mollyapi.order.entity.OrderDetail;
import org.example.mollyapi.order.repository.OrderDetailRepository;
import org.example.mollyapi.order.repository.OrderRepository;
import org.example.mollyapi.product.dto.UploadFile;
import org.example.mollyapi.product.entity.Product;
import org.example.mollyapi.product.entity.ProductImage;
import org.example.mollyapi.product.entity.ProductItem;
import org.example.mollyapi.product.enums.ProductImageType;
import org.example.mollyapi.product.repository.ProductImageRepository;
import org.example.mollyapi.product.repository.ProductItemRepository;
import org.example.mollyapi.product.repository.ProductRepository;
import org.example.mollyapi.review.entity.Review;
import org.example.mollyapi.user.entity.User;
import org.example.mollyapi.user.repository.UserRepository;
import org.example.mollyapi.user.type.Sex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.example.mollyapi.order.type.CancelStatus.NONE;
import static org.example.mollyapi.order.type.OrderStatus.SUCCEEDED;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
public class ReviewRepositoryTest {
    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private ProductItemRepository productItemRepository;

    @Autowired
    private CartRepository cartRepository;

    @DisplayName("작성 권한이 있는 리뷰를 조회한다.")
    @Test
    void findReviewWhenUserHasPermission() {
        // given
        boolean isDeleted = false;
        Long orderDetailId = 1L;
        Long userId = 1L;

        // when
        Review review = reviewRepository.findByIsDeletedAndOrderDetailIdAndUserUserId(isDeleted, orderDetailId, userId);

        // then
        assertThat(review).isNull();
    }

    @DisplayName("작성 권한이 없는 리뷰를 조회한다.")
    @Test
    void findReviewWhenUserHasNotPermission() {
        // given
        User testUser = createAndSaveUser();
        Order testOrder = createAndSaveOrder(testUser);
        Product testProduct = createAndSaveProduct();
        ProductImage testImage = createAndSaveProductImage(testProduct);
        ProductItem testItem = createAndSaveProductItem("S", testProduct);
        Cart testCart = createAndSaveCart(3L, testUser, testItem);
        OrderDetail testOrderDetail = createAndSaveOrderDetail(testOrder, testItem, testCart.getQuantity(), testCart.getCartId());
        Review testReview = createAndSaveReview(testUser, testOrderDetail, testProduct, "test 1");
        boolean isDeleted = false;

        // when
        Review review = reviewRepository.findByIsDeletedAndOrderDetailIdAndUserUserId(isDeleted, testOrderDetail.getId(), testUser.getUserId());

        // then
        assertThat(review).isNotNull();
        assertThat(review.getIsDeleted()).isFalse();
        assertThat(review.getUser()).isEqualTo(testReview.getUser());
        assertThat(review.getOrderDetail()).isEqualTo(testReview.getOrderDetail());
        assertThat(review.getProduct()).isEqualTo(testReview.getProduct());
    }

    @DisplayName("수정/변경할 수 있는 리뷰가 존재하는 지 조회한다.")
    @Test
    void findReviewEditableWhenUserHasPermission() {
        // given
        User testUser = createAndSaveUser();
        Order testOrder = createAndSaveOrder(testUser);
        Product testProduct = createAndSaveProduct();
        ProductImage testImage = createAndSaveProductImage(testProduct);
        ProductItem testItem = createAndSaveProductItem("S", testProduct);
        Cart testCart = createAndSaveCart(3L, testUser, testItem);
        OrderDetail testOrderDetail = createAndSaveOrderDetail(testOrder, testItem, testCart.getQuantity(), testCart.getCartId());
        Review testReview = createAndSaveReview(testUser, testOrderDetail, testProduct, "test 1");
        boolean isDeleted = false;

        // when
        Optional<Review> review = reviewRepository.findByIdAndUserUserIdAndIsDeleted(testReview.getId(), testUser.getUserId(), isDeleted);

        // then
        assertThat(review).isPresent();
        assertThat(review.get().getIsDeleted()).isFalse();
        assertThat(review.get().getUser()).isEqualTo(testReview.getUser());
        assertThat(review.get().getOrderDetail()).isEqualTo(testReview.getOrderDetail());
        assertThat(review.get().getProduct()).isEqualTo(testReview.getProduct());
    }

    private User createAndSaveUser() {
        return userRepository.save(User.builder()
                .sex(Sex.FEMALE)
                .nickname("망고")
                .cellPhone("01011112222")
                .birth(LocalDate.of(2000, 1, 2))
                .profileImage("default.jpg")
                .name("김망고")
                .build());
    }

    private Order createAndSaveOrder(User user) {
        return orderRepository.save(Order.builder()
                .tossOrderId("ORD-20250309021413-5931")
                .user(user)
                .totalAmount(10000L)
                .status(SUCCEEDED)
                .cancelStatus(NONE)
                .orderedAt(LocalDateTime.of(2025, 3, 9, 11,10))
                .expirationTime(LocalDateTime.of(2025, 3, 9, 11, 10))
                .build());
    }

    private OrderDetail createAndSaveOrderDetail(Order order, ProductItem item, Long quantity, Long cartId) {
        return orderDetailRepository.save(OrderDetail.builder()
                .order(order)
                .productItem(item)
                .size(item.getSize())
                .price(item.getProduct().getPrice())
                .quantity(quantity)
                .brandName(item.getProduct().getBrandName())
                .productName(item.getProduct().getProductName())
                .cartId(cartId)
                .build());
    }

    private Product createAndSaveProduct() {
        return productRepository.save(Product.builder()
                .productName("테스트 상품")
                .brandName("테스트 브랜드")
                .price(50000L)
                .build());
    }

    private ProductItem createAndSaveProductItem(String size, Product product) {
        return productItemRepository.save(ProductItem.builder()
                .color("WHITE")
                .colorCode("#FFFFFF")
                .size(size)
                .quantity(30L)
                .product(product)
                .build());
    }

    private ProductImage createAndSaveProductImage(Product product) {
        return productImageRepository.save(ProductImage.builder()
                .uploadFile(UploadFile.builder()
                        .storedFileName("/images/product/coolfit_bra_volumefit_1.jpg")
                        .uploadFileName("coolfit_bra_volumefit_1.jpg")
                        .build())
                        .type(ProductImageType.THUMBNAIL)
                .imageIndex(0L)
                .product(product)
                .build());
    }

    private Cart createAndSaveCart(Long quantity, User user, ProductItem productItem) {
        return cartRepository.save(Cart.builder()
                .quantity(quantity)
                .user(user)
                .productItem(productItem)
                .build());
    }

    private Review createAndSaveReview(User user, OrderDetail orderDetail, Product product, String content) {
        return reviewRepository.save(Review.builder()
                .content(content)
                .isDeleted(false)
                .count(0L)
                .user(user)
                .orderDetail(orderDetail)
                .product(product)
                .build());
    }
}