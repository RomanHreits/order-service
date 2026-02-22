package com.polarbookshop.orderservice.order.event;

import com.polarbookshop.orderservice.order.domain.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@Configuration
public class OrderFunctions {
    private static final Logger log = LoggerFactory.getLogger(OrderFunctions.class);

    @Bean
    /*  Since the order-service is reactive, we need to consume the order dispatched events in a reactive way as well.
      By defining a Consumer<Flux<OrderDispatchedMessage>>, we can process the incoming stream of order dispatched
      messages and update the order status accordingly in a non-blocking manner.
      Reactive streams are activated only when there is a subscriber, so by subscribing to the flux,
      we ensure that the processing of the order dispatched
    */
    public Consumer<Flux<OrderDispatchedMessage>> dispatchOrder(OrderService orderService) {
        return flux -> orderService.consumeOrderDispatchedEvent(flux)
                .doOnNext(order ->
                        log.info("Received order dispatched message for order with id: {}", order.id()))
                .subscribe();
    }
}
