package dev.alimov.telegram.core;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive queue abstraction for asynchronous message passing between components.
 *
 * @param <T> the type of messages transported by this queue
 */
public interface ReactiveQueue<T> {

    /**
     * Publish a message to the queue.
     *
     * @param message the message to publish
     * @return a {@link Mono} that completes when the message has been accepted by the queue
     */
    Mono<Void> publish(T message);

    /**
     * Subscribe to messages from the queue.
     *
     * @return a {@link Flux} that emits messages as they become available
     */
    Flux<T> subscribe();

    /**
     * Close the queue and release any underlying resources.
     *
     * @return a {@link Mono} that completes when the queue has been closed
     */
    Mono<Void> close();
}
