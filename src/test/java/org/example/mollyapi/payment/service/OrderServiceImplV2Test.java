package org.example.mollyapi.payment.service;


import org.example.mollyapi.delivery.dto.DeliveryReqDto;
import org.example.mollyapi.order.entity.Order;
import org.example.mollyapi.order.repository.OrderRepository;
import org.example.mollyapi.order.service.OrderServiceImplV2;
import org.example.mollyapi.order.type.CancelStatus;
import org.example.mollyapi.order.type.OrderStatus;
import org.example.mollyapi.payment.dto.request.PaymentConfirmReqDto;
import org.example.mollyapi.payment.dto.response.TossConfirmResDto;
import org.example.mollyapi.payment.entity.Payment;
import org.example.mollyapi.payment.repository.PaymentRepository;
import org.example.mollyapi.payment.type.PaymentStatus;
import org.example.mollyapi.payment.util.AESUtil;
import org.example.mollyapi.payment.util.PaymentWebClientUtil;
import org.example.mollyapi.user.entity.User;
import org.example.mollyapi.user.repository.UserRepository;
import org.example.mollyapi.user.type.Sex;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;

@SpringBootTest
public class OrderServiceImplV2Test {

    @Autowired
    private OrderServiceImplV2 orderServiceImplV2;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentService paymentService;

    @MockBean
    private PaymentWebClientUtil paymentWebClientUtil;

    User user;
    Order order;
    Payment payment1;
    Payment payment2;
    Payment payment3;

    @BeforeEach
    void setUp() {
        user = createUser("momo");
        order = createOrder(user, "ord-20250213132349-6572", "pay-20250213132349-6572",50000L);
        payment1 = createPayment(user,order,"pay-20250213132349-6573",50000L);
        payment2 = createPayment(user,order,"pay-20250213132349-6574",50000L);
        payment3 = createPayment(user,order,"pay-20250213132349-6575",50000L);
        userRepository.save(user);
        orderRepository.save(order);
        paymentRepository.saveAll(List.of(payment1, payment2, payment3));
    }

    @BeforeAll
    public static void beforeAll() {
        // AESUtil Mocking
        MockedStatic<AESUtil> mockedStatic = mockStatic(AESUtil.class);
        mockedStatic.when(() -> AESUtil.decryptWithSalt(anyString()))
                .thenReturn("0");
    }



    @DisplayName("주문을 성공합니다")
    @Test
    void processOrder() {

        //given
        ResponseEntity<TossConfirmResDto> successResponse = getResponse(HttpStatus.OK);
        given(paymentWebClientUtil.confirmPayment(any(), any()))
                .willReturn(successResponse);

        DeliveryReqDto deliveryReqDto = new DeliveryReqDto(
            "momo",
                    "010-5134-1111",
                    "판교판교",
                    "12345",
                    "배송 조심히 해주세요",
                order.getId()
        );

        //when
        orderServiceImplV2.processOrder(user.getUserId(), order.getPaymentId(), order.getTossOrderId(), order.getTotalAmount(), "0","NORMAL",deliveryReqDto);

        //then
        Order newOrder = orderRepository.findById(order.getId()).get();
        assertThat(newOrder.getStatus()).isEqualTo(OrderStatus.SUCCEEDED);

        Payment payment = paymentRepository.findByPaymentKey(order.getPaymentId())
                        .orElseThrow(() -> new RuntimeException("payment not found"));
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);


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

    private ResponseEntity<TossConfirmResDto> getResponse(HttpStatus status) {
        return ResponseEntity
                .status(status)
                .body(null);
    }
}
