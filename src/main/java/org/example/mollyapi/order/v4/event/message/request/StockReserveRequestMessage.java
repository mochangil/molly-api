package org.example.mollyapi.order.v4.event.message.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.example.mollyapi.order.v4.event.message.BaseMessage;

@SuperBuilder
@Getter
@NoArgsConstructor
public class StockReserveRequestMessage extends BaseMessage {
}
