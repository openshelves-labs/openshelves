package com.openshelves.auth.entity;

import com.openshelves.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.annotation.CreatedDate;

import java.time.Instant;

/// Entity representing a user session in the OpenShelves system, including session metadata and expiration details.
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "user_sessions")
public class UserSessionEntity extends BaseEntity<Long> {

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

    /// SHA-256 hash of the session identifier; unique.
    @Column(name = "session_token_hash", nullable = false, updatable = false)
    private String sessionTokenHash;


    // -------------------------------------------------------------------------
    // Device metadata
    // -------------------------------------------------------------------------

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "device_location")
    private String deviceLocation;

    @CreatedDate
    @Column(name = "last_active_at", nullable = false)
    private Instant lastActiveAt;


    // -------------------------------------------------------------------------
    // Timeouts
    // -------------------------------------------------------------------------

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;


    // -------------------------------------------------------------------------
    // Audit
    // -------------------------------------------------------------------------

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;


    // -------------------------------------------------------------------------
    // Data Pre-Processing
    // -------------------------------------------------------------------------

    @PrePersist
    @PreUpdate
    public void prePersistUpdate() {
        trimStringFields();
    }

    private void trimStringFields() {
        this.ipAddress = StringUtils.trimToNull(this.ipAddress);
        this.userAgent = StringUtils.trimToNull(this.userAgent);
        this.deviceName = StringUtils.trimToNull(this.deviceName);
        this.deviceLocation = StringUtils.trimToNull(this.deviceLocation);
    }
}
