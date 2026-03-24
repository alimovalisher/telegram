package dev.alimov.telegram.poller;

import dev.alimov.telegram.api.TelegramBotClient;
import dev.alimov.telegram.api.Update;
import dev.alimov.telegram.core.BotResponse;
import dev.alimov.telegram.core.ReactiveChannel;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Polls updates from the Telegram Bot API on a scheduled interval and publishes them
 * to an inbound {@link ReactiveChannel}. Also subscribes to an outbound {@link ReactiveChannel}
 * to forward {@link BotResponse} messages back through the Telegram API.
 */
public class TelegramBotUpdatePoller implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotUpdatePoller.class);

    private final TelegramBotClient botClient;

    private final int limit;
    private final int timeout;
    private final List<String> allowedUpdates;


    private final AtomicLong lastOffset = new AtomicLong(0);
    private final AtomicBoolean inFlight = new AtomicBoolean(false);
    private final AtomicReference<Disposable> subscription = new AtomicReference<>();

    public TelegramBotUpdatePoller(
            TelegramBotClient botClient,
            int limit,
            int timeout,
            List<String> allowedUpdates
    ) {
        this.botClient = botClient;
        this.limit = limit;
        this.timeout = timeout;
        this.allowedUpdates = allowedUpdates;
    }

    /**
     * Start polling for updates and forwarding outbound responses.
     * This method subscribes to both the polling schedule and the outbound queue.
     */
    public Flux<Update> subscribe() {
        return Flux.<Update>create(sink -> {

                       sink.onRequest(requested -> {
                           Disposable disposable = pollUpdates(sink, lastOffset.get()).doOnNext(sink::next)
                                                                                      .collectList()
                                                                                      .expand(list -> {
                                                                                          if (list.size() >= limit && sink.requestedFromDownstream() > 0 && !sink.isCancelled()) {
                                                                                              return pollUpdates(sink, lastOffset.get()).collectList();
                                                                                          }

                                                                                          sink.complete();

                                                                                          return Flux.empty();
                                                                                      })
                                                                                      .doOnError(e -> {
                                                                                          sink.error(e);
                                                                                      })
                                                                                      .subscribeOn(Schedulers.boundedElastic())
                                                                                      .subscribe();


                           sink.onDispose(disposable);
                           sink.onCancel(disposable);
                       });

                   })
                   .doOnError(e -> log.error("Error during update polling", e));


    }

    @VisibleForTesting
    Flux<Update> pollUpdates(FluxSink<Update> sink, long offset) {
        if (sink.isCancelled() || !inFlight.compareAndSet(false, true)) {
            log.debug("sink has been cancelled or can't set inFlight. sink: {} inFlight: {}", sink.isCancelled(), inFlight);
            return Flux.empty();
        }


        if (sink.requestedFromDownstream() <= 0) { // no more elements
            inFlight.set(false);
            return Flux.empty();
        }

        return botClient.getUpdates(offset, limit, timeout, allowedUpdates)
                        .onErrorResume(WebClientRequestException.class, e -> {
                            if (e.getCause() instanceof io.netty.handler.timeout.ReadTimeoutException) {
                                return Flux.empty();
                            }

                            return Flux.error(e);
                        })
                        .doOnNext(update -> {
                            long nextOffset = lastOffset.updateAndGet(currentVal -> {
                                if (update.updateId() >= currentVal) {
                                    return update.updateId() + 1;
                                }

                                return currentVal;
                            });

                            log.debug("Received update id={}, next offset={}", update.updateId(), nextOffset);

                        })
                        .doOnError(e -> {
                            log.error("Error polling updates at offset={}", offset, e);
                        });
    }


    @Override
    public void close() {
        inFlight.set(false);
        subscription.getAndUpdate(disposable -> {
            if (disposable != null && !disposable.isDisposed()) {
                disposable.dispose();
            }

            return null;
        });
        log.info("TelegramUpdatePoller stopped");
    }
}
