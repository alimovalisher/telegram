package dev.alimov.telegram.queue.pulsar;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.alimov.telegram.core.ReactiveChannel;
import dev.alimov.telegram.core.WritableReactiveChannel;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.reactive.client.api.MessageSpec;
import org.apache.pulsar.reactive.client.api.ReactiveMessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;

/**
 * Apache Pulsar implementation of {@link WritableReactiveChannel}, enabling reactive message publishing.
 * This class uses a non-blocking, reactive programming paradigm to publish messages to an Apache Pulsar topic
 * via {@link ReactiveMessageSender}. It integrates seamlessly with Reactor's {@link Mono} for asynchronous
 * operations and does not block the calling thread.
 * <p>
 * This implementation is useful for scenarios requiring efficient message publishing to Pulsar, ensuring
 * reactivity and backpressure compliance. It supports publishing arbitrary binary payloads byte arrays encapsulated
 * in {@link MessageSpec}, and returns a {@link MessageId} upon successful publishing.
 */
public class PulsarWritableReactiveChannel<T> implements WritableReactiveChannel<MessageSpec<T>, MessageId> {

    private static final Logger log = LoggerFactory.getLogger(PulsarWritableReactiveChannel.class);

    private final ReactiveMessageSender<T> producer;

    public PulsarWritableReactiveChannel(ReactiveMessageSender<T> producer) {
        this.producer = producer;
    }


    @Override
    public Mono<MessageId> publish(MessageSpec<T> message) {
        return producer.sendOne(message);
    }


    @Override
    public void close() {

    }

}
