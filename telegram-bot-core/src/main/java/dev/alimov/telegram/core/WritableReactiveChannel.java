package dev.alimov.telegram.core;

import reactor.core.publisher.Mono;

/**
 * Represents a writable reactive communication channel that supports publishing messages.
 * This interface is designed to be non-blocking and integrates with the reactive programming paradigm,
 * allowing asynchronous operations through Reactor's {@link Mono}.
 *
 * @param <I> the type of messages handled by this channel
 * @param <O> the type of messages handled result by this channel
 */
public interface WritableReactiveChannel<I, O> extends ReactiveChannel {
    /**
     * Publish a message to the channel.
     *
     * @param message the message to publish
     * @return a {@link Mono} that completes when the message has been accepted by the channel
     */
    Mono<O> publish(I message);
}
