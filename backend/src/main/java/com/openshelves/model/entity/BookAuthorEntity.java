package com.openshelves.model.entity;

import com.openshelves.model.enums.AuthorRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/// Join entity representing the relationship between a [BookEntity] and an [AuthorEntity].
/// Includes additional metadata such as the contributor's role and sort order.
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "book_authors")
public class BookAuthorEntity extends BaseEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    // -------------------------------------------------------------------------
    // Relationships
    // -------------------------------------------------------------------------

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private BookEntity book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private AuthorEntity author;


    // -------------------------------------------------------------------------
    // Additional Metadata
    // -------------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private AuthorRole role;

    @Column(name = "sort_order", nullable = false)
    private Short sortOrder;
}
