package org.example.mollyapi.review.repository;

import org.example.mollyapi.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long>, ReviewCustomRepository {
    Review findByIsDeletedAndOrderDetailIdAndUserUserId(Boolean isDeleted, Long orderDetail, Long userId);
    Optional<Review> findByIdAndUserUserIdAndIsDeleted(Long reviewId, Long userId, Boolean isDeleted);

    @Query("SELECT r.id FROM Review r WHERE r.orderDetail.id IN :orderDetailIds")
    List<Long> findReviewIdsByOrderDetailIds(@Param("orderDetailIds") List<Long> orderDetailIds);

    @Transactional
    @Modifying
    @Query("DELETE FROM Review r WHERE r.orderDetail.id IN :orderDetailIds")
    int deleteByOrderDetailIds(@Param("orderDetailIds") List<Long> orderDetailIds);
}
