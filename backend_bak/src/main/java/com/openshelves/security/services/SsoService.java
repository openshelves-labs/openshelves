package com.openshelves.security.services;

import com.openshelves.security.entity.AuthProviderEntity;
import com.openshelves.security.entity.SsoCodeEntity;
import com.openshelves.security.entity.UserEntity;
import com.openshelves.security.entity.UserIdentityEntity;
import com.openshelves.security.enums.SsoCodePurpose;
import com.openshelves.security.exception.IdentityInUseException;
import com.openshelves.security.exception.IdentityNotLinkedException;
import com.openshelves.security.exception.LinkExpiredException;
import com.openshelves.security.model.DeviceInfo;
import com.openshelves.security.model.LoginResponse;
import com.openshelves.security.model.ProviderResponse;
import com.openshelves.security.repository.AuthProviderRepository;
import com.openshelves.security.repository.SsoCodeRepository;
import com.openshelves.security.repository.UserIdentityRepository;
import com.openshelves.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/// Service for signing in with an SSO identity provider (SAML or OIDC).
///
/// The provider round-trip itself is handled by Spring Security. This service covers what happens
/// around it: listing providers, turning a verified identity into a one-time code, exchanging that
/// code for a session, and linking a new identity to a signed-in user.
///
/// SSO never creates users. An identity signs in only once its user has connected it from settings.
@Slf4j
@Service
@RequiredArgsConstructor
public class SsoService {

    /// How long the code from the SSO callback can be exchanged for a session.
    private static final Duration LOGIN_CODE_TTL = Duration.ofSeconds(60);

    /// How long the code from `POST /auth/identities/{provider}` can start the link round-trip.
    private static final Duration LINK_CODE_TTL = Duration.ofMinutes(5);

    private final AuthService authService;
    private final TokenService tokenService;

    private final AuthProviderRepository providerRepository;
    private final UserIdentityRepository identityRepository;
    private final SsoCodeRepository codeRepository;
    private final UserRepository userRepository;


    // -----------------------------------------------------------------------
    // Providers
    // -----------------------------------------------------------------------

    /// Lists the providers for the sign-in page's dropdown: enabled ones, in display order.
    public List<ProviderResponse> listProviders() {
        return providerRepository.findAllByEnabledTrueOrderBySortOrderAscDisplayNameAsc().stream()
            .map(provider -> ProviderResponse.builder()
                .withKey(provider.getKey())
                .withType(provider.getType())
                .withDisplayName(provider.getDisplayName())
                .build())
            .toList();
    }

    /// Looks up an enabled provider by its URL key.
    ///
    /// @throws ResponseStatusException `404` if the provider is unknown or disabled
    public AuthProviderEntity getEnabledProvider(String providerKey) {
        return providerRepository.findByKeyAndEnabledTrue(providerKey)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sign-in provider not found."));
    }


    // -----------------------------------------------------------------------
    // Sign In
    // -----------------------------------------------------------------------

    /// Signs in the user behind an identity the provider has just verified, by issuing a one-time code
    /// for the frontend to exchange. Called from the SSO callback.
    ///
    /// @param providerKey the provider the user signed in with
    /// @param issuer the provider's own ID, from the verified response
    /// @param subject the user's ID at the provider, from the verified response
    /// @param persistent whether a long-lived ("remember me") session was requested at start
    /// @param codeChallenge PKCE challenge from start; the exchange must present its verifier
    /// @return the raw one-time code, to put on the frontend callback URL
    /// @throws IdentityNotLinkedException if no user has connected this identity
    /// @throws DisabledException if the user is disabled
    @Transactional
    public String completeLogin(String providerKey, String issuer, String subject, boolean persistent, String codeChallenge) {

        // Only identities a user has connected; deleted users never match
        UserIdentityEntity identity = identityRepository.findByIssuerAndSubject(issuer, subject)
            .filter(i -> i.getUserProfile().getDeletedAt() == null)
            .orElseThrow(() -> new IdentityNotLinkedException(
                "This account isn't connected to OpenShelves. Sign in another way and connect it from settings."));

        UserEntity user = identity.getUserProfile();
        if (!user.isActive()) {
            throw new DisabledException("This account is disabled.");
        }

        identity.setLastUsedAt(Instant.now());
        identityRepository.save(identity);

        return createCode(user, providerKey, SsoCodePurpose.LOGIN, persistent, codeChallenge, LOGIN_CODE_TTL);
    }

    /// Swaps the one-time code from the SSO callback for a session.
    ///
    /// The code is used up by any attempt, even a failed one, so a stolen code can't be retried.
    ///
    /// @param code the raw code from the callback URL
    /// @param codeVerifier the PKCE secret the client kept; must match the challenge sent at start
    /// @param device the device the sign-in comes from
    /// @return the token and when it expires, same as a password login
    /// @throws BadCredentialsException if the code is unknown, already used, expired, or the verifier doesn't match
    /// @throws DisabledException if the user was disabled since the code was issued
    @Transactional(noRollbackFor = AuthenticationException.class)    // Keep the code deleted when the exchange fails
    public LoginResponse exchangeCode(String code, String codeVerifier, DeviceInfo device) {

        // Lock and delete the code, so it can be used only once
        SsoCodeEntity loginCode = codeRepository.findAndLockByCodeHashAndPurpose(tokenService.hashToken(code), SsoCodePurpose.LOGIN)
            .orElseThrow(() -> new BadCredentialsException("This sign-in link is invalid or has expired. Please sign in again."));
        codeRepository.delete(loginCode);

        // Expired, or presented by someone other than the client that started the sign-in (e.g. an intercepted redirect).
        // Same message for both, so a failed attempt reveals nothing.
        boolean expired = loginCode.getExpiresAt().isBefore(Instant.now());
        boolean verified = tokenService.verifyPkce(codeVerifier, loginCode.getCodeChallenge());

        if (expired || !verified) {
            throw new BadCredentialsException("This sign-in link is invalid or has expired. Please sign in again.");
        }

        UserEntity user = loginCode.getUserProfile();
        if (!user.isActive() || user.getDeletedAt() != null) {
            throw new DisabledException("This account is disabled.");
        }

        // The session keeps the provider's display name as of now, as a snapshot
        String authProvider = providerRepository.findByKey(loginCode.getProviderKey())
            .map(AuthProviderEntity::getDisplayName)
            .orElse(loginCode.getProviderKey());

        return authService.openSession(user, loginCode.getPersistent(), device, authProvider);
    }


    // -----------------------------------------------------------------------
    // Linking
    // -----------------------------------------------------------------------

    /// Issues a one-time code that starts connecting a provider for the given user.
    ///
    /// @param user the signed-in user
    /// @param providerKey the provider being connected
    /// @return the raw link code, to put on the SSO start URL
    @Transactional
    public String createLinkCode(UserEntity user, String providerKey) {
        return createCode(user, providerKey, SsoCodePurpose.LINK, null, null, LINK_CODE_TTL);
    }

    /// Uses up a link code at the start of the round-trip, returning the user it was issued for.
    ///
    /// @param providerKey the provider being started; must match the code's
    /// @param code the raw link code from the start URL
    /// @return the ID of the user connecting the provider
    /// @throws LinkExpiredException if the code is unknown, used, expired or for another provider
    @Transactional(noRollbackFor = AuthenticationException.class)    // Keep the code deleted when it's refused
    public Long consumeLinkCode(String providerKey, String code) {
        Optional<SsoCodeEntity> linkCode = codeRepository.findAndLockByCodeHashAndPurpose(tokenService.hashToken(code), SsoCodePurpose.LINK);
        linkCode.ifPresent(codeRepository::delete);

        boolean usable = linkCode.isPresent()
            && linkCode.get().getProviderKey().equals(providerKey)
            && linkCode.get().getExpiresAt().isAfter(Instant.now());

        if (!usable) {
            throw new LinkExpiredException("This link has expired. Please try connecting again.");
        }

        return linkCode.get().getUserProfile().getId();
    }

    /// Links an identity the provider has just verified to the user connecting it. Called from the SSO callback.
    /// Linking an identity the user already has does nothing.
    ///
    /// @param userId the user connecting the provider, from the link code
    /// @param issuer the provider's own ID, from the verified response
    /// @param subject the user's ID at the provider, from the verified response
    /// @param metadata display hints from the provider, e.g. `email` or `name`; may be `null`
    /// @throws IdentityInUseException if the identity is linked to a different user
    @Transactional
    public void completeLink(Long userId, String issuer, String subject, Map<String, Object> metadata) {
        Optional<UserIdentityEntity> existing = identityRepository.findByIssuerAndSubject(issuer, subject);

        if (existing.isPresent()) {
            if (!existing.get().getUserProfile().getId().equals(userId)) {
                throw new IdentityInUseException("This account is already connected to another OpenShelves user.");
            }

            // Already linked to this user
            return;
        }

        UserIdentityEntity identity = new UserIdentityEntity();
        identity.setUserProfile(userRepository.getReferenceById(userId));
        identity.setIssuer(issuer);
        identity.setSubject(subject);
        identity.setMetadata(metadata);
        identityRepository.save(identity);

        log.debug("Identity linked [userId={}, identityId={}]", userId, identity.getId());
    }


    // -----------------------------------------------------------------------
    // Helper Methods
    // -----------------------------------------------------------------------

    /// Creates a one-time code and stores its hash.
    ///
    /// @param user the user the code is for
    /// @param providerKey the provider the code is for
    /// @param purpose what the code is for
    /// @param persistent whether the session will be long-lived; `LOGIN` only, `null` otherwise
    /// @param codeChallenge PKCE challenge the exchange must answer; `LOGIN` only, `null` otherwise
    /// @param ttl how long the code stays usable
    /// @return the raw code; only its hash is stored
    private String createCode(UserEntity user, String providerKey, SsoCodePurpose purpose, Boolean persistent, String codeChallenge, Duration ttl) {
        String code = tokenService.generateToken();

        SsoCodeEntity entity = new SsoCodeEntity();
        entity.setUserProfile(user);
        entity.setProviderKey(providerKey);
        entity.setPurpose(purpose);
        entity.setCodeHash(tokenService.hashToken(code));
        entity.setPersistent(persistent);
        entity.setCodeChallenge(codeChallenge);
        entity.setExpiresAt(Instant.now().plus(ttl));
        codeRepository.save(entity);

        return code;
    }
}
