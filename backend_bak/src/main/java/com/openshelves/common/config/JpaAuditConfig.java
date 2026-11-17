package com.openshelves.common.config;

import com.openshelves.common.model.SessionPrincipal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/// Activates Spring Data JPA auditing, enabling automatic population of
/// `@CreatedDate`, `@LastModifiedDate`, `@CreatedBy` and `@LastModifiedBy` fields.
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider", modifyOnCreate = true)
public class JpaAuditConfig {

    /// Resolves the current auditor as the user ID of the authenticated [SessionPrincipal].
    ///
    /// Empty for unauthenticated requests (e.g. login) and background jobs.
    @Bean
    public AuditorAware<Long> auditorProvider() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth != null && auth.getPrincipal() instanceof SessionPrincipal principal) {
                return Optional.of(principal.getUserId());
            }

            return Optional.empty();
        };
    }
}
