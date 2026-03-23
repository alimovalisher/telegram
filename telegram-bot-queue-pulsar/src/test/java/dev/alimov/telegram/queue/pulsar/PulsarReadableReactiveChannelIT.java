package dev.alimov.telegram.queue.pulsar;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.SubscriptionMode;
import org.apache.pulsar.client.api.SubscriptionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.file.AccessMode;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PulsarReadableReactiveChannelIT {


    private static final Logger log = LoggerFactory.getLogger(PulsarReadableReactiveChannelIT.class);
    private PulsarClient pulsarClient;
    private ObjectMapper objectMapper;
    private PulsarReadableReactiveChannel<TestMessage> pulsarReadableReactiveChannel;
    private PulsarWritableReactiveChannel<TestMessage> pulsarWritableReactiveChannel;

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TestMessage(String text, int value) {
    }

    private String topic;

    @BeforeEach
    void setUp() throws Exception {
        topic = "persistent://public/default/in-" + System.currentTimeMillis();
        pulsarClient = PulsarClient.builder()
                                   .serviceUrl("pulsar://localhost:6650")
                                   .build();
        objectMapper = new ObjectMapper();

        pulsarReadableReactiveChannel = new PulsarReadableReactiveChannel<>(
                objectMapper,
                pulsarClient.newConsumer()
                            .topic(topic)
                            .consumerName("test-consumer")
                            .subscriptionName("test-%s".formatted(UUID.randomUUID()))
                            .subscriptionMode(SubscriptionMode.NonDurable)
                            .subscriptionType(SubscriptionType.Shared)
                            .maxAcknowledgmentGroupSize(Integer.MAX_VALUE)
                            .subscribe(),
                TestMessage.class
        );

        pulsarWritableReactiveChannel = new PulsarWritableReactiveChannel<>(
                objectMapper,
                pulsarClient.newProducer()
                            .topic(topic)
                            .enableBatching(false)
                            .producerName("test")
                            .create()
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        pulsarWritableReactiveChannel.close();
        pulsarReadableReactiveChannel.close();

        if (pulsarClient != null) {
            pulsarClient.close();
        }

    }

    @Test
    void publishAndSubscribe_singleMessage() throws PulsarClientException, InterruptedException {
        TestMessage message = new TestMessage("hello", 42);

        StepVerifier.create(
                            Mono.delay(Duration.ofSeconds(1))
                                .flatMap(t -> pulsarWritableReactiveChannel.publish(message)))
                    .verifyComplete();

        StepVerifier.create(pulsarReadableReactiveChannel.subscribe()
                                                         .take(1))
                    .assertNext(received -> {
                        log.info("Received message: {}", received);
                        assertEquals("hello", received.text());
                        assertEquals(42, received.value());
                    })
                    .expectComplete()
                    .verify(Duration.ofSeconds(30));

        Thread.sleep(1000); // to ack msg id
    }

    @Test
    void publishAndSubscribe_multipleMessages() {

        // Schedule publish after a delay
        StepVerifier.create(pulsarWritableReactiveChannel.publish(new TestMessage("first", 1)))
                    .verifyComplete();

        StepVerifier.create(pulsarWritableReactiveChannel.publish(new TestMessage("second", 2)))
                    .verifyComplete();

        StepVerifier.create(pulsarWritableReactiveChannel.publish(new TestMessage("third", 3)))
                    .verifyComplete();

        StepVerifier.create(pulsarWritableReactiveChannel.publish(new TestMessage("four", 3)))
                    .verifyComplete();

        StepVerifier.create(pulsarReadableReactiveChannel.subscribe().take(3))
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

    }

    @Test
    void publish_multipleTimes_allDelivered() {

        int count = 10;

        for (int i = 0; i < count; i++) {
            // Schedule publishing after consumer setup
            int finalI = i;
            StepVerifier.create(
                                Mono.delay(Duration.ofSeconds(1))
                                    .flatMap(t -> {
                                        return pulsarWritableReactiveChannel.publish(new TestMessage("msg-" + finalI, finalI));

                                    })
                        )
                        .verifyComplete();
        }


        StepVerifier.create(pulsarReadableReactiveChannel.subscribe()
                                                         .take(count))
                    .expectNextCount(count)
                    .expectComplete()
                    .verify(Duration.ofSeconds(30));
    }
}
