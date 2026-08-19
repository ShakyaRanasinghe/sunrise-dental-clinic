package com.sunrise.clinic.domain;

import java.time.Instant;
import java.util.Objects;

/** A delivery receipt for a confirmation/reminder message sent to a patient. */
public class Notification {
    private String id;
    private String appointmentNo;
    private ChannelType channel;
    private String recipient;
    private String subject;
    private String body;
    private NotificationStatus status;
    private Instant sentAt;

    public Notification() {
    }

    public Notification(String id, String appointmentNo, ChannelType channel, String recipient, String subject, String body, NotificationStatus status, Instant sentAt) {
        this.id = id;
        this.appointmentNo = appointmentNo;
        this.channel = channel;
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
        return "Notification{id=" + id + "}";
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Hand-written Builder — replaces Lombok's {@code @Builder}. */
    public static class Builder {
        private String id;
        private String appointmentNo;
        private ChannelType channel;
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
            return new Notification(id, appointmentNo, channel, recipient, subject, body, status, sentAt);
        }
    }
}
