package com.openshelves.auth.entity;

import com.openshelves.auth.enums.CredentialType;
import com.openshelves.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;
import java.util.Map;

/// Entity representing a way a user can prove their identity in the OpenShelves system,
/// including credentials and authentication methods.
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "user_credentials")
public class UserCredentialEntity extends BaseEntity<Long> {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private CredentialType credentialType;


    // -------------------------------------------------------------------------
    // Metadata
    // -------------------------------------------------------------------------

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private Map<String, Object> metadata;


    // -------------------------------------------------------------------------
    // Secrets
    // -------------------------------------------------------------------------

    @Column(name = "password_hash")
    private String passwordHash;

    /// Identifier at the external provider (SAML NameID, OIDC `sub`, etc.).
    @Column(name = "provider_key")
    private String providerKey;


    // -------------------------------------------------------------------------
    // Usage & lockout
    // -------------------------------------------------------------------------

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;


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
    // Data Pre-Processing
    // -------------------------------------------------------------------------

    @PrePersist
    @PreUpdate
    public void prePersistUpdate() {
        trimStringFields();
    }

    private void trimStringFields() {
        this.providerKey = StringUtils.trimToNull(this.providerKey);
    }
}
