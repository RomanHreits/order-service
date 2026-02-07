package com.polarbookshop.orderservice.book;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

public class BookClientTest {
    private MockWebServer mockWebServer;
    private BookClient bookClient;

    @BeforeEach
    public void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").uri().toString())
                .build();

        bookClient = new BookClient(webClient);
    }

    @Test
    public void whenBookExistsThenReturnBook() {
        String isbn = "1234567890";

        MockResponse mockResponse = new MockResponse()
                .setResponseCode(200)
                .addHeader(CONTENT_TYPE, "application/json")
                .setBody("""
                    {
                        "isbn": "%s",
                        "title": "Title",
                        "author": "Author",
                        "price": 9.90,
                        "publisher": "Polarsophia"
                    }
                    """.formatted(isbn));

        mockWebServer.enqueue(mockResponse);

        Mono<Book> book = bookClient.getBookByIsbn(isbn);

        StepVerifier.create(book)
                .expectNextMatches(b -> b.isbn().equals(isbn))
                .verifyComplete();
    }

    @AfterEach
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }
}
