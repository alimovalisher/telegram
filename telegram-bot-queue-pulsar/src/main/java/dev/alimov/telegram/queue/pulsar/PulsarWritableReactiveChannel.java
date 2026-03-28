package dev.alimov.telegram.queue.pulsar;

import dev.alimov.telegram.core.WritableReactiveChannel;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.Producer;
import reactor.core.publisher.Mono;

/**
 * A reactive implementation of {@link WritableReactiveChannel} for publishing messages
 * to an Apache Pulsar topic. This class integrates with the Apache Pulsar's {@link Producer}
 * to enable non-blocking, asynchronous message publishing using Reactor's {@link Mono}.
 *
 * @param <T> the type of the messages to be published to the Pulsar topic
 *
 * The key functionalities provided by this class include:
 * - Publishing messages reactively using the {@link #publish(Object)} method, which returns a {@link Mono}
 *   that completes when the message has been successfully sent and yields the resulting {@link MessageId}.
 * - Graceful resource cleanup via the {@link #close()} method, which can be used to release resources
 *   associated with the underlying Pulsar producer.
 *
 * This class ensures compatibility with reactive programming principles and is designed to handle
 * asynchronous operations efficiently.
 */
public class PulsarWritableReactiveChannel<T> implements WritableReactiveChannel<T, MessageId> {

    private final Producer<T> producer;

    public PulsarWritableReactiveChannel(Producer<T> producer) {
        this.producer = producer;
    }


    @Override
    public Mono<MessageId> publish(T message) {
        return Mono.fromFuture(producer.sendAsync(message));
    }


    @Override
    public void close() {

    }

}
