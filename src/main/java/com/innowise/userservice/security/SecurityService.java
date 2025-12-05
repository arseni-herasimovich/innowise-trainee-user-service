package com.innowise.userservice.security;

import com.innowise.userservice.dto.CardCreateRequest;
import com.innowise.userservice.dto.UserCreateRequest;
import com.innowise.userservice.exception.AccessDeniedException;
import com.innowise.userservice.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("securityService")
@RequiredArgsConstructor
@Slf4j
public class SecurityService {
    private final CardRepository cardRepository;

    public boolean canCreateUser(String userId, UserCreateRequest request) {
        log.debug("Authorizing user creation with id {} by user with id {}", request != null ? request.userId() : "null", userId);
        if (userId != null && request != null && userId.equals(request.userId())) {
            return true;
        }
        throw new AccessDeniedException("You do not have rights to create user with id not equals to yours");
    }

    public boolean canCreateCard(String userId, CardCreateRequest request) {
        log.debug("Authorizing card creation with userId {} by user with id {}", request != null ? request.userId() : "null", userId);
        if (userId != null && request != null && userId.equals(request.userId())) {
            return true;
        }
        throw new AccessDeniedException("You do not have rights to create card with userId not equals to yours");
    }

    public boolean canAccessCard(String userId, UUID cardId) {
        log.debug("Authorizing accessing card with cardId {} by user with id {}", cardId, userId);
        if (userId == null || cardId == null) {
            throw new AccessDeniedException("You do not have rights to access this card");
        }

        return cardRepository.findCardById(cardId)
                .filter(card -> card.getUser().getUserId().equals(userId))
                .map(card -> true)
                .orElseThrow(() -> new AccessDeniedException("You do not have rights to access this card"));
    }
}
