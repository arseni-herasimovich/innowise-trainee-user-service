package com.innowise.userservice.dto;

import java.time.LocalDate;
import java.util.UUID;

public record CardResponse(
        UUID id,
        String userId,
        String number,
        String holder,
        LocalDate expirationDate
) {
}
