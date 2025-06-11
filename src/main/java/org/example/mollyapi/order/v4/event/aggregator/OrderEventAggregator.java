package org.example.mollyapi.order.v4.event.aggregator;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mollyapi.common.kafka.KafkaProducer;
import org.example.mollyapi.order.v4.event.message.request.PaymentRequestMessage;
import org.example.mollyapi.order.v4.event.message.suceess.*;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;


@Slf4j
@RequiredArgsConstructor
@Service
public class OrderEventAggregator {

    /// todo - kafka streams 사용해서 aggregate하기

    private final KafkaProducer kafkaProducer;

    @KafkaListener(topics = "order.validated.v1",
            groupId = "aggregator",
            containerFactory = "orderValidatedContainerFactory")
    public void initiateOrder(OrderValidateSuccessMessage message){
        log.info("received");
        try {
            OrderInitiateMessage newMessage = OrderInitiateMessage.builder()
                    .tossOrderId(message.getTossOrderId())
                    .build();

            kafkaProducer.produce("order.initiated.v1",newMessage);
        } catch (Exception e){
            log.error("Failed to validate for tossOrderId: {}", message.getTossOrderId(),e);
        }
    }

    @KafkaListener(topics = {
            "stock.reserve.completed.v1",
            "point.deduct.completed.v1"
    },  groupId = "aggregator")
    public void paymentRequest(PointDeductCompleteMessage message){
        PaymentRequestMessage newMessage = PaymentRequestMessage.builder()
                .tossOrderId(message.getTossOrderId())
                .build();
        kafkaProducer.produce("payment.requested.v1",newMessage);
    }

    @KafkaListener(topics = {
            "payment.success.v1"
    }, groupId = "aggregator")
    public void postOrderProcess(PaymentSuccessMessage message){
        OrderCompletedMessage newMessage = OrderCompletedMessage.builder()
                .tossOrderId(message.getTossOrderId())
                .build();
        kafkaProducer.produce("order.completed.v1",newMessage);
    }
}
