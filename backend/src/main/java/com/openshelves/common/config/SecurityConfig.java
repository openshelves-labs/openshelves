package com.openshelves.common.config;

import com.openshelves.security.filter.BearerTokenFilter;
import com.openshelves.security.services.AuthService;
import com.openshelves.security.sso.CookieOidcRequestRepository;
import com.openshelves.security.sso.DbOidcRegistrationRepository;
import com.openshelves.security.sso.DbSamlRegistrationRepository;
import com.openshelves.security.sso.SsoCallbackHandler;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${spring.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthService authService,
                                                   DbOidcRegistrationRepository oidcRegistrations,
                                                   DbSamlRegistrationRepository samlRegistrations,
                                                   CookieOidcRequestRepository oidcRequests,
                                                   SsoCallbackHandler ssoCallbackHandler) throws Exception {
        Filter authFilter = new BearerTokenFilter(authService);

        http
            // CORS configuration
            .cors(Customizer.withDefaults())

            // Stateless session management
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Turn off default login routing
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)

            // Authorize requests
            .authorizeHttpRequests(auth -> auth
                // Login, since the caller has no token yet
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()

                // Logout, so a stale or missing token can still clear itself
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").permitAll()

                // SSO: the sign-in page's provider list, and the whole round-trip
                // (start, provider redirects, callbacks, metadata, exchange), which happens before there's a token
                .requestMatchers(HttpMethod.GET, "/api/v1/auth/providers").permitAll()
                .requestMatchers("/api/v1/auth/sso/**").permitAll()

                // Health checks for load balancers and probes
                .requestMatchers("/actuator/health/**").permitAll()

                // Error dispatches, so error responses aren't turned into 401s
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()

                // Everything else requires a valid bearer token
                .anyRequest().authenticated()
            )

            // Auth Filter
            .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class)

            // SSO with OIDC: providers from auth_providers, pending request in a cookie, our own callback handling.
            // Start is our controller (/sso/{provider}/start), which redirects to the authorize endpoint below.
            .oauth2Login(oidc -> oidc
                .loginPage("/api/v1/auth/providers")    // Turns off Spring's generated login page
                .clientRegistrationRepository(oidcRegistrations)
                .authorizedClientRepository(new NoStoredClients())
                .authorizationEndpoint(a -> a
                    .baseUri("/api/v1/auth/sso/oidc-authorize")
                    .authorizationRequestRepository(oidcRequests))
                .loginProcessingUrl("/api/v1/auth/sso/*/oidc-callback")
                .successHandler(ssoCallbackHandler)
                .failureHandler(ssoCallbackHandler)
            )

            // SSO with SAML: same idea; the pending request lives in a cookie (CookieSamlRequestRepository bean)
            .saml2Login(saml -> saml
                .loginPage("/api/v1/auth/providers")    // Turns off Spring's generated login page
                .relyingPartyRegistrationRepository(samlRegistrations)
                .authenticationRequestUri("/api/v1/auth/sso/saml-authorize/{registrationId}")
                .loginProcessingUrl("/api/v1/auth/sso/{registrationId}/saml-callback")
                .successHandler(ssoCallbackHandler)
                .failureHandler(ssoCallbackHandler)
            )

            // SAML service-provider metadata, for the university's IT to register OpenShelves
            .saml2Metadata(metadata -> metadata
                .metadataUrl("/api/v1/auth/sso/{registrationId}/metadata")
            )

            // Exception handling
            .exceptionHandling(e -> e
                .authenticationEntryPoint((_, res, _) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication is required to access this resource."))
                .accessDeniedHandler((_, res, _) -> res.sendError(HttpServletResponse.SC_FORBIDDEN, "You do not have permission to access this resource."))
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
        config.setExposedHeaders(List.of(HttpHeaders.WWW_AUTHENTICATE));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /// Delegating encoder: hashes with bcrypt and reads the `{id}` prefix, so stored hashes can be upgraded later.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }


    /// OIDC is used only to sign in; the provider's access token is never needed afterwards,
    /// so it isn't stored. Spring's default would keep it in an HTTP session, which the API doesn't have.
    private static final class NoStoredClients implements OAuth2AuthorizedClientRepository {

        @Override
        public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String clientRegistrationId, Authentication principal, HttpServletRequest request) {
            return null;
        }

        @Override
        public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication principal, HttpServletRequest request, HttpServletResponse response) {
            // Not stored
        }

        @Override
        public void removeAuthorizedClient(String clientRegistrationId, Authentication principal, HttpServletRequest request, HttpServletResponse response) {
            // Nothing stored
        }
    }
}
