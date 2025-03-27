package org.example.mollyapi.review.service;

import lombok.RequiredArgsConstructor;
import org.example.mollyapi.common.exception.CustomException;
import org.example.mollyapi.review.dto.request.UpdateReviewLikeReqDto;
import org.example.mollyapi.review.entity.Review;
import org.example.mollyapi.review.entity.ReviewLike;
import org.example.mollyapi.review.repository.ReviewLikeRepository;
import org.example.mollyapi.review.repository.ReviewRepository;
import org.example.mollyapi.user.entity.User;
import org.example.mollyapi.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.example.mollyapi.common.exception.error.impl.ReviewError.NOT_EXIST_REVIEW;

@Service
@RequiredArgsConstructor
public class ReviewLikeService {
    private final UserService userService;
    private final ReviewRepository reviewRep;
    private final ReviewLikeRepository reviewLikeRep;

    /**
     * 좋아요 상태 변경
     * @param likeDto 리뷰 Pk, 좋아요 상태를 담은 Dto
     * @param userId 사용자 PK
     * */
    @Transactional
    public void changeReviewLike(UpdateReviewLikeReqDto likeDto, Long userId) {
        // 가입된 사용자 여부 체크
        User user = userService.findByUser(userId);

        // 해당 리뷰 존재 여부 체크
        Review review = reviewRep.findById(likeDto.reviewId())
                .orElseThrow(() -> new CustomException(NOT_EXIST_REVIEW));

        // 사용자가 이전에 누른 좋아요가 있는 지
        ReviewLike reviewLike = reviewLikeRep.findByReviewIdAndUserUserId(likeDto.reviewId(), userId);

        // ReviewLike Entity에 데이터를 추가한 적이 없을 경우
        if(reviewLike == null) {
            // 좋아요 생성
            reviewLike = ReviewLike.builder()
                    .user(user)
                    .review(review)
                    .build();

            reviewLikeRep.save(reviewLike);
            review.increaseLikeCount();
        } else { // 이미 좋아요를 눌렀던 적이 있을 경우
            reviewLikeRep.delete(reviewLike);
            review.decreaseLikeCount();
        }
    }
}
