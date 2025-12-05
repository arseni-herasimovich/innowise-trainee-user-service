package com.innowise.userservice.service.impl;

import com.innowise.userservice.dto.CardCreateRequest;
import com.innowise.userservice.dto.CardResponse;
import com.innowise.userservice.exception.CardNotFoundException;
import com.innowise.userservice.exception.CardNumberAlreadyExistsException;
import com.innowise.userservice.mapper.CardMapper;
import com.innowise.userservice.repository.CardRepository;
import com.innowise.userservice.service.CardService;
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
public class CardServiceImpl implements CardService {
    private final UserService userService;
    private final CardRepository cardRepository;
    private final CardMapper cardMapper;

    @Override
    public CardResponse create(CardCreateRequest request) {
        if (cardRepository.existsByNumber(request.number())) {
            throw new CardNumberAlreadyExistsException(request.number());
        }

        var user = userService.getEntityByUserId(request.userId());
        var card = cardMapper.toEntity(request);
        card.setUser(user);
        cardRepository.save(card);
        userService.evictUserCache(user);
        return cardMapper.toResponse(card);
    }

    @Override
    public CardResponse getById(UUID id) {
        return cardRepository.findCardById(id)
                .map(cardMapper::toResponse)
                .orElseThrow(() -> new CardNotFoundException(id));
    }

    @Override
    public Page<CardResponse> getAllPaged(Pageable pageable) {
        var ids = cardRepository.findCardIds(pageable);
        var cards = cardRepository.findAllWithUsersByIds(ids.getContent())
                .stream()
                .map(cardMapper::toResponse)
                .toList();
        return new PageImpl<>(cards, pageable, ids.getTotalElements());
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        cardRepository.findCardById(id)
                .ifPresentOrElse(
                        card -> {
                            cardRepository.delete(id);
                            userService.evictUserCache(card.getUser());
                        },
                        () -> {
                            throw new CardNotFoundException(id);
                        }
                );
    }
}
