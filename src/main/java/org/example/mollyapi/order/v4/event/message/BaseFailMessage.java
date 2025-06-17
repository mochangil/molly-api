package org.example.mollyapi.order.v4.event.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class BaseFailMessage {
    Long userId;
    String tossOrderId;
    String failMessage;
}
