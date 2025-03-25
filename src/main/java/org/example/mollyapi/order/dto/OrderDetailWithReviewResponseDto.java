package org.example.mollyapi.order.dto;

import org.example.mollyapi.order.entity.OrderDetail;
import org.example.mollyapi.review.repository.ReviewRepository;

public record OrderDetailWithReviewResponseDto(
        Long orderId,
        Long orderDetailId,
        Long productId,
        Long itemId,
        String brandName,
        String productName,
        String size,
        Long price,
        Long quantity,
        String image,
        String color,
        String reviewType
) {
    public static OrderDetailWithReviewResponseDto from(Long userId, OrderDetail orderDetail, ReviewRepository reviewRepository) {
        return new OrderDetailWithReviewResponseDto(
                orderDetail.getOrder().getId(),
                orderDetail.getId(),
                orderDetail.getProductItem().getProduct().getId(),
                orderDetail.getProductItem().getId(),
                orderDetail.getBrandName(),
                orderDetail.getProductName(),
                orderDetail.getSize(),
                orderDetail.getPrice(),
                orderDetail.getQuantity(),
                orderDetail.getProductItem().getProduct().getThumbnail().getStoredFileName(),
                orderDetail.getProductItem().getColor(),
                reviewRepository.getReviewStatus(orderDetail.getId(), userId)
        );
    }
}
