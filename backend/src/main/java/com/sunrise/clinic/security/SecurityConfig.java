package com.sunrise.clinic.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless security. Method-level authorization is enabled ({@code @PreAuthorize}) and
 * the {@link DevHeaderAuthFilter} establishes the principal. Endpoints are currently open
 * at the URL level; role enforcement is applied per-method and tightened in issue #8.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final FirebaseAuthFilter firebaseAuthFilter;
    private final DevHeaderAuthFilter devHeaderAuthFilter;

    public SecurityConfig(FirebaseAuthFilter firebaseAuthFilter, DevHeaderAuthFilter devHeaderAuthFilter) {
        this.firebaseAuthFilter = firebaseAuthFilter;
        this.devHeaderAuthFilter = devHeaderAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                // Firebase token first (production); dev-header fallback second (local/QA).
                .addFilterBefore(firebaseAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(devHeaderAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
