package org.example.mollyapi.order.v4.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mollyapi.common.kafka.KafkaProducer;
import org.example.mollyapi.order.v4.event.message.suceess.OrderInitiateMessage;
import org.example.mollyapi.order.v4.event.message.suceess.PointDeductCompleteMessage;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointServiceV4 {

    private final KafkaProducer kafkaProducer;

    @KafkaListener(topics = {
            "order.initiated.v1"
    }, groupId = "point-deduct-consumer-group")
    public void deductStock(OrderInitiateMessage message){
        PointDeductCompleteMessage newMessage = PointDeductCompleteMessage.builder()
                .userId(message.getUserId())
                .tossOrderId(message.getTossOrderId())
                .build();
//        kafkaProducer.produce("point.deduct.completed.v1", newMessage);
    }


}
