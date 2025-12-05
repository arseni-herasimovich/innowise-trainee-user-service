package com.innowise.userservice.repository;

import com.innowise.userservice.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findUserByEmail(String email);

    boolean existsByEmail(String email);

    /*
        There's no way to use pageable with entity graph (we need to resolve n+1)
        So we get user ids firstly and then fetch users with cards
     */

    @Query("select u.id from User u")
    Page<UUID> findUserIds(Pageable pageable);

    @EntityGraph(attributePaths = {"cards"})
    @Query("select u from User u where u.id in :ids")
    List<User> findAllWithCardsByIds(@Param("ids") List<UUID> ids);

    // redundant methods only to follow the task requirements

    @Query("from User u where u.userId = :userId")
    Optional<User> findUserByUserId(String userId);

    @Modifying
    @Query("""
            update User u
            set u.name = :name, u.surname = :surname, u.birthDate = :birthDate, u.email = :email
            where u.id = :id
    """)
    void update(UUID id, String name, String surname, LocalDate birthDate, String email);

    @Modifying
    @Query(value = """
            DELETE FROM users
            WHERE id = :id
            """, nativeQuery = true)
    void delete(UUID id);

    boolean existsByUserId(String userId);

    void deleteByUserId(String userId);
}
