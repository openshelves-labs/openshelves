package com.openshelves.security.entity;

import com.openshelves.common.entity.BaseEntity;
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

/// Entity representing an SSO account a user has connected.
///
/// Identified by `(issuer, subject)`, as OIDC and SAML define it. Matched to its provider
/// by issuer rather than a foreign key, so re-adding a provider keeps its users linked.
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "user_identities")
public class UserIdentityEntity extends BaseEntity<Long> {

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


    // -------------------------------------------------------------------------
    // Identity at the provider
    // -------------------------------------------------------------------------

    /// The provider's own ID: OIDC `iss` or SAML `entityID`.
    @Column(name = "issuer", nullable = false, updatable = false)
    private String issuer;

    /// The user's ID at the provider: OIDC `sub` or SAML NameID.
    @Column(name = "subject", nullable = false, updatable = false)
    private String subject;

    /// Display hints from the provider, e.g. `email` or `name`.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private Map<String, Object> metadata;


    // -------------------------------------------------------------------------
    // Usage & audit
    // -------------------------------------------------------------------------

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
