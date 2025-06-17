package org.example.mollyapi.order.v4.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record OrderRequestDtoList(
        List<OrderRequestDto> orderRequests
) {
    @JsonCreator
    public OrderRequestDtoList(@JsonProperty("orderRequests") List<OrderRequestDto> orderRequests) {
        this.orderRequests = orderRequests;
    }
}