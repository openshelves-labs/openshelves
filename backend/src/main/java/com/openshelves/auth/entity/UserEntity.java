package com.openshelves.auth.entity;

import com.openshelves.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;

/// Entity representing a user in the OpenShelves system, including core metadata, contact information,
/// localization preferences, and status flags.
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "users")
public class UserEntity extends BaseEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;


    // -------------------------------------------------------------------------
    // Core metadata
    // -------------------------------------------------------------------------

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "bio")
    private String bio;


    @Column(name = "department")
    private String department;

    @Column(name = "program")
    private String program;


    @Column(name = "institutional_id")
    private String institutionalId;


    // -------------------------------------------------------------------------
    // Contact
    // -------------------------------------------------------------------------

    @Column(name = "email")
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;


    // -------------------------------------------------------------------------
    // Localization
    // -------------------------------------------------------------------------

    @Column(name = "language")
    private String language;
    /// ISO language code

    @Column(name = "timezone")
    private String timezone;


    // -------------------------------------------------------------------------
    // Status Flags
    // -------------------------------------------------------------------------

    @Column(name = "is_active", nullable = false)
    private boolean active = true;


    // -------------------------------------------------------------------------
    // Media
    // -------------------------------------------------------------------------

    @Column(name = "avatar_image_url")
    private String avatarImageUrl;


    // -------------------------------------------------------------------------
    // Audit
    // -------------------------------------------------------------------------

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;


    // -------------------------------------------------------------------------
    // Data Pre-Processing
    // -------------------------------------------------------------------------

    @PrePersist
    @PreUpdate
    public void prePersistUpdate() {
        trimStringFields();
    }

    private void trimStringFields() {
        this.displayName = StringUtils.trimToNull(this.displayName);
        this.bio = StringUtils.trimToNull(this.bio);
        this.department = StringUtils.trimToNull(this.department);
        this.program = StringUtils.trimToNull(this.program);
        this.institutionalId = StringUtils.trimToNull(this.institutionalId);
        this.email = StringUtils.trimToNull(this.email);
        this.phoneNumber = StringUtils.trimToNull(this.phoneNumber);
        this.language = StringUtils.trimToNull(this.language);
        this.timezone = StringUtils.trimToNull(this.timezone);
        this.avatarImageUrl = StringUtils.trimToNull(this.avatarImageUrl);
    }
}
