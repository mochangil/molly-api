package org.example.mollyapi.order.service;

import lombok.RequiredArgsConstructor;
import org.example.mollyapi.common.exception.CustomException;
import org.example.mollyapi.common.exception.error.impl.PaymentError;
import org.example.mollyapi.delivery.dto.DeliveryReqDto;
import org.example.mollyapi.order.entity.Order;
import org.example.mollyapi.order.event.V3.event.order.OrderInitiateEvent;
import org.example.mollyapi.order.event.V3.event.order.OrderPostProcessEvent;
import org.example.mollyapi.order.event.V3.event.order.OrderPreProcessEvent;
import org.example.mollyapi.order.event.V3.event.order.OrderProcessEvent;
import org.example.mollyapi.order.repository.OrderRepository;
import org.example.mollyapi.order.type.OrderStatus;
import org.example.mollyapi.payment.dto.request.PaymentConfirmReqDto;
import org.example.mollyapi.payment.dto.response.PaymentResDto;
import org.example.mollyapi.payment.entity.Payment;
import org.example.mollyapi.payment.repository.PaymentRepository;
import org.example.mollyapi.payment.util.AESUtil;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class OrderProcessServiceV3 implements OrderProcessService {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher publisher;
    private final PaymentRepository paymentRepository;

    public PaymentResDto initiateOrder(Long userId, String paymentKey, String tossOrderId, Long amount, String point, String paymentType, DeliveryReqDto deliveryInfo) {
        // 주문 시작
        handleOrderInitiateEvent(new OrderInitiateEvent(userId, paymentKey, tossOrderId, amount, paymentType, point, deliveryInfo));
        return null;
    }


    public void handleOrderInitiateEvent(OrderInitiateEvent event) {
        //order 검증 및 order에 정보 넣기
        //delivery, point,
        System.out.println("Order Initiate Event: " + event.tossOrderId());
        CompletableFuture<Void> future = publishEvent(event);
        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
            }else{
                handleOrderPreProcessEvent(event.userId(), new OrderPreProcessEvent(event.tossOrderId()));
            }
        });

    }

    public void handleOrderPreProcessEvent(Long userId, OrderPreProcessEvent event) {
        System.out.println("Order Pre-Process Event: " + event.tossOrderId());
        CompletableFuture<Void> future = publishEvent(event);
        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
            }else{
                Order order = orderRepository.findByTossOrderId(event.tossOrderId())
                        .orElseThrow((() -> new CustomException(PaymentError.ORDER_NOT_FOUND)));
                PaymentConfirmReqDto paymentConfirmReqDto = getPaymentConfirmReqDto(order);
                handleOrderProcessEvent(new OrderProcessEvent(userId, paymentConfirmReqDto));
            }
        });

    }

    public void handleOrderProcessEvent(OrderProcessEvent event) {
        // 주문 처리
        // 이벤트 처리 후, 마지막 후처리 이벤트 발행
        CompletableFuture<Void> future = publishEvent(event);
        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
            }else{
                handleOrderPostProcessEvent(new OrderPostProcessEvent(event.paymentConfirmReqDto().tossOrderId()));
            }
        });
    }

    public void handleOrderPostProcessEvent(OrderPostProcessEvent event) {
        // 주문 후처리
        System.out.println("Order Post-Process Event: " + event.tossOrderId());
    }


    private PaymentConfirmReqDto getPaymentConfirmReqDto(Order order) {
        return new PaymentConfirmReqDto(
//                order.getId(),
                order.getTossOrderId(),
                order.getPaymentId(),
                order.getTotalAmount(),
                order.getPaymentType(),
                order.getPointUsage()
        );
    }

    private CompletableFuture<Void> publishEvent(Object event) {
        return CompletableFuture.supplyAsync(() -> {
            publisher.publishEvent(event);
            return null;
        });
    }

    @Override
    public void validateOrder(OrderInitiateEvent event, Order order) {
        ///  재시도되는 주문 = CANCELED
        if (!(order.isPending() || order.isCanceled())) { throw new IllegalArgumentException("결제 시도는 PENDING 상태에서만 가능합니다.");}
        ///  만료 검증
        order.validateExpiration();
    }

    @Override
    public void addOrderInfo(OrderInitiateEvent event, Order order) {
//        Integer pointUsage = Optional.of(AESUtil.decryptWithSalt(event.point())

        order.addOrderInfo(
                event.paymentKey(),
                event.amount(),
                event.paymentType()
        );
    }

    @Override
    public void processOrder(String tossOrderId, Order order) {
    }

    @Transactional
    public void successOrder(String tossOrderId, String paymentKey) {
        System.out.println(tossOrderId);
        Order order = orderRepository.findByTossOrderId(tossOrderId)
                .orElseThrow( () -> new CustomException(PaymentError.ORDER_NOT_FOUND));
        Payment payment = paymentRepository.findByPaymentKey(paymentKey)
                .orElseThrow( () -> new CustomException(PaymentError.PAYMENT_NOT_FOUND));
        order.updateStatus(OrderStatus.SUCCEEDED);
        order.addPayment(payment);
        orderRepository.save(order);
    }
}
