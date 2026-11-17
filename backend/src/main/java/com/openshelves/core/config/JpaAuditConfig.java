package com.openshelves.core.config;

import com.openshelves.security.model.SessionPrincipal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Activates Spring Data JPA auditing, enabling automatic population of
 * {@link CreatedDate}, {@link LastModifiedDate}, {@link CreatedBy} and {@link LastModifiedBy} fields.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider", modifyOnCreate = true)
public class JpaAuditConfig {

    /**
     * Resolves the current auditor as the user ID of the authenticated {@link SessionPrincipal}.
     */
    @Bean
    public AuditorAware<Long> auditorProvider() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            // If the user is authenticated and the principal is a SessionPrincipal, return the user ID.
            if (auth != null && auth.getPrincipal() instanceof SessionPrincipal principal) {
                return Optional.of(principal.getUserId());
            }

            // Empty for unauthenticated requests (e.g. login) and background jobs.
            return Optional.empty();
        };
    }
}
