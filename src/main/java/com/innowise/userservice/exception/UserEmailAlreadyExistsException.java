package com.innowise.userservice.exception;

import org.springframework.http.HttpStatus;

public class UserEmailAlreadyExistsException extends ApiException {
    public UserEmailAlreadyExistsException(String email) {
        super("User with email " + email + " already exists!", HttpStatus.CONFLICT);
    }
}
