package org.example.mollyapi.review.service;

import org.example.mollyapi.cart.entity.Cart;
import org.example.mollyapi.cart.repository.CartRepository;
import org.example.mollyapi.common.client.ImageClient;
import org.example.mollyapi.common.enums.ImageType;
import org.example.mollyapi.common.exception.CustomException;
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
import org.example.mollyapi.review.dto.request.AddReviewReqDto;
import org.example.mollyapi.review.dto.response.GetMyReviewResDto;
import org.example.mollyapi.review.dto.response.GetReviewResDto;
import org.example.mollyapi.review.entity.Review;
import org.example.mollyapi.review.entity.ReviewImage;
import org.example.mollyapi.review.entity.ReviewLike;
import org.example.mollyapi.review.repository.ReviewImageRepository;
import org.example.mollyapi.review.repository.ReviewLikeRepository;
import org.example.mollyapi.review.repository.ReviewRepository;
import org.example.mollyapi.user.entity.User;
import org.example.mollyapi.user.repository.UserRepository;
import org.example.mollyapi.user.type.Sex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.example.mollyapi.common.exception.error.impl.OrderDetailError.NOT_EXIST_ORDERDETIAL;
import static org.example.mollyapi.common.exception.error.impl.ProductItemError.NOT_EXISTS_PRODUCT;
import static org.example.mollyapi.common.exception.error.impl.ReviewError.*;
import static org.example.mollyapi.common.exception.error.impl.UserError.NOT_EXISTS_USER;
import static org.example.mollyapi.order.type.CancelStatus.NONE;
import static org.example.mollyapi.order.type.OrderStatus.SUCCEEDED;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
public class ReviewServiceTest {
    @Autowired
    private ReviewService reviewService;

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

    @MockBean
    private ImageClient imageClient;

    private UploadFile uploadFile;

    @DisplayName("배송 완료 상태인 상품의 리뷰를 등록한다.")
    @Test
    void registerReview() {
        // given
        User testUser = createAndSaveUser("사과", "김사과");
        Product testProduct = createAndSaveProduct();
        ProductItem testItem = createAndSaveProductItem("S", testProduct);
        Cart testCart = createAndSaveCart(3L, testUser, testItem);
        Order testOrder = createAndSaveOrder(testUser);
        OrderDetail testOrderDetail = createAndSaveOrderDetail(testOrder, testItem, testCart.getQuantity(), testCart.getCartId());

        String content = "Test Content";
        AddReviewReqDto addReviewReqDto = new AddReviewReqDto(testOrderDetail.getId(), content);
        List<MultipartFile> testFiles = List.of(new MockMultipartFile("file1", "test.jpg", "image/jpeg", "test content".getBytes()));

        List<UploadFile> mockUploadFile = testFiles.stream()
                .map(file -> UploadFile.builder()
                        .storedFileName("/images/review/" + file.getOriginalFilename())
                        .uploadFileName(file.getOriginalFilename())
                        .build())
                .collect(Collectors.toList());

        // 이미지 저장을 Stub 처리
        when(imageClient.upload(ImageType.REVIEW, testFiles)).thenReturn(mockUploadFile);

        // when
        reviewService.registerReview(addReviewReqDto, testFiles, testUser.getUserId());
        Review newReview = reviewRepository.findByIsDeletedAndOrderDetailIdAndUserUserId(false, testOrderDetail.getId(), testUser.getUserId());

        // then
        assertThat(newReview).isNotNull();
        assertThat(newReview.getContent()).isEqualTo(content);
        assertThat(newReview.getUser().getUserId()).isEqualTo(testUser.getUserId());
        assertThat(reviewImageRepository.findAllByReviewId(newReview.getId()))
                .hasSize(1)
                .extracting(ReviewImage::getFilename)
                .containsExactly(testFiles.get(0).getOriginalFilename());
    }

    @DisplayName("존재하지 않는 사용자가 리뷰를 등록하려고 하면 예외가 발생한다.")
    @Test
    void shouldThrowExceptionWhenUserNotFoundOnRegisterReview() {
        // given
        Long userId = 999L;
        Long orderDetailId = 2L;
        AddReviewReqDto addReviewReqDto = new AddReviewReqDto(orderDetailId, "Test content");
        List<MultipartFile> testFiles = List.of(new MockMultipartFile("file1", "test.jpg", "image/jpeg", "test content".getBytes()));

        // when & then
        assertThatThrownBy(() -> reviewService.registerReview(addReviewReqDto, testFiles, userId))
                .isInstanceOf(CustomException.class)
                .hasMessage(NOT_EXISTS_USER.getMessage());
    }

    @DisplayName("존재하지 않는 주문 상세로 리뷰를 등록하려고 하면 예외가 발생한다.")
    @Test
    void shouldThrowExceptionWhenOrderDetailNotFoundOnRegisterReview() {
        // given
        User testUser = createAndSaveUser("사과", "김사과");
        Long orderDetailId = 999L;
        AddReviewReqDto addReviewReqDto = new AddReviewReqDto(orderDetailId, "Test content");
        List<MultipartFile> uploadImages = List.of(new MockMultipartFile("file1", "test.jpg", "image/jpeg", "test content".getBytes()));

        // when & then
        assertThatThrownBy(() -> reviewService.registerReview(addReviewReqDto, uploadImages, testUser.getUserId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(NOT_EXIST_ORDERDETIAL.getMessage());
    }

    @DisplayName("리뷰 작성 권한이 없는 경우 예외가 발생한다.")
    @Test
    void shouldThrowExceptionWhenUserHasNoPermissionOnRegisterReview() {
        // given
        User testUser = createAndSaveUser("사과", "김사과");
        Product testProduct = createAndSaveProduct();
        ProductItem testItem = createAndSaveProductItem("S", testProduct);
        Cart testCart = createAndSaveCart(3L, testUser, testItem);
        Order testOrder = createAndSaveOrder(testUser);
        OrderDetail testOrderDetail = createAndSaveOrderDetail(testOrder, testItem, testCart.getQuantity(), testCart.getCartId());
        Review testReview = createAndSaveReview(testUser, testOrderDetail, testProduct, "Test content");

        testReview.updateIsDeleted(true);
        AddReviewReqDto addReviewReqDto = new AddReviewReqDto(testOrderDetail.getId(), "리뷰 텍스트");
        List<MultipartFile> uploadImages = List.of(new MockMultipartFile("file1", "test.jpg", "image/jpeg", "test content".getBytes()));

        // when & then
        assertThatThrownBy(() -> reviewService.registerReview(addReviewReqDto, uploadImages, testUser.getUserId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(NOT_ACCESS_REVIEW.getMessage());
    }

    @DisplayName("상품별 리뷰 리스트를 조회한다.")
    @Test
    void findReviewListByProduct() {
        // given
        User testUser = createAndSaveUser("사과", "김사과");
        Product testProduct = createAndSaveProduct();
        ProductItem testItem = createAndSaveProductItem("S", testProduct);
        ProductItem testItem2 = createAndSaveProductItem("M", testProduct);
        Cart testCart = createAndSaveCart(3L, testUser, testItem);
        Order testOrder = createAndSaveOrder(testUser);
        Cart testCart2 = createAndSaveCart(3L, testUser, testItem2);
        OrderDetail testOrderDetail = createAndSaveOrderDetail(testOrder, testItem, testCart.getQuantity(), testCart.getCartId());
        OrderDetail testOrderDetail2 = createAndSaveOrderDetail(testOrder, testItem2, testCart2.getQuantity(), testCart2.getCartId());

        Review testReview1 = createAndSaveReview(testUser, testOrderDetail, testProduct, "Test content 1");
        Review testReview2 = createAndSaveReview(testUser, testOrderDetail2, testProduct, "Test content 2");
        ReviewImage testReviewImage1 = createAndSaveReviewImage(testReview1, 0L, UploadFile.builder()
                .storedFileName("/images/review/test_1.jpg")
                .uploadFileName("test_1.jpg")
                .build());
        ReviewImage testReviewImage2 = createAndSaveReviewImage(testReview2, 0L, UploadFile.builder()
                .storedFileName("/images/review/test_2.jpg")
                .uploadFileName("test_2.jpg")
                .build());

        PageRequest pageable = PageRequest.of(0, 5);

        // when
        SliceImpl<GetReviewResDto> reviewList = reviewService.getReviewList(pageable, testProduct.getId(), testUser.getUserId());

        // then
        assertThat(reviewList).isNotNull();
        assertThat(reviewList).hasSize(2);
        assertThat(reviewList)
                .extracting(
                        GetReviewResDto -> GetReviewResDto.reviewInfo().reviewId(),
                        GetReviewResDto -> GetReviewResDto.reviewInfo().content(),
                        GetReviewResDto -> GetReviewResDto.images().get(0)
                )
                .contains(
                        tuple(testReview1.getId(), testReview1.getContent(), testReviewImage1.getUrl()),
                        tuple(testReview2.getId(), testReview2.getContent(), testReviewImage2.getUrl())
                );
    }

    @DisplayName("존재하지 않는 상품의 리뷰를 조회하면 예외가 발생한다")
    @Test
    void shouldThrowExceptionWhenProductDoesNotExist() {
        // given
        User testUser = createAndSaveUser("사과", "김사과");
        Long productId = 0L;
        PageRequest pageable = PageRequest.of(0, 5);

        // when & then
        assertThatThrownBy(() -> reviewService.getReviewList(pageable, productId, testUser.getUserId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(NOT_EXISTS_PRODUCT.getMessage());
    }

    @DisplayName("해당 상품의 리뷰가 없으면 예외가 발생한다.")
    @Test
    void shouldThrowExceptionWhenReviewNotExist() {
        // given
        User testUser = createAndSaveUser("사과", "김사과");
        Product testProduct = createAndSaveProduct();
        PageRequest pageable = PageRequest.of(0, 5);

        // when & then
        assertThatThrownBy(() -> reviewService.getReviewList(pageable, testProduct.getId(), testUser.getUserId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(NOT_EXIST_REVIEW.getMessage());
    }

    @DisplayName("사용자가 작성한 리뷰 리스트를 조회한다.")
    @Test
    void findMyReviewList() {
        // given
        User testUser = createAndSaveUser("사과", "김사과");
        Product testProduct = createAndSaveProduct();
        ProductImage testImage = createAndSaveProductImage(testProduct);
        ProductItem testItem = createAndSaveProductItem("S", testProduct);
        ProductItem testItem2 = createAndSaveProductItem("M", testProduct);
        Cart testCart = createAndSaveCart(3L, testUser, testItem);
        Cart testCart2 = createAndSaveCart(3L, testUser, testItem2);
        Order testOrder = createAndSaveOrder(testUser);
        OrderDetail testOrderDetail = createAndSaveOrderDetail(testOrder, testItem, testCart.getQuantity(), testCart.getCartId());
        OrderDetail testOrderDetail2 = createAndSaveOrderDetail(testOrder, testItem2, testCart2.getQuantity(), testCart2.getCartId());

        Review testReview1 = createAndSaveReview(testUser, testOrderDetail, testProduct, "Test review 1");
        Review testReview2 = createAndSaveReview(testUser, testOrderDetail2, testProduct, "Test review 2");

        ReviewImage testImage1 = createAndSaveReviewImage(testReview1, 0L, UploadFile.builder()
                .storedFileName("/images/review/review_1.jpg")
                .uploadFileName("review_1.jpg")
                .build());
        ReviewImage testImage2 = createAndSaveReviewImage(testReview2, 0L, UploadFile.builder()
                .storedFileName("/images/review/review_2.jpg")
                .uploadFileName("review_2.jpg")
                .build());

        PageRequest pageable = PageRequest.of(0, 5);

        // when
        SliceImpl<GetMyReviewResDto> reviewList = reviewService.getMyReviewList(pageable, testUser.getUserId());

        // then
        assertThat(reviewList).isNotNull();
        assertThat(reviewList).hasSize(2);
        assertThat(reviewList.hasNext()).isFalse(); // hasNext 플래그 검증
        assertThat(reviewList.getContent())
                .extracting(
                        GetMyReviewResDto -> GetMyReviewResDto.myReviewInfo().reviewId(),
                        GetMyReviewResDto -> GetMyReviewResDto.myReviewInfo().content(),
                        GetMyReviewResDto -> GetMyReviewResDto.images().get(0)
                )
                .contains(
                        tuple(testReview1.getId(), testReview1.getContent(), testImage1.getUrl()),
                        tuple(testReview2.getId(), testReview2.getContent(), testImage2.getUrl())
                );
    }

    @DisplayName("작성한 리뷰가 없으면 예외가 발생한다.")
    @Test
    void shouldThrowExceptionWhenMyReviewNotExist() {
        // given
        User testUser = createAndSaveUser("사과", "김사과");
        PageRequest pageable = PageRequest.of(0, 5);

        // when & then
        assertThatThrownBy(() -> reviewService.getMyReviewList(pageable, testUser.getUserId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(NOT_EXIST_REVIEW.getMessage());
    }

    @DisplayName("본인이 작성한 리뷰의 내용을 수정할 수 있다.")
    @Test
    void updateMyReviewContent() {
        // given
        User testUser = createAndSaveUser("사과", "김사과");
        Product testProduct = createAndSaveProduct();
        ProductItem testItem = createAndSaveProductItem("S", testProduct);
        Cart testCart = createAndSaveCart(3L, testUser, testItem);
        Order testOrder = createAndSaveOrder(testUser);
        OrderDetail testOrderDetail = createAndSaveOrderDetail(testOrder, testItem, testCart.getQuantity(), testCart.getCartId());
        Review testReview = createAndSaveReview(testUser, testOrderDetail, testProduct, "Test content");

        String updateContent = "Update Content";
        AddReviewReqDto addReviewReqDto = new AddReviewReqDto(testReview.getId(), updateContent);

        // when
        reviewService.updateReview(addReviewReqDto, null, testUser.getUserId());
        Optional<Review> updateReview = reviewRepository.findById(testReview.getId());

        // then
        assertThat(updateReview).isPresent();
        assertThat(updateReview.get().getContent()).isEqualTo(updateContent);
    }

    @DisplayName("본인이 작성한 리뷰의 이미지를 수정할 수 있다.")
    @Test
    void updateReviewImage() {
        // given
        User testUser = createAndSaveUser("사과", "김사과");
        Product testProduct = createAndSaveProduct();
        ProductItem testItem = createAndSaveProductItem("S", testProduct);
        Cart testCart = createAndSaveCart(3L, testUser, testItem);
        Order testOrder = createAndSaveOrder(testUser);
        OrderDetail testOrderDetail = createAndSaveOrderDetail(testOrder, testItem, testCart.getQuantity(), testCart.getCartId());
        Review testReview = createAndSaveReview(testUser, testOrderDetail, testProduct, "Test content");

        AddReviewReqDto addReviewReqDto = new AddReviewReqDto(testReview.getId(), null);
        List<MultipartFile> updateFiles = List.of(
                new MockMultipartFile("updateImage", "updateImage.jpg", "image/jpeg", "update image".getBytes()),
                new MockMultipartFile("updateImage2", "updateImage2.jpg", "image/jpeg", "update image2".getBytes())
        );

        List<UploadFile> mockUploadFile = updateFiles.stream()
                .map(file -> UploadFile.builder()
                            .storedFileName("/images/review/" + file.getOriginalFilename())
                            .uploadFileName(file.getOriginalFilename())
                            .build())
                .collect(Collectors.toList());

        List<ReviewImage> mockImages = updateFiles.stream()
                .map(file -> new ReviewImage(
                        UploadFile.builder()
                                .storedFileName("/images/review/" + file.getOriginalFilename())
                                .uploadFileName(file.getOriginalFilename())
                                .build(),
                        0L,
                        false,
                        testReview))
                .toList();

        // 이미지 저장을 Stub 처리
        doNothing().when(imageClient).delete(eq(ImageType.REVIEW), anyString());
        when(imageClient.upload(eq(ImageType.REVIEW), anyList())).thenReturn(mockUploadFile);

        // when
        reviewService.updateReview(addReviewReqDto, updateFiles, testUser.getUserId());
        Optional<Review> updateReview = reviewRepository.findById(testReview.getId());
        List<ReviewImage> imageList = reviewImageRepository.findAllByReviewId(updateReview.get().getId());

        // then
        assertThat(updateReview).isPresent();
        assertThat(imageList)
                .hasSize(2)
                .extracting(ReviewImage::getFilename)
                .containsExactly(
                        updateFiles.get(0).getOriginalFilename(),
                        updateFiles.get(1).getOriginalFilename()
                );
    }

    @DisplayName("본인이 작성한 리뷰의 내용과 이미지 모두 수정할 수 있다.")
    @Test
    void updateReviewContentAndImage() {
        // given
        User testUser = createAndSaveUser("사과", "김사과");
        Product testProduct = createAndSaveProduct();
        ProductItem testItem = createAndSaveProductItem("S", testProduct);
        Cart testCart = createAndSaveCart(3L, testUser, testItem);
        Order testOrder = createAndSaveOrder(testUser);
        OrderDetail testOrderDetail = createAndSaveOrderDetail(testOrder, testItem, testCart.getQuantity(), testCart.getCartId());
        Review testReview = createAndSaveReview(testUser, testOrderDetail, testProduct, "Test content");

        String updateContent = "Update Content";
        AddReviewReqDto addReviewReqDto = new AddReviewReqDto(testReview.getId(), updateContent);

        List<MultipartFile> testFiles = List.of(
                new MockMultipartFile("updateImage", "updateImage.jpg", "image/jpeg", "update image".getBytes()),
                new MockMultipartFile("updateImage2", "updateImage2.jpg", "image/jpeg", "update image2".getBytes())
        );

        List<UploadFile> mockUploadFile = testFiles.stream()
                .map(file -> UploadFile.builder()
                        .storedFileName("/images/review/" + file.getOriginalFilename())
                        .uploadFileName(file.getOriginalFilename())
                        .build())
                .collect(Collectors.toList());

        List<ReviewImage> mockImages = testFiles.stream()
                .map(file -> new ReviewImage(
                        UploadFile.builder()
                                .storedFileName("/images/review/" + file.getOriginalFilename())
                                .uploadFileName(file.getOriginalFilename())
                                .build(),
                        0L,
                        false,
                        testReview))
                .toList();

        doNothing().when(imageClient).delete(eq(ImageType.REVIEW), anyString());
        when(imageClient.upload(eq(ImageType.REVIEW), anyList())).thenReturn(mockUploadFile);

        // when
        reviewService.updateReview(addReviewReqDto, testFiles, testUser.getUserId());
        Optional<Review> updateReview = reviewRepository.findById(testReview.getId());
        List<ReviewImage> imageList = reviewImageRepository.findAllByReviewId(updateReview.get().getId());

        // then
        assertThat(updateReview).isPresent();
        assertThat(imageList)
                .hasSize(2)
                .extracting(ReviewImage::getFilename)
                .containsExactly(
                        testFiles.get(0).getOriginalFilename(),
                        testFiles.get(1).getOriginalFilename()
                );
    }

    @DisplayName("수정 권한이 없는 리뷰를 수정할 경우 예외가 발생한다.")
    @Test
    void shouldThrowExceptionUpdateReviewNotExist() {
        // given
        User testUser = createAndSaveUser("사과", "김사과");
        Product testProduct = createAndSaveProduct();
        ProductItem testItem = createAndSaveProductItem("S", testProduct);
        Cart testCart = createAndSaveCart(3L, testUser, testItem);
        Order testOrder = createAndSaveOrder(testUser);
        OrderDetail testOrderDetail = createAndSaveOrderDetail(testOrder, testItem, testCart.getQuantity(), testCart.getCartId());
        Review testReview = createAndSaveReview(testUser, testOrderDetail, testProduct, "Test content");

        ReviewImage testReviewImages = createAndSaveReviewImage(testReview, 0L, UploadFile.builder()
                .storedFileName("/images/review/review_1.jpg")
                .uploadFileName("review_1.jpg")
                .build());
        AddReviewReqDto addReviewReqDto = new AddReviewReqDto(testReview.getId(), null);
        testReview.updateIsDeleted(true); //리뷰 삭제

        // when & then
        assertThatThrownBy(() -> reviewService.updateReview(addReviewReqDto, null, testUser.getUserId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(NOT_ACCESS_REVIEW.getMessage());
    }

    @DisplayName("리뷰 수정 시 변경사항이 없으면 예외가 발생한다.")
    @Test
    void shouldThrowExceptionWithoutChangesOnUpdateReview() {
        // given
        User testUser = createAndSaveUser("사과", "김사과");
        Product testProduct = createAndSaveProduct();
        ProductItem testItem = createAndSaveProductItem("S", testProduct);
        Cart testCart = createAndSaveCart(3L, testUser, testItem);
        Order testOrder = createAndSaveOrder(testUser);
        OrderDetail testOrderDetail = createAndSaveOrderDetail(testOrder, testItem, testCart.getQuantity(), testCart.getCartId());
        Review testReview = createAndSaveReview(testUser, testOrderDetail, testProduct, "Test content");

        ReviewImage testReviewImage = createAndSaveReviewImage(testReview, 0L, UploadFile.builder()
                .storedFileName("/images/review/review_1.jpg")
                .uploadFileName("review_1.jpg")
                .build());

        String updateContent = null;
        AddReviewReqDto addReviewReqDto = new AddReviewReqDto(testReview.getId(), updateContent);
        List<MultipartFile> testFiles = null;

        // when & then
        assertThatThrownBy(() -> reviewService.updateReview(addReviewReqDto, testFiles, testUser.getUserId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(NOT_CHANGED.getMessage());
    }

    @DisplayName("본인이 작성한 리뷰는 삭제할 수 있다.")
    @Test
    void deleteReview() {
        // given
        User testUser = createAndSaveUser("사과", "김사과");
        Product testProduct = createAndSaveProduct();
        ProductImage testImage = createAndSaveProductImage(testProduct);
        ProductItem testItem = createAndSaveProductItem("S", testProduct);
        Cart testCart = createAndSaveCart(3L, testUser, testItem);
        Order testOrder = createAndSaveOrder(testUser);
        OrderDetail testOrderDetail = createAndSaveOrderDetail(testOrder, testItem, testCart.getQuantity(), testCart.getCartId());
        Review testReview = createAndSaveReview(testUser, testOrderDetail, testProduct, "Test content");

        ReviewImage testReviewImages = createAndSaveReviewImage(testReview, 0L, UploadFile.builder()
                .storedFileName("/images/review/review_1.jpg")
                .uploadFileName("review_1.jpg")
                .build());
        ReviewLike testLike = createAndSaveReviewLike(true, testUser, testReview);

        doNothing().when(imageClient).delete(eq(ImageType.REVIEW), anyString());

        // when
        reviewService.deleteReview(testReview.getId(), testUser.getUserId());
        Optional<Review> deleteReview = reviewRepository.findById(testReview.getId());
        boolean existImage = reviewImageRepository.existsById(testImage.getId());
        boolean existLike = reviewLikeRepository.existsById(testLike.getId());

        // then
        assertThat(deleteReview).isPresent();
        assertThat(deleteReview.get().getIsDeleted()).isTrue();
        assertThat(existImage).isFalse();
        assertThat(existLike).isFalse();
    }

    @DisplayName("존재하지 않는 리뷰를 삭제하려고 하면 예외가 발생한다.")
    @Test
    void shouldThrowExceptionWhenReviewNotFoundOnDeleteReview() {
        // given
        User testUser = createAndSaveUser("사과", "김사과");
        Long reviewId = 999L; // 존재하지 않는 리뷰 ID

        // when & then
        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, testUser.getUserId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(NOT_EXIST_REVIEW.getMessage());
    }

    @DisplayName("다른 사용자의 리뷰를 삭제하려고 하면 예외가 발생한다.")
    @Test
    void shouldThrowExceptionWhenDeleteOtherUsersReview() {
        // given
        User anotherUser = createAndSaveUser("뉴비", "최뉴비");
        User testUser = createAndSaveUser("사과", "김사과");
        Product testProduct = createAndSaveProduct();
        ProductItem testItem = createAndSaveProductItem("S", testProduct);
        Cart testCart = createAndSaveCart(3L, testUser, testItem);
        Order testOrder = createAndSaveOrder(testUser);
        OrderDetail testOrderDetail = createAndSaveOrderDetail(testOrder, testItem, testCart.getQuantity(), testCart.getCartId());
        Review testReview = createAndSaveReview(testUser, testOrderDetail, testProduct, "Test content");

        // when & then
        assertThatThrownBy(() -> reviewService.deleteReview(testReview.getId(), anotherUser.getUserId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(NOT_EXIST_REVIEW.getMessage()); // 접근 권한이 없을 때 예외 발생
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

    private ReviewImage createAndSaveReviewImage(Review review, Long idx, UploadFile uploadFile) {
        return reviewImageRepository.save(ReviewImage.builder()
                .uploadFile(uploadFile)
                .imageIndex(idx)
                .isVideo(false)
                .review(review)
                .build());
    }

    private ReviewLike createAndSaveReviewLike(boolean status, User user, Review review) {
        return reviewLikeRepository.save(ReviewLike.builder()
                .isLike(status)
                .user(user)
                .review(review)
                .build());
    }
}