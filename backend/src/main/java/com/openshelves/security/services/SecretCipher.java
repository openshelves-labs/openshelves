package com.openshelves.security.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/// Service for encrypting secrets the application stores or hands to the browser:
/// provider secrets in `auth_providers`, and the short-lived SSO cookies.
///
/// Uses AES-256-GCM, so anything tampered with fails to decrypt. The key comes from the
/// deployment environment (`OPENSHELVES_SECRET_KEY`, 32 bytes as Base64), never the database.
/// It's only needed for SSO; without it, everything else still works.
@Service
public class SecretCipher {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecureRandom secureRandom = new SecureRandom();

    /// The AES key, or `null` when none is configured.
    private final SecretKeySpec key;

    public SecretCipher(@Value("${openshelves.security.secret-key:}") String base64Key) {
        this.key = base64Key.isBlank() ? null : new SecretKeySpec(Base64.getDecoder().decode(base64Key), "AES");
    }


    // -----------------------------------------------------------------------
    // Encryption
    // -----------------------------------------------------------------------

    /// Encrypts data.
    ///
    /// @param plain the data to encrypt
    /// @return the random IV followed by the ciphertext, as URL-safe Base64 without padding
    public String encrypt(byte[] plain) {
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, requireKey(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain);

            byte[] out = ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    /// Decrypts data produced by [#encrypt].
    ///
    /// @param encrypted the encrypted data
    /// @return the original data
    /// @throws IllegalArgumentException if the data is malformed, tampered with, or from another key
    public byte[] decrypt(String encrypted) {
        try {
            byte[] in = Base64.getUrlDecoder().decode(encrypted);
            if (in.length <= IV_BYTES) {
                throw new IllegalArgumentException("Encrypted data is too short");
            }

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, requireKey(), new GCMParameterSpec(TAG_BITS, in, 0, IV_BYTES));
            return cipher.doFinal(in, IV_BYTES, in.length - IV_BYTES);
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Decryption failed", e);
        }
    }


    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private SecretKeySpec requireKey() {
        if (key == null) {
            throw new IllegalStateException("No secret key configured; set OPENSHELVES_SECRET_KEY to use SSO");
        }
        return key;
    }
}
