package org.example.mollyapi.order.event.V2;

public enum EventFutureType{
    PROCESS_PAYMENT, APPROVE_PAYMENT, FAIL_PAYMENT,
    STOCK, CART, DELIVERY, POINT,
    INITIATE_ORDER, SUCCESS_ORDER, FAIL_ORDER;

    public String getKey(String tossOrderId){
        return tossOrderId + "-" + this.name();
    }
}