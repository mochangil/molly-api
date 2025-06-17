package org.example.mollyapi.order.v4.event.aggregator;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mollyapi.common.kafka.KafkaProducer;
import org.example.mollyapi.order.v4.event.message.request.PaymentRequestMessage;
import org.example.mollyapi.order.v4.event.message.request.StockReserveRequestMessage;
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
        log.info("OrderEventAggregator : OrderValidateMessage has received");
        try {
            OrderInitiateMessage newMessage = OrderInitiateMessage.builder()
                    .userId(message.getUserId())
                    .tossOrderId(message.getTossOrderId())
                    .build();

            kafkaProducer.produce("order.initiated.v1", newMessage);
        } catch (Exception e){
            log.error("Failed to validate for tossOrderId: {}", message.getTossOrderId(),e);
        }
    }


    // todo - stockReserveMessage 생성, pointDeductedContianerFactory 생성
    @KafkaListener(topics = {
            "point.deduct.completed.v1"
    },  groupId = "aggregator",
    containerFactory = "pointDeductedContainerFactory")
    public void stockReserveRequest(PointDeductCompleteMessage message){
        log.info("OrderEventAggregator : PointDeductMessage has received");
        StockReserveRequestMessage newMessage = StockReserveRequestMessage.builder()
                .userId(message.getUserId())
                .tossOrderId(message.getTossOrderId())
                .build();
        kafkaProducer.produce("stock.reserve.requested.v1",newMessage);
    }

    @KafkaListener(topics = {
            "stock.reserve.completed.v1"
    },  groupId = "aggregator",
            containerFactory = "stockReservedContainerFactory")
    public void paymentRequest(StockReservationCompleteMessage message){
        log.info("OrderEventAggregator : StockReservedMessage has received");
        PaymentRequestMessage newMessage = PaymentRequestMessage.builder()
                .userId(message.getUserId())
                .tossOrderId(message.getTossOrderId())
                .build();
        kafkaProducer.produce("payment.requested.v1",newMessage);
    }

    @KafkaListener(topics = {
            "payment.success.v1"
    }, groupId = "aggregator",
    containerFactory = "paymentSuccessContainerFactory")
    public void postOrderProcess(PaymentSuccessMessage message){
        log.info("OrderEventAggregator : PaymentSuccessMessage has received");
        OrderCompletedMessage newMessage = OrderCompletedMessage.builder()
                .tossOrderId(message.getTossOrderId())
                .build();
        kafkaProducer.produce("order.completed.v1",newMessage);
    }
}
