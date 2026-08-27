package com.sunrise.clinic.pattern;

import com.sunrise.clinic.domain.ChannelType;
import com.sunrise.clinic.pattern.factory.EmailChannel;
import com.sunrise.clinic.pattern.factory.NotificationChannel;
import com.sunrise.clinic.pattern.factory.NotificationChannelFactory;
import com.sunrise.clinic.pattern.factory.SmsChannel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * TC-CLI-A07 — Factory Method: returns the correct channel per type.
 */
class NotificationChannelFactoryTest {

    private NotificationChannelFactory factory() {
        EmailChannel email = new EmailChannel("no-reply@sunrisedental.lk");
        SmsChannel sms = new SmsChannel();
        return new NotificationChannelFactory(List.of(email, sms));
    }

    @Test
    void createsEmailChannel() {
        NotificationChannel c = factory().create(ChannelType.EMAIL);
        assertEquals(ChannelType.EMAIL, c.type());
    }

    @Test
    void createsSmsChannel() {
        NotificationChannel c = factory().create(ChannelType.SMS);
        assertEquals(ChannelType.SMS, c.type());
    }

    @Test
    void emailChannelRecordsTheMessage() {
        NotificationChannel c = factory().create(ChannelType.EMAIL);
        var result = c.send("patient@x.lk", "subject", "body");
        assertEquals("logged", result.reason());
    }
}
