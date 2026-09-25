package com.openshelves.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/// Activates Spring Data JPA auditing, enabling automatic population of
/// `@CreatedDate` and `@LastModifiedDate` fields on entity persistence events.
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider", modifyOnCreate = true)
public class JpaAuditConfig {

    /// Provides an `AuditorAware` bean that returns an empty `Optional`, indicating that no auditor information is available.
    @Bean
    public AuditorAware<String> auditorProvider() {
        return Optional::empty;
    }
}
