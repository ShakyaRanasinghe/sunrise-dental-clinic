package com.sunrise.clinic.pattern.factory;

import com.sunrise.clinic.domain.ChannelType;

/**
 * A delivery channel for notifications (the product interface of the
 * FACTORY METHOD pattern). Concrete channels ({@code EmailChannel},
 * {@code SmsChannel}) are created by {@link NotificationChannelFactory}.
 */
public interface NotificationChannel {

    /** @return which channel this is (used by the factory to look it up). */
    ChannelType type();

    /**
     * Send a message. Never throws — returns a {@link DispatchResult} instead,
     * because a notification failure must never break the business action
     * (booking, billing) that triggered it.
     */
    DispatchResult send(String recipient, String subject, String body);
}
