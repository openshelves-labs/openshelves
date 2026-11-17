package com.openshelves.security.entity;

import com.openshelves.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;

import java.time.Instant;

/// Entity representing an emailed link to set a new password, for forgot password and first-time setup.
///
/// Only the token's hash is stored. The row is deleted when the token is used,
/// or replaced when the user asks for a new link.
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "password_reset_tokens")
public class PasswordResetTokenEntity extends BaseEntity<Long> {

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
    // Token
    // -------------------------------------------------------------------------

    /// SHA-256 hash of the token.
    @Column(name = "token_hash", nullable = false, updatable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;


    // -------------------------------------------------------------------------
    // Audit
    // -------------------------------------------------------------------------

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
