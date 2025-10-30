package com.innowise.userservice.controller;

import com.innowise.userservice.dto.ApiResponse;
import com.innowise.userservice.dto.UserCreateRequest;
import com.innowise.userservice.dto.UserResponse;
import com.innowise.userservice.dto.UserUpdateRequest;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerTest {

    @BeforeEach
    void clearUserRepository() {
        userRepository.deleteAll();
    }

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15")
    );

    @Container
    private static final GenericContainer<?> redis = new GenericContainer<>("redis:7.0")
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort());

    @Autowired
    private UserRepository userRepository;

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Should create user and return response when valid request provided")
    void givenValidRequest_whenCreate_thenSavesToDatabaseAndResponses() {
        // Given
        var request = new UserCreateRequest(
                "TEST_NAME",
                "TEST_SURNAME",
                LocalDate.now().minusDays(1),
                "TEST@EMAIL"
        );

        // When
        var response = restTemplate.exchange(
                URI.CREATE_USER,
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<ApiResponse<UserResponse>>() {
                }
        );

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertNotNull(response.getBody().getData().id());

        Optional<User> user = userRepository.findUserById(response.getBody().getData().id());
        assertTrue(user.isPresent());
        assertEquals(request.name(), user.get().getName());
        assertEquals(request.surname(), user.get().getSurname());
        assertEquals(request.birthDate(), user.get().getBirthDate());
        assertEquals(request.email(), user.get().getEmail());
    }

    @Test
    @DisplayName("Should return conflict when creating user with existing email")
    void givenExistingEmail_whenCreate_thenReturnsConflict() {
        // Given
        var user = new User();
        user.setEmail("TEST@EMAIL");
        userRepository.save(user);

        var request = new UserCreateRequest(
                "TEST_NAME",
                "TEST_SURNAME",
                LocalDate.now().minusDays(1),
                "TEST@EMAIL"
        );

        // When
        var response = restTemplate.exchange(
                URI.CREATE_USER,
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<ApiResponse<UserResponse>>() {
                }
        );

        // Then
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertNull(response.getBody().getData());

        List<User> users = userRepository.findAll();
        assertEquals(1, users.size());
        assertEquals("TEST@EMAIL", users.get(0).getEmail());
    }

    @Test
    @DisplayName("Should return bad request when creating user with not valid date")
    void givenCorruptedDate_whenCreate_thenReturnsBadRequest() {
        // Given
        var map = Map.of(
                "name", "TEST_NAME",
                "surname", "TEST_SURNAME",
                "birthDate", "NOT VALID DATE",
                "email", "TEST@EMAIL"
        );

        // When
        var response = restTemplate.exchange(
                URI.CREATE_USER,
                HttpMethod.POST,
                new HttpEntity<>(map),
                new ParameterizedTypeReference<ApiResponse<UserResponse>>() {
                }
        );

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertNull(response.getBody().getData());

        List<User> users = userRepository.findAll();
        assertEquals(0, users.size());
    }

    @Test
    @DisplayName("Should return bad request when creating user with not valid email")
    void givenCorruptedEmail_whenCreate_thenReturnsBadRequest() {
        // Given
        var request = new UserCreateRequest(
                "TEST_NAME",
                "TEST_SURNAME",
                LocalDate.now().minusDays(1),
                "TESTEMAIL"
        );

        // When
        var response = restTemplate.exchange(
                URI.CREATE_USER,
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<ApiResponse<List<String>>>() {
                }
        );

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getData().stream().anyMatch(s -> s.contains("email")));

        List<User> users = userRepository.findAll();
        assertEquals(0, users.size());
    }

    @Test
    @DisplayName("Should return user response when getting existing user by id")
    void givenExistingId_whenGetById_thenReturnsUserResponse() {
        // Given
        var newUser = new User();
        newUser.setEmail("TEST@EMAIL");
        var id = userRepository.save(newUser).getId();

        // When
        var response = restTemplate.exchange(
                URI.GET_USER_BY_ID,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<UserResponse>>() {
                },
                id
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertNotNull(response.getBody().getData().id());

        Optional<User> user = userRepository.findUserById(id);
        assertTrue(user.isPresent());
        assertEquals(user.get().getName(), response.getBody().getData().name());
        assertEquals(user.get().getEmail(), response.getBody().getData().email());
        assertEquals(user.get().getBirthDate(), response.getBody().getData().birthDate());
        assertEquals(user.get().getSurname(), response.getBody().getData().surname());
    }

    @Test
    @DisplayName("Should return not found when getting non-existing user by Id")
    void givenNotExistingId_whenGetById_thenReturnsNotFound() {
        // Given
        var newUser = new User();
        newUser.setEmail("TEST@EMAIL");
        userRepository.save(newUser);

        // When
        var response = restTemplate.exchange(
                URI.GET_USER_BY_ID,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<UserResponse>>() {
                },
                UUID.randomUUID()
        );

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertNull(response.getBody().getData());
    }

    @Test
    @DisplayName("Should return user response when getting existing user by email")
    void givenExistingEmail_whenGetByEmail_thenReturnsUserResponse() {
        // Given
        var newUser = new User();
        newUser.setEmail("TEST@EMAIL");
        var email = userRepository.save(newUser).getEmail();

        // When
        var response = restTemplate.exchange(
                URI.GET_USER_BY_EMAIL,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<UserResponse>>() {
                },
                email
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertNotNull(response.getBody().getData().id());

        Optional<User> user = userRepository.findUserByEmail(email);
        assertTrue(user.isPresent());
        assertEquals(user.get().getName(), response.getBody().getData().name());
        assertEquals(user.get().getEmail(), response.getBody().getData().email());
        assertEquals(user.get().getBirthDate(), response.getBody().getData().birthDate());
        assertEquals(user.get().getSurname(), response.getBody().getData().surname());
    }

    @Test
    @DisplayName("Should return not found when getting non-existing user by email")
    void givenNotExistingEmail_whenGetByEmail_thenReturnsNotFound() {
        // Given
        var newUser = new User();
        newUser.setEmail("TEST@EMAIL");
        userRepository.save(newUser);

        // When
        var response = restTemplate.exchange(
                URI.GET_USER_BY_EMAIL,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<UserResponse>>() {
                },
                "NOTEXISTINGEMAIL@MAIL"
        );

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertNull(response.getBody().getData());
    }

    @Test
    @DisplayName("Should return page of users when getting all users")
    void givenFewerPagesThanPageSize_whenGetAllUsers_thenReturnsPageOfUsers() {
        // Given
        for (int i = 1; i <= 2; i++) {
            var user = new User();
            user.setEmail("user" + i + "@example.com");
            userRepository.save(user);
        }

        // When
        var response = restTemplate.exchange(
                URI.GET_ALL_USERS + "?page=0&size=10",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<PagedResponse<UserResponse>>>() {
                }
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertNotNull(response.getBody().getData());

        assertEquals(2, response.getBody().getData().totalElements());
        assertEquals(1, response.getBody().getData().totalPages());
        assertEquals(2, response.getBody().getData().content().size());
    }

    @Test
    @DisplayName("Should return empty page when no users exist")
    void givenNoUsers_whenGetAllUsers_thenReturnsEmptyPage() {
        // Given

        // When
        var response = restTemplate.exchange(
                URI.GET_ALL_USERS + "?page=0&size=10",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<PagedResponse<UserResponse>>>() {
                }
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertNotNull(response.getBody().getData());

        assertEquals(0, response.getBody().getData().totalPages());
        assertEquals(0, response.getBody().getData().content().size());
    }

    @Test
    @DisplayName("Should return paginated results when users count more than page size")
    void givenMoreUsersThanPageSize_whenGetAllUsers_thenReturnsPaginatedResult() {
        // Given
        for (int i = 1; i <= 5; i++) {
            var user = new User();
            user.setEmail("user" + i + "@example.com");
            userRepository.save(user);
        }

        // When
        var response = restTemplate.exchange(
                URI.GET_ALL_USERS + "?page=0&size=2",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<PagedResponse<UserResponse>>>() {
                }
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertNotNull(response.getBody().getData());

        assertEquals(5, response.getBody().getData().totalElements());
        assertEquals(3, response.getBody().getData().totalPages());
        assertEquals(2, response.getBody().getData().content().size());
    }

    @Test
    @DisplayName("Should update user when user exists and email is not changing")
    void givenExistingUserWithoutChangingEmail_whenUpdate_thenUpdatesUser() {
        // Given
        var newUser = new User();
        newUser.setEmail("TEST@EMAIL");
        var id = userRepository.save(newUser).getId();

        var request = new UserUpdateRequest(
                "TEST_NAME",
                "TEST_SURNAME",
                LocalDate.now().minusDays(1),
                null
        );

        // When
        var response = restTemplate.exchange(
                URI.UPDATE_USER,
                HttpMethod.PATCH,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<ApiResponse<Void>>() {
                },
                id
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertNull(response.getBody().getData());

        var user = userRepository.findUserById(id).orElseThrow();

        assertEquals(request.name(), user.getName());
        assertEquals(request.surname(), user.getSurname());
        assertNotEquals(request.email(), user.getEmail());
        assertEquals(request.birthDate(), user.getBirthDate());
    }

    @Test
    @DisplayName("Should update user when user exists, email is changing and email does not exist")
    void givenExistingUserWithChangingEmailAndEmailNotExists_whenUpdate_thenUpdatesUser() {
        // Given
        var newUser = new User();
        newUser.setEmail("TEST@EMAIL");
        var id = userRepository.save(newUser).getId();

        var request = new UserUpdateRequest(
                "TEST_NAME",
                "TEST_SURNAME",
                LocalDate.now().minusDays(1),
                "NOTEXISTINGEMAIL@EMAIL"
        );

        // When
        var response = restTemplate.exchange(
                URI.UPDATE_USER,
                HttpMethod.PATCH,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<ApiResponse<Void>>() {
                },
                id
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertNull(response.getBody().getData());

        var user = userRepository.findUserById(id).orElseThrow();

        assertEquals(request.name(), user.getName());
        assertEquals(request.surname(), user.getSurname());
        assertEquals(request.email(), user.getEmail());
        assertEquals(request.birthDate(), user.getBirthDate());
    }

    @Test
    @DisplayName("Should not update user when user exists, email is changing and email exists")
    void givenExistingUserWithChangingEmailAndEmailExists_whenUpdate_thenReturnsConflict() {
        // Given
        var existingUser = new User();
        existingUser.setEmail("EXISTING@EMAIL");
        userRepository.save(existingUser);

        var newUser = new User();
        newUser.setEmail("TEST@EMAIL");
        var id = userRepository.save(newUser).getId();

        var request = new UserUpdateRequest(
                "TEST_NAME",
                "TEST_SURNAME",
                LocalDate.now().minusDays(1),
                "EXISTING@EMAIL"
        );

        // When
        var response = restTemplate.exchange(
                URI.UPDATE_USER,
                HttpMethod.PATCH,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<ApiResponse<Void>>() {
                },
                id
        );

        // Then
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertNull(response.getBody().getData());

        var user = userRepository.findUserById(id).orElseThrow();

        assertNotEquals(request.name(), user.getName());
        assertNotEquals(request.surname(), user.getSurname());
        assertNotEquals(request.email(), user.getEmail());
        assertNotEquals(request.birthDate(), user.getBirthDate());
    }

    @Test
    @DisplayName("Should return Not Found when updating non-existing user")
    void givenNonExistingUser_whenUpdate_thenReturnsNotFound() {
        // Given
        var id = UUID.randomUUID();
        var request = new UserUpdateRequest(
                "TEST_NAME",
                "TEST_SURNAME",
                LocalDate.now().minusDays(1),
                "EXISTING@EMAIL"
        );

        // When
        var response = restTemplate.exchange(
                URI.UPDATE_USER,
                HttpMethod.PATCH,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<ApiResponse<Void>>() {
                },
                id
        );

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertNull(response.getBody().getData());

        assertFalse(userRepository.existsById(id));
    }

    @Test
    @DisplayName("Should delete user when user exists")
    void givenExistingUser_whenDelete_thenDeletesUser() {
        // Given
        var newUser = new User();
        newUser.setEmail("TEST@EMAIL");
        var id = userRepository.save(newUser).getId();

        // When
        var response = restTemplate.exchange(
                URI.DELETE_USER,
                HttpMethod.DELETE,
                null,
                new ParameterizedTypeReference<ApiResponse<Void>>() {
                },
                id
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertNull(response.getBody().getData());

        assertFalse(userRepository.existsById(id));
    }

    @Test
    @DisplayName("Should delete user when user exists")
    void givenNotExistingUser_whenDelete_thenReturnsNotFound() {
        // Given
        var newUser = new User();
        newUser.setEmail("TEST@EMAIL");
        var newUserId = userRepository.save(newUser).getId();
        var id = UUID.randomUUID();

        // When
        var response = restTemplate.exchange(
                URI.DELETE_USER,
                HttpMethod.DELETE,
                null,
                new ParameterizedTypeReference<ApiResponse<Void>>() {
                },
                id
        );

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertNull(response.getBody().getData());

        assertTrue(userRepository.existsById(newUserId));
    }

    private record PagedResponse<T>(
            List<T> content,
            int totalPages,
            long totalElements,
            int size,
            int number
    ) {
    }

    private static class URI {
        private static final String CREATE_USER = "/api/v1/users";
        private static final String GET_USER_BY_ID = "/api/v1/users/{id}";
        private static final String GET_USER_BY_EMAIL = "/api/v1/users/search?email={email}";
        private static final String GET_ALL_USERS = "/api/v1/users";
        private static final String UPDATE_USER = "/api/v1/users/{id}";
        private static final String DELETE_USER = "/api/v1/users/{id}";
    }
}
