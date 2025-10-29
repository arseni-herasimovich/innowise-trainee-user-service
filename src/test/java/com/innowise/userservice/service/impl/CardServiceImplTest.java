package com.innowise.userservice.service.impl;

import com.innowise.userservice.dto.CardCreateRequest;
import com.innowise.userservice.dto.CardResponse;
import com.innowise.userservice.entity.Card;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.exception.CardNotFoundException;
import com.innowise.userservice.exception.CardNumberAlreadyExistsException;
import com.innowise.userservice.exception.UserNotFoundException;
import com.innowise.userservice.mapper.CardMapper;
import com.innowise.userservice.repository.CardRepository;
import com.innowise.userservice.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceImplTest {
    @Mock
    private UserService userService;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardMapper cardMapper;

    @InjectMocks
    private CardServiceImpl cardService;

    @Test
    @DisplayName("Should create card and return response when valid data provided")
    void givenValidData_whenCreate_thenSavesCardAndReturnsResponse() {
        // Given
        var request = createCardCreateRequest();
        var user = createUser(request.userId());
        var card = createCard(request.number(), request.holder(), request.expirationDate());
        var response = createCardResponse(card, user.getId());

        // When
        when(cardRepository.existsByNumber(request.number())).thenReturn(false);
        when(userService.getEntityById(request.userId())).thenReturn(user);
        when(cardMapper.toEntity(any(CardCreateRequest.class))).thenReturn(card);
        when(cardRepository.save(any(Card.class))).thenReturn(card);
        when(cardMapper.toResponse(any(Card.class))).thenReturn(response);

        var serviceResponse = cardService.create(request);

        // Then
        assertEquals(response, serviceResponse);

        verify(cardRepository, times(1)).save(card);
        verify(cardRepository, times(1)).existsByNumber(request.number());
    }

    @Test
    @DisplayName("Should throw CardNumberAlreadyExistsException when creating card with existing number")
    void givenExistingCardNumber_whenCreate_thenThrowsCardNumberAlreadyExistsException() {
        // Given
        var request = createCardCreateRequest();

        // When
        when(cardRepository.existsByNumber(request.number())).thenReturn(true);

        // Then
        assertThrows(CardNumberAlreadyExistsException.class, () -> cardService.create(request));

        verify(cardRepository, times(1)).existsByNumber(request.number());
        verify(cardRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when creating card for non-existing user")
    void givenNonExistingUser_whenCreate_thenThrowsUserNotFoundException() {
        // Given
        var request = createCardCreateRequest();

        // When
        when(cardRepository.existsByNumber(request.number())).thenReturn(false);
        when(userService.getEntityById(request.userId())).thenThrow(new UserNotFoundException(request.userId()));

        // Then
        assertThrows(UserNotFoundException.class, () -> cardService.create(request));

        verify(cardRepository, times(1)).existsByNumber(request.number());
        verify(cardRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return card response when getting existing card by ID")
    void givenExistingCard_whenGetById_thenReturnsCardResponse() {
        // Given
        var card = createCard("TEST_NUMBER", "TEST_HOLDER", Instant.now().plus(24, ChronoUnit.HOURS));
        var response = createCardResponse(card, UUID.randomUUID());

        // When
        when(cardRepository.findCardById(card.getId())).thenReturn(Optional.of(card));
        when(cardMapper.toResponse(any(Card.class))).thenReturn(response);

        var serviceResponse = cardService.getById(card.getId());

        // Then
        assertEquals(response, serviceResponse);

        verify(cardRepository, times(1)).findCardById(card.getId());
        verify(cardMapper, times(1)).toResponse(card);
    }

    @Test
    @DisplayName("Should throw CardNotFoundException when getting non-existing card by ID")
    void givenNonExistingCard_whenGetById_thenThrowsCardNotFoundException() {
        // Given
        var card = createCard("TEST_NUMBER", "TEST_HOLDER", Instant.now().plus(24, ChronoUnit.HOURS));

        // When
        when(cardRepository.findCardById(card.getId())).thenReturn(Optional.empty());

        // Then
        assertThrows(CardNotFoundException.class, () -> cardService.getById(card.getId()));

        verify(cardRepository, times(1)).findCardById(card.getId());
        verify(cardMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("Should return page of card responses when getting all existing cards")
    void givenExistingCards_whenGetAllPaged_thenReturnsPageOfCardResponses() {
        // Given
        var card = createCard("TEST_NUMBER", "TEST_HOLDER",
                Instant.now().plus(24, ChronoUnit.HOURS));
        var response = createCardResponse(card, UUID.randomUUID());
        var pageable = PageRequest.of(0, 10);
        var page = new PageImpl<>(List.of(card), pageable, 1);

        // When
        when(cardRepository.findAll(pageable)).thenReturn(page);
        when(cardMapper.toResponse(any(Card.class))).thenReturn(response);

        var serviceResponse = cardService.getAllPaged(pageable);

        // Then
        assertEquals(1, serviceResponse.getTotalElements());
        assertEquals(response, serviceResponse.getContent().get(0));

        verify(cardRepository, times(1)).findAll(pageable);
        verify(cardMapper, times(1)).toResponse(card);
    }

    @Test
    @DisplayName("Should return empty page when no cards exist")
    void givenNoCards_whenGetAllPaged_thenReturnsEmptyPage() {
        // Given
        var pageable = PageRequest.of(0, 10);
        Page<Card> page = new PageImpl<>(java.util.List.of(), pageable, 0);

        // When
        when(cardRepository.findAll(pageable)).thenReturn(page);

        var serviceResponse = cardService.getAllPaged(pageable);

        // Then
        assertEquals(0, serviceResponse.getTotalElements());
        assertEquals(0, serviceResponse.getContent().size());

        verify(cardRepository, times(1)).findAll(pageable);
        verify(cardMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("Should delete card and evict user cache when card exists")
    void givenExistingCard_whenDelete_thenDeletesCardAndEvictsUserCache() {
        // Given
        var card = createCard("TEST_NUMBER", "TEST_HOLDER", Instant.now().plus(24, ChronoUnit.HOURS));
        var user = createUser(UUID.randomUUID());
        card.setUser(user);

        // When
        when(cardRepository.findCardById(card.getId())).thenReturn(Optional.of(card));

        cardService.delete(card.getId());

        // Then
        verify(cardRepository, times(1)).findCardById(card.getId());
        verify(cardRepository, times(1)).delete(card.getId());
        verify(userService, times(1)).evictUserCache(card.getUser());
    }

    @Test
    @DisplayName("Should throw CardNotFoundException when deleting non-existing card")
    void givenNonExistingCard_whenDelete_thenThrowsCardNotFoundException() {
        // Given
        var cardId = UUID.randomUUID();

        // When
        when(cardRepository.findCardById(cardId)).thenReturn(Optional.empty());

        // Then
        assertThrows(CardNotFoundException.class, () -> cardService.delete(cardId));

        verify(cardRepository, times(1)).findCardById(cardId);
        verify(cardRepository, never()).delete(any(UUID.class));
    }

    private CardCreateRequest createCardCreateRequest() {
        return new CardCreateRequest(
                UUID.randomUUID(),
                "TEST_NUMBER",
                "TEST_HOLDER",
                Instant.now().plus(24, ChronoUnit.HOURS)
        );
    }

    private User createUser(UUID id) {
        var user = new User();
        user.setId(id);
        return user;
    }

    private Card createCard(String number, String holder, Instant expirationDate) {
        return new Card(
                UUID.randomUUID(),
                null,
                number,
                holder,
                expirationDate
        );
    }

    private CardResponse createCardResponse(Card card, UUID userId) {
        return new CardResponse(
                card.getId(),
                userId,
                card.getNumber(),
                card.getHolder(),
                card.getExpirationDate()
        );
    }
}