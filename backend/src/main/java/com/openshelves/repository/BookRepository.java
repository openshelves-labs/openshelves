package com.openshelves.repository;

import com.openshelves.model.entity.BookEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link BookEntity} instances.
 * Provides basic CRUD operations and custom query methods through Spring Data JPA.
 */
@Repository
public interface BookRepository extends JpaRepository<BookEntity, Long> {

}
