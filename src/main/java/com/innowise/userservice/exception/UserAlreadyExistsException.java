package com.innowise.userservice.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class UserAlreadyExistsException extends ApiException {
    public UserAlreadyExistsException(String email) {
        super("User with email " + email + " already exists!", HttpStatus.CONFLICT);
    }

    public UserAlreadyExistsException(UUID id) {
        super("User with id " + id + " already exists!", HttpStatus.CONFLICT);
    }
}
