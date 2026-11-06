package com.openshelves.security.repository;

import com.openshelves.security.entity.AuthProviderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

    /// Stores a newly generated SAML key pair, but only if the provider has none yet,
    /// so two app instances generating at the same time can't each store a different one.
    ///
    /// @param secret the encrypted private key
    /// @param certificate the certificate (PEM), stored as `settings.certificate`
    /// @return `1` if stored, `0` if the provider already had a key pair
    @Transactional
    @Modifying
    @Query(nativeQuery = true, value = """
        UPDATE auth_providers
        SET secret = :secret,
            settings = jsonb_set(settings, '{certificate}', to_jsonb(CAST(:certificate AS text))),
            updated_at = now()
        WHERE id = :id AND settings ->> 'certificate' IS NULL
        """)
    int storeSamlKeysIfAbsent(Long id, String secret, String certificate);
}
