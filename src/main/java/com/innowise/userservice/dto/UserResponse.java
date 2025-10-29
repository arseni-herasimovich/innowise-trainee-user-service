package com.innowise.userservice.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String surname,
        LocalDate birthDate,
        String email,
        List<CardResponse> cards
) {
}
