package com.sunrise.clinic.notifications;

import com.sunrise.clinic.notifications.domain.ChannelType;
import com.sunrise.clinic.notifications.domain.DispatchResult;
import com.sunrise.clinic.notifications.domain.NotificationStatus;
import com.sunrise.clinic.notifications.service.EmailChannel;
import com.sunrise.clinic.notifications.service.NotificationChannel;
import com.sunrise.clinic.notifications.service.NotificationChannelFactory;
import com.sunrise.clinic.notifications.service.SmsChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Moved from {@code layered/} and extended. The original checked that the factory returned the
 * right type; what needed testing as well is the two guarantees the module rests on — that a
 * channel never breaks the operation that triggered it, and that it cannot claim a delivery it
 * did not make.
 */
class NotificationChannelFactoryTest {

    private NotificationChannelFactory factory;

    @BeforeEach
    void setUp() {
        factory = new NotificationChannelFactory(List.of(
                new EmailChannel("no-reply@sunrisedental.lk"), new SmsChannel()));
    }

    // --- the factory ---------------------------------------------------

    @Test
    void handsOutTheChannelAskedFor() {
        assertEquals(ChannelType.EMAIL, factory.create(ChannelType.EMAIL).type());
        assertEquals(ChannelType.SMS, factory.create(ChannelType.SMS).type());
    }

    @Test
    void anUnregisteredChannelIsAWiringMistakeAndSaysSo() {
        // Throwing is right: a missing channel is a mistake in AppContext, discovered on the
        // first attempt to use it. It is not a delivery failure, which is what DispatchResult
        // carries.
        NotificationChannelFactory emailOnly = new NotificationChannelFactory(
                List.of(new EmailChannel("no-reply@sunrisedental.lk")));

        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> emailOnly.create(ChannelType.SMS)).getMessage().contains("SMS"));
    }

    @Test
    void findLetsACallerNotCare() {
        // A clinic with no SMS gateway should not have its reminders fail — it should simply
        // not send them by SMS.
        NotificationChannelFactory emailOnly = new NotificationChannelFactory(
                List.of(new EmailChannel("no-reply@sunrisedental.lk")));

        assertTrue(emailOnly.find(ChannelType.EMAIL).isPresent());
        assertTrue(emailOnly.find(ChannelType.SMS).isEmpty());
        assertEquals(java.util.Set.of(ChannelType.EMAIL), emailOnly.available());
    }

    @Test
    void aLaterChannelReplacesAnEarlierOneOfTheSameType() {
        // So a real transport can be appended after the recording one and take over, without
        // the list having to be filtered first.
        NotificationChannel real = new NotificationChannel() {
            @Override
            public ChannelType type() {
                return ChannelType.EMAIL;
            }

            @Override
            public DispatchResult send(String recipient, String subject, String body) {
                return DispatchResult.sent(ChannelType.EMAIL, "delivered by a real transport");
            }
        };
        NotificationChannelFactory withReal = new NotificationChannelFactory(
                List.of(new EmailChannel("no-reply@sunrisedental.lk"), real));

        assertSame(real, withReal.create(ChannelType.EMAIL));
    }

    @Test
    void aNewChannelNeedsNoChangeToTheFactory() {
        // FR-NOT-05, stated as a test: this whole class is written against the interface, and
        // registering something the factory has never heard of is one constructor argument.
        NotificationChannel whatsApp = new NotificationChannel() {
            @Override
            public ChannelType type() {
                return ChannelType.SMS;      // no WhatsApp value exists yet; the shape is the point
            }

            @Override
            public DispatchResult send(String recipient, String subject, String body) {
                return DispatchResult.sent(ChannelType.SMS, "delivered by WhatsApp");
            }
        };

        assertTrue(new NotificationChannelFactory(List.of(whatsApp))
                .create(ChannelType.SMS).dispatch("0771234567", "Hello", "Body").sent());
    }

    // --- a channel must never break what triggered it -------------------

    @Test
    void aChannelThatThrowsBecomesAFailedResult() {
        // FR-NOT-04. The booking is already committed by the time anything is sent about it, so
        // an exception escaping here would report a failure for an appointment that exists —
        // and the patient would book again.
        NotificationChannel broken = new NotificationChannel() {
            @Override
            public ChannelType type() {
                return ChannelType.EMAIL;
            }

            @Override
            public DispatchResult send(String recipient, String subject, String body) {
                throw new IllegalStateException("the mail server is down");
            }
        };

        DispatchResult result = broken.dispatch("nimal@example.lk", "Confirmed", "Body");

        assertEquals(NotificationStatus.FAILED, result.status());
        assertFalse(result.sent());
        assertTrue(result.detail().contains("the mail server is down"), result.detail());
    }

    @Test
    void aChannelReturningNothingAlsoBecomesAFailedResult() {
        NotificationChannel silent = new NotificationChannel() {
            @Override
            public ChannelType type() {
                return ChannelType.SMS;
            }

            @Override
            public DispatchResult send(String recipient, String subject, String body) {
                return null;
            }
        };

        assertEquals(NotificationStatus.FAILED,
                silent.dispatch("0771234567", "Hi", "Body").status());
    }

    @Test
    void theGuaranteeIsInTheInterfaceNotInEachChannel() {
        // The interface used to say "never throws" and leave every implementation to honour it.
        // One did, with a try-catch; the other did not, so the promise held by coincidence.
        for (ChannelType type : ChannelType.values()) {
            assertFalse(factory.create(type)
                    .dispatch(null, "Confirmed", "Body").sent(),
                    "a null recipient must not be reported as delivered");
        }
    }

    // --- and cannot claim a delivery it did not make ---------------------

    @Test
    void bothChannelsRecordRatherThanSendAndSaySo() {
        // The defect this move fixed: both returned a boolean true with reason "logged", so a
        // caller reading the boolean recorded that the patient had been told.
        for (ChannelType type : ChannelType.values()) {
            DispatchResult result = factory.create(type)
                    .dispatch("nimal@example.lk", "Confirmed", "Body");

            assertEquals(NotificationStatus.LOGGED, result.status(), type + " records only");
            assertFalse(result.sent(), type + " must not claim a delivery");
            assertEquals("logged", result.reason());
        }
    }

    @Test
    void onlySentCountsAsSent() {
        assertTrue(DispatchResult.sent(ChannelType.EMAIL, "delivered").sent());
        assertFalse(DispatchResult.logged(ChannelType.EMAIL, "recorded").sent());
        assertFalse(DispatchResult.failed(ChannelType.EMAIL, "refused").sent());
    }

    @Test
    void aPatientWithNoAddressIsAFailureAndNotADelivery() {
        // A patient may register without an email or a phone — that is deliberate. Sending to
        // nobody is not a success, and it is not worth retrying either.
        DispatchResult email = factory.create(ChannelType.EMAIL).dispatch("", "Confirmed", "Body");
        DispatchResult sms = factory.create(ChannelType.SMS).dispatch(null, "Confirmed", "Body");

        assertEquals(NotificationStatus.FAILED, email.status());
        assertEquals(NotificationStatus.FAILED, sms.status());
        assertTrue(email.detail().contains("no email address"), email.detail());
        assertTrue(sms.detail().contains("no contact number"), sms.detail());
    }
}
