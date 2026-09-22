package com.openshelves.catalog.repository;

import com.openshelves.metadata.entity.BookEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/// Spring Data JPA repository for [BookEntity] instances.
///
/// Provides standard CRUD operations and can be extended with custom query methods.
@Repository
public interface BookRepository extends JpaRepository<BookEntity, Long> {

}
