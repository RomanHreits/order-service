package com.polarbookshop.orderservice.web;

import jakarta.validation.constraints.*;

public record OrderRequest (
        @NotBlank(message = "The book ISBN must be provided")
    @Pattern(regexp = "^([0-9]{10}|[0-9]{13})$", message = "ISBN must be either 10 or 13 digits")
    String bookIsbn,

    @NotNull(message = "The book quantity must be provided")
    @Min(value = 1, message = "The book quantity must be at least 1 item")
    @Max(value = 5, message = "The book quantity must not exceed 5 items")
    Integer quantity
) {}