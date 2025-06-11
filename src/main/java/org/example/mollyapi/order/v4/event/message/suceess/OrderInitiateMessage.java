package org.example.mollyapi.order.v4.event.message.suceess;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.example.mollyapi.order.v4.event.message.BaseMessage;


@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
public class OrderInitiateMessage extends BaseMessage {
}
