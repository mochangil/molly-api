package org.example.mollyapi.review.service;

import lombok.RequiredArgsConstructor;
import org.example.mollyapi.common.client.ImageClient;
import org.example.mollyapi.common.exception.CustomException;
import org.example.mollyapi.order.entity.OrderDetail;
import org.example.mollyapi.order.repository.OrderDetailRepository;
import org.example.mollyapi.product.dto.UploadFile;
import org.example.mollyapi.product.entity.Product;
import org.example.mollyapi.common.enums.ImageType;
import org.example.mollyapi.product.service.impl.ProductServiceImpl;
import org.example.mollyapi.review.dto.request.AddReviewReqDto;
import org.example.mollyapi.review.dto.response.GetMyReviewResDto;
import org.example.mollyapi.review.dto.response.GetReviewResDto;
import org.example.mollyapi.review.dto.response.MyReviewInfoDto;
import org.example.mollyapi.review.dto.response.ReviewInfoDto;
import org.example.mollyapi.review.entity.Review;
import org.example.mollyapi.review.entity.ReviewImage;
import org.example.mollyapi.review.repository.ReviewImageRepository;
import org.example.mollyapi.review.repository.ReviewLikeRepository;
import org.example.mollyapi.review.repository.ReviewRepository;
import org.example.mollyapi.user.entity.User;
import org.example.mollyapi.user.service.UserService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

import static org.example.mollyapi.common.exception.error.impl.OrderDetailError.NOT_EXIST_ORDERDETIAL;
import static org.example.mollyapi.common.exception.error.impl.ReviewError.*;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ImageClient imageClient;
    private final UserService userService;
    private final ProductServiceImpl productServiceImpl;
    private final OrderDetailRepository orderDetailRep;
    private final ReviewRepository reviewRep;
    private final ReviewImageRepository reviewImageRep;
    private final ReviewLikeRepository reviewLikeRep;

    /**
     * 리뷰 작성 기능
     * @param addReviewReqDto 주문상세 PK와 내용이 담긴 DTO
     * @param uploadImages 업로드한 이미지 파일
     * @param userId 사용자 PK
     * */
    @Transactional
    public void registerReview(
            AddReviewReqDto addReviewReqDto, // Long orderDetailId, String content
            List<MultipartFile> uploadImages,
            Long userId
    ) {
        Long orderDetailId = addReviewReqDto.id(); //주문상세 PK
        String content = addReviewReqDto.content(); //리뷰 내용

        // 가입된 사용자 여부 체크
        User user = userService.findByUser(userId);

        // 주문 상세 조회
        OrderDetail orderDetail = orderDetailRep.findByIdAndOrderUserUserId(orderDetailId, userId)
                .orElseThrow(() -> new CustomException(NOT_EXIST_ORDERDETIAL));

        // 상품 정보 조회
        Product product = productServiceImpl.findByProduct(orderDetail.getProductItem().getProduct().getId());

        //리뷰 작성 권한 체크
        Review review = reviewRep.findByIsDeletedAndOrderDetailIdAndUserUserId(true, orderDetailId, userId);
        if(review != null) throw new CustomException(NOT_ACCESS_REVIEW);

        // 리뷰 생성
        Review newReview = Review.builder()
                .content(content)
                .isDeleted(Boolean.FALSE)
                .user(user)
                .orderDetail(orderDetail)
                .product(product)
                .build();

        // 업로드된 이미지 파일 저장
        saveReviewImages(newReview, uploadImages);

        reviewRep.save(newReview);
    }

    /**
     * 상품별 리뷰 조회
     * @param pageable 페이지 처리에 필요한 정보를 담는 인터페이스
     * @param productId 상품 PK
     * @param userId 사용자 PK
     * @return reviewResDtoList 리뷰 정보를 담은 DtoList
     * */
    @Transactional(readOnly = true)
    public SliceImpl<GetReviewResDto> getReviewList(Pageable pageable, Long productId, Long userId) {
        // 상품 존재 여부 체크
        productServiceImpl.validProduct(productId);

        // 해당 상품의 리뷰 정보 조회
        List<ReviewInfoDto> reviewInfoList = reviewRep.getReviewInfo(pageable, productId, userId);
        if(reviewInfoList.isEmpty()) throw new CustomException(NOT_EXIST_REVIEW);

        // Response로 전달할 상품 리뷰 정보 담기
        List<GetReviewResDto> reviewResDtoList = new ArrayList<>();
        for(ReviewInfoDto info : reviewInfoList) {
            List<String> images = reviewRep.getImageList(info.reviewId());
            if(images.isEmpty()) continue;

            // 리뷰 정보를 DTO에 추가
            reviewResDtoList.add(new GetReviewResDto(info, images));
        }

        // 페이지네이션을 위한 hasNext 플래그 설정
        boolean hasNext = false;
        if (reviewResDtoList.size() > pageable.getPageSize()) {
            reviewResDtoList.remove(pageable.getPageSize());
            hasNext = true;
        }

        // Slice 형태로 리뷰 리스트 생성
        return new SliceImpl<>(reviewResDtoList, pageable, hasNext);
    }

    /**
     * 사용자 본인이 작성한 리뷰 조회
     * @param pageable 페이지 처리에 필요한 정보를 담는 인터페이스
     * @param userId 사용자 PK
     * @return SliceImpl<GetMyReviewResDto> 사용자 본인이 작성한 리뷰 정보를 담은 DtoList
     * */
    @Transactional(readOnly = true)
    public SliceImpl<GetMyReviewResDto> getMyReviewList(Pageable pageable, Long userId) {
        // 가입된 사용자 여부 체크
        userService.validUser(userId);

        // 사용자 본인이 작성한 리뷰 정보 조회
        List<MyReviewInfoDto> myReviewInfoList = reviewRep.getMyReviewInfo(pageable, userId);
        if(myReviewInfoList.isEmpty()) throw new CustomException(NOT_EXIST_REVIEW);

        // Response로 전달할 상품 리뷰 정보 담기
        List<GetMyReviewResDto> myReviewResDtoList = new ArrayList<>();
        for(MyReviewInfoDto info : myReviewInfoList) {
            List<String> images = reviewRep.getImageList(info.reviewId());
            if(images.isEmpty()) continue;

            // 리스트에 리뷰 정보 담기
            myReviewResDtoList.add(new GetMyReviewResDto(info, images));
        }

        // 페이지네이션을 위한 hasNext 플래그 설정
        boolean hasNext = false;
        if (myReviewResDtoList.size() > pageable.getPageSize()) {
            myReviewResDtoList.remove(pageable.getPageSize());
            hasNext = true;
        }

        // Slice 형태로 리뷰 리스트 생성
        return new SliceImpl<>(myReviewResDtoList, pageable, hasNext);
    }

    /**
     * 리뷰 수정 기능
     * @param addReviewReqDto 주문상세 PK와 내용이 담긴 DTO
     * @param uploadImages 업로드한 이미지 파일
     * @param userId 사용자 PK
     * */
    @Transactional
    public void updateReview(
            AddReviewReqDto addReviewReqDto, // Long reviewId, String content
            List<MultipartFile> uploadImages,
            Long userId
    ) {
        Long reviewId = addReviewReqDto.id(); //리뷰 PK
        String content = addReviewReqDto.content();

        // 가입된 사용자 여부 체크
        userService.validUser(userId);

        // 변경하려는 리뷰 체크
        Review review = validReview(reviewId, userId, false);

        boolean flag = false; //변경 여부 체크 변수

        // 리뷰 내용 변경 요청이 들어 왔을 떄
        if(content != null) {
            review.updateContent(content);
            flag = true;
        }

        // 리뷰 이미지 변경 요청이 들어 왔을 떄
        if(uploadImages != null) {
            deleteReviewImage(reviewId); // 리뷰 이미지 삭제
            reviewImageRep.flush(); //duplicate entry 방지
            saveReviewImages(review, uploadImages);  // 리뷰에 새로운 이미지 추가
            flag = true;
        }

        if(!flag) throw new CustomException(NOT_CHANGED);
    }

    /**
     * 리뷰 삭제 기능(삭제 후 재작성이 불가능하기 때문에, 삭제 여부 칼럼 업데이트
     * @param reviewId 리뷰 PK
     * @param userId 사용자 PK
     * */
    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        // 가입된 사용자 여부 체크
        userService.validUser(userId);

        // 해당하는 리뷰가 있다면
        Review review = validReview(reviewId, userId, false);

        // 리뷰 삭제 여부 변경
        boolean isUpdate = review.updateIsDeleted(Boolean.TRUE);
        if(!isUpdate) throw new CustomException(FAIL_UPDATE);

        // 리뷰 이미지 삭제
        deleteReviewImage(reviewId);

        // 리뷰와 연결된 좋아요 삭제
        reviewLikeRep.deleteAllByReviewId(reviewId);
    }

    /**
     * 업로드된 이미지 파일 저장
     * @param review review Entity
     * @param uploadImages 업로드한 이미지 파일
     * */
    private void saveReviewImages(Review review, List<MultipartFile> uploadImages) {
        List<UploadFile> uploadFiles = imageClient.upload(ImageType.REVIEW, uploadImages);

        for (int i = 0; i < uploadFiles.size(); i++) {
            UploadFile uploadFile = uploadFiles.get(i);
            ReviewImage reviewImage = ReviewImage.createReviewImage(review, uploadFile, i);
            review.addImage(reviewImage);  // 리뷰에 이미지 추가
        }
    }

    /**
     * 업로드된 이미지 파일 삭제
     * @param reviewId 리뷰 PK
     * */
    private void deleteReviewImage(Long reviewId) {
        // 이미지 서버에 저장된 이미지 삭제
        List<ReviewImage> reviewImageList = reviewImageRep.findAllByReviewId(reviewId);
        for(ReviewImage image : reviewImageList) {
            imageClient.delete(ImageType.REVIEW, image.getUrl());
        }

        // 기존의 리뷰 이미지 삭제
        reviewImageRep.deleteAllByReviewId(reviewId);
    }

    /**
     * 해당 리뷰가 존재하는 지 체크
     * @param reviewId 리뷰 PK
     * @param userId 사용자 PK
     * @param isDeleted 삭제여부
     * @return Review Entity
     * */
    public Review validReview(Long reviewId, Long userId, boolean isDeleted) {
        return reviewRep.findByIdAndUserUserIdAndIsDeleted(reviewId, userId, isDeleted)
                .orElseThrow(() -> new CustomException(NOT_EXIST_REVIEW));
    }
}
