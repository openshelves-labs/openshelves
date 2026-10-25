package com.openshelves.security.repository;

import com.openshelves.security.entity.UserSessionEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/// Data access for login sessions.
@Repository
public interface UserSessionRepository extends JpaRepository<UserSessionEntity, Long> {

    /// Looks up the session behind a bearer token, using the token's hash.
    @EntityGraph(attributePaths = "userProfile")
    Optional<UserSessionEntity> findBySessionTokenHash(String sessionTokenHash);

    /// Looks up a session by its ID, along with the user it belongs to.
    @EntityGraph(attributePaths = "userProfile")
    Optional<UserSessionEntity> findWithUserProfileById(Long id);
}
