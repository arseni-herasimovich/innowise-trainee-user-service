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
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
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
    private final CacheManager cacheManager;

    @Override
    @CacheEvict(value = "USER_CACHE", key = "#request.userId()")
    public CardResponse create(CardCreateRequest request) {
        if (cardRepository.existsByNumber(request.number())) {
            throw new CardNumberAlreadyExistsException(request.number());
        }

        var user = userService.getEntityById(request.userId());
        var card = cardMapper.toEntity(request);
        card.setUser(user);
        return cardMapper.toResponse(
                cardRepository.save(card)
        );
    }

    @Override
    public CardResponse getById(UUID id) {
        return cardRepository.findCardById(id)
                .map(cardMapper::toResponse)
                .orElseThrow(() -> new CardNotFoundException(id));
    }

    @Override
    public Page<CardResponse> getAllPaged(Pageable pageable) {
        return cardRepository.findAll(pageable).map(cardMapper::toResponse);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        var cache = cacheManager.getCache("USER_CACHE");
        cardRepository.findCardById(id)
                .ifPresentOrElse(
                        card -> {
                            cardRepository.delete(id);
                            if (cache != null) {
                                cache.evict(card.getUser().getId());
                            }
                        },
                        () -> {
                            throw new CardNotFoundException(id);
                        }
                );
    }
}
