package org.example.mollyapi.order.v4.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mollyapi.address.dto.AddressResponseDto;
import org.example.mollyapi.address.repository.AddressRepository;
import org.example.mollyapi.cart.entity.Cart;
import org.example.mollyapi.cart.repository.CartRepository;
import org.example.mollyapi.common.exception.CustomException;
import org.example.mollyapi.common.exception.error.impl.OrderError;
import org.example.mollyapi.common.exception.error.impl.UserError;
import org.example.mollyapi.common.kafka.KafkaProducer;
import org.example.mollyapi.order.dto.OrderConfirmRequestDto;
import org.example.mollyapi.order.entity.Order;
import org.example.mollyapi.order.entity.OrderDetail;
import org.example.mollyapi.order.repository.OrderDetailRepository;
import org.example.mollyapi.order.repository.OrderRepository;
import org.example.mollyapi.order.v4.dto.request.OrderRequestDto;
import org.example.mollyapi.order.v4.dto.response.OrderResponseDto;
import org.example.mollyapi.order.v4.event.message.fail.OrderValidateFailMessage;
import org.example.mollyapi.order.v4.event.message.suceess.OrderValidateSuccessMessage;
import org.example.mollyapi.order.v4.redis.RedisService;
import org.example.mollyapi.product.entity.ProductItem;
import org.example.mollyapi.product.repository.ProductItemRepository;
import org.example.mollyapi.user.entity.User;
import org.example.mollyapi.user.repository.UserRepository;
import org.example.mollyapi.user.service.UserService;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceV4 {

    private final KafkaProducer kafkaProducer;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final AddressRepository addressRepository;
    private final CartRepository cartRepository;
    private final ProductItemRepository productItemRepository;
    private final UserService userService;
    private final RedisService redisService;

    public OrderResponseDto createOrder(Long userId, List<OrderRequestDto> orderRequests){

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserError.NOT_EXISTS_USER));

        String tossOrderId = generateUniqueTossOrderId();

        Order order = new Order(user, tossOrderId);
        List<OrderDetail> orderDetails = orderRequests.stream()
                .map(req -> createOrderDetail(order, req))
                .toList();

        orderRepository.save(order);
        orderDetailRepository.saveAll(orderDetails);

        order.updateTotalAmount(
                orderDetails.stream()
                        .mapToLong(OrderDetail::getPrice)
                        .sum()
        );

        AddressResponseDto defaultAddress = addressRepository.findByUserAndDefaultAddr(user, true)
                .map(AddressResponseDto::from)
                .orElse(null);

        return OrderResponseDto.from(order, orderDetails, user.getPoint(), defaultAddress);
    }


    private String generateUniqueTossOrderId() {

        String tossOrderId;
        do {
            tossOrderId = "ORD-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-" + new Random().nextInt(9000);
        } while(orderRepository.existsByTossOrderId(tossOrderId));

        return tossOrderId;
    }


    /**
     * 주문 상세 생성 - 장바구니 주문, 바로 주문 구분
     */
    public OrderDetail createOrderDetail(Order order, OrderRequestDto req) {
        Cart cart = null;
        if (req.cartId() != null) {
            cart = cartRepository.findById(req.cartId())
                    .orElseThrow(() -> new IllegalArgumentException("장바구니 항목을 찾을 수 없습니다. cartId=" + req.cartId()));
        }

        Long itemId = (cart != null) ? cart.getProductItem().getId() : req.itemId();
        Long quantity = (cart != null) ? cart.getQuantity() : req.quantity();

        // itemId 및 quantity 검증
        if (itemId == null) {
            throw new IllegalArgumentException("상품 ID가 존재하지 않습니다.");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("주문 수량은 1개 이상이어야 합니다.");
        }

        ProductItem productItem = productItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다. itemId=" + itemId));

//        // 중복 주문 방지: 같은 상품을 이미 주문했는지 체크
//        long existingOrderCount = orderRepository.countByUserAndProductItem(order.getUser(), productItem.getId());
//        if (existingOrderCount > 0) {
//            throw new IllegalArgumentException("이미 동일 상품에 대한 주문이 진행 중입니다.");
//        }

        // 재고 조회(차감 X)
        if (productItem.getQuantity() < quantity) {
            throw new IllegalArgumentException("재고가 부족하여 주문할 수 없습니다. itemId=" + itemId);
        }

        return new OrderDetail( // create method
                order,
                productItem,
                productItem.getSize(),
                productItem.getProduct().getPrice(),
                quantity,
                productItem.getProduct().getBrandName(),
                productItem.getProduct().getProductName(),
                req.cartId()
        );
    }

    public void initiateOrder(Long userId, OrderConfirmRequestDto req) {

        // 정보 저장 (주문정보, 배송정보 등)
//        redisService.save(req);

        // 1. 사용자 검증
        try{userService.validUser(userId);}
        catch (CustomException e){
            handleFailure(req.tossOrderId(),e.getMessage());
        }

        // 2. 주문 검증
        Order order = orderRepository.findByTossOrderIdWithDetails(req.tossOrderId())
                .orElse(null);

        System.out.println(order.getId());
        if(order == null){
            handleFailure(req.tossOrderId(), OrderError.NOT_EXIST_ORDER.getMessage());
        }

        // 2-1. 주문의 상태 검증
        if(!order.isPending()) {
            handleFailure(req.tossOrderId(), OrderError.INVALID_ORDER.getMessage());
        };

        // 2-2. 주문 만료시간 검증
        if(order.isExpired()){
            handleFailure(req.tossOrderId(), OrderError.EXPIRED_ORDER.getMessage());
        }

        OrderValidateSuccessMessage message = OrderValidateSuccessMessage.builder()
                        .tossOrderId(req.tossOrderId())
                                .build();

        kafkaProducer.produce("order.validated.v1", message);
        log.info("produced = {}", message.getTossOrderId());
    }

    public void handleFailure(String tossOrderId, String failMessage){
        log.error(failMessage);
        OrderValidateFailMessage message =
                OrderValidateFailMessage.builder()
                        .tossOrderId(tossOrderId)
                        .failMessage(failMessage)
                        .build();
//        kafkaProducer.produce("order.validate.failed.v1",message);
    }


    public void rollbackOrder(String tossOrderId){

    }





    /**
     * 주문이 시작가능한 상태인지 검증
     */
    public void validateInitiateOrder(Order order){


    }


}
