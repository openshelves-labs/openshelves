package com.openshelves.security.services;

import com.openshelves.common.exception.ApiException;
import com.openshelves.common.model.SessionPrincipal;
import com.openshelves.security.entity.AuthProviderEntity;
import com.openshelves.security.entity.UserIdentityEntity;
import com.openshelves.security.model.IdentityResponse;
import com.openshelves.security.model.LinkResponse;
import com.openshelves.security.repository.AuthProviderRepository;
import com.openshelves.security.repository.UserIdentityRepository;
import com.openshelves.security.repository.UserPasswordRepository;
import com.openshelves.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/// Service for a user's sign-in methods: the identity providers they've connected from settings.
@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityService {

    private final SsoService ssoService;

    private final AuthProviderRepository providerRepository;
    private final UserIdentityRepository identityRepository;
    private final UserPasswordRepository passwordRepository;
    private final UserRepository userRepository;


    // -----------------------------------------------------------------------
    // Listing
    // -----------------------------------------------------------------------

    /// Lists the identities the caller has connected, oldest first.
    ///
    /// @param principal the caller
    /// @return the identities, each with its provider's current key and name if it still exists
    public List<IdentityResponse> listIdentities(SessionPrincipal principal) {
        List<UserIdentityEntity> identities = identityRepository.findAllByUserProfileIdOrderByCreatedAtAsc(principal.getUserId());

        // Identities point to their provider by issuer, not by foreign key
        List<String> issuers = identities.stream().map(UserIdentityEntity::getIssuer).toList();
        Map<String, AuthProviderEntity> providersByIssuer = providerRepository.findAllByIssuerIn(issuers).stream()
            .collect(Collectors.toMap(AuthProviderEntity::getIssuer, Function.identity()));

        return identities.stream()
            .map(identity -> toIdentityResponse(identity, providersByIssuer.get(identity.getIssuer())))
            .toList();
    }


    // -----------------------------------------------------------------------
    // Connecting & Disconnecting
    // -----------------------------------------------------------------------

    /// Starts connecting a provider for the caller.
    ///
    /// A browser redirect can't carry the bearer token, so this issues a short-lived link code
    /// and returns the SSO start URL carrying it; the frontend then sends the browser there.
    ///
    /// @param principal the caller
    /// @param providerKey the provider to connect
    /// @return where to send the browser
    /// @throws ApiException `404` if the provider is unknown or disabled
    @Transactional
    public LinkResponse startLink(SessionPrincipal principal, String providerKey) {
        AuthProviderEntity provider = ssoService.getEnabledProvider(providerKey);

        String code = ssoService.createLinkCode(userRepository.getReferenceById(principal.getUserId()), provider.getKey());

        String redirectUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/api/v1/auth/sso/{provider}/start")
            .queryParam("link", code)
            .buildAndExpand(provider.getKey())
            .toUriString();

        return LinkResponse.builder()
            .withRedirectUrl(redirectUrl)
            .build();
    }

    /// Disconnects one of the caller's identities. Sessions opened with it stay signed in.
    ///
    /// @param principal the caller
    /// @param identityId the identity to disconnect
    /// @throws ApiException `404` if the identity isn't the caller's
    /// @throws ApiException `409 LAST_SIGN_IN_METHOD` if it's the caller's only way to sign in
    @Transactional
    public void unlink(SessionPrincipal principal, Long identityId) {
        Long userId = principal.getUserId();

        UserIdentityEntity identity = identityRepository.findByIdAndUserProfileId(identityId, userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, null, "Sign-in method not found."));

        // Keep at least one way to sign in: a password, or another identity
        boolean hasOtherMethod = passwordRepository.existsByUserProfileId(userId)
            || identityRepository.countByUserProfileId(userId) > 1;

        if (!hasOtherMethod) {
            throw new ApiException(HttpStatus.CONFLICT, "LAST_SIGN_IN_METHOD",
                "This is your only way to sign in, so it can't be disconnected.");
        }

        identityRepository.delete(identity);

        log.debug("Identity unlinked [userId={}, identityId={}]", userId, identityId);
    }


    // -----------------------------------------------------------------------
    // Helper Methods
    // -----------------------------------------------------------------------

    /// Maps an identity to its sign-in methods entry.
    ///
    /// @param identity the identity
    /// @param provider the provider with the same issuer, or `null` if it no longer exists
    /// @return the entry
    private IdentityResponse toIdentityResponse(UserIdentityEntity identity, AuthProviderEntity provider) {
        Map<String, Object> metadata = identity.getMetadata() != null ? identity.getMetadata() : Map.of();
        Object linkedAs = metadata.getOrDefault("email", metadata.get("name"));

        return IdentityResponse.builder()
            .withId(identity.getId())
            .withProviderKey(provider != null ? provider.getKey() : null)
            .withProviderName(provider != null ? provider.getDisplayName() : null)
            .withLinkedAs(linkedAs != null ? linkedAs.toString() : null)
            .withCreatedAt(identity.getCreatedAt())
            .withLastUsedAt(identity.getLastUsedAt())
            .build();
    }
}
