package com.innowise.userservice.service.impl;

import com.innowise.userservice.dto.UserCreateRequest;
import com.innowise.userservice.dto.UserResponse;
import com.innowise.userservice.dto.UserUpdateRequest;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.exception.UserAlreadyExistsException;
import com.innowise.userservice.exception.UserNotFoundException;
import com.innowise.userservice.mapper.UserMapper;
import com.innowise.userservice.repository.UserRepository;
import com.innowise.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CacheManager cacheManager;
    public static final String USER_CACHE = "USER_CACHE";

    @Override
    @Caching(put = {
            @CachePut(value = USER_CACHE, key = "#result.userId()"),
            @CachePut(value = USER_CACHE, key = "#result.email()")
    })
    public UserResponse create(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException(request.email());
        }

        if (userRepository.existsByUserId(request.userId())) {
            throw new UserAlreadyExistsException(request.userId());
        }

        return userMapper.toResponse(
                userRepository.save(userMapper.toEntity(request))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getByUserId(String userId) {
        var userResponse = userRepository.findUserByUserId(userId)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new UserNotFoundException(userId));

        cacheById(userResponse);
        cacheByEmail(userResponse);

        return userResponse;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getByEmail(String email) {
        var userResponse = userRepository.findUserByEmail(email)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new UserNotFoundException(email));

        cacheById(userResponse);
        cacheByEmail(userResponse);

        return userResponse;
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
    @Transactional
    public void update(String userId, UserUpdateRequest request) {
        userRepository.findUserByUserId(userId)
                .ifPresentOrElse(
                        user -> {
                            if (userRepository.existsByEmail(request.email()) &&
                                    !user.getEmail().equals(request.email())) {
                                throw new UserAlreadyExistsException(request.email());
                            }
                            userMapper.update(request, user);
                            userRepository.update(user.getId(), user.getName(), user.getSurname(), user.getBirthDate(),
                                    user.getEmail());
                            evictUserCache(user);
                        },
                        () -> {
                            throw new UserNotFoundException(userId);
                        }
                );
    }

    @Override
    @Transactional
    public void delete(String userId) {
        userRepository.findUserByUserId(userId)
                .ifPresentOrElse(
                        user -> {
                            userRepository.deleteByUserId(userId);
                            evictUserCache(user);
                        },
                        () -> {
                            throw new UserNotFoundException(userId);
                        }
                );
    }

    @Override
    public User getEntityByUserId(String userId) {
        return userRepository.findUserByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Override
    public void evictUserCache(User user) {
        evictUserCache(user.getUserId(), user.getEmail());
    }

    private void evictUserCache(String userId, String email) {
        var cache = cacheManager.getCache(USER_CACHE);
        if (cache != null) {
            cache.evict(userId);
            cache.evict(email);
        }
    }

    private void cacheById(UserResponse user) {
        var cache = cacheManager.getCache(USER_CACHE);
        if (cache != null) {
            cache.put(user.userId(), user);
        }
    }

    private void cacheByEmail(UserResponse user) {
        var cache = cacheManager.getCache(USER_CACHE);
        if (cache != null) {
            cache.put(user.email(), user);
        }
    }
}
