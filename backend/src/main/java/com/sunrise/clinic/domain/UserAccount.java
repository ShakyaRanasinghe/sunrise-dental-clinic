package com.sunrise.clinic.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * A system login. Credentials themselves live in Firebase Authentication —
 * this record holds only the profile + role. {@code failedAttempts}/{@code locked}
 * back the "lock account after failed attempts" security use case.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAccount {
    private String uid;          // Firebase Auth UID (primary key)
    private String email;
    private String displayName;
    private Role role;
    @Builder.Default
    private boolean active = true;
    @Builder.Default
    private int failedAttempts = 0;
    @Builder.Default
    private boolean locked = false;
    private Instant createdAt;
}
