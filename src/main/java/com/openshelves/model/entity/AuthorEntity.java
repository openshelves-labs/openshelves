package com.openshelves.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "authors")
public class AuthorEntity extends BaseEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "bio")
    private String bio;

    @Column(name = "nationality")
    private String nationality;

    @Column(name = "birth_year")
    private Integer birthYear;

    @Column(name = "death_year")
    private Integer deathYear;

    @Column(name = "asin")
    private String asin;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @OneToMany(mappedBy = "author", fetch = FetchType.LAZY)
    @Builder.Default
    private List<BookAuthorMappingEntity> bookMappings = new ArrayList<>();


    /// Data Pre-Processing

    @PrePersist
    @PreUpdate
    public void prePersistUpdate() {
        trimStringFields();
    }

    // Cleans unwanted spaces in metadata fields to ensure consistent storage and searching
    private void trimStringFields() {
        this.name = StringUtils.trimToNull(this.name);
        this.bio = StringUtils.trimToNull(this.bio);
        this.nationality = StringUtils.trimToNull(this.nationality);
        this.asin = StringUtils.trimToNull(this.asin);
        this.profileImageUrl = StringUtils.trimToNull(this.profileImageUrl);
    }
}
