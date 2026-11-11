package com.openshelves.security.repository;

import com.openshelves.security.entity.UserIdentityEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/// Data access for SSO accounts users have connected.
@Repository
public interface UserIdentityRepository extends JpaRepository<UserIdentityEntity, Long> {

    /// Looks up the identity the provider sent back, along with its user.
    @EntityGraph(attributePaths = "userProfile")
    Optional<UserIdentityEntity> findByIssuerAndSubject(String issuer, String subject);

    /// Lists a user's connected identities, oldest first.
    List<UserIdentityEntity> findAllByUserProfileIdOrderByCreatedAtAsc(Long userId);

    /// Looks up an identity by its ID, only if it belongs to the given user.
    Optional<UserIdentityEntity> findByIdAndUserProfileId(Long id, Long userId);

    /// Counts a user's connected identities.
    long countByUserProfileId(Long userId);
}
