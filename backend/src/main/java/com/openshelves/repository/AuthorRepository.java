package com.openshelves.repository;

import com.openshelves.model.entity.AuthorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link AuthorEntity} instances.
 * Provides basic CRUD operations and custom query methods through Spring Data JPA.
 */
@Repository
public interface AuthorRepository extends JpaRepository<AuthorEntity, Long> {

}
