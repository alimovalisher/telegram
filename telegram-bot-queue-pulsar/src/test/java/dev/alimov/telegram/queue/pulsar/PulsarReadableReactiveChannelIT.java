package dev.alimov.telegram.queue.pulsar;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pulsar.client.api.*;
import org.apache.pulsar.common.schema.KeyValue;
import org.apache.pulsar.reactive.client.adapter.AdaptedReactivePulsarClientFactory;
import org.apache.pulsar.reactive.client.api.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.AccessMode;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PulsarReadableReactiveChannelIT {


    private static final Logger log = LoggerFactory.getLogger(PulsarReadableReactiveChannelIT.class);
    private PulsarClient pulsarClient;
    private PulsarReadableReactiveChannel<String> pulsarReadableReactiveChannel;
    private PulsarWritableReactiveChannel<String> pulsarWritableReactiveChannel;
    private ReactivePulsarClient reactivePulsarClient;

    private String topic;

    @BeforeEach
    void setUp() throws Exception {
        topic = "persistent://public/default/in-" + System.currentTimeMillis();
        pulsarClient = PulsarClient.builder()
                                   .serviceUrl("pulsar://localhost:6650")
                                   .build();

        reactivePulsarClient = AdaptedReactivePulsarClientFactory.create(pulsarClient);

        pulsarReadableReactiveChannel = new PulsarReadableReactiveChannel<String>(
                reactivePulsarClient.messageReader(Schema.STRING)
                                    .topic(topic)
                                    .endOfStreamAction(EndOfStreamAction.POLL)
                                    .startAtSpec(StartAtSpec.ofEarliest())
                                    .readerName("test")
                                    .subscriptionName("test-%s".formatted(UUID.randomUUID()))
                                    .build()
        );

        pulsarWritableReactiveChannel = new PulsarWritableReactiveChannel<String>(
                reactivePulsarClient.messageSender(Schema.STRING)
                                    .topic(topic)
                                    .producerName("test")
                                    .build()
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

        StepVerifier.create(
                            pulsarWritableReactiveChannel.publish(
                                                                 MessageSpec.builder("hello")
                                                                            .key("key-1")
                                                                            .build()
                                                         )
                                                         .then()
                    )
                    .verifyComplete();

        StepVerifier.create(pulsarReadableReactiveChannel.subscribe()
                                                         .take(1))
                    .assertNext(received -> {
                        log.info("Received message: {}", received);
                        assertEquals("hello", received.getValue());
                        assertEquals("key-1", received.getKey());
                    })
                    .expectComplete()
                    .verify(Duration.ofSeconds(30));

        Thread.sleep(1000); // to ack msg id
    }

    @Test
    void publishAndSubscribe_multipleMessages() {

        for (int i = 0; i < 4; i++) {
            StepVerifier.create(
                                pulsarWritableReactiveChannel.publish(
                                        MessageSpec.builder("hello-%d".formatted(i))
                                                   .key("key-1")
                                                   .build()
                                )
                        )
                        .expectNextCount(1)
                        .verifyComplete();
        }


        StepVerifier.create(pulsarReadableReactiveChannel.subscribe().take(4))
                    .assertNext(received -> {
                        assertEquals("hello-0", received.getValue());
                        assertEquals("key-1", received.getKey());
                    })
                    .assertNext(received -> {
                        assertEquals("hello-1", received.getValue());
                        assertEquals("key-1", received.getKey());
                    })
                    .assertNext(received -> {
                        assertEquals("hello-2", received.getValue());
                        assertEquals("key-1", received.getKey());
                    })
                    .assertNext(received -> {
                        assertEquals("hello-3", received.getValue());
                        assertEquals("key-1", received.getKey());
                    })
                    .expectComplete()
                    .verify(Duration.ofSeconds(30));

    }

    @Test
    void publish_multipleTimes_allDelivered() {

        int count = 10;

        for (int i = 0; i < count; i++) {
            // Schedule publishing after consumer setup
            StepVerifier.create(
                                pulsarWritableReactiveChannel.publish(MessageSpec.builder("msg-%d".formatted(i))
                                                                                 .key("key-1")
                                                                                 .build())

                        )
                        .expectNextCount(1)
                        .verifyComplete();
        }


        StepVerifier.create(pulsarReadableReactiveChannel.subscribe()
                                                         .take(count))
                    .expectNextCount(count)
                    .expectComplete()
                    .verify(Duration.ofSeconds(30));
    }
}
