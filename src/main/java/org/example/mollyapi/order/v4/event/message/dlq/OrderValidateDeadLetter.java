package org.example.mollyapi.order.v4.event.message.dlq;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.example.mollyapi.order.v4.event.message.BaseMessage;

@Getter
@NoArgsConstructor
@SuperBuilder
public class OrderValidateDeadLetter<T> extends BaseMessage {
}
