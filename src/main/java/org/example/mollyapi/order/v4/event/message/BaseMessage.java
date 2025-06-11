package org.example.mollyapi.order.v4.event.message;


import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class BaseMessage {
    Long userId;
    String tossOrderId;
}
