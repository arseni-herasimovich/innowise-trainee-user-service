package com.innowise.userservice.service;

import com.innowise.userservice.dto.UserCreateRequest;
import com.innowise.userservice.dto.UserResponse;
import com.innowise.userservice.dto.UserUpdateRequest;
import com.innowise.userservice.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public interface UserService {
    UserResponse create(UserCreateRequest request);

    User getEntityByUserId(String userId);

    @Transactional(readOnly = true)
    UserResponse getByUserId(String userId);

    UserResponse getByEmail(String email);

    Page<UserResponse> getAllPaged(Pageable pageable);

    @Transactional
    void update(String userId, UserUpdateRequest request);

    @Transactional
    void delete(String userId);

    void evictUserCache(User user);
}
