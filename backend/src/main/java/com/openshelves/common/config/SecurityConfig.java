package com.openshelves.common.config;

import com.openshelves.security.filter.BearerTokenFilter;
import com.openshelves.security.services.AuthService;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
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
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthService authService) throws Exception {
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

                // Health checks for load balancers and probes
                .requestMatchers("/actuator/health/**").permitAll()

                // Error dispatches, so error responses aren't turned into 401s
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()

                // Everything else requires a valid bearer token
                .anyRequest().authenticated()
            )

            // Auth Filter
            .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class)

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
}
