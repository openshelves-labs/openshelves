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
import java.util.Optional;

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

    /// Client ID, IdP metadata, attribute mapping, and for SAML our SP certificate (public).
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", nullable = false)
    private Map<String, Object> settings;

    /// Encrypted secret, by type: the OIDC client secret, or our SAML SP private key.
    @Column(name = "secret")
    private String secret;


    // -------------------------------------------------------------------------
    // Audit
    // -------------------------------------------------------------------------

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;


    // -------------------------------------------------------------------------
    // Settings Access
    // -------------------------------------------------------------------------

    /// Reads a top-level text setting, e.g. `clientId` or `metadataUrl`.
    ///
    /// @return the value, or empty if it's missing or blank
    public Optional<String> setting(String name) {
        return Optional.ofNullable(settings)
            .map(s -> s.get(name))
            .map(Object::toString)
            .filter(value -> !value.isBlank());
    }

    /// Reads which provider attribute holds a field, from `settings.attributeMapping`,
    /// e.g. `attributeMapping.subject = "urn:oid:1.3.6.1.4.1.5923.1.1.1.6"`.
    ///
    /// @param field `subject`, `email` or `name`
    /// @return the attribute name, or empty to use the protocol's default
    public Optional<String> mappedAttribute(String field) {
        return Optional.ofNullable(settings)
            .map(s -> s.get("attributeMapping"))
            .filter(Map.class::isInstance)
            .map(mapping -> ((Map<?, ?>) mapping).get(field))
            .map(Object::toString)
            .filter(value -> !value.isBlank());
    }
}
