package com.innowise.userservice.controller;

import com.innowise.userservice.dto.ApiResponse;
import com.innowise.userservice.dto.UserCreateRequest;
import com.innowise.userservice.dto.UserResponse;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.repository.UserRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserControllerTest {

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
        userRepository.deleteAll();
        var request = new UserCreateRequest(
                "TEST_NAME",
                "TEST_SURNAME",
                LocalDate.now().minusDays(1),
                "TEST@EMAIL"
        );

        // When
        var response = restTemplate.exchange(
                "/api/v1/users",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<ApiResponse<UserResponse>>() {}
        );

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertNotNull(response.getBody().getData().id());

        Optional<User> user = userRepository.findUserById(response.getBody().getData().id());
        assertTrue(user.isPresent());
        assertEquals(request.name(), user.get().getName());
        assertEquals(request.email(), user.get().getEmail());
        assertEquals(request.birthDate(), user.get().getBirthDate());
        assertEquals(request.email(), user.get().getEmail());
    }

    @Test
    @DisplayName("Should return conflict when creating user with existing email")
    void givenExistingEmail_whenCreate_thenReturnsBadRequest() {
        // Given
        userRepository.deleteAll();
        User user = new User();
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
                "/api/v1/users",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<ApiResponse<UserResponse>>() {}
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
}
