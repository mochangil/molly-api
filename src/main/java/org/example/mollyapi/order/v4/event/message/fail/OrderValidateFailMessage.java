package org.example.mollyapi.order.v4.event.message.fail;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.example.mollyapi.order.v4.event.message.BaseFailMessage;


@Getter
@SuperBuilder
public class OrderValidateFailMessage extends BaseFailMessage {
}
