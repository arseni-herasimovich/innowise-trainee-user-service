package com.innowise.userservice.service;

import com.innowise.userservice.dto.UserCreateRequest;
import com.innowise.userservice.dto.UserResponse;
import com.innowise.userservice.dto.UserUpdateRequest;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {
    UserResponse create(UserCreateRequest request);

    UserResponse getById(UUID id);

    UserResponse getByEmail(String email);

    Page<UserResponse> getAllPaged(Pageable pageable);

    @Transactional
    void update(UUID id, UserUpdateRequest request);

    @Transactional
    void delete(UUID id);
}
