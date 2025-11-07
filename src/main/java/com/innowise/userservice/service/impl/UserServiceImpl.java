package com.innowise.userservice.service.impl;

import com.innowise.userservice.dto.UserCreateRequest;
import com.innowise.userservice.dto.UserResponse;
import com.innowise.userservice.dto.UserUpdateRequest;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.exception.UserEmailAlreadyExistsException;
import com.innowise.userservice.exception.UserNotFoundException;
import com.innowise.userservice.mapper.UserMapper;
import com.innowise.userservice.repository.UserRepository;
import com.innowise.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserResponse create(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new UserEmailAlreadyExistsException(request.email());
        }

        return userMapper.toResponse(
                userRepository.save(userMapper.toEntity(request))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return userRepository.findUserById(id)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Override
    public Page<UserResponse> getAllPaged(Pageable pageable) {
        var ids = userRepository.findUserIds(pageable);
        var users = userRepository.findAllWithCardsByIds(ids.getContent())
                .stream()
                .map(userMapper::toResponse)
                .toList();
        return new PageImpl<>(users, pageable, ids.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getByEmail(String email) {
        return userRepository.findUserByEmail(email)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new UserNotFoundException(email));
    }

    @Override
    @Transactional
    public void update(UUID id, UserUpdateRequest request) {
        userRepository.findUserById(id)
                .ifPresentOrElse(
                        user -> {
                            if (userRepository.existsByEmail(request.email()) &&
                                    !user.getEmail().equals(request.email())) {
                                throw new UserEmailAlreadyExistsException(request.email());
                            }
                            userMapper.update(request, user);
                            userRepository.update(user.getId(), user.getName(), user.getSurname(), user.getBirthDate(),
                                    user.getEmail());
                        },
                        () -> {
                            throw new UserNotFoundException(id);
                        }
                );
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        userRepository.findUserById(id)
                .ifPresentOrElse(
                        user -> userRepository.delete(id),
                        () -> {
                            throw new UserNotFoundException(id);
                        }
                );
    }

    @Override
    public User getEntityById(UUID id) {
        return userRepository.findUserById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }
}
