package com.innowise.userservice.exception;

import org.springframework.http.HttpStatus;

public class CardNumberAlreadyExistsException extends ApiException {
    public CardNumberAlreadyExistsException(String number) {
        super("Card with this number: " + number + " already exists!", HttpStatus.CONFLICT);
    }
}
