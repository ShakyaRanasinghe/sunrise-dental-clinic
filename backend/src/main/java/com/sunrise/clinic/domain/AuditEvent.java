package com.sunrise.clinic.domain;

import java.time.Instant;
import java.util.Objects;

/** An immutable, append-only record of an action taken in the system (audit trail). */
public class AuditEvent {
    private String id;
    private String actorUid;
    private Role actorRole;
    private String action;
    private String targetType;
    private String targetId;
    private Instant timestamp;

    public AuditEvent() {
    }

    public AuditEvent(String id, String actorUid, Role actorRole, String action, String targetType, String targetId, Instant timestamp) {
        this.id = id;
        this.actorUid = actorUid;
        this.actorRole = actorRole;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getActorUid() {
        return actorUid;
    }

    public void setActorUid(String actorUid) {
        this.actorUid = actorUid;
    }

    public Role getActorRole() {
        return actorRole;
    }

    public void setActorRole(Role actorRole) {
        this.actorRole = actorRole;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuditEvent)) return false;
        return Objects.equals(id, ((AuditEvent) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AuditEvent{id=" + id + "}";
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Hand-written Builder — replaces Lombok's {@code @Builder}. */
    public static class Builder {
        private String id;
        private String actorUid;
        private Role actorRole;
        private String action;
        private String targetType;
        private String targetId;
        private Instant timestamp;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder actorUid(String actorUid) {
            this.actorUid = actorUid;
            return this;
        }

        public Builder actorRole(Role actorRole) {
            this.actorRole = actorRole;
            return this;
        }

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        public Builder targetType(String targetType) {
            this.targetType = targetType;
            return this;
        }

        public Builder targetId(String targetId) {
            this.targetId = targetId;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public AuditEvent build() {
            return new AuditEvent(id, actorUid, actorRole, action, targetType, targetId, timestamp);
        }
    }
}
