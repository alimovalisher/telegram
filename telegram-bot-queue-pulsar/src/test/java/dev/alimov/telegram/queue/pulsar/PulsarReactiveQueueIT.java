package dev.alimov.telegram.queue.pulsar;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pulsar.client.api.PulsarClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PulsarReactiveQueueIT {


    private PulsarClient pulsarClient;
    private ObjectMapper objectMapper;

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TestMessage(String text, int value) {
    }

    @BeforeEach
    void setUp() throws Exception {
        pulsarClient = PulsarClient.builder()
                                   .serviceUrl("pulsar://localhost:6650")
                                   .build();
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (pulsarClient != null) {
            pulsarClient.close();
        }
    }

    @Test
    void publishAndSubscribe_singleMessage() {
        var queue = new PulsarReactiveQueue<>(
                pulsarClient, "persistent://public/default/test-single",
                "test-sub-single", objectMapper, TestMessage.class);

        var message = new TestMessage("hello", 42);

        // Schedule publish after a delay to allow consumer to connect
        Mono.delay(Duration.ofSeconds(1))
            .flatMap(t -> queue.publish(message))
            .subscribe();

        StepVerifier.create(queue.subscribe().take(1))
                    .assertNext(received -> {
                        assertEquals("hello", received.text());
                        assertEquals(42, received.value());
                    })
                    .expectComplete()
                    .verify(Duration.ofSeconds(30));

        queue.close().block();
    }

    @Test
    void publishAndSubscribe_multipleMessages() {
        var queue = new PulsarReactiveQueue<>(
                pulsarClient, "persistent://public/default/test-multi",
                "test-sub-multi", objectMapper, TestMessage.class);

        // Schedule publish after a delay
        Mono.delay(Duration.ofSeconds(1))
            .flatMap(t -> queue.publish(new TestMessage("first", 1))
                               .then(queue.publish(new TestMessage("second", 2)))
                               .then(queue.publish(new TestMessage("third", 3))))
            .subscribe();

        StepVerifier.create(queue.subscribe().take(3))
                    .assertNext(m -> {
                        assertEquals("first", m.text());
                        assertEquals(1, m.value());
                    })
                    .assertNext(m -> {
                        assertEquals("second", m.text());
                        assertEquals(2, m.value());
                    })
                    .assertNext(m -> {
                        assertEquals("third", m.text());
                        assertEquals(3, m.value());
                    })
                    .expectComplete()
                    .verify(Duration.ofSeconds(30));

        queue.close().block();
    }

    @Test
    void publish_multipleTimes_allDelivered() {
        var queue = new PulsarReactiveQueue<>(
                pulsarClient, "persistent://public/default/test-count",
                "test-sub-count", objectMapper, TestMessage.class);

        int count = 10;

        // Schedule publishing after consumer setup
        Mono.delay(Duration.ofSeconds(1))
            .flatMap(t -> {
                Mono<Void> chain = Mono.empty();
                for (int i = 0; i < count; i++) {
                    final int idx = i;
                    chain = chain.then(queue.publish(new TestMessage("msg-" + idx, idx)));
                }
                return chain;
            })
            .subscribe();

        StepVerifier.create(queue.subscribe().take(count))
                    .expectNextCount(count)
                    .expectComplete()
                    .verify(Duration.ofSeconds(30));

        queue.close().block();
    }

    @Test
    void close_releasesResources() {
        var queue = new PulsarReactiveQueue<>(
                pulsarClient, "persistent://public/default/test-close",
                "test-sub-close", objectMapper, TestMessage.class);

        // Trigger producer creation
        queue.publish(new TestMessage("x", 0)).block(Duration.ofSeconds(10));

        // Close should complete without error
        StepVerifier.create(queue.close())
                    .expectComplete()
                    .verify(Duration.ofSeconds(10));
    }
}
