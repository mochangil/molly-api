package org.example.mollyapi.payment.service;


import lombok.extern.slf4j.Slf4j;
import org.example.mollyapi.common.exception.CustomException;
import org.example.mollyapi.common.exception.error.CustomError;
import org.example.mollyapi.order.entity.Order;
import org.example.mollyapi.order.repository.OrderRepository;
import org.example.mollyapi.order.service.OrderService;
import org.example.mollyapi.order.type.CancelStatus;
import org.example.mollyapi.order.type.OrderStatus;
import org.example.mollyapi.payment.dto.request.PaymentConfirmReqDto;
import org.example.mollyapi.payment.dto.response.TossConfirmResDto;
import org.example.mollyapi.payment.entity.Payment;
import org.example.mollyapi.payment.exception.RetryablePaymentException;
import org.example.mollyapi.payment.repository.PaymentRepository;
import org.example.mollyapi.payment.service.impl.PaymentServiceImpl;
import org.example.mollyapi.payment.type.PaymentStatus;
import org.example.mollyapi.payment.util.PaymentWebClientUtil;
import org.example.mollyapi.user.entity.User;
import org.example.mollyapi.user.repository.UserRepository;
import org.example.mollyapi.user.type.Sex;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest
@Transactional
public class TossPaymentServiceImplTest {

    @Autowired
    private PaymentServiceImpl paymentServiceImpl;

    @MockBean
    private PaymentWebClientUtil paymentWebClientUtil;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    private User user;
    private Order order;
    Long orderAmount = 10000L;
    String tossOrderId = "ORD-20250213132349-6572";
    String paymentKey = "PAY-20250213132349-6572";
    LocalDate userRegisterDate = LocalDate.now().minusDays(1);
    LocalDateTime orderDate = LocalDateTime.now().minusDays(1);

    @BeforeEach
    void setUp() {

        // Mock Stubbing
        user = new User(1L, "mo","01051345633", Sex.MALE,true,"www.example.com", userRegisterDate,5000,"Jerry");
        userRepository.save(user);

        // Mock paymentWebClientUtil
        TossConfirmResDto mockResponse = new TossConfirmResDto(
                null,  // mId
                null,  // version
                "tORD-20250213132349-6572",  // paymentKey
                "SUCCESS",  // status
                null,  // lastTransactionKey
                null,  // method
                tossOrderId,  // orderId
                null,  // orderName
                10000L,  // totalAmount
                null,  // card (nullable)
                null   // easyPay (nullable)
        );
    }

    @DisplayName("Toss API 에서 2xx 응답을 리턴합니다. 결제는 APPROVED 상태로 저장됩니다..")
    @Test
    void processPaymentWithToss2xxResponse(){
        //given
        LocalDateTime orderDate = LocalDateTime.now();
        order = new Order(1L,tossOrderId,user,null,null,null, 10000L, 0L,paymentKey,"NORMAL",0,0, OrderStatus.PENDING, CancelStatus.NONE, orderDate, orderDate.plusMinutes(30));
        orderRepository.save(order);

        PaymentConfirmReqDto paymentConfirmReqDto = new PaymentConfirmReqDto(order.getId(), order.getTossOrderId(), order.getPaymentId(), order.getTotalAmount(),order.getPaymentType(),order.getPointUsage());

        ResponseEntity<TossConfirmResDto> response = ResponseEntity
                .status(HttpStatus.CREATED)
                .body(null);
        given(paymentWebClientUtil.confirmPayment(any(), any())).willReturn(response);

        //when
        Payment payment = paymentServiceImpl.processPayment(
                user.getUserId(),
                paymentConfirmReqDto
        );


        //then
        assertThat(payment)
                .extracting(Payment::getPaymentStatus, Payment::getTossOrderId, Payment::getAmount, Payment::getPaymentType)
                .isEqualTo(List.of(PaymentStatus.APPROVED, tossOrderId, order.getTotalAmount(), "NORMAL"));

    }


    @DisplayName("Toss API 에서 4xx 에러를 리턴합니다. 결제는 FAILED 상태로 존재합니다..")
    @Test
    void processPaymentWithToss4xxError() {

        //given
        LocalDateTime orderDate = LocalDateTime.now();
        order = new Order(1L,tossOrderId,user,null,null,null, 10000L, 0L,paymentKey,"NORMAL",0,0, OrderStatus.PENDING, CancelStatus.NONE, orderDate, orderDate.plusMinutes(30));
        orderRepository.save(order);

        PaymentConfirmReqDto paymentConfirmReqDto = new PaymentConfirmReqDto(order.getId(), order.getTossOrderId(), order.getPaymentId(), order.getTotalAmount(),order.getPaymentType(),order.getPointUsage());

        ResponseEntity<TossConfirmResDto> response = ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(null);
        given(paymentWebClientUtil.confirmPayment(any(), any())).willReturn(response);

        //when & then
        assertThatThrownBy(() -> paymentServiceImpl
                .processPayment(user.getUserId(), paymentConfirmReqDto))
                .isInstanceOf(CustomException.class)
                .hasMessage("결제가 실패했습니다. 다시 시도하시겠습니까? (API: /orders/{orderId}/fail-payment)");

    }

    @DisplayName("Toss API 에서 5xx 에러를 리턴합니다. 결제는 PENDING 상태로 생성됩니다.")
    @Test
    void processPaymentWithToss5xxError(){
        //given
        LocalDateTime orderDate = LocalDateTime.now();
        order = new Order(1L,tossOrderId,user,null,null,null, 10000L, 0L,paymentKey,"NORMAL",0,0, OrderStatus.PENDING, CancelStatus.NONE, orderDate, orderDate.plusMinutes(30));
        orderRepository.save(order);

        PaymentConfirmReqDto paymentConfirmReqDto = new PaymentConfirmReqDto(order.getId(), order.getTossOrderId(), order.getPaymentId(), order.getTotalAmount(),order.getPaymentType(),order.getPointUsage());

        ResponseEntity<TossConfirmResDto> response = ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(null);
        given(paymentWebClientUtil.confirmPayment(any(), any())).willReturn(response);
        paymentServiceImpl = new PaymentServiceImpl(paymentRepository, paymentWebClientUtil,userRepository, orderRepository, applicationEventPublisher);

        //when & then
        assertThatThrownBy(() -> paymentServiceImpl
                .processPayment(user.getUserId(), paymentConfirmReqDto))
                .isInstanceOf(RetryablePaymentException.class)
                .hasMessage("서버 내부 오류");

    }
}
