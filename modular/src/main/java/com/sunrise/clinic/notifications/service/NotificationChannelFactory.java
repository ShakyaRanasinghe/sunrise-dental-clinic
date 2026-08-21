package com.sunrise.clinic.notifications.service;

import com.sunrise.clinic.notifications.domain.ChannelType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Factory Method — hands out the channel for a requested {@link ChannelType} — FR-NOT-05.
 *
 * <p>{@code AppContext} supplies every channel and this indexes them by type. Adding a third
 * means writing one implementation and passing it in: no change here, and none in anything that
 * sends. That is the whole return on the pattern, and it is why the alternative — a
 * {@code switch} on channel type wherever something is sent — would have to be edited in every
 * one of those places instead.</p>
 */
public class NotificationChannelFactory {

    private final Map<ChannelType, NotificationChannel> channels = new EnumMap<>(ChannelType.class);

    /**
     * @param availableChannels every channel this deployment has. Later entries win, so a real
     *                          transport can be handed in after the recording one and take over
     *                          without the list having to be filtered
     */
    public NotificationChannelFactory(List<NotificationChannel> availableChannels) {
        for (NotificationChannel channel : availableChannels) {
            channels.put(channel.type(), channel);
        }
    }

    /**
     * @throws IllegalArgumentException if nothing is registered for {@code type}
     *
     * <p>Throwing is right here and not a contradiction of the module's never-throw rule. A
     * missing channel is a wiring mistake, discovered on the first attempt to use it and fixed
     * in {@code AppContext} — not a delivery failure, which is what {@link DispatchResult}
     * exists to carry. See {@link #find} for the caller that would rather not care.</p>
     */
    public NotificationChannel create(ChannelType type) {
        return find(type).orElseThrow(() -> new IllegalArgumentException(
                "No notification channel is registered for " + type));
    }

    /**
     * The channel, if this deployment has one.
     *
     * <p>For a caller sending on a best-effort basis: a clinic with no SMS gateway configured
     * should not have its reminders fail, it should simply not send them by SMS.</p>
     */
    public Optional<NotificationChannel> find(ChannelType type) {
        return Optional.ofNullable(channels.get(type));
    }

    /** @return the types this deployment can actually use. */
    public java.util.Set<ChannelType> available() {
        return java.util.Collections.unmodifiableSet(channels.keySet());
    }
}
