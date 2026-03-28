package dev.alimov.telegram.queue.pulsar;

import org.apache.pulsar.client.api.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PulsarReadableReactiveChannelIT {


    private static final Logger log = LoggerFactory.getLogger(PulsarReadableReactiveChannelIT.class);
    private PulsarClient pulsarClient;
    private PulsarReadableReactiveChannel<String> pulsarReadableReactiveChannel;
    private PulsarWritableReactiveChannel<String> pulsarWritableReactiveChannel;

    private String topic;

    @BeforeEach
    void setUp() throws Exception {
        topic = "persistent://public/default/in-" + System.currentTimeMillis();
        pulsarClient = PulsarClient.builder()
                                   .serviceUrl("pulsar://localhost:6650")
                                   .build();

        pulsarReadableReactiveChannel = new PulsarReadableReactiveChannel<String>(
                createConsumer("test-%s".formatted(UUID.randomUUID()))
        );

        pulsarWritableReactiveChannel = new PulsarWritableReactiveChannel<String>(
                pulsarClient.newProducer(Schema.STRING)
                            .topic(topic)
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

        StepVerifier.create(
                            pulsarWritableReactiveChannel.publish(
                                                                 "hello"
                                                         )
                                                         .then()
                    )
                    .verifyComplete();

        StepVerifier.create(pulsarReadableReactiveChannel.subscribe()
                                                         .take(1))
                    .assertNext(received -> {
                        log.info("Received message: {}", received);
                        assertEquals("hello", received.getValue());
                    })
                    .expectComplete()
                    .verify(Duration.ofSeconds(30));

        Thread.sleep(1000); // to ack msg id
    }

    @Test
    void publishAndSubscribe_multipleMessages() {

        for (int i = 0; i < 4; i++) {
            StepVerifier.create(
                                pulsarWritableReactiveChannel.publish("hello-%d".formatted(i))
                        )
                        .expectNextCount(1)
                        .verifyComplete();
        }


        StepVerifier.create(pulsarReadableReactiveChannel.subscribe()
                                                         .flatMap(msg -> {
                                                             return pulsarReadableReactiveChannel.acknowledge(msg)
                                                                                                 .thenReturn(msg.getValue());
                                                         })
                                                         .take(4)
                    )
                    .expectNext("hello-0")
                    .expectNext("hello-1")
                    .expectNext("hello-2")
                    .expectNext("hello-3")
                    .expectComplete()
                    .verify(Duration.ofSeconds(10));

    }

    @Test
    void endOfStream() {

        int count = 10;

        for (int i = 0; i < count; i++) {
            // Schedule publishing after consumer setup
            StepVerifier.create(
                                pulsarWritableReactiveChannel.publish("msg-%d".formatted(i))

                        )
                        .expectNextCount(1)
                        .verifyComplete();
        }


        StepVerifier.create(pulsarReadableReactiveChannel.subscribe()
                                                         .flatMap(msg -> {
                                                             return pulsarReadableReactiveChannel.acknowledge(msg)
                                                                                                 .thenReturn(msg.getValue());
                                                         }))
                    .expectNextCount(10)
                    .expectComplete()
                    .verify(Duration.ofSeconds(30));
    }


    private Consumer<String> createConsumer(String subscriptionName) throws PulsarClientException {
        return pulsarClient.newConsumer(Schema.STRING)
                           .topic(topic)
                           .subscriptionName(subscriptionName)
                           .consumerName("test")
                           .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
                           .subscriptionMode(SubscriptionMode.Durable)
                           .subscriptionType(SubscriptionType.Shared)
                           .receiverQueueSize(1)
                           .subscribe();
    }
}
