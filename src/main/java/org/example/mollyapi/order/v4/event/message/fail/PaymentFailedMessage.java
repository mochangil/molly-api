package org.example.mollyapi.order.v4.event.message.fail;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.example.mollyapi.order.v4.event.message.BaseFailMessage;

@Getter
@SuperBuilder
public class PaymentFailedMessage extends BaseFailMessage {
}
