package org.example.mollyapi.payment.event.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mollyapi.order.event.V2.event.order.OrderProcessEvent;
import org.example.mollyapi.payment.dto.response.PaymentResDto;
import org.example.mollyapi.payment.event.event.PaymentApprovedEvent;
import org.example.mollyapi.payment.event.event.PaymentFailedEvent;
import org.example.mollyapi.payment.service.impl.PaymentServiceImpl;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventHandler {
    private final PaymentServiceImpl paymentService;
    private final ConcurrentMap<String, CompletableFuture<PaymentResDto>> paymentResultMap = new ConcurrentHashMap<>();

    public CompletableFuture<PaymentResDto> registerPaymentFuture(String tossOrderId) {
        CompletableFuture<PaymentResDto> future = new CompletableFuture<>();
        paymentResultMap.put(tossOrderId, future);
        return future;
    }


    @EventListener
    @Async("processOrderExecutor")
    @Transactional
    public void handleOrderProcessEvent(OrderProcessEvent event) {
        paymentService.processPaymentV2(event.userId(), event.paymentConfirmReqDto());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("paymentExecutor")
    public void handlePaymentApprovedEvent(PaymentApprovedEvent event) {
        CompletableFuture<PaymentResDto> future = paymentResultMap.remove(event.tossOrderId());
        if (future != null) {
            PaymentResDto paymentResDto = new PaymentResDto(
                    event.paymentId(),
                    event.paymentType(),
                    event.amount(),
                    event.paymentStatus(),
                    event.tossOrderId(),
                    event.tossPaymentKey()
            );
            System.out.println(paymentResDto.paymentStatus());
            future.complete(paymentResDto);

        }
        paymentService.approvePayment(event.tossPaymentKey());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("paymentExecutor")
    public void handlePaymentFailedEvent(PaymentFailedEvent event) {
        CompletableFuture<PaymentResDto> future = paymentResultMap.remove(event.tossOrderId());
        if (future != null) {
            PaymentResDto paymentResDto = new PaymentResDto(
                    event.paymentId(),
                    event.paymentType(),
                    event.amount(),
                    event.paymentStatus(),
                    event.tossOrderId(),
                    event.tossPaymentKey()
            );
            future.complete(paymentResDto);
        }
        paymentService.failPayment(event.paymentId(),event.error());
    }
}
