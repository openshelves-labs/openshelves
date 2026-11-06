package com.openshelves.security.sso;

import com.openshelves.security.entity.AuthProviderEntity;
import com.openshelves.security.enums.AuthProviderType;
import com.openshelves.security.repository.AuthProviderRepository;
import com.openshelves.security.services.SecretCipher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/// Builds Spring Security's OIDC provider registrations from the `auth_providers` table,
/// so providers are managed at runtime and never in `application.yml`.
///
/// Each registration reads the provider's discovery document (`{issuer}/.well-known/openid-configuration`),
/// so built registrations are cached briefly; changes to a provider apply within [#CACHE_TTL].
@Slf4j
@Component
@RequiredArgsConstructor
public class DbOidcRegistrationRepository implements ClientRegistrationRepository {

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private static final List<String> DEFAULT_SCOPES = List.of("openid", "email", "profile");

    private final AuthProviderRepository providerRepository;
    private final SecretCipher cipher;

    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    private record Cached(ClientRegistration registration, Instant loadedAt) {
    }


    /// Looks up an enabled OIDC provider by its key.
    ///
    /// @param registrationId the provider key
    /// @return the registration, or `null` if the provider is unknown, disabled, not OIDC, or can't be loaded
    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        Cached cached = cache.get(registrationId);
        if (cached != null && cached.loadedAt().plus(CACHE_TTL).isAfter(Instant.now())) {
            return cached.registration();
        }

        Optional<AuthProviderEntity> provider = providerRepository.findByKeyAndEnabledTrue(registrationId)
            .filter(p -> p.getType() == AuthProviderType.OIDC);

        if (provider.isEmpty()) {
            cache.remove(registrationId);
            return null;
        }

        try {
            ClientRegistration registration = build(provider.get());
            cache.put(registrationId, new Cached(registration, Instant.now()));
            return registration;
        } catch (RuntimeException e) {
            log.error("Could not load OIDC provider [key={}, issuer={}]", registrationId, provider.get().getIssuer(), e);
            return null;
        }
    }

    /// Builds the registration from the provider's row and its discovery document.
    private ClientRegistration build(AuthProviderEntity provider) {
        String clientSecret = provider.getSecret() != null
            ? new String(cipher.decrypt(provider.getSecret()), StandardCharsets.UTF_8)
            : null;

        return ClientRegistrations.fromIssuerLocation(provider.getIssuer())
            .registrationId(provider.getKey())
            .clientName(provider.getDisplayName())
            .clientId(provider.setting("clientId").orElseThrow(() -> new IllegalStateException("settings.clientId is missing")))
            .clientSecret(clientSecret)
            .scope(scopes(provider))
            .redirectUri("{baseUrl}/api/v1/auth/sso/{registrationId}/oidc-callback")
            .build();
    }

    /// Reads `settings.scopes` (a list), or the default `openid email profile`.
    private List<String> scopes(AuthProviderEntity provider) {
        Object scopes = provider.getSettings() != null ? provider.getSettings().get("scopes") : null;
        if (scopes instanceof List<?> list && !list.isEmpty()) {
            return list.stream().map(Object::toString).toList();
        }
        return DEFAULT_SCOPES;
    }
}
