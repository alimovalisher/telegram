package dev.alimov.telegram.core;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Represents a reactive communication channel that supports publishing messages and subscribing to them.
 * It provides an abstraction for non-blocking message queuing operations.
 *
 * @param <V> the type of messages handled by this channel
 */
public interface ReactiveChannel<V> extends AutoCloseable {


    /**
     * Close the queue and release any underlying resources.
     *
     */
    void close();
}
