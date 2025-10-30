package com.innowise.userservice.controller;

import com.innowise.userservice.dto.ApiResponse;
import com.innowise.userservice.dto.CardCreateRequest;
import com.innowise.userservice.dto.CardResponse;
import com.innowise.userservice.dto.UserResponse;
import com.innowise.userservice.entity.Card;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.repository.CardRepository;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CardControllerTest {

    @BeforeEach
    void clearCardRepository() {
        cardRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @Container
    private static final GenericContainer<?> redis = new GenericContainer<>("redis:7")
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort());
    ;

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Should create card and return response when valid data provided")
    void givenValidData_whenCreate_thenSavesCardAndReturnsCardResponse() {
        // Given
        var newUser = new User();
        newUser.setEmail("TEST@EMAIL");
        var id = userRepository.save(newUser).getId();

        var request = new CardCreateRequest(
                id,
                "TEST_NUMBER",
                "TEST_HOLDER",
                Instant.now().plus(24, ChronoUnit.HOURS)
        );

        // When
        var response = restTemplate.exchange(
                URI.CARD_CREATE,
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<ApiResponse<CardResponse>>() {
                }
        );

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertNotNull(response.getBody().getData().id());

        var card = cardRepository.findCardById(response.getBody().getData().id());
        assertTrue(card.isPresent());
        assertEquals(request.userId(), card.get().getUser().getId());
        assertEquals(request.number(), card.get().getNumber());
        assertEquals(request.holder(), card.get().getHolder());
    }

    @Test
    @DisplayName("Should return Conflict when creating card with existing number")
    void givenExistingCardNumber_whenCreate_thenReturnsConflict() {
        // Given
        var newUser = new User();
        newUser.setEmail("TEST@EMAIL");
        var id = userRepository.save(newUser).getId();
        var newCard = new Card(
                null,
                newUser,
                "TEST_NUMBER",
                "TEST_HOLDER",
                Instant.now().plus(24, ChronoUnit.HOURS)
        );
        cardRepository.save(newCard);
        var request = new CardCreateRequest(
                id,
                "TEST_NUMBER",
                "TEST_HOLDER",
                Instant.now().plus(24, ChronoUnit.HOURS)
        );

        // When
        var response = restTemplate.exchange(
                URI.CARD_CREATE,
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<ApiResponse<CardResponse>>() {
                }
        );

        // Then
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertNull(response.getBody().getData());

        var cards = userRepository.findAll();
        assertEquals(1, cards.size());
    }

    @Test
    @DisplayName("Should return card response when getting existing card by id")
    void givenExistingId_whenGetById_thenReturnsCardResponse() {
        // Given
        var newUser = new User();
        newUser.setEmail("TEST@EMAIL");
        userRepository.save(newUser);

        var newCard = new Card(
                null,
                newUser,
                "TEST_NUMBER",
                "TEST_HOLDER",
                Instant.now().plus(24, ChronoUnit.HOURS)
        );
        var id = cardRepository.save(newCard).getId();

        // When
        var response = restTemplate.exchange(
                URI.GET_CARD_BY_ID,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<CardResponse>>() {
                },
                id
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertNotNull(response.getBody().getData().id());

        var card = cardRepository.findCardById(id);
        assertTrue(card.isPresent());
        assertEquals(card.get().getId(), response.getBody().getData().id());
        assertEquals(card.get().getNumber(), response.getBody().getData().number());
    }

    @Test
    @DisplayName("Should return not found when getting non-existing card by id")
    void givenNotExistingId_whenGetById_thenReturnsNotFound() {
        // Given
        var newUser = new User();
        newUser.setEmail("TEST@EMAIL");
        userRepository.save(newUser);

        var newCard = new Card(
                null,
                newUser,
                "TEST_NUMBER",
                "TEST_HOLDER",
                Instant.now().plus(24, ChronoUnit.HOURS)
        );
        cardRepository.save(newCard);

        // When
        var response = restTemplate.exchange(
                URI.GET_CARD_BY_ID,
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
    @DisplayName("Should return page of cards when getting all cards")
    void givenFewerPagesThanPageSize_whenGetAllCards_thenReturnsPageOfCards() {
        // Given
        var newUser = new User();
        newUser.setEmail("TEST@EMAIL");
        userRepository.save(newUser);

        for (int i = 1; i <= 2; i++) {
            cardRepository.save(new Card(
                    null,
                    newUser,
                    "TEST_NUMBER" + i,
                    "TEST_HOLDER" + i,
                    Instant.now().plus(24, ChronoUnit.HOURS)
            ));
        }

        // When
        var response = restTemplate.exchange(
                URI.CARD_GET_ALL + "?page=0&size=10",
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
    @DisplayName("Should return empty page when no cards exist")
    void givenNoCards_whenGetAllCards_thenReturnsEmptyPage() {
        // Given

        // When
        var response = restTemplate.exchange(
                URI.CARD_GET_ALL + "?page=0&size=10",
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
    @DisplayName("Should return paginated results when cards count more than page size")
    void givenMoreCardsThanPageSize_whenGetAllCards_thenReturnsPaginatedResult() {
        // Given
        var newUser = new User();
        newUser.setEmail("TEST@EMAIL");
        userRepository.save(newUser);

        for (int i = 1; i <= 5; i++) {
            cardRepository.save(new Card(
                    null,
                    newUser,
                    "TEST_NUMBER" + i,
                    "TEST_HOLDER" + i,
                    Instant.now().plus(24, ChronoUnit.HOURS)
            ));
        }

        // When
        var response = restTemplate.exchange(
                URI.CARD_GET_ALL + "?page=0&size=2",
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
    @DisplayName("Should delete card when card exists")
    void givenExistingCard_whenDelete_thenDeletesCard() {
        // Given
        var newUser = new User();
        newUser.setEmail("TEST@EMAIL");
        userRepository.save(newUser);

        var newCard = new Card(
                null,
                newUser,
                "TEST_NUMBER",
                "TEST_HOLDER",
                Instant.now().plus(24, ChronoUnit.HOURS)
        );
        var id = cardRepository.save(newCard).getId();

        // When
        var response = restTemplate.exchange(
                URI.CARD_DELETE,
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

        assertFalse(cardRepository.existsById(id));
    }

    @Test
    @DisplayName("Should return not found when deleting non-existing card")
    void givenNotExistingCard_whenDelete_thenReturnsNotFound() {
        // Given
        var newUser = new User();
        newUser.setEmail("TEST@EMAIL");
        userRepository.save(newUser);

        var newCard = new Card(
                null,
                newUser,
                "TEST_NUMBER",
                "TEST_HOLDER",
                Instant.now().plus(24, ChronoUnit.HOURS)
        );
        cardRepository.save(newCard);

        // When
        var response = restTemplate.exchange(
                URI.CARD_DELETE,
                HttpMethod.DELETE,
                null,
                new ParameterizedTypeReference<ApiResponse<Void>>() {
                },
                UUID.randomUUID()
        );

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertNull(response.getBody().getData());

        assertEquals(1, cardRepository.findAll().size());
    }

    private record PagedResponse<T>(
            java.util.List<T> content,
            int totalPages,
            long totalElements,
            int size,
            int number
    ) {
    }

    private static class URI {
        private static final String CARD_CREATE = "/api/v1/cards";
        private static final String GET_CARD_BY_ID = "/api/v1/cards/{id}";
        private static final String CARD_GET_ALL = "/api/v1/cards";
        private static final String CARD_DELETE = "/api/v1/cards/{id}";
    }
}
