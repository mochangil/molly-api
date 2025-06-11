package org.example.mollyapi.order.v4.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mollyapi.common.kafka.KafkaProducer;
import org.example.mollyapi.order.v4.event.message.request.PaymentRequestMessage;
import org.example.mollyapi.order.v4.event.message.suceess.PaymentSuccessMessage;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceV4 {

    private final KafkaProducer kafkaProducer;

    @KafkaListener(topics = {
            "payment.requested.v1"
    }, groupId = "payment-consumer-group")
    public void processPayment(PaymentRequestMessage message){
        PaymentSuccessMessage newMessage = PaymentSuccessMessage.builder()
                .userId(message.getUserId())
                .tossOrderId(message.getTossOrderId())
                .build();
//        kafkaProducer.produce("payment.suceess.v1",newMessage);
    }
}
