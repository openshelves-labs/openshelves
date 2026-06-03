package com.openshelves.repository;

import com.openshelves.model.entity.AuthorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/// Spring Data JPA repository for [AuthorEntity] instances.
/// Provides standard CRUD operations and can be extended with custom query methods.
@Repository
public interface AuthorRepository extends JpaRepository<AuthorEntity, Long> {

}
