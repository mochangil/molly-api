package org.example.mollyapi.order.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.mollyapi.delivery.entity.Delivery;
import org.example.mollyapi.order.type.CancelStatus;
import org.example.mollyapi.order.type.OrderStatus;
import org.example.mollyapi.payment.entity.Payment;
import org.example.mollyapi.user.entity.User;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @Column(name = "toss_order_id", unique = true, length = 30)
    private String tossOrderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderDetail> orderDetails = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "delivery_id")
    private Delivery delivery;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Payment> payments = new ArrayList<>();

    @Column(nullable = false)
    private Long totalAmount; // 포인트 적용 전 금액

    @Column
    private Long paymentAmount; // 결제된 금액

    @Column
    private String paymentId; // 결제 ID

    @Column
    private String paymentType; // 결제 수단

    @Column
    private Integer pointUsage; // 사용한 포인트

    @Column
    private Integer pointSave; // 적립 포인트

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CancelStatus cancelStatus;

    @Column(nullable = false)
    private LocalDateTime orderedAt;

    @Column(nullable = false)
    private LocalDateTime expirationTime;

    public Order(User user, String tossOrderId) {
        this.user = user;
        this.tossOrderId = tossOrderId;
        this.totalAmount = 0L;
        this.status = OrderStatus.PENDING;
        this.cancelStatus = CancelStatus.NONE;
        this.expirationTime = LocalDateTime.now().plusMinutes(10);
    }

    @PrePersist
    protected void onCreate() {
        this.orderedAt = LocalDateTime.now();
        this.expirationTime = this.orderedAt.plusMinutes(10);
    }

    public void updateStatus(OrderStatus newStatus) {
        if (this.status == OrderStatus.FAILED || this.status == OrderStatus.CANCELED) {
            throw new IllegalStateException("이미 실패 또는 취소된 주문은 상태를 변경할 수 없습니다.");
        }
        this.status = newStatus;
    }

    public void updateCancelStatus(CancelStatus newCancelStatus) {
        if (this.cancelStatus == CancelStatus.COMPLETED) {
            throw new IllegalStateException("이미 완료된 취소 상태는 변경할 수 없습니다.");
        }
        this.cancelStatus = newCancelStatus;
    }


    public void updateTotalAmount(long totalAmount) {
        this.totalAmount = totalAmount;
    }

//    public void addPayment(Payment payment) {
//        if (payment == null) return;
//        this.payments.add(payment);
//        this.orderedAt = payment.getPaymentDate();
//    }
//
//    //결제한개밖에 등록안됨
//    public void updatePaymentInfo() {
//        payments.stream()
//                .max(Comparator.comparing(Payment::getPaymentDate))
//                .ifPresent(payment -> {
//                    this.paymentId = payment.getPaymentKey();
//                    this.paymentType = payment.getPaymentType();
//                    this.paymentAmount = payment.getAmount();
//                    this.pointUsage = payment.getPoint();
//                    this.orderedAt = payment.getPaymentDate();
//                });
//    }

    public void addPayment(Payment payment) {
        if (payment == null) return;

        if (this.payments == null) this.payments = new ArrayList<>();
        this.payments.add(payment);
        this.paymentId = payment.getPaymentKey();
        this.paymentType = payment.getPaymentType();
        this.paymentAmount = payment.getAmount();
        this.pointUsage = payment.getPoint();
        this.orderedAt = payment.getPaymentDate();
        this.updateStatus(OrderStatus.SUCCEEDED); // ✅ 주문 상태 변경까지 포함
    }

    public void setDelivery(Delivery delivery) {
        this.delivery = delivery;
    }

    public void setPointSave(int point) {
        this.pointSave = point;
    }

    public void validateExpiration() {
        if (expirationTime.isBefore(LocalDateTime.now())) {
            // 이 경우, 주문 객체 내부에서 failOrder를 호출할 수 있다면 호출하거나,
            // 주문 실패 처리를 별도 서비스에서 호출 후 예외를 던지도록 할 수 있다.
            throw new IllegalStateException("결제 가능 시간이 초과되었습니다. 주문을 다시 생성해주세요.");
        }
    }

    public boolean isPending(){
        return status.equals(OrderStatus.PENDING);
    }

    public boolean isCanceled(){
        return status.equals(OrderStatus.CANCELED);
    }
}