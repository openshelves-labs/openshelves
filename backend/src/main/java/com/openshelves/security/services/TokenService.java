package com.openshelves.security.services;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

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
}
