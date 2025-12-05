package com.innowise.userservice.security;

import com.innowise.userservice.dto.CardCreateRequest;
import com.innowise.userservice.dto.UserCreateRequest;
import com.innowise.userservice.entity.Card;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.exception.AccessDeniedException;
import com.innowise.userservice.repository.CardRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {
    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private SecurityService securityService;

    @Nested
    @DisplayName("Ability of creating user")
    class CanCreateUserTests {
        @Test
        @DisplayName("Should create user only with the same id")
        void givenSameId_whenCanCreateUser_thenReturnTrue() {
            // Given
            var request = getUserCreateRequest();

            // When
            var result = securityService.canCreateUser(request.userId(), request);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should throw exception when ids differ")
        void givenDifferentId_whenCanCreateUser_thenThrowsException() {
            // Given
            var request = getUserCreateRequest();

            // When, Then
            assertThrows(AccessDeniedException.class,
                    () -> securityService.canCreateUser(UUID.randomUUID().toString(), request));
        }

        @Test
        @DisplayName("Should throw exception when id is null")
        void givenNoId_whenCanCreateUser_thenThrowsException() {
            // Given
            var request = getUserCreateRequest();

            // When, Then
            assertThrows(AccessDeniedException.class, () -> securityService.canCreateUser(null, request));
        }

        @Test
        @DisplayName("Should throw exception when request is null")
        void givenNoRequest_whenCanCreateUser_thenThrowsException() {
            // Given, When, Then
            assertThrows(AccessDeniedException.class, () -> securityService.canCreateUser(null, null));
        }
    }

    @Nested
    @DisplayName("Ability of creating card")
    class CanCreateCardTests {
        @Test
        @DisplayName("Should create card only with the principal's user id")
        void givenRightId_whenCanCreateCard_thenReturnTrue() {
            // Given
            var request = getCardCreateRequest();

            // When
            var result = securityService.canCreateCard(request.userId(), request);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should throw exception when principal's user id and card's user id differ")
        void givenDifferentId_whenCanCreateCard_thenThrowsException() {
            // Given
            var request = getCardCreateRequest();

            // When, Then
            assertThrows(AccessDeniedException.class,
                    () -> securityService.canCreateCard(UUID.randomUUID().toString(), request));
        }

        @Test
        @DisplayName("Should throw exception when id is null")
        void givenNoId_whenCanCreateCard_thenThrowsException() {
            // Given
            var request = getCardCreateRequest();

            // When, Then
            assertThrows(AccessDeniedException.class, () -> securityService.canCreateCard(null, request));
        }

        @Test
        @DisplayName("Should throw exception when request is null")
        void givenNoRequest_whenCanCreateCard_thenThrowsException() {
            // Given, When, Then
            assertThrows(AccessDeniedException.class, () -> securityService.canCreateCard(null, null));
        }
    }

    @Nested
    @DisplayName("Ability of accessing card")
    class CanAccessCardTests {
        @Test
        @DisplayName("Should access card only with the principal's user id")
        void givenRightId_whenCanAccessCard_thenReturnTrue() {
            // Given
            var userId = UUID.randomUUID().toString();
            var cardId = UUID.randomUUID();
            var card = new Card(
                    cardId,
                    getUserWithUserId(userId),
                    "TEST_NUMBER",
                    "TEST_HOLDER",
                    LocalDate.now().plusDays(1)
            );

            // When
            when(cardRepository.findCardById(cardId)).thenReturn(Optional.of(card));
            var result = securityService.canAccessCard(userId, cardId);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should throw exception when principal's user id and card's user id differ")
        void givenDifferentId_whenCanAccessCard_thenThrowsException() {
            // Given
            var userId = UUID.randomUUID().toString();
            var card = new Card(
                    UUID.randomUUID(),
                    getUserWithUserId(userId),
                    "TEST_NUMBER",
                    "TEST_HOLDER",
                    LocalDate.now().plusDays(1)
            );

            // When, Then
            when(cardRepository.findCardById(card.getId())).thenReturn(Optional.of(card));
            assertThrows(AccessDeniedException.class,
                    () -> securityService.canAccessCard(UUID.randomUUID().toString(), card.getId()));
        }

        @Test
        @DisplayName("Should throw exception when principal's user id is null")
        void givenNoUserId_whenCanAccessCard_thenThrowsException() {
            // Given, When, Then
            assertThrows(AccessDeniedException.class, () -> securityService.canAccessCard(null, UUID.randomUUID()));
        }

        @Test
        @DisplayName("Should throw exception when card's user id is null")
        void givenNoCardId_whenCanAccessCard_thenThrowsException() {
            // Given, When, Then
            assertThrows(AccessDeniedException.class,
                    () -> securityService.canAccessCard(UUID.randomUUID().toString(), null));
        }
    }

    private UserCreateRequest getUserCreateRequest() {
        return new UserCreateRequest(
                UUID.randomUUID().toString(),
                "TEST_NAME",
                "TEST_SURNAME",
                LocalDate.now().minusDays(1),
                "TEST@EMAIL"
        );
    }

    private CardCreateRequest getCardCreateRequest() {
        return new CardCreateRequest(
                UUID.randomUUID().toString(),
                "TEST_NUMBER",
                "TEST_HOLDER",
                LocalDate.now().plusDays(1)
        );
    }

    private User getUserWithUserId(String userId) {
        return new User(
                UUID.randomUUID(),
                userId,
                "TEST_NAME",
                "TEST_SURNAME",
                LocalDate.now().minusDays(1),
                "test",
                null
        );
    }
}

