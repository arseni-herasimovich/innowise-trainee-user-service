package com.innowise.userservice.mapper;

import com.innowise.userservice.dto.UserCreateRequest;
import com.innowise.userservice.dto.UserResponse;
import com.innowise.userservice.dto.UserUpdateRequest;
import com.innowise.userservice.entity.User;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = CardMapper.class)
public interface UserMapper {
    @Mapping(target = "cards", ignore = true)
    User toEntity(UserCreateRequest request);
    UserResponse toResponse(User user);
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cards", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(UserUpdateRequest request, @MappingTarget User user);
}
