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
@Table(name = "books")
public class BookEntity extends BaseEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;


    // -------------------------------------------------------------------------
    // Core metadata
    // -------------------------------------------------------------------------

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "subtitle")
    private String subtitle;

    @Column(name = "description")
    private String description;

    @Column(name = "language")
    private String language;

    @Column(name = "page_count")
    private Integer pageCount;


    // -------------------------------------------------------------------------
    // Classification
    // -------------------------------------------------------------------------

    @Column(name = "dewey_decimal")
    private String deweyDecimal;

    @Column(name = "lc_classification")
    private String lcClassification;


    // -------------------------------------------------------------------------
    // Publisher
    // -------------------------------------------------------------------------

    @Column(name = "publisher")
    private String publisher;

    @Column(name = "publication_year")
    private Short publicationYear;


    // -------------------------------------------------------------------------
    // Series
    // -------------------------------------------------------------------------

    @Column(name = "series_name")
    private String seriesName;

    @Column(name = "series_number")
    private Integer seriesNumber;


    // -------------------------------------------------------------------------
    // Identifiers
    // -------------------------------------------------------------------------

    @Column(name = "isbn_10")
    private String isbn10;

    @Column(name = "isbn_13")
    private String isbn13;

    @Column(name = "asin")
    private String asin;

    @Column(name = "olid")
    private String olid;


    // -------------------------------------------------------------------------
    // Media
    // -------------------------------------------------------------------------

    @Column(name = "cover_image_url")
    private String coverImageUrl;


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

    // Deleting a book should cascade and delete all associated book-author relationships, but not the authors themselves
    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<BookAuthorEntity> bookAuthors = new ArrayList<>();


    // -------------------------------------------------------------------------
    // Derived
    // -------------------------------------------------------------------------

    /*
     Dev Note: We need to access the names of the primary authors frequently due to grid displays, but it is expensive
               to query the entire output every time. For this we use @Formula to query directly during the intial query
     */

//    @Formula("""
//        (SELECT STRING_AGG(a.name, ':::' ORDER BY ba.sort_order)
//         FROM book_authors ba JOIN authors a ON a.id = ba.author_id
//         WHERE ba.book_id = id AND ba.role IN ('AUTHOR', 'CO_AUTHOR'))
//    """)
//    @Getter(AccessLevel.NONE)
//    @Setter(AccessLevel.NONE)
//    private String primaryAuthorsQueryResult;
//
//    public List<String> getPrimaryAuthors() {
//        if (StringUtils.isEmpty(primaryAuthorsQueryResult)) {
//            return Collections.emptyList();
//        }
//        return Arrays.asList(primaryAuthorsQueryResult.split(":::"));
//    }

    // -------------------------------------------------------------------------
    // Data Pre-Processing
    // -------------------------------------------------------------------------

    @PrePersist
    @PreUpdate
    public void prePersistUpdate() {
        trimStringFields();
    }

    // Cleans unwanted spaces in metadata fields to ensure consistent storage and searching
    private void trimStringFields() {
        this.title = StringUtils.trimToNull(this.title);
        this.subtitle = StringUtils.trimToNull(this.subtitle);
        this.description = StringUtils.trimToNull(this.description);
        this.language = StringUtils.trimToNull(this.language);
        this.deweyDecimal = StringUtils.trimToNull(this.deweyDecimal);
        this.lcClassification = StringUtils.trimToNull(this.lcClassification);
        this.publisher = StringUtils.trimToNull(this.publisher);
        this.seriesName = StringUtils.trimToNull(this.seriesName);
        this.isbn10 = StringUtils.trimToNull(this.isbn10);
        this.isbn13 = StringUtils.trimToNull(this.isbn13);
        this.asin = StringUtils.trimToNull(this.asin);
        this.olid = StringUtils.trimToNull(this.olid);
        this.coverImageUrl = StringUtils.trimToNull(this.coverImageUrl);
    }
}
