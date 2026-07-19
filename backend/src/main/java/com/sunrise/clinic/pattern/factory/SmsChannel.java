package com.sunrise.clinic.pattern.factory;

import com.sunrise.clinic.domain.ChannelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * SMS channel. There is no genuinely-free SMS gateway, so this channel logs
 * the message (a drop-in adapter for Twilio/Dialog can be added later without
 * changing any caller — that is the point of the Factory + interface).
 */
@Component
public class SmsChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(SmsChannel.class);

    @Override
    public ChannelType type() {
        return ChannelType.SMS;
    }

    @Override
    public DispatchResult send(String recipient, String subject, String body) {
        log.info("sms_logged recipient={} subject={}", recipient, subject);
        return new DispatchResult(true, "logged", "no SMS gateway configured — SMS logged", ChannelType.SMS);
    }
}
