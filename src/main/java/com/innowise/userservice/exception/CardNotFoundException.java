package com.innowise.userservice.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class CardNotFoundException extends ApiException {
    public CardNotFoundException(UUID id) {
        super("Card with id " + id + " not found!", HttpStatus.NOT_FOUND);
    }
}
