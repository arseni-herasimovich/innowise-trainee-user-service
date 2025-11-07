package com.innowise.userservice.dto.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.innowise.userservice.exception.InvalidDateFormatException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class LocalDateDeserializer extends JsonDeserializer<LocalDate> {
    @Override
    public LocalDate deserialize(JsonParser parser,
                                 DeserializationContext context) throws IOException {
        try {
            return LocalDate.parse(parser.getText());
        } catch (DateTimeParseException e) {
            throw new InvalidDateFormatException(parser.currentName());
        }
    }
}
