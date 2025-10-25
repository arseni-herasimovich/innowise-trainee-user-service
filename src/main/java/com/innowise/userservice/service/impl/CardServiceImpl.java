package com.innowise.userservice.service.impl;

import com.innowise.userservice.dto.CardCreateRequest;
import com.innowise.userservice.dto.CardResponse;
import com.innowise.userservice.exception.CardNotFoundException;
import com.innowise.userservice.mapper.CardMapper;
import com.innowise.userservice.repository.CardRepository;
import com.innowise.userservice.service.CardService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {
    private final CardRepository cardRepository;
    private final CardMapper cardMapper;

    @Override
    public CardResponse create(CardCreateRequest request) {
        return cardMapper.toResponse(
                cardRepository.save(cardMapper.toEntity(request))
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
        cardRepository.delete(id);
    }
}
