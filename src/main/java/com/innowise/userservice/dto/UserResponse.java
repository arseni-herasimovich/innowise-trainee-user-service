package com.innowise.userservice.dto;

import java.time.LocalDate;
import java.util.List;

public record UserResponse(
        String userId,
        String name,
        String surname,
        LocalDate birthDate,
        String email,
        List<CardResponse> cards
) {
}
