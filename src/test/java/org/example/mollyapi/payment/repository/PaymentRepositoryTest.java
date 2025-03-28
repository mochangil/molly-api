package org.example.mollyapi.payment.repository;


import lombok.extern.slf4j.Slf4j;
import org.example.mollyapi.common.exception.CustomException;
import org.example.mollyapi.common.exception.error.impl.PaymentError;
import org.example.mollyapi.order.entity.Order;
import org.example.mollyapi.order.repository.OrderRepository;
import org.example.mollyapi.order.type.CancelStatus;
import org.example.mollyapi.order.type.OrderStatus;
import org.example.mollyapi.payment.entity.Payment;
import org.example.mollyapi.payment.service.PaymentService;
import org.example.mollyapi.payment.type.PaymentStatus;
import org.example.mollyapi.user.entity.User;
import org.example.mollyapi.user.repository.UserRepository;
import org.example.mollyapi.user.type.Sex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
@Slf4j
@Transactional
@ActiveProfiles("test")
public class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentService paymentService;




    ///  생성
    @DisplayName("새로운 결제를 생성합니다")
    @Test
    void createPayment(){

        //given
        User user = createUser("momo");
        Order order = createOrder(user, "ord-20250213132349-6572", "pay-20250213132349-6572",50000L);
        userRepository.save(user);
        orderRepository.save(order);

        Payment payment1 = createPayment(user,order,"pay-20250213132349-6572",50000L);
        //when
        Payment newPayment = paymentService.createPayment(user.getUserId(),order.getId(),order.getTossOrderId(),order.getPaymentId(),"NORMAL",order.getTotalAmount());

        //then
        assertThat(newPayment)
                .extracting("tossOrderId", "paymentKey","paymentStatus")
                .contains(order.getTossOrderId(),order.getPaymentId(),PaymentStatus.PENDING);
    }

    @DisplayName("")
    @Test
    void createDuplicatePayment(){
        //given
        User user = createUser("momo");
        Order order = createOrder(user, "ord-20250213132349-6572", "pay-20250213132349-6572",50000L);
        userRepository.save(user);
        orderRepository.save(order);
        Payment newPayment = paymentService.createPayment(user.getUserId(),order.getId(),order.getTossOrderId(),order.getPaymentId(),"NORMAL",order.getTotalAmount());
        Payment newPayment2 = paymentService.createPayment(user.getUserId(),order.getId(),order.getTossOrderId(),order.getPaymentId(),"NORMAL",order.getTotalAmount());

        //when
        paymentRepository.save(newPayment);
        paymentRepository.save(newPayment2);
        //then
        assertThat(newPayment)
                .extracting("tossOrderId", "paymentKey","paymentStatus")
                .contains(order.getTossOrderId(),order.getPaymentId(),PaymentStatus.PENDING);

    }


    /// 조회

    @DisplayName("주문번호에 해당하는 결제를 최신순으로 조회합니다")
    @Test
    void findLatestPaymentByOrderId() {

        //given
        User user = createUser("momo");
        Order order = createOrder(user, "ord-20250213132349-6572", "pay-20250213132349-6572", 50000L);
        Payment payment1 = createPayment(user,order,"pay-20250213132349-6572",50000L);
        Payment payment2 = createPayment(user,order,"pay-20250213132349-6573",50000L);
        Payment payment3 = createPayment(user,order,"pay-20250213132349-6574",50000L);

        userRepository.save(user);
        orderRepository.save(order);
        paymentRepository.saveAll(List.of(payment1, payment2, payment3));

        //when
        List<Payment> paymentList = paymentRepository.findLatestPaymentByOrderId(order.getId(), PageRequest.of(0, 3));


        //then
        assertThat(paymentList)
                .extracting(Payment::getPaymentKey)
                .containsExactly("pay-20250213132349-6574", "pay-20250213132349-6573", "pay-20250213132349-6572");
    }

    @DisplayName("PaymentKey 에 해당하는 결제를 조회합니다.")
    @Test
    void findByPaymentKey() {
        //given
        User user = createUser("momo");
        Order order = createOrder(user, "ord-20250213132349-6572", "pay-20250213132349-6572", 50000L);
        Payment payment1 = createPayment(user,order,"pay-20250213132349-6572",50000L);
        Payment payment2 = createPayment(user,order,"pay-20250213132349-6573",50000L);
        Payment payment3 = createPayment(user,order,"pay-20250213132349-6574",50000L);

        userRepository.save(user);
        orderRepository.save(order);
        paymentRepository.saveAll(List.of(payment1, payment2, payment3));

        //when
        Payment payment = paymentRepository.findByPaymentKey("pay-20250213132349-6573")
                .orElseThrow(() -> new CustomException(PaymentError.PAYMENT_NOT_FOUND));

        //then
        assertThat(payment)
                .extracting(Payment::getId)
                .isEqualTo(payment2.getId());
    }

    @DisplayName("userID 에 해당하는 모든 결제정보를 조회합니다")
    @Test
    void findAllByUserId(){

        //given
        User user = createUser("momo");
        Order order = createOrder(user, "ord-20250213132349-6572", "pay-20250213132349-6572", 50000L);
        Payment payment1 = createPayment(user,order,"pay-20250213132349-6572",50000L);
        Payment payment2 = createPayment(user,order,"pay-20250213132349-6573",50000L);
        Payment payment3 = createPayment(user,order,"pay-20250213132349-6574",50000L);

        userRepository.save(user);
        orderRepository.save(order);
        paymentRepository.saveAll(List.of(payment1, payment2, payment3));

        //when
        List<Payment> paymentList = paymentRepository.findAllByUserId(user.getUserId())
                .orElseThrow(() -> new CustomException(PaymentError.PAYMENT_NOT_FOUND));

        //then
        assertThat(paymentList)
                .extracting(Payment::getId,Payment::getUser)
                .hasSize(3)
                .containsExactlyInAnyOrder(
                tuple(payment1.getId(), user),
                tuple(payment2.getId(), user),
                tuple(payment3.getId(), user)
        );
    }

    private User createUser(String nickname) {
        return User.builder()
                .sex(Sex.MALE)
                .nickname(nickname)
                .cellPhone("01051212121")
                .birth(LocalDate.of(1990, 1, 1))
                .profileImage("ss")
                .build();
    }

    private Order createOrder(User user, String tossOrderId, String paymentKey, Long amount) {
        return Order.builder()
                .tossOrderId(tossOrderId)
                .orderedAt(LocalDateTime.now())
                .totalAmount(amount)
                .paymentId(paymentKey)
                .user(user)
                .cancelStatus(CancelStatus.NONE)
                .expirationTime(LocalDateTime.now().plusDays(1))
                .status(OrderStatus.PENDING)
                .build();
    }

    private Payment createPayment(User user, Order order, String paymentKey, Long amount) {
        return Payment.create(
                user,
                order,
                order.getTossOrderId(),
                paymentKey,
                "NORMAL",
                amount
        );
    }


}
