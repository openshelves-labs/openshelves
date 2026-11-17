package com.openshelves.security.services;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/// Service for generating opaque tokens and the hashes stored in their place.
///
/// Tokens are random and carry no data. Only their SHA-256 hash is ever stored,
/// so a leaked database can't be used to replay them.
@Service
public class TokenService {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();


    // -----------------------------------------------------------------------
    // Token Generation & Hashing
    // -----------------------------------------------------------------------

    /// Generates a new random token.
    ///
    /// @return 32 random bytes, encoded as URL-safe Base64 without padding
    public String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /// Hashes a token for storage and lookup.
    ///
    /// A fast hash is enough here, since the token itself is long and random.
    ///
    /// @param token the raw token
    /// @return the SHA-256 hash of the token, as hex
    public String hashToken(String token) {
        return DigestUtils.sha256Hex(token);
    }


    // -----------------------------------------------------------------------
    // PKCE
    // -----------------------------------------------------------------------

    /// Checks a PKCE verifier against the challenge sent earlier (S256, RFC 7636).
    ///
    /// @param verifier the secret the client kept, sent at exchange
    /// @param challenge the challenge the client sent at start: `BASE64URL(SHA-256(verifier))`
    /// @return `true` if they match; compared in constant time
    public boolean verifyPkce(String verifier, String challenge) {
        byte[] hash = DigestUtils.sha256(verifier.getBytes(StandardCharsets.US_ASCII));
        String expected = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);

        return MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.US_ASCII),
            challenge.getBytes(StandardCharsets.US_ASCII));
    }
}
