package com.openshelves.security.repository;

import com.openshelves.security.entity.UserSessionEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/// Data access for login sessions.
@Repository
public interface UserSessionRepository extends JpaRepository<UserSessionEntity, Long> {

    // -----------------------------------------------------------------------
    // Lookups
    // -----------------------------------------------------------------------

    /// Looks up the session behind a bearer token, using the token's hash.
    @EntityGraph(attributePaths = "userProfile")
    Optional<UserSessionEntity> findBySessionTokenHash(String sessionTokenHash);

    /// Looks up a session by its ID, along with the user it belongs to.
    @EntityGraph(attributePaths = "userProfile")
    Optional<UserSessionEntity> findWithUserProfileById(Long id);

    /// Looks up a session by its ID, only if it belongs to the given user.
    Optional<UserSessionEntity> findByIdAndUserProfileId(Long id, Long userId);

    /// Lists the user's sessions that are neither revoked nor expired, most recently active first.
    @Query("""
        SELECT s FROM UserSessionEntity s
        WHERE s.userProfile.id = :userId AND s.revokedAt IS NULL AND s.expiresAt > :now
        ORDER BY s.lastActiveAt DESC
        """)
    List<UserSessionEntity> findActiveByUserId(Long userId, Instant now);


    // -----------------------------------------------------------------------
    // Updates
    // -----------------------------------------------------------------------

    /// Records activity on a session, but only if the last recorded activity is older than `threshold`,
    /// so a busy session writes at most once per throttle window.
    ///
    /// @return `1` if the session was updated, `0` if it was still recent
    @Transactional
    @Modifying
    @Query("""
        UPDATE UserSessionEntity s
        SET s.lastActiveAt = :now
        WHERE s.id = :sessionId AND s.lastActiveAt < :threshold
        """)
    int touchLastActiveAt(Long sessionId, Instant now, Instant threshold);

    /// Revokes every active session of the user except the given one.
    ///
    /// @return the number of sessions revoked
    @Transactional
    @Modifying
    @Query("""
        UPDATE UserSessionEntity s
        SET s.revokedAt = :now
        WHERE s.userProfile.id = :userId AND s.id <> :keepSessionId
          AND s.revokedAt IS NULL AND s.expiresAt > :now
        """)
    int revokeAllExcept(Long userId, Long keepSessionId, Instant now);
}
