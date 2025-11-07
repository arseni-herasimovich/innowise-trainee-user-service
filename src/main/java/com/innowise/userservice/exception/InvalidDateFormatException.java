package com.innowise.userservice.exception;

import org.springframework.http.HttpStatus;

public class InvalidDateFormatException extends ApiException {
    public InvalidDateFormatException(String field) {
        super("Invalid date format for field: " + field + ". Use yyyy-mm-dd!", HttpStatus.BAD_REQUEST);
    }

}
