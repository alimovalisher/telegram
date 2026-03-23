package dev.alimov.telegram.queue.pulsar;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.alimov.telegram.core.ReactiveChannel;
import dev.alimov.telegram.core.WritableReactiveChannel;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;

/**
 * Apache Pulsar implementation of {@link WritableReactiveChannel}.
 * <p>
 * This class provides reactive message publishing via Pulsar's producer interface,
 * allowing messages to be sent asynchronously in a non-blocking manner. The messages
 * are serialized to JSON format using Jackson before being transmitted to the Pulsar
 * cluster.
 * <p>
 * The class leverages Reactor's {@link Mono} for representing the asynchronous
 * completion of a message-publishing operation.
 *
 * @param <T> the type of messages handled by this channel
 */
public class PulsarWritableReactiveChannel<T> implements WritableReactiveChannel<T> {

    private static final Logger log = LoggerFactory.getLogger(PulsarWritableReactiveChannel.class);

    private final ObjectMapper objectMapper;

    private final Producer<byte[]> producer;

    public PulsarWritableReactiveChannel(
            ObjectMapper objectMapper,
            Producer<byte[]> producer
    ) {
        this.objectMapper = objectMapper;
        this.producer = producer;
    }


    @Override
    public Mono<Void> publish(T message) {
        try {
            byte[] data = objectMapper.writeValueAsBytes(message);
            producer.send(data);

            return Mono.empty();
        } catch (Exception e) {
            return Mono.<Void>error(e);
        }
    }


    @Override
    public void close() {

    }

}
