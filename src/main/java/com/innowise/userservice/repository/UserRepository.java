package com.innowise.userservice.repository;

import com.innowise.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findUserByEmail(String email);

    // redundant methods only to follow the task requirements

    @Query("from User u where u.id = :id")
    Optional<User> findUserById(UUID id);

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

    boolean existsByEmail(String email);
}
