package com.innowise.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

public record UserUpdateRequest(
        String name,
        String surname,

        @Past(message = "Birth date must be in the past")
        LocalDate birthDate,

        @Email(message = "Email should be valid")
        String email
) {
}
