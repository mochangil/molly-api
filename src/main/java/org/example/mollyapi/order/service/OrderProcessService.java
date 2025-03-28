package org.example.mollyapi.order.service;

import org.example.mollyapi.order.entity.Order;
import org.example.mollyapi.order.event.V3.event.order.OrderInitiateEvent;
import org.springframework.stereotype.Service;

@Service
public interface OrderProcessService {
    public void validateOrder(OrderInitiateEvent orderInitiateEvent, Order order);
    public void addOrderInfo(OrderInitiateEvent orderInitiateEvent, Order order);
    public void processOrder(String tossOrderId, Order order);


}
