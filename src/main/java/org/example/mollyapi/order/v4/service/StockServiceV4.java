package org.example.mollyapi.order.v4.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mollyapi.common.exception.CustomException;
import org.example.mollyapi.common.exception.error.impl.ProductItemError;
import org.example.mollyapi.common.kafka.KafkaProducer;
import org.example.mollyapi.order.entity.Order;
import org.example.mollyapi.order.entity.OrderDetail;
import org.example.mollyapi.order.v4.event.message.request.StockReserveRequestMessage;
import org.example.mollyapi.order.v4.event.message.suceess.OrderInitiateMessage;
import org.example.mollyapi.order.v4.event.message.suceess.StockReservationCompleteMessage;
import org.example.mollyapi.product.entity.ProductItem;
import org.example.mollyapi.product.repository.ProductItemRepository;
import org.example.mollyapi.user.entity.User;
import org.example.mollyapi.user.service.UserService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockServiceV4 {

    private final KafkaProducer kafkaProducer;
    private final UserService userService;
    private final OrderServiceV4 orderServiceV4;
    private final ProductItemRepository productItemRepository;

    @KafkaListener(topics = {
            "stock.reserve.requested.v1"
    }, groupId = "stock-reserve-consumer-group",
    containerFactory = "stockReserveRequestContainerFactory")
    @Transactional
    public void reserveStock(StockReserveRequestMessage message){

        log.info("StockService : stockReserveRequestMessage has received");

        try{
            User user = userService.findByUser(message.getUserId());
            Order order = orderServiceV4.findByTossOrderIdWithDetails(message.getTossOrderId());
            for (OrderDetail detail : order.getOrderDetails()){
                ProductItem productItem = findByIdWithLock((detail.getProductItem().getId()));
                log.info("{}",detail.getQuantity());
                productItem.decreaseStock(detail.getQuantity());
            }
        } catch (Exception e){
            log.error(e.getMessage());
        }


        log.info("StockService : 2");
        StockReservationCompleteMessage newMessage = StockReservationCompleteMessage.builder()
                .userId(message.getUserId())
                .tossOrderId(message.getTossOrderId())
                .build();


        log.info("StockService : StockReservationCompleteMessage has produced");
        kafkaProducer.produce("stock.reserve.completed.v1", newMessage);
    }


    /**
     *
     * @param id
     * @return
     * Lock을 사용한 재고 조회 서비스 메서드
     */
    @Transactional
    public ProductItem findByIdWithLock(Long id){

        ProductItem productItem = productItemRepository.findByIdWithLock(id)
                .orElseThrow(() -> new CustomException(ProductItemError.NOT_EXISTS_ITEM));

        log.info("productItem name = {}, quantity = {}", productItem.getProduct().getProductName(), productItem.getQuantity());

        return productItem;
    }
}
