package com.sunrise.clinic.access.domain;

import com.sunrise.clinic.access.service.PasswordHasher;

import java.time.Instant;
import java.util.Objects;

/**
 * A system login. The password hash is held by {@code PasswordHasher} (PBKDF2);
 * this record holds the profile, the role and the lock-out counters that back
 * the "lock account after failed attempts" security use case.
 */
public class UserAccount {
    private String uid;   // primary key
    private String email;
    private String passwordHash;   // PBKDF2 hash, never the plaintext
    private String displayName;
    private Role role;
    private boolean active = true;
    private int failedAttempts = 0;
    private boolean locked = false;
    private Instant createdAt;

    public UserAccount() {
    }

    public UserAccount(String uid, String email, String passwordHash, String displayName, Role role, boolean active, int failedAttempts, boolean locked, Instant createdAt) {
        this.uid = uid;
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = role;
        this.active = active;
        this.failedAttempts = failedAttempts;
        this.locked = locked;
        this.createdAt = createdAt;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserAccount)) return false;
        return Objects.equals(uid, ((UserAccount) o).uid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid);
    }

    @Override
    public String toString() {
        return "UserAccount{uid=" + uid + "}";
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Hand-written Builder — replaces Lombok's {@code @Builder}. */
    public static class Builder {
        private String uid;
        private String email;
        private String passwordHash;
        private String displayName;
        private Role role;
        private boolean active = true;
        private int failedAttempts = 0;
        private boolean locked = false;
        private Instant createdAt;

        public Builder uid(String uid) {
            this.uid = uid;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder role(Role role) {
            this.role = role;
            return this;
        }

        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        public Builder failedAttempts(int failedAttempts) {
            this.failedAttempts = failedAttempts;
            return this;
        }

        public Builder locked(boolean locked) {
            this.locked = locked;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public UserAccount build() {
            return new UserAccount(uid, email, passwordHash, displayName, role, active, failedAttempts, locked, createdAt);
        }
    }
}
