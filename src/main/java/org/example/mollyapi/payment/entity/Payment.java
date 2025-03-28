package org.example.mollyapi.payment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.mollyapi.common.entity.Base;
import org.example.mollyapi.common.exception.CustomException;
import org.example.mollyapi.common.exception.error.impl.PaymentError;
import org.example.mollyapi.order.entity.Order;
import org.example.mollyapi.payment.type.PaymentStatus;
import org.example.mollyapi.user.entity.User;

import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
public class Payment extends Base {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String paymentType;

    @Column
    private Long amount;

    @Column(nullable = false, unique = true, length = 30)
    private String paymentKey;

    @Column
    private String tossOrderId;

    @Column
    private LocalDateTime paymentDate;

    @Column
    private String failureReason;

    @Column
    private Integer point;

    @Column
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

//    private int retry_count;

    // 생성자 팩토리 메서드
    public static Payment create(User user, Order order, String tossOrderId,
                                 String paymentKey, String paymentType, Long amount) {
        return Payment.builder()
                .user(user)
                .order(order)
                .tossOrderId(tossOrderId)
                .paymentKey(paymentKey)
                .paymentType(paymentType)
                .amount(amount)
                .paymentStatus(PaymentStatus.PENDING)
                .paymentDate(LocalDateTime.now())
                .build();
    }

    public static Payment create(User user, Order order, String tossOrderId,
                                 String paymentKey, String paymentType, Long amount, PaymentStatus paymentStatus) {
        return Payment.builder()
                .user(user)
                .order(order)
                .tossOrderId(tossOrderId)
                .paymentKey(paymentKey)
                .paymentType(paymentType)
                .amount(amount)
                .paymentStatus(paymentStatus)
                .paymentDate(LocalDateTime.now())
                .build();
    }

    public boolean failPayment(String failureReason) {
        this.paymentStatus = PaymentStatus.FAILED;
        this.failureReason = failureReason;
        return true;
    }

    public void cancelPayment() {
        if (this.paymentStatus != PaymentStatus.APPROVED) {
            throw new CustomException(PaymentError.PAYMENT_ALREADY_CANCELED);
        }
        this.paymentStatus = PaymentStatus.CANCELED;
    }


    public void approvePayment() {
        this.paymentStatus = PaymentStatus.APPROVED;
    }

    public void pendingPayment() {
        this.paymentStatus = PaymentStatus.PENDING;
    }

    public PaymentStatus getStatus() {
        return this.paymentStatus;
    }

    public boolean isApproved() {
        return this.paymentStatus == PaymentStatus.APPROVED;
    }
//    public void increaseRetryCount() {
//        this.retry_count++;
//    }

}
