package com.polarbookshop.orderservice.order.domain;

import com.polarbookshop.orderservice.web.OrderController;
import com.polarbookshop.orderservice.web.OrderRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@WebFluxTest(OrderController.class)
@TestPropertySource(properties = "spring.cloud.config.enabled=false")
public class OrderControllerWebFluxTests {
    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private OrderService orderService;
    
    @Test
    void whenBookNotAvailableThenRejectOrder() {
        OrderRequest orderRequest = new OrderRequest("1234567890", 3);
        Order expectedOrder = OrderService.buildRejectedOrder(orderRequest.bookIsbn(), orderRequest.quantity());
        given(orderService.submitOrder(orderRequest.bookIsbn(), orderRequest.quantity()))
                .willReturn(Mono.just(expectedOrder));

        webTestClient.post().uri("/orders")
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Order.class)
                .value(actualOrder -> {
                    assertThat(actualOrder).isNotNull();
                    assertThat(actualOrder.status()).isEqualTo(OrderStatus.REJECTED);
                });
    }
}
