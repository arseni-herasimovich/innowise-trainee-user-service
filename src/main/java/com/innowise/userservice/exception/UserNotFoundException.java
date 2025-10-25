package com.innowise.userservice.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class UserNotFoundException extends ApiException {
    public UserNotFoundException(UUID id) {
        super("User with id " + id + " not found!", HttpStatus.NOT_FOUND);
    }

    public UserNotFoundException(String email) {
        super("User with email " + email + " not found!", HttpStatus.NOT_FOUND);
    }
}
