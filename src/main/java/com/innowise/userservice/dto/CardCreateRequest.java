package com.innowise.userservice.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CardCreateRequest(
        @NotNull(message = "User ID cannot be null")
        UUID userId,
        
        @NotBlank(message = "Card number cannot be blank")
        String number,
        
        @NotBlank(message = "Card holder name cannot be blank")
        String holder,
        
        @NotNull(message = "Expiration date can not be null")
        @Future(message = "Expiration date must be in the future")
        LocalDate expirationDate
) {
}
