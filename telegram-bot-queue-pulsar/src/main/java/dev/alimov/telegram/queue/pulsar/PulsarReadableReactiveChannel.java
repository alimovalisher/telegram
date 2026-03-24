package dev.alimov.telegram.queue.pulsar;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.alimov.telegram.core.ReadableReactiveChannel;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.reactive.client.api.ReactiveMessageReader;
import org.apache.pulsar.reactive.client.api.ReactivePulsarClient;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Apache Pulsar implementation of {@link ReadableReactiveChannel}.
 * <p>
 * This class provides reactive message consumption via Pulsar's consumer interface,
 * allowing subscribers to asynchronously receive messages in a non-blocking manner.
 * The messages are deserialized from JSON format using Jackson.
 * Reactor's {@link Flux} is used to represent a stream of messages emitted to subscribers.
 *
 */
public class PulsarReadableReactiveChannel<T> implements ReadableReactiveChannel<Message<T>> {

    private final ReactiveMessageReader<T> consumer;

    public PulsarReadableReactiveChannel(ReactiveMessageReader<T> consumer) {
        this.consumer = consumer;
    }


    @Override
    public Flux<Message<T>> subscribe() {
        return consumer.readMany();
    }

    @Override
    public void close() {

    }
}
