package com.openshelves.security.services;

import com.openshelves.security.entity.AuthProviderEntity;
import com.openshelves.security.enums.AuthProviderType;
import com.openshelves.security.repository.AuthProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/// Turns rows of `auth_providers` into the provider configurations Spring Security needs,
/// so providers are managed at runtime and never in `application.yml`.
///
/// - [#findOidc]: an OIDC client registration, built from the issuer's discovery document.
/// - [#findSaml]: a SAML relying-party registration, built from the IdP's metadata URL,
///   with this provider's own key pair for signing requests and decrypting assertions.
///
/// Both read a remote document, so results are cached briefly; provider changes apply within [#CACHE_TTL].
/// `SecurityConfig` hands these methods to Spring as its two registration lookups.
@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityProviderService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private static final List<String> DEFAULT_OIDC_SCOPES = List.of("openid", "email", "profile");

    private final AuthProviderRepository providerRepository;
    private final SecretCipher cipher;

    private final Map<String, Cached<ClientRegistration>> oidcCache = new ConcurrentHashMap<>();
    private final Map<String, Cached<RelyingPartyRegistration>> samlCache = new ConcurrentHashMap<>();

    /// How long a generated SAML certificate is valid.
    private static final Duration SAML_CERTIFICATE_VALIDITY = Duration.ofDays(3650);

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private record Cached<T>(T registration, Instant loadedAt) {
    }

    /// One SAML provider's key pair.
    private record SamlKeys(PrivateKey privateKey, X509Certificate certificate) {
    }


    // -----------------------------------------------------------------------
    // Lookups
    // -----------------------------------------------------------------------

    /// Looks up an enabled OIDC provider by its key.
    ///
    /// @return the registration, or `null` if the provider is unknown, disabled, not OIDC, or can't be loaded
    public ClientRegistration findOidc(String providerKey) {
        return find(providerKey, AuthProviderType.OIDC, oidcCache, this::buildOidc);
    }

    /// Looks up an enabled SAML provider by its key.
    ///
    /// @return the registration, or `null` if the provider is unknown, disabled, not SAML, or can't be loaded
    public RelyingPartyRegistration findSaml(String providerKey) {
        return find(providerKey, AuthProviderType.SAML, samlCache, this::buildSaml);
    }

    /// Returns a cached registration, or loads the provider and builds a fresh one.
    private <T> T find(String providerKey, AuthProviderType type, Map<String, Cached<T>> cache, Function<AuthProviderEntity, T> builder) {
        Cached<T> cached = cache.get(providerKey);
        if (cached != null && cached.loadedAt().plus(CACHE_TTL).isAfter(Instant.now())) {
            return cached.registration();
        }

        Optional<AuthProviderEntity> provider = providerRepository.findByKeyAndEnabledTrue(providerKey)
            .filter(p -> p.getType() == type);

        if (provider.isEmpty()) {
            cache.remove(providerKey);
            return null;
        }

        try {
            T registration = builder.apply(provider.get());
            cache.put(providerKey, new Cached<>(registration, Instant.now()));
            return registration;
        } catch (RuntimeException e) {
            log.error("Could not load {} provider [key={}, issuer={}]", type, providerKey, provider.get().getIssuer(), e);
            return null;
        }
    }


    // -----------------------------------------------------------------------
    // OIDC
    // -----------------------------------------------------------------------

    /// Settings used: `clientId`, optional `scopes` (list); `secret` holds the client secret.
    private ClientRegistration buildOidc(AuthProviderEntity provider) {
        String clientSecret = provider.getSecret() != null
            ? new String(cipher.decrypt(provider.getSecret()), StandardCharsets.UTF_8)
            : null;

        return ClientRegistrations.fromIssuerLocation(provider.getIssuer())
            .registrationId(provider.getKey())
            .clientName(provider.getDisplayName())
            .clientId(provider.setting("clientId").orElseThrow(() -> new IllegalStateException("settings.clientId is missing")))
            .clientSecret(clientSecret)
            .scope(oidcScopes(provider))
            .redirectUri("{baseUrl}/api/v1/auth/sso/{registrationId}/oidc-callback")
            .build();
    }

    private List<String> oidcScopes(AuthProviderEntity provider) {
        Object scopes = provider.getSettings() != null ? provider.getSettings().get("scopes") : null;
        if (scopes instanceof List<?> list && !list.isEmpty()) {
            return list.stream().map(Object::toString).toList();
        }
        return DEFAULT_OIDC_SCOPES;
    }


    // -----------------------------------------------------------------------
    // SAML
    // -----------------------------------------------------------------------

    /// Settings used: `metadataUrl`, and our SP `certificate` (PEM); `secret` holds our SP private key (PEM, PKCS#8).
    /// The key pair is generated the first time the provider is used.
    private RelyingPartyRegistration buildSaml(AuthProviderEntity provider) {
        String metadataUrl = provider.setting("metadataUrl")
            .orElseThrow(() -> new IllegalStateException("settings.metadataUrl is missing"));

        RelyingPartyRegistration.Builder builder = RelyingPartyRegistrations.fromMetadataLocation(metadataUrl)
            .registrationId(provider.getKey())
            .entityId("{baseUrl}/api/v1/auth/sso/{registrationId}/metadata")
            .assertionConsumerServiceLocation("{baseUrl}/api/v1/auth/sso/{registrationId}/saml-callback");

        // Our key pair for this provider: signs our requests, decrypts the IdP's assertions
        SamlKeys keys = loadOrCreateSamlKeys(provider);

        return builder
            .signingX509Credentials(credentials -> credentials.add(Saml2X509Credential.signing(keys.privateKey(), keys.certificate())))
            .decryptionX509Credentials(credentials -> credentials.add(Saml2X509Credential.decryption(keys.privateKey(), keys.certificate())))
            .build();
    }


    // -----------------------------------------------------------------------
    // SAML Key Pair
    // -----------------------------------------------------------------------

    /// Returns the provider's key pair, generating and storing one the first time the provider is used.
    ///
    /// If another app instance stores its own first, that one is used instead,
    /// so every instance ends up with the same key pair.
    private SamlKeys loadOrCreateSamlKeys(AuthProviderEntity provider) {
        Optional<SamlKeys> stored = storedSamlKeys(provider);
        if (stored.isPresent()) {
            return stored.get();
        }

        SamlKeys generated = generateSamlKeys(provider.getKey());
        String encryptedKey = cipher.encrypt(toPem("PRIVATE KEY", generated.privateKey().getEncoded()).getBytes(StandardCharsets.US_ASCII));
        String certificatePem = toPem("CERTIFICATE", encoded(generated.certificate()));

        if (providerRepository.storeSamlKeysIfAbsent(provider.getId(), encryptedKey, certificatePem) == 1) {
            log.info("Generated SAML key pair [key={}]", provider.getKey());
            return generated;
        }

        // Another instance stored its key pair first; use that one
        AuthProviderEntity reloaded = providerRepository.findById(provider.getId()).orElseThrow();
        return storedSamlKeys(reloaded).orElseThrow(() -> new IllegalStateException("SAML key pair missing after concurrent generation"));
    }

    /// Reads the stored key pair: private key from `secret` (encrypted PEM), certificate from `settings.certificate`.
    private Optional<SamlKeys> storedSamlKeys(AuthProviderEntity provider) {
        Optional<String> certificatePem = provider.setting("certificate");
        if (provider.getSecret() == null || certificatePem.isEmpty()) {
            return Optional.empty();
        }

        PrivateKey privateKey = parsePrivateKey(new String(cipher.decrypt(provider.getSecret()), StandardCharsets.US_ASCII));
        return Optional.of(new SamlKeys(privateKey, parseCertificate(certificatePem.get())));
    }

    /// Generates an RSA 2048 key pair and a self-signed certificate for it, valid for [#SAML_CERTIFICATE_VALIDITY].
    ///
    /// Self-signed and long-lived is normal for a SAML service provider: the IdP trusts the exact
    /// certificate it imported from our metadata, not a chain or an expiry date.
    ///
    /// @param providerKey used as the certificate's common name
    private static SamlKeys generateSamlKeys(String providerKey) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();

            X500Name subject = new X500NameBuilder(BCStyle.INSTANCE)
                .addRDN(BCStyle.CN, "OpenShelves SAML " + providerKey)
                .build();
            Instant now = Instant.now();

            X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                subject,
                new BigInteger(160, SECURE_RANDOM),                     // Random positive serial number
                Date.from(now.minus(Duration.ofDays(1))),               // Tolerates small clock differences
                Date.from(now.plus(SAML_CERTIFICATE_VALIDITY)),
                subject,                                                // Self-signed: issuer is the subject
                keyPair.getPublic());

            ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
            X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(builder.build(signer));

            return new SamlKeys(keyPair.getPrivate(), certificate);
        } catch (GeneralSecurityException | OperatorCreationException e) {
            throw new IllegalStateException("Could not generate SAML key pair", e);
        }
    }

    private static byte[] encoded(X509Certificate certificate) {
        try {
            return certificate.getEncoded();
        } catch (CertificateEncodingException e) {
            throw new IllegalStateException("Could not encode certificate", e);
        }
    }

    /// Encodes DER bytes as PEM, e.g. `-----BEGIN CERTIFICATE-----`, with 64-character lines.
    private static String toPem(String type, byte[] der) {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(der);
        return "-----BEGIN " + type + "-----\n" + base64 + "\n-----END " + type + "-----\n";
    }

    /// Parses an X.509 certificate (`-----BEGIN CERTIFICATE-----`).
    private static X509Certificate parseCertificate(String pem) {
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(pem.getBytes(StandardCharsets.US_ASCII)));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Invalid PEM certificate", e);
        }
    }

    /// Parses an unencrypted PKCS#8 RSA private key (`-----BEGIN PRIVATE KEY-----`).
    private static PrivateKey parsePrivateKey(String pem) {
        String base64 = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", "");

        try {
            byte[] der = Base64.getDecoder().decode(base64);
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid PEM private key; expected unencrypted PKCS#8 RSA", e);
        }
    }
}
