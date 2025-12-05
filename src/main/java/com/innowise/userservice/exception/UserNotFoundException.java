package com.innowise.userservice.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends ApiException {
    public UserNotFoundException(String identifier) {
        super("User " + identifier + " not found!", HttpStatus.NOT_FOUND);
    }
}
