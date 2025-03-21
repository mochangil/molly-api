package org.example.mollyapi.payment.service.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mollyapi.common.exception.CustomException;
import org.example.mollyapi.common.exception.error.impl.OrderError;
import org.example.mollyapi.common.exception.error.impl.PaymentError;
import org.example.mollyapi.common.exception.error.impl.UserError;
import org.example.mollyapi.order.entity.Order;
import org.example.mollyapi.order.repository.OrderRepository;
import org.example.mollyapi.payment.dto.request.*;
import org.example.mollyapi.payment.dto.request.PaymentCancelReqDto;
import org.example.mollyapi.payment.dto.request.TossCancelReqDto;
import org.example.mollyapi.payment.dto.request.TossConfirmReqDto;
import org.example.mollyapi.payment.dto.response.PaymentInfoResDto;
import org.example.mollyapi.payment.dto.response.TossCancelResDto;
import org.example.mollyapi.payment.dto.response.TossConfirmResDto;
import org.example.mollyapi.payment.entity.Payment;
import org.example.mollyapi.payment.event.event.PaymentApprovedEvent;
import org.example.mollyapi.payment.event.event.PaymentFailedEvent;
import org.example.mollyapi.payment.exception.RetryablePaymentException;
import org.example.mollyapi.payment.repository.PaymentRepository;
import org.example.mollyapi.payment.service.PaymentService;
import org.example.mollyapi.payment.type.PaymentStatus;
import org.example.mollyapi.payment.util.PaymentWebClientUtil;
import org.example.mollyapi.user.entity.User;
import org.example.mollyapi.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentWebClientUtil paymentWebClientUtil;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher publisher;

    @Value("${secret.payment-api-key}")
    private String apiKey;

    @Override
    public Payment findPaymentByPaymentKey(String paymentKey) {
        return paymentRepository.findByPaymentKey(paymentKey)
                .orElseThrow(() -> new CustomException(PaymentError.PAYMENT_NOT_FOUND));
    }

    @Override
    public Optional<PaymentInfoResDto> findLatestPayment(Long orderId) {
        Pageable pageable = PageRequest.of(0, 1);
        Optional<PaymentInfoResDto> paymentInfoResDto = paymentRepository.findLatestPaymentByOrderId(orderId, pageable).stream()
                .findFirst()
                .map(PaymentInfoResDto::from);
        log.info("findLatestPayment {}", paymentInfoResDto);
        return paymentInfoResDto;
    }

    @Override
    public List<PaymentInfoResDto> findUserPayments(Long userId) {
        return paymentRepository.findAllByUserId(userId)
                .orElseThrow(() -> new CustomException(PaymentError.PAYMENT_NOT_FOUND))
                .stream()
                .map(PaymentInfoResDto::from)
                .collect(Collectors.toList());
    }

    public void isApprovedPayment(String tossOrderID) {
        if (paymentRepository.existsByTossOrderIdAndPaymentStatus(tossOrderID, PaymentStatus.APPROVED)){
            throw new CustomException(PaymentError.PAYMENT_ALREADY_PROCESSED);
        }
    }

    /*
        결제 요청 실행 (API 호출 및 결제 데이터 저장)
     */
    @Transactional
    public Payment processPayment(Long userId,
                                  PaymentConfirmReqDto requestDto) {

        ///  승인된 주문인지 확인
        isApprovedPayment(requestDto.tossOrderId());

        // 1. 결제 엔티티 생성
        Payment payment = createPayment(userId, requestDto.orderId(), requestDto.tossOrderId(), requestDto.paymentKey(), requestDto.paymentType(), requestDto.amount());
        paymentRepository.save(payment);

        // 2. toss payments API 호출 (멱등성 헤더 추가하기)
        ResponseEntity<TossConfirmResDto> response = tossPaymentApi(new TossConfirmReqDto(requestDto.tossOrderId(),
                requestDto.paymentKey(),
                requestDto.amount()));

        // 3. 응답 검증
        // pending -> 자동 재시도, fail -> 수동 재시도, approve -> 완료
        switch (getStatusCodeToString(response)) {
            case "200" -> {
                PaymentApprovedEvent event = new PaymentApprovedEvent(requestDto.tossOrderId(),payment.getPaymentKey());
                publisher.publishEvent(event);
//                payment.successPayment();
            }
            case "400" -> {
//                payment.failPayment("결제 실패");
                publisher.publishEvent(new PaymentFailedEvent(payment.getId(),PaymentError.PAYMENT_AMOUNT_MISMATCH));
            }
            case "500" -> {
//                payment.pendingPayment();
                publisher.publishEvent(new PaymentFailedEvent(payment.getId(),PaymentError.PAYMENT_GATEWAY_ERROR));
            }
        }
        return payment;
    }

    public void approvePayment(String paymentKey) {
        Payment payment = paymentRepository.findByPaymentKey(paymentKey)
                .orElseThrow(() -> new CustomException(PaymentError.PAYMENT_NOT_FOUND));

        payment.approvePayment();

        paymentRepository.save(payment);
    }

    public void failPayment(Long paymentId, PaymentError paymentError) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(PaymentError.PAYMENT_NOT_FOUND));

        payment.failPayment(paymentError.getMessage());

        paymentRepository.save(payment);
    }

    @Retryable(
            include = {RuntimeException.class},
//            exclude = {CustomException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public Payment processPaymentTest(Long userId,
                                  PaymentConfirmReqDto requestDto, String status) {
        System.out.println("----------------------------------결제 트랜잭션 시작----------------------------------");
        // 1. 결제 엔티티 생성
        Payment payment = createOrGetPaymentTest(userId, requestDto.orderId(), requestDto.tossOrderId(), requestDto.paymentKey(), requestDto.paymentType(), requestDto.amount());

        //jmeter 테스트 시 정한 상태 값에 따라 동적으로 변경
        if(status.equals("SUCCESS")) {
            status = "200";
        } else if(status.equals("FAIL")){
            status = "400";
        } else {
            status = "500";
        }

        // 3. 응답 검증
        // pending -> 자동 재시도, fail -> 수동 재시도, approve -> 완료
        switch (status) {
            case "200" -> payment.approvePayment();
            case "400" -> {
                log.info("status 400");
                payment.failPayment("결제 실패");
                throw new CustomException(OrderError.PAYMENT_RETRY_REQUIRED);
            }
            case "500" -> {
                payment.pendingPayment();
                throw new RetryablePaymentException("서버 내부 오류");
            }
        }
        paymentRepository.save(payment);
        log.info("processPayment = {}", payment);

        System.out.println("----------------------------------결제 트랜잭션 종료----------------------------------");
        return payment;
    }

    /*
        결제 요청 생성
     */
    @Override
    public Payment createPayment(Long userId, Long orderId, String tossOrderId,
                                 String paymentKey, String paymentType, Long amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserError.NOT_EXISTS_USER));
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(PaymentError.ORDER_NOT_FOUND));

        return Payment.create(user, order, tossOrderId, paymentKey, paymentType, amount);
    }

    public Payment createOrGetPaymentTest(Long userId, Long orderId, String tossOrderId,
                                      String paymentKey, String paymentType, Long amount) {
        log.info("createOrGetPayment 실행");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserError.NOT_EXISTS_USER));
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(PaymentError.ORDER_NOT_FOUND));

        Optional<Payment> existingPayment = paymentRepository.findByTossOrderId(tossOrderId);
        if (existingPayment.isPresent()){
            Payment payment = existingPayment.get();
//            payment.increaseRetryCount();
            switch (payment.getStatus()) {
                case APPROVED -> {
                    throw new CustomException(PaymentError.PAYMENT_ALREADY_PROCESSED);
                }
                case PENDING, FAILED -> {
                    return payment;
                }
            }
        }
        return Payment.create(user, order, tossOrderId, paymentKey, paymentType, amount);
    }

    /*
        Toss 결제 요청 API 호출 (결제 승인)
     */
    private ResponseEntity<TossConfirmResDto> tossPaymentApi(TossConfirmReqDto tossConfirmReqDto) {
        return paymentWebClientUtil.confirmPayment(tossConfirmReqDto, apiKey);
    }

    /*
        결제 취소
     */
    @Transactional
    public boolean cancelPayment(Long userId, PaymentCancelReqDto paymentCancelReqDto, PaymentStatus paymentStatus) {
        Payment payment = findPaymentByPaymentKey(paymentCancelReqDto.paymentKey());
        TossCancelReqDto tossCancelReqDto = new TossCancelReqDto(paymentCancelReqDto.cancelReason(), paymentCancelReqDto.cancelAmount());

        ResponseEntity<TossCancelResDto> response = tossPaymentCancelApi(tossCancelReqDto, paymentCancelReqDto.paymentKey());

        boolean res = validateResponse(response);
        if (res) {
            payment.cancelPayment();
        }

        return res;
    }

    /*
        Toss 결제 취소 API 호출
     */
    public ResponseEntity<TossCancelResDto> tossPaymentCancelApi(TossCancelReqDto tossCancelReqDto, String paymentKey) {
        return ResponseEntity.ok(paymentWebClientUtil.cancelPayment(tossCancelReqDto, apiKey, paymentKey));
    }

    @Transactional
    public Payment retryPayment(Long userId, String tossOrderId, String paymentKey) {
//        Payment payment = paymentRepository.findTopLatestPaymentByOrderId(tossOrderId)
//                .orElseThrow(() -> new CustomException(PaymentError.PAYMENT_NOT_FOUND));
        Payment payment = findPaymentByPaymentKey(paymentKey);

        // 기존 결제 정보를 기반으로 새로운 결제 요청 생성
        PaymentConfirmReqDto retryRequest = new PaymentConfirmReqDto(
                payment.getOrder().getId(),
                payment.getTossOrderId(),
                payment.getPaymentKey(),
                payment.getAmount(),
                payment.getPaymentType(),
                0// 포인트는 이미 차감되었으므로 0으로 설정 -> 결제 서비스에서 포인트차감이 아님
        );
        processPayment(userId, retryRequest);
        if (payment.getPaymentStatus() == PaymentStatus.PENDING) {
            throw new RetryablePaymentException("결제서버 내부 오류. 재시도를 수행할 수 있습니다.");
        }
        return payment;
    }

    /*
        HTTP 응답 검증
     */
    private <T> String getStatusCodeToString(ResponseEntity<T> response) {
        HttpStatusCode statusCode = response.getStatusCode();
        int statusValue = statusCode.value(); // 상태 코드 정수값 가져오기

        if (statusValue >= 200 && statusValue < 300) {
            return "200"; // 모든 2xx 응답을 200으로 변환
        } else if (statusValue >= 400 && statusValue < 500) {
            return "400"; // 모든 4xx 응답을 400으로 변환
        } else if (statusValue >= 500 && statusValue < 600) {
            return "500";
        }
        return String.valueOf(statusValue); // 1xx, 3xx 등은 원래 값 유지
    }

    private <T> boolean validateResponse(ResponseEntity<T> response) {
        return response.getStatusCode().is2xxSuccessful() && response.getBody() != null;
    }



    /*
        결제 취소
     */
    public boolean cancelPayment(Long userId, PaymentCancelReqDto paymentCancelReqDto) {

        //Payment 있는지 확인
        Payment payment = findPaymentByPaymentKey(paymentCancelReqDto.paymentKey());

        //Toss request 객체로 변환
        TossCancelReqDto tossCancelReqDto = new TossCancelReqDto(paymentCancelReqDto.cancelReason(), paymentCancelReqDto.cancelAmount());

        //tossApi 호출
        ResponseEntity<TossCancelResDto> response = tossPaymentCancelApi(tossCancelReqDto, paymentCancelReqDto.paymentKey());

        // response 정합성 검사
        boolean res = validateResponse(response);

        // body 추출
        TossCancelResDto tossResDto = response.getBody();

        // 취소 성공 로직 (payment 상태 canceled 로 변경)
        if (res) {
            payment.cancelPayment();
        }

        // 성공 여부 리턴
        return res;
    }

}