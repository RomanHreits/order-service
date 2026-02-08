package com.polarbookshop.orderservice.order.domain;

import com.polarbookshop.orderservice.config.DataConfig;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@DataR2dbcTest
@Import(DataConfig.class)
@TestPropertySource(properties = "spring.cloud.config.enabled=false")
@Testcontainers
public class OrderRepositoryTest {
/*    The @Container annotation is used to define a Testcontainers container that will be started before the tests run
      and stopped after the tests complete. In the CatalogService we relied on the AutoConfiguration of Testcontainers,
      and used the "integration" profile for defining the PostgreSQL container properties.
 */
    @Container
    static PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>(DockerImageName.parse("postgres:14.4"));

    @Autowired
    private OrderRepository orderRepository;

//    The @DynamicPropertySource annotation is used to register dynamic properties that will be applied to the Spring application context.
    @DynamicPropertySource
    static void postgresqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", OrderRepositoryTest::r2dbcUrl);
        registry.add("spring.r2dbc.username", postgresql::getUsername);
        registry.add("spring.r2dbc.password", postgresql::getPassword);
        registry.add("spring.flyway.url", postgresql::getJdbcUrl);
    }

    private static String r2dbcUrl() {
        return String.format("r2dbc:postgresql://%s:%s/%s",
                postgresql.getHost(),
                postgresql.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
                postgresql.getDatabaseName());
    }

    @Test
    void findOrderByIdWhenNotExisting() {
        StepVerifier.create(orderRepository.findById(394L))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void createRejectedOrder() {
        var rejectedOrder = OrderService.buildRejectedOrder( "1234567890", 3);
        StepVerifier.create(orderRepository.save(rejectedOrder))
                .expectNextMatches(order -> order.status().equals(OrderStatus.REJECTED))
                .verifyComplete();
    }
}