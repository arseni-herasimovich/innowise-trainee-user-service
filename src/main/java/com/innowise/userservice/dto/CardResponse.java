package com.innowise.userservice.dto;

import java.time.Instant;
import java.util.UUID;

public record CardResponse(
        UUID id,
        UUID userId,
        String number,
        String holder,
        Instant expirationDate
) {
}
