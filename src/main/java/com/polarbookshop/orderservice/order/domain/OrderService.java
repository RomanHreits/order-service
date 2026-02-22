package com.polarbookshop.orderservice.order.domain;

import com.polarbookshop.orderservice.book.Book;
import com.polarbookshop.orderservice.book.BookClient;
import com.polarbookshop.orderservice.order.event.OrderAcceptedMessage;
import com.polarbookshop.orderservice.order.event.OrderDispatchedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final OrderRepository orderRepository;
    private final BookClient bookClient;
    private final StreamBridge streamBridge;

    public OrderService(OrderRepository orderRepository,
                        BookClient bookClient, StreamBridge streamBridge) {
        this.orderRepository = orderRepository;
        this.bookClient = bookClient;
        this.streamBridge = streamBridge;
    }

    public Flux<Order> getOrders() {
        return orderRepository.findAll();
    }

    @Transactional
    public Mono<Order> submitOrder(String isbn, int quantity) {
        return bookClient.getBookByIsbn(isbn)
                .map(book -> buildAcceptedOrder(book, quantity))
                .defaultIfEmpty(buildRejectedOrder(isbn, quantity))
                .flatMap(orderRepository::save)
                .doOnNext(this::publishOrderAcceptedEvent);
    }

    public void publishOrderAcceptedEvent(Order order) {
        if (order.status() != OrderStatus.ACCEPTED) {
            return;
        }
        log.info("Publishing order accepted event for order with id: {}", order.id());
        // here the binding name will be treated as dynamic destination
        boolean isSent = streamBridge.send("order-accepted", new OrderAcceptedMessage(order.id()));
        log.info("Order accepted event sent: {}", isSent);
    }

    public Flux<Order> consumeOrderDispatchedEvent(Flux<OrderDispatchedMessage> flux) {
        return flux.flatMap(orderDispatchedMessage ->
                orderRepository.findById(orderDispatchedMessage.orderId())
                        .map(this::buildDispatchedOrder)
                        .flatMap(orderRepository::save));
    }

    private Order buildDispatchedOrder(Order order) {
        return new Order(order.id(), order.version(), order.orderDate(), order.lastModifiedDate(),
                order.bookIsbn(), order.bookName(), order.bookPrice(), order.quantity(), OrderStatus.DISPATCHED);
    }

    protected static Order buildAcceptedOrder(Book book, int quantity) {
        return Order.of(book.isbn(), book.title() + " - "
                + book.author(), book.price(), quantity, OrderStatus.ACCEPTED);
    }

    protected static Order buildRejectedOrder(String isbn, int quantity) {
        return Order.of(isbn, null, null, quantity, OrderStatus.REJECTED);
    }
}
