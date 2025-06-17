package org.example.mollyapi.order.v4.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mollyapi.common.exception.CustomException;
import org.example.mollyapi.common.kafka.KafkaProducer;
import org.example.mollyapi.order.entity.Order;
import org.example.mollyapi.order.v4.event.message.suceess.OrderInitiateMessage;
import org.example.mollyapi.order.v4.event.message.suceess.PointDeductCompleteMessage;
import org.example.mollyapi.user.entity.User;
import org.example.mollyapi.user.repository.UserRepository;
import org.example.mollyapi.user.service.UserService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointServiceV4 {

    private final KafkaProducer kafkaProducer;
    private final OrderServiceV4 orderServiceV4;
    private final UserService userService;
    private final UserRepository userRepository;

    @Transactional
    @KafkaListener(topics = {
            "order.initiated.v1"
    }, groupId = "point-deduct-consumer-group",
    containerFactory = "orderInitiateContainerFactory")
    public void deductPoint(OrderInitiateMessage message){

        log.info("PointService : OrderInitiateMessage has received");

        User user = userService.findByUser(message.getUserId());
        Order order = orderServiceV4.findByTossOrderId(message.getTossOrderId());
        try {
            user.deductPoint(order.getPointUsage());
        }catch (Exception e){
            e.printStackTrace();
        }

        userRepository.save(user);

        PointDeductCompleteMessage newMessage = PointDeductCompleteMessage.builder()
                .userId(message.getUserId())
                .tossOrderId(message.getTossOrderId())
                .build();

        log.info("PointService : PointDeductCompleteMessage has produced");
        kafkaProducer.produce("point.deduct.completed.v1", newMessage);
    }


}
