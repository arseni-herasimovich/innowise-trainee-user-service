package com.innowise.userservice.mapper;

import com.innowise.userservice.dto.UserCreateRequest;
import com.innowise.userservice.dto.UserResponse;
import com.innowise.userservice.dto.UserUpdateRequest;
import com.innowise.userservice.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toEntity (UserCreateRequest request);
    UserResponse toResponse(User user);
    @Mapping(target = "id", ignore = true)
    void update(UserUpdateRequest request, @MappingTarget User user);
}
