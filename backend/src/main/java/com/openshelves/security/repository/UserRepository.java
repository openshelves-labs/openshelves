package com.openshelves.security.repository;

import com.openshelves.security.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/// Data access for users.
@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
}
