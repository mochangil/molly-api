package org.example.mollyapi.order.v4.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mollyapi.order.v4.event.message.suceess.OrderCompletedMessage;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryServiceV4 {

    @KafkaListener(topics = {
            "order.completed.v1"
    }, groupId = "delivery-process-group",
    containerFactory = "orderCompletedContainerFactory")
    public void deliveryProcess(OrderCompletedMessage message){
        log.info("DeliveryService : OrderCompletedMessage has received");
    }
}
