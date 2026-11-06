package com.openshelves.security.entity;

import com.openshelves.common.entity.BaseEntity;
import com.openshelves.security.enums.SsoCodePurpose;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;

import java.time.Instant;

/// Entity representing a short-lived, single-use code for an SSO round-trip.
///
/// Only the code's hash is stored. The row is deleted when the code is used.
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "sso_codes")
public class SsoCodeEntity extends BaseEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;


    // -------------------------------------------------------------------------
    // Relationships
    // -------------------------------------------------------------------------

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private UserEntity userProfile;

    /// Key of the provider the code is for.
    @Column(name = "provider_key", nullable = false, updatable = false)
    private String providerKey;


    // -------------------------------------------------------------------------
    // Code
    // -------------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, updatable = false)
    private SsoCodePurpose purpose;

    /// SHA-256 hash of the code.
    @Column(name = "code_hash", nullable = false, updatable = false)
    private String codeHash;

    /// Whether the session opened at exchange is long-lived; `LOGIN` only.
    @Column(name = "is_persistent", updatable = false)
    private Boolean persistent;

    /// PKCE challenge (`BASE64URL(SHA-256(verifier))`) the exchange must answer; `LOGIN` only.
    @Column(name = "code_challenge", updatable = false)
    private String codeChallenge;


    // -------------------------------------------------------------------------
    // Timeouts & audit
    // -------------------------------------------------------------------------

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
