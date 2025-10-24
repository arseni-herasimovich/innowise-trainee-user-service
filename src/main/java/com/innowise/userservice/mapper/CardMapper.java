package com.innowise.userservice.mapper;

import com.innowise.userservice.dto.CardCreateRequest;
import com.innowise.userservice.dto.CardResponse;
import com.innowise.userservice.entity.Card;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CardMapper {
    Card toEntity(CardCreateRequest request);
    @Mapping(target = "userId", source = "user.id")
    CardResponse toResponse(Card card);
}
