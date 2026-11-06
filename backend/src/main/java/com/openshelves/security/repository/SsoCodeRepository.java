package com.openshelves.security.repository;

import com.openshelves.security.entity.SsoCodeEntity;
import com.openshelves.security.enums.SsoCodePurpose;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/// Data access for short-lived SSO codes.
@Repository
public interface SsoCodeRepository extends JpaRepository<SsoCodeEntity, Long> {

    /// Looks up a code by its hash and purpose, locking its row until the transaction ends,
    /// so two parallel requests can't both use it.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "userProfile")
    Optional<SsoCodeEntity> findAndLockByCodeHashAndPurpose(String codeHash, SsoCodePurpose purpose);
}
