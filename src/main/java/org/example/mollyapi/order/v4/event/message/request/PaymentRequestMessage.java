package org.example.mollyapi.order.v4.event.message.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.example.mollyapi.order.v4.event.message.BaseMessage;

@Getter
@SuperBuilder
@NoArgsConstructor
public class PaymentRequestMessage extends BaseMessage {
}
