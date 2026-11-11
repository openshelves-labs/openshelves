package com.openshelves.security.repository;

import com.openshelves.security.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/// Data access for users.
@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    /// Looks up a user from what they typed: their email or institutional ID. Deleted users are never matched.
    @Query("""
        SELECT u FROM UserEntity u
        WHERE u.deletedAt IS NULL AND (
            lower(u.email) = lower(:identifier) OR u.institutionalId = :identifier
        )
        """)
    Optional<UserEntity> findByLoginIdentifier(String identifier);
}
