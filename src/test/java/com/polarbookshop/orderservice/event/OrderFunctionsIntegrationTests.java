package com.polarbookshop.orderservice.event;

import com.polarbookshop.orderservice.config.DataConfig;
import com.polarbookshop.orderservice.order.domain.Order;
import com.polarbookshop.orderservice.order.domain.OrderRepository;
import com.polarbookshop.orderservice.order.domain.OrderStatus;
import com.polarbookshop.orderservice.order.event.OrderDispatchedMessage;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

import java.util.function.Supplier;

@SpringBootTest
@Testcontainers
@Import(DataConfig.class)
@ActiveProfiles("integration")
public class OrderFunctionsIntegrationTests {
    @Container
    public static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.8-management-alpine")
            .withExposedPorts(5672, 15672)
            .withEnv("RABBITMQ_DEFAULT_USER", "guest")
            .withEnv("RABBITMQ_DEFAULT_PASS", "guest");

    @Container
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer("postgres:14.4");

    @DynamicPropertySource
    static void rabbitProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", () -> rabbit.getMappedPort(5672));
        registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
        registry.add("spring.r2dbc.url", r2dbcUrl);
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);

        registry.add("spring.flyway.url", () -> postgres.getJdbcUrl());
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
//        registry.add("spring.flyway.enabled", () -> "true");

        System.out.println("RabbitMQ UI: http://localhost:" + rabbit.getMappedPort(15672));
    }

    private static final Supplier<Object> r2dbcUrl = () -> String.format("r2dbc:postgresql://%s:%s/%s",
            postgres.getHost(),
            postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
            postgres.getDatabaseName());

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private OrderRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testConsumeOrderDispatchedEvent() throws InterruptedException {
        // Given
        OrderDispatchedMessage dispatchedMessage = new OrderDispatchedMessage(1L);

        Order acceptedOrder = Order.of("1234567890", "TestName", 9.90, 1, OrderStatus.ACCEPTED);
        repository.save(acceptedOrder).block();

        MessageProperties properties = new MessageProperties();
        properties.setHeader("contentType", "application/json");
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setContentEncoding("UTF-8");

        Message message = new Message(objectMapper.writeValueAsBytes(dispatchedMessage), properties);

        rabbitTemplate.send("order-dispatched", null, message);

        Thread.sleep(1000); // Wait for the message to be processed

        StepVerifier.create(repository.findById(dispatchedMessage.orderId()))
                .expectNextMatches(order -> order.status().name().equals("DISPATCHED"))
                .verifyComplete();
    }
}
