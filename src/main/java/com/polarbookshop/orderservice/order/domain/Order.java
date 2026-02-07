package com.polarbookshop.orderservice.order.domain;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table(name = "orders")
public record Order (
    @Id
    Long id,

    @Version
    int version,

    @CreatedDate
    Instant orderDate,

    @LastModifiedDate
    Instant lastModifiedDate,
    String bookIsbn,
    String bookName,
    Double bookPrice,
    Integer quantity,
    OrderStatus status
) {

    public static Order of(String bookIsbn, String bookName, Double bookPrice, Integer quantity, OrderStatus status) {
        return new Order(null, 0, null, null, bookIsbn, bookName, bookPrice, quantity, status);
    }
}
