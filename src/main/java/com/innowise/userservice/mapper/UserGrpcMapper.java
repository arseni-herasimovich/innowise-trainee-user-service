package com.innowise.userservice.mapper;

import com.innowise.userservice.generated.User;
import com.innowise.userservice.dto.UserCreateRequest;
import com.innowise.userservice.dto.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserGrpcMapper {

    @Mapping(target = "birthDate", expression = "java(com.innowise.userservice.dto.serialization.LocalDateDeserializer.deserialize(request.getBirthDate()))")
    UserCreateRequest toRequest(User.UserCreateRequest request);

    default User.UserResponse toResponse(UserResponse response) {
        return User.UserResponse.newBuilder()
                .setId(response.id().toString())
                .setName(response.name())
                .setSurname(response.surname())
                .setBirthDate(response.birthDate().toString())
                .setEmail(response.email())
                .build();
    }
}
