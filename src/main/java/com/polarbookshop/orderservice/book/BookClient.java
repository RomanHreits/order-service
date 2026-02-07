package com.polarbookshop.orderservice.book;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Service
public class BookClient {
    private final WebClient webClient;

    public BookClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<Book> getBookByIsbn(String isbn) {
        return webClient.get()
                .uri("/books/{isbn}", isbn)
                .retrieve()
                .bodyToMono(Book.class)
                .timeout(Duration.ofSeconds(3), Mono.empty()) // adding a timeout to handle cases where the book service is unavailable
                .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty()) // If 404 Not Found received, return empty Mono and without retrying
                .retryWhen(Retry.backoff(3, Duration.ofMillis(100)))
                .onErrorResume(Exception.class, e -> Mono.empty()); //
    }
}
