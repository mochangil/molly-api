package org.example.mollyapi.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mollyapi.common.exception.CustomException;
import org.example.mollyapi.common.exception.error.impl.OrderError;
import org.example.mollyapi.common.exception.error.impl.PaymentError;
import org.example.mollyapi.delivery.dto.DeliveryReqDto;
import org.example.mollyapi.order.event.V2.*;
import org.example.mollyapi.order.entity.Order;
import org.example.mollyapi.order.event.V2.event.order.OrderInitiateEvent;
import org.example.mollyapi.order.event.V2.event.order.OrderPostProcessEvent;
import org.example.mollyapi.order.event.V2.event.order.OrderPreProcessEvent;
import org.example.mollyapi.order.event.V2.event.order.OrderProcessEvent;
import org.example.mollyapi.order.event.V2.handler.product.StockEventHandler;
import org.example.mollyapi.order.repository.OrderRepository;
import org.example.mollyapi.order.type.OrderStatus;
import org.example.mollyapi.payment.dto.request.PaymentConfirmReqDto;
import org.example.mollyapi.payment.dto.response.PaymentResDto;
import org.example.mollyapi.payment.entity.Payment;
import org.example.mollyapi.payment.event.handler.PaymentEventHandler;
import org.example.mollyapi.payment.repository.PaymentRepository;
import org.example.mollyapi.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProcessServiceV2 {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderEventBlockingQueue orderEventBlockingQueue;
    private final OrderEventBlockingQueueV2 orderEventBlockingQueueV2;
    private final PaymentEventHandler paymentEventHandler;
    private final EventFutureRegistry eventFutureRegistry;

    // 후보 1
    public PaymentResDto processOrder(Long userId, String paymentKey, String tossOrderId, Long amount, String point, String paymentType, DeliveryReqDto deliveryInfo) {

        CompletableFuture<Void> orderInitiateFuture = eventFutureRegistry.registerFuture(EventFutureType.INITIATE_ORDER, tossOrderId);
        orderEventBlockingQueue.publishEvent(new OrderInitiateEvent(tossOrderId));
        try{
            orderInitiateFuture.get(40, TimeUnit.SECONDS);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
            throw new CustomException(OrderError.PAYMENT_RETRY_REQUIRED);
        }

        //payment 결과 future 등록
        //stock, point
        CompletableFuture<Void> stockFuture = eventFutureRegistry.registerFuture(EventFutureType.STOCK, tossOrderId);
        CompletableFuture<Void> pointFuture = eventFutureRegistry.registerFuture(EventFutureType.POINT, tossOrderId);
        orderEventBlockingQueue.publishEvent(new OrderPreProcessEvent(userId, tossOrderId, point));
        try{
            stockFuture.get(40, TimeUnit.SECONDS);
            pointFuture.get(40, TimeUnit.SECONDS);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
            throw new CustomException(OrderError.PAYMENT_RETRY_REQUIRED);
        }

        //cart, delivery, payment
        PaymentConfirmReqDto paymentConfirmReqDto = getPaymentConfirmReqDto(tossOrderId,paymentKey,amount, paymentType);
        CompletableFuture<PaymentResDto> paymentFuture = paymentEventHandler.registerPaymentFuture(tossOrderId);
        orderEventBlockingQueue.publishEvent(new OrderProcessEvent(userId, tossOrderId, deliveryInfo, paymentConfirmReqDto));
        orderEventBlockingQueue.publishEvent(new OrderPostProcessEvent(tossOrderId));
        try {
            PaymentResDto paymentResDto = paymentFuture.get(40, TimeUnit.SECONDS);
            if(paymentResDto.paymentStatus().equals("결제승인")){
                return paymentResDto;
            }else {
                throw new CustomException(OrderError.PAYMENT_RETRY_REQUIRED);
            }
        } catch (TimeoutException e){
            System.out.println("결제 처리 시간이 초과되었습니다.");
            // paymentFailed 보상트랜잭션
            throw new CustomException(PaymentError.PAYMENT_FAILED);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            throw new CustomException(PaymentError.PAYMENT_FAILED);
        }
    }



    public PaymentResDto processOrder2(Long userId, String paymentKey, String tossOrderId, Long amount, String point, String paymentType, DeliveryReqDto deliveryInfo) {

        CompletableFuture<Void> orderInitiateFuture = eventFutureRegistry.registerFuture(EventFutureType.INITIATE_ORDER, tossOrderId);
        orderEventBlockingQueueV2.publishEvent(new OrderInitiateEvent(tossOrderId));
        try{
            orderInitiateFuture.get(40, TimeUnit.SECONDS);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
            throw new CustomException(OrderError.PAYMENT_RETRY_REQUIRED);
        }

        //payment 결과 future 등록
        //stock, point
        CompletableFuture<Void> stockFuture = eventFutureRegistry.registerFuture(EventFutureType.STOCK, tossOrderId);
        CompletableFuture<Void> pointFuture = eventFutureRegistry.registerFuture(EventFutureType.POINT, tossOrderId);
        orderEventBlockingQueueV2.publishEvent(new OrderPreProcessEvent(userId, tossOrderId, point));
        try{
            stockFuture.get(40, TimeUnit.SECONDS);
            pointFuture.get(40, TimeUnit.SECONDS);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
            throw new CustomException(OrderError.PAYMENT_RETRY_REQUIRED);
        }

        //cart, delivery, payment
        PaymentConfirmReqDto paymentConfirmReqDto = getPaymentConfirmReqDto(tossOrderId,paymentKey,amount, paymentType);
        CompletableFuture<PaymentResDto> paymentFuture = paymentEventHandler.registerPaymentFuture(tossOrderId);
        orderEventBlockingQueueV2.publishEvent(new OrderProcessEvent(userId, tossOrderId, deliveryInfo, paymentConfirmReqDto));
        orderEventBlockingQueueV2.publishEvent(new OrderPostProcessEvent(tossOrderId));
        try {
            PaymentResDto paymentResDto = paymentFuture.get(40, TimeUnit.SECONDS);
            if(paymentResDto.paymentStatus().equals("결제승인")){
                return paymentResDto;
            }else {
                throw new CustomException(OrderError.PAYMENT_RETRY_REQUIRED);
            }
        } catch (TimeoutException e){
            System.out.println("결제 처리 시간이 초과되었습니다.");
            // paymentFailed 보상트랜잭션
            throw new CustomException(PaymentError.PAYMENT_FAILED);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            throw new CustomException(PaymentError.PAYMENT_FAILED);
        }
    }
    private PaymentConfirmReqDto getPaymentConfirmReqDto(String tossOrderId, String paymentKey, Long amount, String paymentType) {
        return new PaymentConfirmReqDto(
                tossOrderId,paymentKey,amount,paymentType,0
        );
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

    @Transactional
    public void validateOrder(String tossOrderId) {

        Order order = orderRepository.findByTossOrderId(tossOrderId)
                .orElseThrow(() -> new CustomException(PaymentError.ORDER_NOT_FOUND));

        ///  재시도되는 주문 = CANCELED
        if (!(order.isPending() || order.isCanceled())) { throw new IllegalArgumentException("결제 시도는 PENDING 상태에서만 가능합니다.");}

        ///  만료 검증
        order.validateExpiration();
    }


}
