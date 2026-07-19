package com.sunrise.clinic.pattern.factory;

import com.sunrise.clinic.domain.ChannelType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * FACTORY METHOD pattern — hands out the right {@link NotificationChannel} for
 * a requested {@link ChannelType}. Spring injects every channel bean, which the
 * factory indexes by type; adding a new channel (e.g. WhatsApp) needs no change
 * here or in any caller.
 */
@Component
public class NotificationChannelFactory {

    private final Map<ChannelType, NotificationChannel> channels = new EnumMap<>(ChannelType.class);

    public NotificationChannelFactory(List<NotificationChannel> availableChannels) {
        for (NotificationChannel channel : availableChannels) {
            channels.put(channel.type(), channel);
        }
    }

    /**
     * @param type the desired channel
     * @return the channel implementation
     * @throws IllegalArgumentException if no channel is registered for the type
     */
    public NotificationChannel create(ChannelType type) {
        NotificationChannel channel = channels.get(type);
        if (channel == null) {
            throw new IllegalArgumentException("No notification channel registered for " + type);
        }
        return channel;
    }
}
