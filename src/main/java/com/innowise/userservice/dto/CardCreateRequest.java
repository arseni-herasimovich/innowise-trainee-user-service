package com.innowise.userservice.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.innowise.userservice.dto.serialization.LocalDateDeserializer;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CardCreateRequest(
        @NotNull(message = "User ID cannot be null")
        String userId,

        @NotBlank(message = "Card number cannot be blank")
        String number,

        @NotBlank(message = "Card holder name cannot be blank")
        String holder,

        @NotNull(message = "Expiration date can not be null")
        @Future(message = "Expiration date must be in the future")
        @JsonDeserialize(using = LocalDateDeserializer.class)
        LocalDate expirationDate
) {
}
