package com.sunrise.clinic.security;

import com.sunrise.clinic.domain.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Development/QA authentication: reads {@code X-User-Uid} + {@code X-User-Role} headers
 * and establishes the security context. This lets the API run and be tested locally
 * without Firebase. In production this filter is replaced by a Firebase ID-token
 * verifier (issue #8) — the rest of the app is unaffected because both produce the same
 * {@link ClinicPrincipal}.
 */
@Component
public class DevHeaderAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // If Firebase (or anything upstream) already authenticated the request, don't override it.
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }
        String uid = request.getHeader("X-User-Uid");
        String roleHeader = request.getHeader("X-User-Role");
        if (uid != null && roleHeader != null) {
            try {
                Role role = Role.valueOf(roleHeader.trim().toUpperCase());
                ClinicPrincipal principal = new ClinicPrincipal(uid, role);
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
                var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (IllegalArgumentException ignored) {
                // unknown role header — leave the request unauthenticated
            }
        }
        chain.doFilter(request, response);
    }
}
