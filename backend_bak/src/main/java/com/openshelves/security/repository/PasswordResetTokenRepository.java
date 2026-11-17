package com.openshelves.security.repository;

import com.openshelves.security.entity.PasswordResetTokenEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/// Data access for emailed password reset links.
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Long> {

    /// Looks up a token by its hash, locking its row until the transaction ends,
    /// so two parallel requests can't both use it.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "userProfile")
    Optional<PasswordResetTokenEntity> findAndLockByTokenHash(String tokenHash);

    /// Checks whether a link was sent to the user after the given time.
    boolean existsByUserProfileIdAndCreatedAtAfter(Long userId, Instant after);

    /// Removes the user's earlier links, so only the newest one works.
    @Modifying
    @Query("DELETE FROM PasswordResetTokenEntity t WHERE t.userProfile.id = :userId")
    void deleteAllByUserId(Long userId);
}
