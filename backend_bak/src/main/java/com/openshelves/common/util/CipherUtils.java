package com.openshelves.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/// Symmetric encryption for data the application must be able to read back,
/// e.g. secrets kept in the database or values handed to the client.
///
/// Uses AES-256-GCM: each call uses a fresh random IV, and anything tampered with fails to decrypt.
/// The key is the `openshelves.security.secret-key` property (32 bytes as Base64). It's required:
/// the application doesn't start without a valid one.
@Component
public class CipherUtils {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    /// AES-256 needs a 32-byte key.
    private static final int KEY_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    private final SecretKeySpec secretKey;

    public CipherUtils(@Value("${openshelves.security.secret-key:}") String base64Key) {
        this.secretKey = new SecretKeySpec(decodeKey(base64Key), "AES");
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
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_BITS, iv));
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
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_BITS, in, 0, IV_BYTES));
            return cipher.doFinal(in, IV_BYTES, in.length - IV_BYTES);
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Decryption failed", e);
        }
    }


    // -----------------------------------------------------------------------
    // Text
    // -----------------------------------------------------------------------

    /// Encrypts text, as UTF-8.
    ///
    /// @param plain the text to encrypt
    /// @return the encrypted text, as from [#encrypt]
    public String encryptString(String plain) {
        return encrypt(plain.getBytes(StandardCharsets.UTF_8));
    }

    /// Decrypts text produced by [#encryptString].
    ///
    /// @param encrypted the encrypted text
    /// @return the original text
    /// @throws IllegalArgumentException if the data is malformed, tampered with, or from another key
    public String decryptString(String encrypted) {
        return new String(decrypt(encrypted), StandardCharsets.UTF_8);
    }


    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /// Decodes the configured key, failing startup if it's missing or not a 32-byte Base64 value.
    private static byte[] decodeKey(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException("openshelves.security.secret-key is required (32 bytes as Base64; generate with: openssl rand -base64 32)");
        }

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64Key.strip());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("openshelves.security.secret-key is not valid Base64", e);
        }

        if (bytes.length != KEY_BYTES) {
            throw new IllegalStateException("openshelves.security.secret-key must be " + KEY_BYTES + " bytes, but is " + bytes.length);
        }
        return bytes;
    }
}
