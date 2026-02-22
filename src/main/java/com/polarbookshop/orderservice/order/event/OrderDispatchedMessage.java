package com.polarbookshop.orderservice.order.event;

import java.io.Serializable;

public record OrderDispatchedMessage(Long orderId) implements Serializable {
}
