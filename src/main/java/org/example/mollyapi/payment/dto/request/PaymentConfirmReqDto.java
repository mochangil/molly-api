package org.example.mollyapi.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.example.mollyapi.delivery.dto.DeliveryReqDto;


@Builder
public record PaymentConfirmReqDto(

//        @Schema(description = "주문 ID", example = "10")
//        Long orderId,
        @Schema(description = "토스 주문 ID", example = "ORD-20250213132349-6572")
        String tossOrderId,
        @Schema(description = "토스 결제 KEY", example = "ORD-20250218131035-3409")
        String paymentKey,
        @Schema(description = "결제금액", example = "112242")
        Long amount,
        @Schema(description = "결제수단", example = "NORMAL")
        String paymentType,
        @Schema(description = "사용한 포인트", example = "5000")
        Integer point
){
}