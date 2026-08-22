package com.sunrise.clinic.notifications.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * The record of one attempt to tell a patient something - FR-NOT-03.
 *
 * <p>Written whatever the outcome. A message that failed leaves a row saying so, because the
 * question the practice asks later is "was the patient told", and silence is not an answer to
 * it.</p>
 *
 * <p>It holds the {@code body} that was sent, which is what makes the record worth keeping -
 * "a reminder went out" is far less useful than the words the patient actually received when
 * they arrive on the wrong day. {@code toString} deliberately carries only the id: it ends up in
 * logs and exception messages, and the body names a patient and their appointment.</p>
 */
public class Notification {
    private String id;
    private String appointmentNo;
    private ChannelType channel;
    private NotificationKind kind;
    private String recipient;
    private String subject;
    private String body;
    private NotificationStatus status;
    private Instant sentAt;

    public Notification() {
    }

    public Notification(String id, String appointmentNo, ChannelType channel,
                        NotificationKind kind, String recipient, String subject, String body,
                        NotificationStatus status, Instant sentAt) {
        this.id = id;
        this.appointmentNo = appointmentNo;
        this.channel = channel;
        this.kind = kind;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.status = status;
        this.sentAt = sentAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAppointmentNo() {
        return appointmentNo;
    }

    public void setAppointmentNo(String appointmentNo) {
        this.appointmentNo = appointmentNo;
    }

    public NotificationKind getKind() {
        return kind;
    }

    public void setKind(NotificationKind kind) {
        this.kind = kind;
    }

    public ChannelType getChannel() {
        return channel;
    }

    public void setChannel(ChannelType channel) {
        this.channel = channel;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Notification)) return false;
        return Objects.equals(id, ((Notification) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Notification{id=" + id + ", kind=" + kind + ", status=" + status + "}";
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Hand-written Builder — replaces Lombok's {@code @Builder}. */
    public static class Builder {
        private String id;
        private String appointmentNo;
        private ChannelType channel;
        private NotificationKind kind;
        private String recipient;
        private String subject;
        private String body;
        private NotificationStatus status;
        private Instant sentAt;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder appointmentNo(String appointmentNo) {
            this.appointmentNo = appointmentNo;
            return this;
        }

        public Builder kind(NotificationKind kind) {
            this.kind = kind;
            return this;
        }

        public Builder channel(ChannelType channel) {
            this.channel = channel;
            return this;
        }

        public Builder recipient(String recipient) {
            this.recipient = recipient;
            return this;
        }

        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder status(NotificationStatus status) {
            this.status = status;
            return this;
        }

        public Builder sentAt(Instant sentAt) {
            this.sentAt = sentAt;
            return this;
        }

        public Notification build() {
            return new Notification(id, appointmentNo, channel, kind, recipient, subject, body,
                    status, sentAt);
        }
    }
}
