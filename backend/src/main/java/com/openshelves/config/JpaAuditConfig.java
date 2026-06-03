package com.openshelves.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/// Activates Spring Data JPA auditing, enabling automatic population of
/// `@CreatedDate` and `@LastModifiedDate` fields on entity persistence events.
@Configuration
@EnableJpaAuditing
public class JpaAuditConfig {

}
