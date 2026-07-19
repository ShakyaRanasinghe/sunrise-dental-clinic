package com.sunrise.clinic.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.sunrise.clinic.domain.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Production authentication: verifies a Firebase ID token from the {@code Authorization:
 * Bearer} header via the Firebase Admin SDK and reads the {@code role} custom claim into a
 * {@link ClinicPrincipal}. Active only when Firebase is configured (the {@link FirebaseAuth}
 * bean exists); otherwise it no-ops and the dev-header filter handles auth. An invalid token
 * is left unauthenticated (→ 401 downstream), never a 500.
 */
@Component
public class FirebaseAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FirebaseAuthFilter.class);

    private final ObjectProvider<FirebaseAuth> firebaseAuth;

    public FirebaseAuthFilter(ObjectProvider<FirebaseAuth> firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            FirebaseAuth auth = firebaseAuth.getIfAvailable();
            String header = request.getHeader("Authorization");
            if (auth != null && header != null && header.startsWith("Bearer ")) {
                try {
                    FirebaseToken token = auth.verifyIdToken(header.substring(7));
                    Object roleClaim = token.getClaims().get("role");
                    Role role = roleClaim != null
                            ? Role.valueOf(roleClaim.toString().trim().toUpperCase())
                            : Role.PATIENT;
                    ClinicPrincipal principal = new ClinicPrincipal(token.getUid(), role);
                    var authToken = new UsernamePasswordAuthenticationToken(
                            principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } catch (Exception e) {
                    // Invalid/expired token — remain unauthenticated (results in 401, not 500).
                    log.debug("firebase_token_rejected error={}", e.toString());
                }
            }
        }
        chain.doFilter(request, response);
    }
}
