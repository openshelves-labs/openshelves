package com.openshelves.security.repository;

import com.openshelves.security.entity.AuthProviderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/// Data access for SSO identity providers.
@Repository
public interface AuthProviderRepository extends JpaRepository<AuthProviderEntity, Long> {

    /// Lists the providers shown on the sign-in page, in display order.
    List<AuthProviderEntity> findAllByEnabledTrueOrderBySortOrderAscDisplayNameAsc();

    /// Looks up an enabled provider by its URL key.
    Optional<AuthProviderEntity> findByKeyAndEnabledTrue(String key);

    /// Looks up a provider by its URL key, enabled or not.
    Optional<AuthProviderEntity> findByKey(String key);

    /// Looks up the providers behind the given issuers.
    List<AuthProviderEntity> findAllByIssuerIn(Collection<String> issuers);
}
