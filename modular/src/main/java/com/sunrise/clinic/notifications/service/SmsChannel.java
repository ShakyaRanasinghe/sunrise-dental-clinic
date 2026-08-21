package com.sunrise.clinic.notifications.service;

import com.sunrise.clinic.notifications.domain.ChannelType;
import com.sunrise.clinic.notifications.domain.DispatchResult;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SMS, recorded rather than sent.
 *
 * <p>No SMS gateway is free, and the clinic has not bought one. A Twilio or Dialog adapter
 * drops in as one more implementation of {@link NotificationChannel} without changing a single
 * caller, which is what the factory is for.</p>
 *
 * <p>Returns {@code LOGGED} for the same reason {@link EmailChannel} does. It also no longer
 * relies on remembering a try-catch: {@link NotificationChannel#dispatch} holds that guarantee
 * for every channel, which is why this one never had one and the contract still held.</p>
 */
public class SmsChannel implements NotificationChannel {

    private static final Logger log = Logger.getLogger(SmsChannel.class.getName());

    @Override
    public ChannelType type() {
        return ChannelType.SMS;
    }

    @Override
    public DispatchResult send(String recipient, String subject, String body) {
        if (recipient == null || recipient.isBlank()) {
            // A patient who registered without a contact number — which is allowed, and is
            // exactly why this is checked rather than assumed.
            return DispatchResult.failed(ChannelType.SMS, "the patient has no contact number");
        }
        log.log(Level.INFO, "sms_logged recipient={0} subject={1}",
                new Object[] { recipient, subject });
        return DispatchResult.logged(ChannelType.SMS, "no SMS gateway configured");
    }
}
