package com.sunrise.clinic.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** A delivery receipt for a confirmation/reminder message sent to a patient. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    private String id;
    private String appointmentNo;
    private ChannelType channel;
    private String recipient;
    private String subject;
    private String body;
    private NotificationStatus status;
    private Instant sentAt;
}
