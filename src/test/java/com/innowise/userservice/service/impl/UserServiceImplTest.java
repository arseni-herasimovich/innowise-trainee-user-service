package com.innowise.userservice.service.impl;

import com.innowise.userservice.dto.UserCreateRequest;
import com.innowise.userservice.dto.UserResponse;
import com.innowise.userservice.dto.UserUpdateRequest;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.exception.UserAlreadyExistsException;
import com.innowise.userservice.exception.UserNotFoundException;
import com.innowise.userservice.mapper.UserMapper;
import com.innowise.userservice.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private UserServiceImpl userService;

    @Nested
    @DisplayName("Create a user")
    class CreateTests {
        @Test
        @DisplayName("Should create user and return response when valid data provided")
        void givenValidData_whenCreate_thenSavesUserAndReturnsResponse() {
            //Given
            var request = createUserCreateRequest();
            var user = createUser(request.name(), request.surname(), request.birthDate(), request.email());
            var response = createUserResponse(user);

            //When
            when(userRepository.existsByEmail(request.email())).thenReturn(false);
            when(userRepository.existsByUserId(request.userId())).thenReturn(false);
            when(userMapper.toEntity(request)).thenReturn(user);
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toResponse(user)).thenReturn(response);

            var serviceResponse = userService.create(request);

            //Then
            assertEquals(response, serviceResponse);

            verify(userRepository, times(1)).existsByEmail(request.email());
            verify(userRepository, times(1)).existsByUserId(request.userId());
            verify(userMapper, times(1)).toEntity(request);
            verify(userMapper, times(1)).toResponse(user);
            verify(userRepository, times(1)).save(user);
        }

        @Test
        @DisplayName("Should throw UserAlreadyExistsException when creating a user with existing email")
        void givenExistingEmail_whenCreate_thenThrowsException() {
            // Given
            var request = createUserCreateRequest();

            // When
            when(userRepository.existsByEmail(request.email())).thenReturn(true);

            // Then
            assertThrows(UserAlreadyExistsException.class, () -> userService.create(request));

            verify(userRepository, times(1)).existsByEmail(request.email());
            verify(userRepository, never()).existsByUserId(any());
            verify(userMapper, never()).toEntity(any());
            verify(userMapper, never()).toResponse(any());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw UserAlreadyExistsException when creating a user with existing user id")
        void givenExistingUserId_whenCreate_thenThrowsException() {
            // Given
            var request = createUserCreateRequest();

            // When
            when(userRepository.existsByEmail(request.email())).thenReturn(false);
            when(userRepository.existsByUserId(request.userId())).thenReturn(true);

            // Then
            assertThrows(UserAlreadyExistsException.class, () -> userService.create(request));

            verify(userRepository, times(1)).existsByEmail(request.email());
            verify(userRepository, times(1)).existsByUserId(any());
            verify(userMapper, never()).toEntity(any());
            verify(userMapper, never()).toResponse(any());
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Get user by user ID")
    class GetByUserIdTests {
        @Test
        @DisplayName("Should return user response when getting existing user by user ID")
        void givenExistingUser_whenGetByUserId_thenReturnsUserResponse() {
            // Given
            var user = createUser("TEST_NAME", "TEST_SURNAME", LocalDate.now(), "TEST@EMAIL");
            var response = createUserResponse(user);

            // When
            when(userRepository.findUserByUserId(user.getUserId())).thenReturn(Optional.of(user));
            when(userMapper.toResponse(user)).thenReturn(response);

            var serviceResponse = userService.getByUserId(user.getUserId());

            // Then
            assertEquals(response, serviceResponse);

            verify(userRepository, times(1)).findUserByUserId(user.getUserId());
            verify(userMapper, times(1)).toResponse(user);
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when getting non-existing user by ID")
        void givenNonExistingUser_whenGetById_thenThrowsUserNotFoundException() {
            // Given
            var userId = UUID.randomUUID().toString();

            // When
            when(userRepository.findUserByUserId(userId)).thenReturn(Optional.empty());

            // Then
            assertThrows(UserNotFoundException.class, () -> userService.getByUserId(userId));

            verify(userRepository, times(1)).findUserByUserId(userId);
            verify(userMapper, never()).toResponse(any());
        }

        @Test
        @DisplayName("Should cache user by ID and email when getting existing user by ID")
        void givenExistingUser_whenGetById_thenCachesUserByIdAndEmail() {
            // Given
            var user = createUser("TEST_NAME", "TEST_SURNAME", LocalDate.now(), "TEST@EMAIL");
            var response = createUserResponse(user);

            // When
            when(userRepository.findUserByUserId(user.getUserId())).thenReturn(Optional.of(user));
            when(userMapper.toResponse(user)).thenReturn(response);
            when(cacheManager.getCache(UserServiceImpl.USER_CACHE)).thenReturn(cache);

            userService.getByUserId(user.getUserId());

            // Then
            verify(cacheManager, times(2)).getCache(UserServiceImpl.USER_CACHE);
            verify(cache, times(1)).put(user.getUserId(), response);
            verify(cache, times(1)).put(user.getEmail(), response);
        }
    }

    @Nested
    @DisplayName("Get user by email")
    class GetByEmailTests {
        @Test
        @DisplayName("Should return user response when getting existing user by email")
        void givenExistingUser_whenGetByEmail_thenReturnsUserResponse() {
            // Given
            var user = createUser("TEST_NAME", "TEST_SURNAME", LocalDate.now(), "TEST@EMAIL");
            var response = createUserResponse(user);

            // When
            when(userRepository.findUserByEmail(user.getEmail())).thenReturn(Optional.of(user));
            when(userMapper.toResponse(user)).thenReturn(response);

            var serviceResponse = userService.getByEmail(user.getEmail());

            // Then
            assertEquals(response, serviceResponse);

            verify(userRepository, times(1)).findUserByEmail(user.getEmail());
            verify(userMapper, times(1)).toResponse(user);
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when getting non-existing user by email")
        void givenNonExistingUser_whenGetByEmail_thenThrowsUserNotFoundException() {
            // Given
            var email = "TEST@EMAIL";

            // When
            when(userRepository.findUserByEmail(email)).thenReturn(Optional.empty());

            // Then
            assertThrows(UserNotFoundException.class, () -> userService.getByEmail(email));

            verify(userRepository, times(1)).findUserByEmail(email);
            verify(userMapper, never()).toResponse(any());
        }

        @Test
        @DisplayName("Should cache user by ID and email when getting existing user by email")
        void givenExistingUser_whenGetByEmail_thenCachesUserByIdAndEmail() {
            // Given
            var user = createUser("TEST_NAME", "TEST_SURNAME", LocalDate.now(), "TEST@EMAIL");
            var response = createUserResponse(user);

            // When
            when(userRepository.findUserByEmail(user.getEmail())).thenReturn(Optional.of(user));
            when(userMapper.toResponse(user)).thenReturn(response);
            when(cacheManager.getCache(UserServiceImpl.USER_CACHE)).thenReturn(cache);

            userService.getByEmail(user.getEmail());

            // Then
            verify(cacheManager, times(2)).getCache(UserServiceImpl.USER_CACHE);
            verify(cache, times(1)).put(user.getUserId(), response);
            verify(cache, times(1)).put(user.getEmail(), response);
        }
    }

    @Nested
    @DisplayName("Get page of users")
    class GetAllPagedTests {
        @Test
        @DisplayName("Should return page of user responses when getting all existing users")
        void givenExistingUsers_whenGetAllPaged_thenReturnsPageOfUserResponses() {
            // Given
            var user = createUser("TEST_NAME", "TEST_SURNAME", LocalDate.now(), "TEST@EMAIL");
            var response = createUserResponse(user);
            var pageable = PageRequest.of(0, 10);
            var ids = new PageImpl<>(List.of(user.getId()), pageable, 1);

            // When
            when(userRepository.findUserIds(pageable)).thenReturn(ids);
            when(userRepository.findAllWithCardsByIds(ids.toList())).thenReturn(List.of(user));
            when(userMapper.toResponse(user)).thenReturn(response);

            var serviceResponse = userService.getAllPaged(pageable);

            // Then
            assertEquals(1, serviceResponse.getTotalElements());
            assertEquals(response, serviceResponse.getContent().get(0));

            verify(userRepository, times(1)).findUserIds(pageable);
            verify(userRepository, times(1)).findAllWithCardsByIds(List.of(user.getId()));
            verify(userMapper, times(1)).toResponse(user);
        }

        @Test
        @DisplayName("Should return empty page when there's no existing users")
        void givenNoUsers_whenGetAllPaged_thenReturnsEmptyPage() {
            // Given
            var pageable = PageRequest.of(0, 10);
            Page<UUID> ids = new PageImpl<>(List.of(), pageable, 1);

            // When
            when(userRepository.findUserIds(pageable)).thenReturn(ids);
            when(userRepository.findAllWithCardsByIds(ids.toList())).thenReturn(List.of());

            var serviceResponse = userService.getAllPaged(pageable);

            // Then
            assertEquals(1, serviceResponse.getTotalElements());

            verify(userRepository, times(1)).findUserIds(pageable);
            verify(userRepository, times(1)).findAllWithCardsByIds(List.of());
            verify(userMapper, never()).toResponse(any());
        }
    }

    @Nested
    @DisplayName("Update user")
    class UpdateTests {
        @Test
        @DisplayName("Should update user when user exists and email is not changing")
        void givenExistingUserWithoutChangingEmail_whenUpdate_thenUpdatesUser() {
            // Given
            var user = createUser("TEST_NAME", "TEST_SURNAME", LocalDate.now(), "TEST@EMAIL");
            var request = createUserUpdateRequest(user.getEmail());

            // When
            when(userRepository.findUserByUserId(user.getUserId())).thenReturn(Optional.of(user));
            when(userRepository.existsByEmail(request.email())).thenReturn(true);

            userService.update(user.getUserId(), request);

            // Then
            verify(userRepository, times(1)).findUserByUserId(user.getUserId());
            verify(userRepository, times(1)).existsByEmail(request.email());
            verify(userMapper, times(1)).update(request, user);
            verify(userRepository, times(1)).update(
                    user.getId(),
                    user.getName(),
                    user.getSurname(),
                    user.getBirthDate(),
                    user.getEmail()
            );
        }

        @Test
        @DisplayName("Should update user when user exists, email is changing and email does not exist")
        void givenExistingUserWithChangingEmailAndEmailNotExists_whenUpdate_thenUpdatesUser() {
            // Given
            var user = createUser("TEST_NAME", "TEST_SURNAME", LocalDate.now(), "TEST@EMAIL");
            var request = createUserUpdateRequest("NEW_EMAIL");

            // When
            when(userRepository.findUserByUserId(user.getUserId())).thenReturn(Optional.of(user));
            when(userRepository.existsByEmail(request.email())).thenReturn(false);

            userService.update(user.getUserId(), request);

            // Then
            verify(userRepository, times(1)).findUserByUserId(user.getUserId());
            verify(userRepository, times(1)).existsByEmail(request.email());
            verify(userMapper, times(1)).update(request, user);
            verify(userRepository, times(1)).update(
                    user.getId(),
                    user.getName(),
                    user.getSurname(),
                    user.getBirthDate(),
                    user.getEmail()
            );
        }

        @Test
        @DisplayName("Should not update user when user exists, email is changing and email exists")
        void givenExistingUserWithChangingEmailAndEmailExists_whenUpdate_thenThrowsUserEmailAlreadyExistsException() {
            // Given
            var user = createUser("TEST_NAME", "TEST_SURNAME", LocalDate.now(), "TEST@EMAIL");
            var request = createUserUpdateRequest("NEW_EMAIL");

            // When
            when(userRepository.findUserByUserId(user.getUserId())).thenReturn(Optional.of(user));
            when(userRepository.existsByEmail(request.email())).thenReturn(true);

            assertThrows(UserAlreadyExistsException.class, () -> userService.update(user.getUserId(), request));

            // Then
            verify(userRepository, times(1)).findUserByUserId(user.getUserId());
            verify(userRepository, times(1)).existsByEmail(request.email());
            verify(userMapper, never()).update(any(), any());
            verify(userRepository, never()).update(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when updating non-existing user")
        void givenNonExistingUser_whenUpdate_thenThrowsUserNotFoundException() {
            // Given
            var userId = UUID.randomUUID().toString();
            var request = createUserUpdateRequest("TEST@EMAIL");

            // When
            when(userRepository.findUserByUserId(userId)).thenReturn(Optional.empty());

            // Then
            assertThrows(UserNotFoundException.class, () -> userService.update(userId, request));

            verify(userRepository, times(1)).findUserByUserId(userId);
            verify(userRepository, never()).existsByEmail(any());
            verify(userMapper, never()).update(any(), any());
            verify(userRepository, never()).update(any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Delete user")
    class DeleteTests {
        @Test
        @DisplayName("Should delete user when user exists")
        void givenExistingUser_whenDelete_thenDeletesUser() {
            // Given
            var user = createUser("TEST_NAME", "TEST_SURNAME", LocalDate.now(), "test@test.com");

            // When
            when(userRepository.findUserByUserId(user.getUserId())).thenReturn(Optional.of(user));

            userService.delete(user.getUserId());

            // Then
            verify(userRepository, times(1)).findUserByUserId(user.getUserId());
            verify(userRepository, times(1)).deleteByUserId(user.getUserId());
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when deleting non-existing user")
        void givenNonExistingUser_whenDelete_thenThrowsUserNotFoundException() {
            // Given
            var userId = UUID.randomUUID().toString();

            // When
            when(userRepository.findUserByUserId(userId)).thenReturn(Optional.empty());

            // Then
            assertThrows(UserNotFoundException.class, () -> userService.delete(userId));

            verify(userRepository, times(1)).findUserByUserId(userId);
            verify(userRepository, never()).delete(any(UUID.class));
        }

        @Test
        @DisplayName("Should evict user cache when deleting existing user")
        void givenExistingUser_whenDelete_thenEvictsUserCache() {
            // Given
            var user = createUser("TEST_NAME", "TEST_SURNAME", LocalDate.now(), "TEST@EMAIL");

            // When
            when(userRepository.findUserByUserId(user.getUserId())).thenReturn(Optional.of(user));
            when(cacheManager.getCache(UserServiceImpl.USER_CACHE)).thenReturn(cache);

            userService.delete(user.getUserId());

            // Then
            verify(cacheManager, times(1)).getCache(UserServiceImpl.USER_CACHE);
            verify(cache, times(1)).evict(user.getUserId());
            verify(cache, times(1)).evict(user.getEmail());
            verify(userRepository, times(1)).deleteByUserId(user.getUserId());
        }
    }

    @Nested
    @DisplayName("Get entity by id")
    class GetEntityByIdTests {
        @Test
        @DisplayName("Should return user entity when getting existing user by userId")
        void givenExistingUser_whenGetEntityById_thenReturnsUser() {
            // Given
            var user = createUser("TEST_NAME", "TEST_SURNAME", LocalDate.now(), "TEST@EMAIL");

            // When
            when(userRepository.findUserByUserId(user.getUserId())).thenReturn(Optional.of(user));

            var result = userService.getEntityByUserId(user.getUserId());

            // Then
            assertEquals(user, result);

            verify(userRepository, times(1)).findUserByUserId(user.getUserId());
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when getting non-existing user entity by userID")
        void givenNonExistingUser_whenGetEntityById_thenThrowsUserNotFoundException() {
            // Given
            var userId = UUID.randomUUID().toString();

            // When
            when(userRepository.findUserByUserId(userId)).thenReturn(Optional.empty());

            // Then
            assertThrows(UserNotFoundException.class, () -> userService.getEntityByUserId(userId));

            verify(userRepository, times(1)).findUserByUserId(userId);
        }
    }

    private UserCreateRequest createUserCreateRequest() {
        return new UserCreateRequest(
                UUID.randomUUID().toString(),
                "TEST_NAME",
                "TEST_SURNAME",
                LocalDate.now(),
                "TEST@EMAIL"
        );
    }

    private UserUpdateRequest createUserUpdateRequest(String email) {
        return new UserUpdateRequest(
                "TEST_NAME",
                "TEST_SURNAME",
                LocalDate.now(),
                email
        );
    }

    private User createUser(String name, String surname, LocalDate birthDate, String email) {
        return new User(
                UUID.randomUUID(),
                UUID.randomUUID().toString(),
                name,
                surname,
                birthDate,
                email,
                null
        );
    }

    private UserResponse createUserResponse(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getName(),
                user.getSurname(),
                user.getBirthDate(),
                user.getEmail(),
                null
        );
    }
}