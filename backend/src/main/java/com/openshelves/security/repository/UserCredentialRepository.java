package com.openshelves.security.repository;

import com.openshelves.security.entity.UserCredentialEntity;
import com.openshelves.security.enums.CredentialType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/// Data access for the ways users sign in: passwords and linked SSO accounts.
@Repository
public interface UserCredentialRepository extends JpaRepository<UserCredentialEntity, Long> {

    /// Looks up a user's password credential from what they typed at login: their email or institutional ID.
    /// Deleted users are never matched.
    @Query("""
        SELECT c FROM UserCredentialEntity c
        JOIN FETCH c.userProfile u
        WHERE c.credentialType = PASSWORD AND u.deletedAt IS NULL AND (
            lower(u.email) = lower(:identifier) OR u.institutionalId = :identifier
        )
        """)
    Optional<UserCredentialEntity> findByLoginIdentifier(String identifier);

    /// Looks up the SSO credential linked to the identity the provider sent back.
    /// Deleted users are never matched.
    @Query("""
        SELECT c FROM UserCredentialEntity c
        JOIN FETCH c.userProfile u
        WHERE c.credentialType = :credentialType AND c.providerKey = :providerKey AND u.deletedAt IS NULL
        """)
    Optional<UserCredentialEntity> findBySSOProviderKey(CredentialType credentialType, String providerKey);

    /// Looks up a credential and locks its row until the transaction ends,
    /// so concurrent logins update its failed-attempt count one at a time.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserCredentialEntity> findAndLockById(Long id);
}
