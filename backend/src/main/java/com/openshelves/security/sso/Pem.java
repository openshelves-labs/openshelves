package com.openshelves.security.sso;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/// Parses PEM-encoded keys and certificates, as stored for SAML providers.
final class Pem {

    private Pem() {
    }

    /// Parses an X.509 certificate (`-----BEGIN CERTIFICATE-----`).
    static X509Certificate certificate(String pem) {
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(pem.getBytes(StandardCharsets.US_ASCII)));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Invalid PEM certificate", e);
        }
    }

    /// Parses an unencrypted PKCS#8 RSA private key (`-----BEGIN PRIVATE KEY-----`).
    static PrivateKey rsaPrivateKey(String pem) {
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
