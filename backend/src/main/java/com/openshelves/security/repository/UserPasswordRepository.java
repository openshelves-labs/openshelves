package com.openshelves.security.repository;

import com.openshelves.security.entity.UserPasswordEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/// Data access for user passwords.
@Repository
public interface UserPasswordRepository extends JpaRepository<UserPasswordEntity, Long> {

    /// Looks up a user's password from what they typed at login: their email or institutional ID.
    /// Deleted users are never matched.
    @Query("""
        SELECT p FROM UserPasswordEntity p
        JOIN FETCH p.userProfile u
        WHERE u.deletedAt IS NULL AND (
            lower(u.email) = lower(:identifier) OR u.institutionalId = :identifier
        )
        """)
    Optional<UserPasswordEntity> findByLoginIdentifier(String identifier);

    /// Looks up a password and locks its row until the transaction ends,
    /// so concurrent logins update its failed-attempt count one at a time.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserPasswordEntity> findAndLockById(Long id);
}
