package dev.alimov.telegram.poller;

import dev.alimov.telegram.api.Update;
import dev.alimov.telegram.core.ReadableReactiveChannel;
import dev.alimov.telegram.core.WritableReactiveChannel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Simple in-memory queue — no Mockito needed
 */
class TestChannel implements ReadableReactiveChannel<Update>, WritableReactiveChannel<Update, Update> {
    private final List<Update> published = new CopyOnWriteArrayList<>();
    private final reactor.core.publisher.Sinks.Many<Update> sink =
            reactor.core.publisher.Sinks.many().multicast().onBackpressureBuffer();

    @Override
    public Mono<Update> publish(Update message) {
        published.add(message);
        sink.tryEmitNext(message);
        return Mono.empty();
    }

    @Override
    public Flux<Update> subscribe() {
        return sink.asFlux();
    }

    @Override
    public void close() {

    }

    public List<Update> getPublished() {
        return published;
    }
}
