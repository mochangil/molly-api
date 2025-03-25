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
import org.example.mollyapi.review.dto.response.MyReviewInfoDto;
import org.example.mollyapi.review.dto.response.ReviewInfoDto;
import org.example.mollyapi.review.dto.response.TrendingReviewResDto;
import org.example.mollyapi.review.entity.Review;
import org.example.mollyapi.review.entity.ReviewImage;
import org.example.mollyapi.review.entity.ReviewLike;
import org.example.mollyapi.user.entity.User;
import org.example.mollyapi.user.repository.UserRepository;
import org.example.mollyapi.user.type.Sex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.example.mollyapi.order.type.CancelStatus.NONE;
import static org.example.mollyapi.order.type.OrderStatus.SUCCEEDED;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
public class ReviewCustomRepositoryTest {
    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReviewImageRepository reviewImageRepository;

    @Autowired
    private ReviewLikeRepository reviewLikeRepository;

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

    @DisplayName("특정 상품의 리뷰 리스트를 조회한다.")
    @Test
    void findReviewInfoByProduct() {
        // given
        User testUser = createAndSaveUser("망고", "김망고");
        User testUser2 = createAndSaveUser("사과", "이사과");
        Product testProduct = createAndSaveProduct();
        ProductImage testImage = createAndSaveProductImage(testProduct);
        ProductItem testItem = createAndSaveProductItem("S", testProduct);
        ProductItem testItem2 = createAndSaveProductItem("M", testProduct);
        Cart testCart = createAndSaveCart(3L, testUser, testItem);
        Cart testCart2 = createAndSaveCart(3L, testUser2, testItem2);
        Order testOrder = createAndSaveOrder(testUser);
        OrderDetail testOrderDetail = createAndSaveOrderDetail(testOrder, testItem, testCart.getQuantity(), testCart.getCartId());
        OrderDetail testOrderDetail2 = createAndSaveOrderDetail(testOrder, testItem2, testCart2.getQuantity(), testCart2.getCartId());
        Review testReview = createAndSaveReview(testUser, testOrderDetail, testProduct, "test 1");
        Review testReview2 = createAndSaveReview(testUser2, testOrderDetail2, testProduct, "test 2");

        PageRequest pageable = PageRequest.of(0, 5);

        // when
        List<ReviewInfoDto> reviewInfoList = reviewRepository.getReviewInfo(pageable, testProduct.getId(), testUser.getUserId());

        // then
        assertThat(reviewInfoList).hasSize(2);
        assertThat(reviewInfoList)
                .extracting(ReviewInfoDto::reviewId, ReviewInfoDto::content,ReviewInfoDto::nickname)
                .contains(
                        tuple(testReview.getId(), testReview.getContent(), testReview.getUser().getNickname()),
                        tuple(testReview2.getId(), testReview2.getContent(), testReview2.getUser().getNickname())
                );
    }

    @DisplayName("특정 리뷰의 이미지 리스트를 조회한다.")
    @Test
    void findImageListByReview() {
        // given
        User testUser = createAndSaveUser("망고", "김망고");
        Product testProduct = createAndSaveProduct();
        ProductImage testImage = createAndSaveProductImage(testProduct);
        ProductItem testItem = createAndSaveProductItem("S", testProduct);
        Cart testCart = createAndSaveCart(3L, testUser, testItem);
        Order testOrder = createAndSaveOrder(testUser);
        OrderDetail testOrderDetail = createAndSaveOrderDetail(testOrder, testItem, testCart.getQuantity(), testCart.getCartId());
        Review testReview = createAndSaveReview(testUser, testOrderDetail, testProduct, "test 1");
        ReviewImage testImage1 = createAndSaveReviewImage(testReview, 0L, UploadFile.builder()
                .storedFileName("/images/review/test_1.jpg")
                .uploadFileName("test_1.jpg")
                .build());
        ReviewImage testImage2 = createAndSaveReviewImage(testReview, 1L, UploadFile.builder()
                .storedFileName("/images/review/test_2.jpg")
                .uploadFileName("test_2.jpg")
                .build());

        // when
        List<String> imageList = reviewRepository.getImageList(testReview.getId());

        // then
        assertThat(imageList).hasSize(2);
        assertThat(imageList).containsExactly("/images/review/test_1.jpg", "/images/review/test_2.jpg");
    }

    @DisplayName("로그인한 사용자의 리뷰 리스트를 조회한다.")
    @Test
    void findMyReviewInfo() {
        // given
        User testUser = createAndSaveUser("망고", "김망고");
        Product testProduct = createAndSaveProduct();
        ProductImage testImage = createAndSaveProductImage(testProduct);
        ProductItem testItem = createAndSaveProductItem("S", testProduct);
        Cart testCart = createAndSaveCart(3L, testUser, testItem);
        Order testOrder = createAndSaveOrder(testUser);
        OrderDetail testOrderDetail = createAndSaveOrderDetail(testOrder, testItem, testCart.getQuantity(), testCart.getCartId());
        Review testReview = createAndSaveReview(testUser, testOrderDetail, testProduct, "test 1");

        PageRequest pageable = PageRequest.of(0, 5);

        // when
        List<MyReviewInfoDto> myReviewInfoList = reviewRepository.getMyReviewInfo(pageable, testUser.getUserId());

        // then
        assertThat(myReviewInfoList).hasSize(1);
        assertThat(myReviewInfoList.get(0).reviewId()).isEqualTo(testReview.getId());
        assertThat(myReviewInfoList.get(0).content()).isEqualTo(testReview.getContent());
    }

    @DisplayName("리뷰 작성 가능 상태를 조회한다.(OPEN)")
    @Test
    void findReviewStatusAsOpen() {
        // given
        User testUser = createAndSaveUser("망고", "김망고");
        Product testProduct = createAndSaveProduct();
        ProductImage testImage = createAndSaveProductImage(testProduct);
        ProductItem testItem = createAndSaveProductItem("S", testProduct);
        ProductItem testItem2 = createAndSaveProductItem("M", testProduct);
        Cart testCart = createAndSaveCart(3L, testUser, testItem);
        Order testOrder = createAndSaveOrder(testUser);
        OrderDetail testOrderDetail = createAndSaveOrderDetail(testOrder, testItem, testCart.getQuantity(), testCart.getCartId());

        // when
        String reviewStatus = reviewRepository.getReviewStatus(testOrderDetail.getId(), testUser.getUserId());

        // then
        assertThat(reviewStatus).isEqualTo("OPEN");
    }

    @DisplayName("리뷰 작성 가능 상태를 조회한다.(MODIFY)")
    @Test
    void findReviewStatusAsModify() {
        // given
        User testUser = createAndSaveUser("망고", "김망고");
        Product testProduct = createAndSaveProduct();
        ProductImage testImage = createAndSaveProductImage(testProduct);
        ProductItem testItem = createAndSaveProductItem("S", testProduct);
        Cart testCart = createAndSaveCart(3L, testUser, testItem);
        Order testOrder = createAndSaveOrder(testUser);
        OrderDetail testOrderDetail = createAndSaveOrderDetail(testOrder, testItem, testCart.getQuantity(), testCart.getCartId());
        Review testReview = createAndSaveReview(testUser, testOrderDetail, testProduct, "test 1");

        // when
        String reviewStatus = reviewRepository.getReviewStatus(testOrderDetail.getId(), testUser.getUserId());

        // then
        assertThat(reviewStatus).isEqualTo("MODIFY");
    }

    @DisplayName("리뷰 작성 가능 상태를 조회한다.(LOCKED)")
    @Test
    void findReviewStatusAsLocked() {
        // given
        User testUser = createAndSaveUser("망고", "김망고");
        Product testProduct = createAndSaveProduct();
        ProductImage testImage = createAndSaveProductImage(testProduct);
        ProductItem testItem = createAndSaveProductItem("S", testProduct);
        Cart testCart = createAndSaveCart(3L, testUser, testItem);
        Order testOrder = createAndSaveOrder(testUser);
        OrderDetail testOrderDetail = createAndSaveOrderDetail(testOrder, testItem, testCart.getQuantity(), testCart.getCartId());
        Review testReview = createAndSaveReview(testUser, testOrderDetail, testProduct, "test 1");
        testReview.updateIsDeleted(true);

        // when
        String reviewStatus = reviewRepository.getReviewStatus(testOrderDetail.getId(), testUser.getUserId());

        // then
        assertThat(reviewStatus).isEqualTo("LOCKED");
    }

    @DisplayName("최근 7일간 좋아요을 제일 많이 받은 인기 리뷰 리스트를 조회한다.")
    @Test
    void findTrendingReviewInfo() {
        // given
        User testUser = createAndSaveUser("망고", "김망고");
        User testUser2 = createAndSaveUser("감자", "최감자");
        Product testProduct = createAndSaveProduct();
        ProductImage testImage = createAndSaveProductImage(testProduct);
        ProductItem testItem = createAndSaveProductItem("S", testProduct);
        ProductItem testItem2 = createAndSaveProductItem("M", testProduct);
        Cart testCart = createAndSaveCart(3L, testUser, testItem);
        Cart testCart2 = createAndSaveCart(3L, testUser, testItem2);
        Order testOrder = createAndSaveOrder(testUser);
        OrderDetail testOrderDetail = createAndSaveOrderDetail(testOrder, testItem, testCart.getQuantity(), testCart.getCartId());
        OrderDetail testOrderDetail2 = createAndSaveOrderDetail(testOrder, testItem2, testCart2.getQuantity(), testCart2.getCartId());
        Review testReview = createAndSaveReview(testUser, testOrderDetail, testProduct, "test 1");
        Review testReview2 = createAndSaveReview(testUser, testOrderDetail2, testProduct, "test 2");

        createAndSaveReviewLike(true, testUser, testReview);
        createAndSaveReviewLike(true, testUser2, testReview);
        createAndSaveReviewLike(true, testUser2, testReview2);

        // when
        List<TrendingReviewResDto> trendingReviewList = reviewRepository.getTrendingReviewInfo();

        // then
        assertThat(trendingReviewList).hasSize(2);
        assertThat(trendingReviewList.get(0).reviewId()).isEqualTo(testReview.getId());
        assertThat(trendingReviewList.get(0).count()).isEqualTo(2);
        assertThat(trendingReviewList.get(1).count()).isEqualTo(1);
    }

    private User createAndSaveUser(String nickname, String name) {
        return userRepository.save(User.builder()
                .sex(Sex.FEMALE)
                .nickname(nickname)
                .cellPhone("01011112222")
                .birth(LocalDate.of(2000, 1, 2))
                .profileImage("default.jpg")
                .name(name)
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

    private ProductItem createAndSaveProductItem(String size, Product product) {
        return productItemRepository.save(ProductItem.builder()
                .color("WHITE")
                .colorCode("#FFFFFF")
                .size(size)
                .quantity(30L)
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

    private ReviewImage createAndSaveReviewImage(Review review, Long idx, UploadFile uploadFile) {
        return reviewImageRepository.save(ReviewImage.builder()
                .uploadFile(uploadFile)
                .imageIndex(idx)
                .isVideo(false)
                .review(review)
                .build());
    }

    private void createAndSaveReviewLike(boolean status, User user, Review review) {
        reviewLikeRepository.save(ReviewLike.builder()
                .isLike(status)
                .user(user)
                .review(review)
                .build());
    }
}