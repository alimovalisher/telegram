package dev.alimov.telegram.queue.pulsar;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.alimov.telegram.core.ReactiveQueue;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.api.SubscriptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Apache Pulsar implementation of {@link ReactiveQueue}.
 * <p>
 * Uses Pulsar's async API wrapped in Reactor types for fully non-blocking queue operations.
 * Messages are serialized to JSON via Jackson for transport.
 *
 * @param <T> the type of messages transported by this queue
 */
public class PulsarReactiveQueue<T> implements ReactiveQueue<T> {

    private static final Logger log = LoggerFactory.getLogger(PulsarReactiveQueue.class);

    private final PulsarClient pulsarClient;
    private final String topic;
    private final String subscriptionName;
    private final ObjectMapper objectMapper;
    private final Class<T> messageType;

    private volatile Producer<byte[]> producer;
    private volatile Consumer<byte[]> consumer;

    public PulsarReactiveQueue(
            PulsarClient pulsarClient,
            String topic,
            String subscriptionName,
            ObjectMapper objectMapper,
            Class<T> messageType
    ) {
        this.pulsarClient = pulsarClient;
        this.topic = topic;
        this.subscriptionName = subscriptionName;
        this.objectMapper = objectMapper;
        this.messageType = messageType;
    }

    @Override
    public Mono<Void> publish(T message) {
        return ensureProducer()
                .flatMap(p -> {
                    try {
                        byte[] data = objectMapper.writeValueAsBytes(message);
                        return Mono.fromCompletionStage(p.sendAsync(data));
                    } catch (JsonProcessingException e) {
                        return Mono.<Void>error(e);
                    }
                })
                .then();
    }

    @Override
    public Flux<T> subscribe() {
        return Flux.<T>create(sink -> {
            try {
                consumer = pulsarClient.newConsumer(Schema.BYTES)
                        .topic(topic)
                        .subscriptionName(subscriptionName)
                        .subscriptionType(SubscriptionType.Shared)
                        .messageListener((c, msg) -> {
                            try {
                                T value = objectMapper.readValue(msg.getData(), messageType);
                                c.acknowledgeAsync(msg);
                                sink.next(value);
                            } catch (Exception e) {
                                c.negativeAcknowledge(msg);
                                log.error("Failed to deserialize message from topic={}", topic, e);
                            }
                        })
                        .subscribe();

                log.info("Subscribed to Pulsar topic={} subscription={}", topic, subscriptionName);

                sink.onDispose(() -> {
                    try {
                        if (consumer != null) {
                            consumer.close();
                        }
                    } catch (PulsarClientException e) {
                        log.error("Error closing Pulsar consumer for topic={}", topic, e);
                    }
                });
            } catch (PulsarClientException e) {
                sink.error(e);
            }
        });
    }

    @Override
    public Mono<Void> close() {
        return Mono.defer(() -> {
            Mono<Void> closeProducer = producer != null
                    ? Mono.fromCompletionStage(producer.closeAsync())
                    : Mono.empty();
            Mono<Void> closeConsumer = consumer != null
                    ? Mono.fromCompletionStage(consumer.closeAsync())
                    : Mono.empty();
            return Mono.when(closeProducer, closeConsumer)
                    .doOnSuccess(v -> log.info("Closed PulsarReactiveQueue for topic={}", topic));
        });
    }

    private Mono<Producer<byte[]>> ensureProducer() {
        if (producer != null) {
            return Mono.just(producer);
        }
        return Mono.<Producer<byte[]>>fromCallable(() -> {
            synchronized (this) {
                if (producer == null) {
                    producer = pulsarClient.newProducer(Schema.BYTES)
                            .topic(topic)
                            .create();
                    log.info("Created Pulsar producer for topic={}", topic);
                }
                return producer;
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
