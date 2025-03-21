package org.example.mollyapi.payment.event;

import org.example.mollyapi.order.event.eventV2.event.order.OrderInitiateEvent;
import org.example.mollyapi.order.event.eventV2.event.order.OrderPreProcessEvent;
import org.example.mollyapi.order.event.eventV2.event.order.OrderProcessEvent;
import org.example.mollyapi.order.event.eventV2.handler.cart.CartEventHandler;
import org.example.mollyapi.order.event.eventV2.handler.delivery.DeliveryEventHandler;
import org.example.mollyapi.order.event.eventV2.handler.order.OrderEventHandler;
import org.example.mollyapi.order.event.eventV2.handler.product.StockEventHandler;
import org.example.mollyapi.order.event.eventV2.handler.user.PointEventHandler;
import org.example.mollyapi.payment.event.handler.PaymentEventHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import static org.mockito.Mockito.*;

@SpringBootTest
public class OrderEventTest {

    @Autowired
    private ApplicationEventPublisher publisher;

    @MockBean
    private OrderEventHandler orderEventHandler;

    @MockBean
    private StockEventHandler stockEventHandler;

    @MockBean
    private PointEventHandler pointEventHandler;

    @MockBean
    private CartEventHandler cartEventHandler;

    @MockBean
    private DeliveryEventHandler deliveryEventHandler;

    @MockBean
    private PaymentEventHandler paymentEventHandler;

    @DisplayName("OrderInitiate 이벤트 전파를 확인합니다")
    @Test
    void testOrderInitiateEvent() {

        //when
        OrderInitiateEvent event = new OrderInitiateEvent("ord-123");
        publisher.publishEvent(event);

        //then
        verify(orderEventHandler).handleOrderInitiateEvent(event);
    }

    @DisplayName("OrderPreProcess 이벤트 전파를 확인합니다")
    @Test
    void testOrderPreProcessEvent() {

        //when
        OrderPreProcessEvent event = new OrderPreProcessEvent(1L,"ord-123","0");
        publisher.publishEvent(event);

        //then
        verify(stockEventHandler).handleOrderPreProcessingEvent(event);
        verify(pointEventHandler).handleOrderPreProcessEvent(event);
    }

    @DisplayName("OrderProcess 이벤트 전파를 확인합니다")
    @Test
    void testOrderProcessEvent() {

        //when
        OrderProcessEvent event = new OrderProcessEvent(1L,"ord-123",null,null);
        publisher.publishEvent(event);

        //then
        verify(cartEventHandler).handleOrderProcessEvent(event);
        verify(deliveryEventHandler).handleOrderProcessEvent(event);
        verify(paymentEventHandler).handleOrderProcessEvent(event);
    }
}