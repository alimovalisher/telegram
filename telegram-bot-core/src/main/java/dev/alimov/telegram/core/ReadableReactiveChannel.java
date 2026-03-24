package dev.alimov.telegram.core;

import reactor.core.publisher.Flux;

/**
 * Represents a readable reactive communication channel capable of emitting messages
 * asynchronously to subscribers. This interface extends {@link ReactiveChannel},
 * allowing components to asynchronously consume messages in a reactive manner.
 *
 * @param <V> the type of messages transported by this channel
 */
public interface ReadableReactiveChannel<V> extends ReactiveChannel {
    /**
     * Subscribe to messages from the channel.
     *
     * @return a {@link Flux} that emits messages as they become available
     */
    Flux<V> subscribe();
}
