package org.example.mollyapi.order.event.V2;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mollyapi.order.event.V2.event.order.OrderInitiateEvent;
import org.example.mollyapi.order.event.V2.event.order.OrderPreProcessEvent;
import org.example.mollyapi.order.event.V2.event.order.OrderProcessEvent;
import org.example.mollyapi.order.event.V2.handler.cart.CartEventHandler;
import org.example.mollyapi.order.event.V2.handler.delivery.DeliveryEventHandler;
import org.example.mollyapi.order.event.V2.handler.order.OrderEventHandler;
import org.example.mollyapi.order.event.V2.handler.product.StockEventHandler;
import org.example.mollyapi.order.event.V2.handler.user.PointEventHandler;
import org.example.mollyapi.payment.event.handler.PaymentEventHandler;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventBlockingQueue {

    private final BlockingQueue<Object> eventQueue = new LinkedBlockingQueue<>(1000);;
    private Thread consumerThread;
    private volatile boolean running = true;

    private final OrderEventHandler orderEventHandler;
    private final PaymentEventHandler paymentEventHandler;
    private final StockEventHandler stockEventHandler;
    private final DeliveryEventHandler deliveryEventHandler;
    private final CartEventHandler cartEventHandler;
    private final PointEventHandler pointEventHandler;

    @PostConstruct
    public void startConsumer() {
        consumerThread = new Thread(() -> {
            while (running) {
                try {
                    System.out.println(Thread.currentThread().getName() + ": Consumer thread started");
                    Object event = eventQueue.take(); // 큐에서 이벤트를 순서대로 가져옴
                    // 이벤트의 타입에 따라 적절한 핸들러 호출
                    if (event instanceof OrderInitiateEvent) {
                        orderEventHandler.handleOrderInitiateEvent((OrderInitiateEvent) event);
                    } else if (event instanceof OrderPreProcessEvent) {
                        stockEventHandler.handleOrderPreProcessingEvent((OrderPreProcessEvent) event);
                        pointEventHandler.handleOrderPreProcessEvent((OrderPreProcessEvent) event);
                    } else if (event instanceof OrderProcessEvent) {
                        cartEventHandler.handleOrderProcessEvent((OrderProcessEvent) event);
                        deliveryEventHandler.handleOrderProcessEvent((OrderProcessEvent) event);
                        paymentEventHandler.handleOrderProcessEvent((OrderProcessEvent) event);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        consumerThread.start();
    }

    @PreDestroy
    public void stopConsumer() {
        running = false;
        consumerThread.interrupt();
    }

    public void publishEvent(Object event) {
        eventQueue.offer(event);
    }
}