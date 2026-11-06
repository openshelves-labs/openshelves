package com.openshelves.security.sso;

import com.openshelves.security.entity.AuthProviderEntity;
import com.openshelves.security.enums.AuthProviderType;
import com.openshelves.security.repository.AuthProviderRepository;
import com.openshelves.security.services.SecretCipher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/// Builds Spring Security's SAML provider registrations from the `auth_providers` table,
/// so providers are managed at runtime and never in `application.yml`.
///
/// Each provider reads the IdP's metadata from `settings.metadataUrl`, so built registrations are
/// cached briefly; changes to a provider apply within [#CACHE_TTL].
///
/// Each provider has its own SP key pair: the private key in `secret` (encrypted), the certificate in
/// `settings.certificate`. It signs our requests and decrypts the IdP's assertions.
@Slf4j
@Component
@RequiredArgsConstructor
public class DbSamlRegistrationRepository implements RelyingPartyRegistrationRepository {

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final AuthProviderRepository providerRepository;
    private final SecretCipher cipher;

    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    private record Cached(RelyingPartyRegistration registration, Instant loadedAt) {
    }


    /// Looks up an enabled SAML provider by its key.
    ///
    /// @param registrationId the provider key
    /// @return the registration, or `null` if the provider is unknown, disabled, not SAML, or can't be loaded
    @Override
    public RelyingPartyRegistration findByRegistrationId(String registrationId) {
        Cached cached = cache.get(registrationId);
        if (cached != null && cached.loadedAt().plus(CACHE_TTL).isAfter(Instant.now())) {
            return cached.registration();
        }

        Optional<AuthProviderEntity> provider = providerRepository.findByKeyAndEnabledTrue(registrationId)
            .filter(p -> p.getType() == AuthProviderType.SAML);

        if (provider.isEmpty()) {
            cache.remove(registrationId);
            return null;
        }

        try {
            RelyingPartyRegistration registration = build(provider.get());
            cache.put(registrationId, new Cached(registration, Instant.now()));
            return registration;
        } catch (RuntimeException e) {
            log.error("Could not load SAML provider [key={}, issuer={}]", registrationId, provider.get().getIssuer(), e);
            return null;
        }
    }

    /// Builds the registration from the provider's row and the IdP's metadata.
    private RelyingPartyRegistration build(AuthProviderEntity provider) {
        String metadataUrl = provider.setting("metadataUrl")
            .orElseThrow(() -> new IllegalStateException("settings.metadataUrl is missing"));

        RelyingPartyRegistration.Builder builder = RelyingPartyRegistrations.fromMetadataLocation(metadataUrl)
            .registrationId(provider.getKey())
            .entityId("{baseUrl}/api/v1/auth/sso/{registrationId}/metadata")
            .assertionConsumerServiceLocation("{baseUrl}/api/v1/auth/sso/{registrationId}/saml-callback");

        // Our key pair for this provider, if it has one yet
        Optional<String> certificatePem = provider.setting("certificate");
        if (provider.getSecret() != null && certificatePem.isPresent()) {
            PrivateKey privateKey = Pem.rsaPrivateKey(new String(cipher.decrypt(provider.getSecret()), StandardCharsets.US_ASCII));
            X509Certificate certificate = Pem.certificate(certificatePem.get());

            builder
                .signingX509Credentials(credentials -> credentials.add(Saml2X509Credential.signing(privateKey, certificate)))
                .decryptionX509Credentials(credentials -> credentials.add(Saml2X509Credential.decryption(privateKey, certificate)));
        }

        return builder.build();
    }
}
