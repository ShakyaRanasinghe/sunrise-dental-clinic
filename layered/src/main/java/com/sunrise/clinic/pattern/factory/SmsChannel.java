package com.sunrise.clinic.pattern.factory;

import com.sunrise.clinic.domain.ChannelType;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SMS channel. There is no genuinely-free SMS gateway, so this channel logs
 * the message (a drop-in adapter for Twilio/Dialog can be added later without
 * changing any caller — that is the point of the Factory + interface).
 */
public class SmsChannel implements NotificationChannel {

    private static final Logger log = Logger.getLogger(SmsChannel.class.getName());

    @Override
    public ChannelType type() {
        return ChannelType.SMS;
    }

    @Override
    public DispatchResult send(String recipient, String subject, String body) {
        log.log(Level.INFO, "sms_logged recipient={0} subject={1}",
                new Object[]{recipient, subject});
        return new DispatchResult(true, "logged", "no SMS gateway configured — SMS logged", ChannelType.SMS);
    }
}
