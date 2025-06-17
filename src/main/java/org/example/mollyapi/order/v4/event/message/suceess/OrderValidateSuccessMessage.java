package org.example.mollyapi.order.v4.event.message.suceess;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.example.mollyapi.order.v4.event.message.BaseMessage;

@Getter
@SuperBuilder
@NoArgsConstructor
public class OrderValidateSuccessMessage extends BaseMessage {
}
