package com.openshelves.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "authors")
public class AuthorEntity extends BaseEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;


    // -------------------------------------------------------------------------
    // Core metadata
    // -------------------------------------------------------------------------

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "bio")
    private String bio;

    @Column(name = "nationality")
    private String nationality;


    // -------------------------------------------------------------------------
    // Dates
    // -------------------------------------------------------------------------

    @Column(name = "birth_year")
    private Short birthYear;

    @Column(name = "death_year")
    private Short deathYear;


    // -------------------------------------------------------------------------
    // Identifiers
    // -------------------------------------------------------------------------

    @Column(name = "asin")
    private String asin;

    @Column(name = "olid")
    private String olid;


    // -------------------------------------------------------------------------
    // Media
    // -------------------------------------------------------------------------

    @Column(name = "profile_image_url")
    private String profileImageUrl;


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
    // Relationships
    // -------------------------------------------------------------------------

    // Restrict deletion of author if they are associated with any books to maintain referential integrity
    @OneToMany(mappedBy = "author")
    private List<BookAuthorEntity> bookAuthors = new ArrayList<>();


    // -------------------------------------------------------------------------
    // Data Pre-Processing
    // -------------------------------------------------------------------------

    @PrePersist
    @PreUpdate
    public void prePersistUpdate() {
        trimStringFields();
    }

    private void trimStringFields() {
        this.name = StringUtils.trimToNull(this.name);
        this.bio = StringUtils.trimToNull(this.bio);
        this.nationality = StringUtils.trimToNull(this.nationality);
        this.asin = StringUtils.trimToNull(this.asin);
        this.olid = StringUtils.trimToNull(this.olid);
        this.profileImageUrl = StringUtils.trimToNull(this.profileImageUrl);
    }
}
