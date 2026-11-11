package com.openshelves.security.entity;

import com.openshelves.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;

/// Entity representing the password a user signs in with.
///
/// A user has at most one. It also carries the lockout state, since lockout
/// only applies to passwords; for SSO, the identity provider handles it.
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "user_passwords")
public class UserPasswordEntity extends BaseEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;


    // -------------------------------------------------------------------------
    // Relationships
    // -------------------------------------------------------------------------

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private UserEntity userProfile;


    // -------------------------------------------------------------------------
    // Secret
    // -------------------------------------------------------------------------

    /// Encoded password, with its encoder prefix (e.g. `{bcrypt}`).
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;


    // -------------------------------------------------------------------------
    // Lockout
    // -------------------------------------------------------------------------

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;


    // -------------------------------------------------------------------------
    // Usage
    // -------------------------------------------------------------------------

    @Column(name = "last_used_at")
    private Instant lastUsedAt;


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
