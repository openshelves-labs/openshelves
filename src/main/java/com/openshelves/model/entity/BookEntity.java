package com.openshelves.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Formula;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "books")
public class BookEntity extends BaseEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "subtitle")
    private String subtitle;

    @OneToMany(mappedBy = "book", fetch = FetchType.LAZY, orphanRemoval = true, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @OrderColumn(name = "sort_order")
    @Builder.Default
    private List<BookAuthorMappingEntity> authorMappings = new ArrayList<>();

    @Column(name = "description")
    private String description;

    @Column(name = "publisher")
    private String publisher;

    @Column(name = "publication_year")
    private Integer publicationYear;

    @Column(name = "language")
    private String language;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "isbn_10")
    private String isbn10;

    @Column(name = "isbn_13")
    private String isbn13;

    @Column(name = "asin")
    private String asin;

    @Column(name = "dewey_decimal")
    private Integer deweyDecimal;

    @Column(name = "lc_classification")
    private String lcClassification;

    @Column(name = "series_name")
    private String seriesName;

    @Column(name = "series_number")
    private Integer seriesNumber;

    @Column(name = "cover_image_url")
    private String coverImageUrl;


    /// Primary Authors (Readonly)

    /*
     Dev Note: We need to access the names of the primary authors frequently due to grid displays, but it is expensive
               to query the entire output every time. For this we use @Formula to query directly during the intial query

     */

//    @Generated
//    @Formula("""
//        (SELECT string_agg(a.name, ':::' ORDER BY m.sort_order)
//         FROM book_authors m
//         JOIN authors a ON a.id = m.author_id
//         WHERE m.book_id = id AND m.role = 'PRIMARY_AUTHOR')
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


    /// Data Pre-Processing

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
        this.publisher = StringUtils.trimToNull(this.publisher);
        this.language = StringUtils.trimToNull(this.language);
        this.isbn10 = StringUtils.trimToNull(this.isbn10);
        this.isbn13 = StringUtils.trimToNull(this.isbn13);
        this.asin = StringUtils.trimToNull(this.asin);
        this.lcClassification = StringUtils.trimToNull(this.lcClassification);
        this.seriesName = StringUtils.trimToNull(this.seriesName);
        this.coverImageUrl = StringUtils.trimToNull(this.coverImageUrl);
    }
}
