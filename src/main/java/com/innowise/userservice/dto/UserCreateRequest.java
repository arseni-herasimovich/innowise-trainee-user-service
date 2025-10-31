package com.innowise.userservice.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.innowise.userservice.dto.serialization.LocalDateDeserializer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

public record UserCreateRequest(

        @NotBlank(message = "Name is required")
        String name,
        @NotBlank(message = "Surname is required")
        String surname,

        @Past(message = "Birth date must be in the past")
        @JsonDeserialize(using = LocalDateDeserializer.class)
        LocalDate birthDate,

        @NotBlank(message = "Email is required")
        @Email(message = "Email should be valid")
        String email
) {
}
