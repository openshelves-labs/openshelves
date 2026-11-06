package com.openshelves.security.entity;

import com.openshelves.common.entity.BaseEntity;
import com.openshelves.security.enums.AuthProviderType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;
import java.util.Map;

/// Entity representing an SSO identity provider (SAML or OIDC), managed by library admins.
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "auth_providers")
public class AuthProviderEntity extends BaseEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;


    // -------------------------------------------------------------------------
    // Core metadata
    // -------------------------------------------------------------------------

    /// Label used in URLs, e.g. `/sso/{key}/start`.
    @Column(name = "key", nullable = false)
    private String key;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private AuthProviderType type;

    @Column(name = "display_name", nullable = false)
    private String displayName;


    // -------------------------------------------------------------------------
    // Sign-in page
    // -------------------------------------------------------------------------

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;


    // -------------------------------------------------------------------------
    // Provider identity & configuration
    // -------------------------------------------------------------------------

    /// The provider's own ID: OIDC `iss` or SAML `entityID`.
    @Column(name = "issuer", nullable = false)
    private String issuer;

    /// Client ID, metadata URL, attribute mapping, etc.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", nullable = false)
    private Map<String, Object> settings;

    /// OIDC client secret, encrypted.
    @Column(name = "client_secret")
    private String clientSecret;


    // -------------------------------------------------------------------------
    // Audit
    // -------------------------------------------------------------------------

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
