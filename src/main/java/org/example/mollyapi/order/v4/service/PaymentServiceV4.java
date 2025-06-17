package org.example.mollyapi.order.v4.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mollyapi.common.kafka.KafkaProducer;
import org.example.mollyapi.order.entity.Order;
import org.example.mollyapi.order.v4.event.message.fail.PaymentFailedMessage;
import org.example.mollyapi.order.v4.event.message.request.PaymentRequestMessage;
import org.example.mollyapi.order.v4.event.message.suceess.PaymentSuccessMessage;
import org.example.mollyapi.payment.dto.request.TossConfirmReqDto;
import org.example.mollyapi.payment.dto.response.TossConfirmResDto;
import org.example.mollyapi.payment.entity.Payment;
import org.example.mollyapi.payment.repository.PaymentRepository;
import org.example.mollyapi.payment.util.PaymentWebClientUtil;
import org.example.mollyapi.user.entity.User;
import org.example.mollyapi.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceV4 {

    private final KafkaProducer kafkaProducer;
    private final UserService userService;
    private final OrderServiceV4 orderServiceV4;
    private final PaymentWebClientUtil paymentWebClientUtil;
    private final PaymentRepository paymentRepository;

    @Value("${secret.payment-api-key}")
    private String apiKey;

    @KafkaListener(topics = {
            "payment.requested.v1"
    }, groupId = "payment-consumer-group",
    containerFactory = "paymentRequestContainerFactory")
    public void processPayment(PaymentRequestMessage message){
        log.info("PaymentService : PaymentRequestMessage has received");

        userService.validUser(message.getUserId());
        User user = userService.findByUser(message.getUserId());
        Order order = orderServiceV4.findByTossOrderId(message.getTossOrderId());

        Payment payment = Payment.create(user, order, message.getTossOrderId(), order.getPaymentId(), order.getPaymentType(), order.getTotalAmount());


        // todo - blocking 발생, 전환하기
//        ResponseEntity<TossConfirmResDto> response = tossPaymentApi(new TossConfirmReqDto(
//                order.getPaymentId(),
//                order.getTossOrderId(),
//                order.getTotalAmount()
//        ));

        // mocking response
        ResponseEntity<TossConfirmResDto> response = mockTossPaymentApi(new TossConfirmReqDto(
                order.getPaymentId(),
                order.getTossOrderId(),
                order.getTotalAmount()
        ));

        switch (getStatusCodeToString(response)){
            case "200" -> {
                payment.approvePayment();
                paymentRepository.save(payment);

                PaymentSuccessMessage paymentSuccessMessage = PaymentSuccessMessage.builder()
                        .userId(message.getUserId())
                        .tossOrderId(message.getTossOrderId())
                        .build();
                kafkaProducer.produce("payment.success.v1", paymentSuccessMessage);
            }
            case "400" -> {
                payment.failPayment("결제 실패");
                paymentRepository.save(payment);

                // todo - 실패 메시지 추출하기
                PaymentFailedMessage paymentSuccessMessage = PaymentFailedMessage.builder()
                                .userId(message.getUserId())
                                        .tossOrderId(message.getTossOrderId())
                                                .failMessage("결제 실패")
                                                        .build();
//                kafkaProducer.produce("payment.failed.v1", paymentSuccessMessage);
            }
            case "500" -> {
                payment.pendingPayment();
                paymentRepository.save(payment);

                PaymentFailedMessage paymentSuccessMessage = PaymentFailedMessage.builder()
                        .userId(message.getUserId())
                        .tossOrderId(message.getTossOrderId())
                        .failMessage("결제 실패")
                        .build();
//                kafkaProducer.produce("payment.failed.v1", paymentSuccessMessage);
            }
        }
    }




    /**
     *
     * @param tossConfirmReqDto
     * @return
     *
     */
    private ResponseEntity<TossConfirmResDto> tossPaymentApi(TossConfirmReqDto tossConfirmReqDto) {
        System.out.println("tossConfirmReqDto.paymentKey = " + tossConfirmReqDto.paymentKey());
        return paymentWebClientUtil.confirmPayment(tossConfirmReqDto, apiKey);
    }

    /**
     *
     * @param tossConfirmReqDto
     * @return
     */
    private ResponseEntity<TossConfirmResDto> mockTossPaymentApi(TossConfirmReqDto tossConfirmReqDto) {
        return new ResponseEntity<>(HttpStatus.CREATED);
    }
    /**
     *
     * @param response
     * @return
     * @param <T>
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


    private void handleFailure(){

    }
}
