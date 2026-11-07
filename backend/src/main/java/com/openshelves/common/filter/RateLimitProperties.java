package com.openshelves.common.filter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/// Rate limits for [RateLimitFilter], from `openshelves.rate-limit` in `application.yml`.
/// Checked at startup, so a malformed entry stops the app instead of being ignored.
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "openshelves.rate-limit")
public class RateLimitProperties {

    /// Shared limit per IP for unauthenticated requests no endpoint limit covers; unlimited if not set.
    @Positive
    private Integer unauthenticatedPerMinute;

    /// Path prefixes never limited, e.g. `/actuator/health`.
    private List<String> exemptPaths = new ArrayList<>();

    /// Per-endpoint limits, applied whether signed in or not.
    @Valid
    private List<Endpoint> endpoints = new ArrayList<>();


    /// A limit on one endpoint.
    @Getter
    @Setter
    public static class Endpoint {

        /// The HTTP method, e.g. `POST`.
        @NotNull
        private HttpMethod method;

        /// The exact request path, e.g. `/api/v1/auth/login`.
        @NotBlank
        private String path;

        /// Requests allowed per IP per minute.
        @Positive
        private int perMinute;
    }
}
