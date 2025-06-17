package org.example.mollyapi.order.v4.event.aggregator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mollyapi.common.kafka.KafkaProducer;
import org.example.mollyapi.order.v4.event.message.dlq.OrderValidateDeadLetter;
import org.example.mollyapi.order.v4.event.message.suceess.OrderValidateSuccessMessage;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class OrderFailEventAggregator {

    private final KafkaProducer kafkaProducer;

    @KafkaListener(topics = "order.validated.v1.dlq",
            groupId = "aggregator.dlq",
            containerFactory = ""
    )
    public void handleOrderValidationDlq(OrderValidateDeadLetter<?> message){
        log.error("DLQ received: message={}", message);
    }
}
