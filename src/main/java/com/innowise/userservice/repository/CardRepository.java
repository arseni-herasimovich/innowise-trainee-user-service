package com.innowise.userservice.repository;

import com.innowise.userservice.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, UUID> {

    // redundant methods only to follow the task requirements

    @Query("from Card c where c.id = :id")
    Optional<Card> findCardById(UUID id);

    @Modifying
    @Query(value = """
        UPDATE card_info
        SET user_id = :userId, number = :number, holder = :holder, expiration_date = :expirationDate
        WHERE id = :id
    """, nativeQuery = true)
    void update(UUID id, UUID userId, String number, String holder, Instant expirationDate);

    @Modifying
    @Query(value = """
        delete from Card c
        where c.id = :id
    """)
    void delete(UUID id);
}
