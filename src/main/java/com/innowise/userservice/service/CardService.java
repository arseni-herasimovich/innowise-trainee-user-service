package com.innowise.userservice.service;

import com.innowise.userservice.dto.CardCreateRequest;
import com.innowise.userservice.dto.CardResponse;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CardService {
    CardResponse create(CardCreateRequest request);

    CardResponse getById(UUID id);

    Page<CardResponse> getAllPaged(Pageable pageable);

    @Transactional
    void delete(UUID id);
}
