package org.example.mollyapi.order.v4.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mollyapi.common.kafka.KafkaProducer;
import org.example.mollyapi.order.v4.event.message.suceess.OrderInitiateMessage;
import org.example.mollyapi.order.v4.event.message.suceess.StockReservationCompleteMessage;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockServiceV4 {

    private final KafkaProducer kafkaProducer;

    @KafkaListener(topics = {
            "order.initiated.v1"
    }, groupId = "stock-reserve-consumer-group")
    public void reserveStock(OrderInitiateMessage message){
        StockReservationCompleteMessage newMessage = StockReservationCompleteMessage.builder()
                .userId(message.getUserId())
                .tossOrderId(message.getTossOrderId())
                .build();

        // some logics..

//        kafkaProducer.produce("stock.reserve.completed.v1", newMessage);
    }
}
