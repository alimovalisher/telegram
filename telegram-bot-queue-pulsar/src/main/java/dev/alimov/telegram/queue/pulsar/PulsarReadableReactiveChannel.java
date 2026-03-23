package dev.alimov.telegram.queue.pulsar;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.alimov.telegram.core.ReadableReactiveChannel;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClientException;
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
 * @param <T> the type of messages handled by this channel
 */
public class PulsarReadableReactiveChannel<T> implements ReadableReactiveChannel<T> {

    private static final Logger log = LoggerFactory.getLogger(PulsarReadableReactiveChannel.class);

    private final ObjectMapper objectMapper;

    private final Consumer<byte[]> consumer;

    private final Class<T> messageType;

    public PulsarReadableReactiveChannel(
            ObjectMapper objectMapper,
            Consumer<byte[]> consumer,
            Class<T> messageType
    ) {
        this.objectMapper = objectMapper;
        this.consumer = consumer;
        this.messageType = messageType;
    }


    @Override
    public Flux<T> subscribe() {
        return Flux.<T>create(sink -> {
            AtomicBoolean inFlight = new AtomicBoolean(false);

            sink.onRequest(requested -> {
                log.debug("requested items count: {}", requested);
                fetchNext(sink, inFlight);
            });

            sink.onDispose(() -> {

            });
        }, FluxSink.OverflowStrategy.ERROR);
    }

    private void fetchNext(FluxSink<T> sink, AtomicBoolean inFlight) {
        if (sink.isCancelled() || !inFlight.compareAndSet(false, true)) {
            log.debug("sink has been cancelled or can't set inFlight. sink: {} inFlight: {}", sink.isCancelled(), inFlight);
            return;
        }


        if (sink.requestedFromDownstream() <= 0) { // no more elements
            inFlight.set(false);
            return;
        }

        consumer.receiveAsync()
                .whenComplete((msg, error) -> {
                    if (error != null) {
                        sink.error(error);
                        return;
                    }


                    try {
                        T t = deserializeMessage(msg.getData());

                        sink.next(t);

                        consumer.acknowledge(msg);
                    } catch (IOException e) {
                        consumer.negativeAcknowledge(msg);
                        sink.error(e);
                        log.error("Error while reading message", e);
                    }

                    inFlight.set(false);

                    if (consumer.hasReachedEndOfTopic()) {
                        sink.complete(); // source exhausted
                        return;
                    }


                    // Keep pulling if downstream still has demand
                    if (sink.requestedFromDownstream() > 0 && !sink.isCancelled()) {
                        fetchNext(sink, inFlight);
                    }
                });
    }

    private void receiveNext(FluxSink<T> sink) {
        if (sink.isCancelled()) {
            return;
        }

        CompletableFuture<Message<byte[]>> messageCompletableFuture = consumer.receiveAsync()
                                                                              .whenComplete((msg, ex) -> {
                                                                                  if (ex != null) {
                                                                                      log.error("Error while reading message", ex);
                                                                                      sink.error(ex);
                                                                                      return;
                                                                                  }

                                                                                  if (msg != null) {
                                                                                      try {
                                                                                          T message = deserializeMessage(msg.getData());
                                                                                          sink.next(message);
                                                                                          consumer.acknowledgeAsync(msg)
                                                                                                  .whenComplete((v, ackEx) -> {
                                                                                                      if (ackEx != null) {
                                                                                                          log.error("Error acknowledging message", ackEx);
                                                                                                      }
                                                                                                      receiveNext(sink);
                                                                                                  });
                                                                                      } catch (IOException e) {
                                                                                          log.error("Error while reading message", e);
                                                                                          sink.error(e);
                                                                                      }
                                                                                  } else {
                                                                                      // Should not happen with receiveAsync() unless consumer is closed
                                                                                      receiveNext(sink);
                                                                                  }
                                                                              });

        sink.onRequest(val -> {
            log.info("Received a message from Pulsar: {}", val);


        });

        sink.onDispose(() -> {
            log.info("Disposed pulsar channel");
            messageCompletableFuture.cancel(true);
        });
    }

    private T deserializeMessage(byte[] data) throws IOException {
        T message = objectMapper.readValue(data, messageType);
        return message;
    }


    @Override
    public void close() {
        try {
            consumer.close();
        } catch (IOException e) {
            log.error("Error closing consumer", e);
        }
    }

}
