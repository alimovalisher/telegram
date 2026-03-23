package dev.alimov.telegram.poller;

import dev.alimov.telegram.api.TelegramBotClient;
import dev.alimov.telegram.api.Update;
import dev.alimov.telegram.core.BotResponse;
import dev.alimov.telegram.core.ReactiveChannel;
import dev.alimov.telegram.core.WritableReactiveChannel;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Polls updates from the Telegram Bot API on a scheduled interval and publishes them
 * to an inbound {@link ReactiveChannel}. Also subscribes to an outbound {@link ReactiveChannel}
 * to forward {@link BotResponse} messages back through the Telegram API.
 */
public class TelegramBotUpdatePoller implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotUpdatePoller.class);

    private final TelegramBotClient botClient;
    private final WritableReactiveChannel<Update> inboundQueue;

    private final Duration pollingInterval;
    private final int limit;
    private final int timeout;
    private final List<String> allowedUpdates;
    private final Scheduler scheduler;


    private final AtomicLong lastOffset = new AtomicLong(0);
    private final Disposable.Composite disposables = Disposables.composite();


    public TelegramBotUpdatePoller(
            TelegramBotClient botClient,
            WritableReactiveChannel<Update> inboundQueue,
            Duration pollingInterval,
            int limit,
            int timeout,
            List<String> allowedUpdates,
            Scheduler scheduler
    ) {
        this.botClient = botClient;
        this.inboundQueue = inboundQueue;
        this.pollingInterval = pollingInterval;
        this.limit = limit;
        this.timeout = timeout;
        this.allowedUpdates = allowedUpdates;
        this.scheduler = scheduler;
    }

    /**
     * Start polling for updates and forwarding outbound responses.
     * This method subscribes to both the polling schedule and the outbound queue.
     */
    public void start() {
        Disposable polling = Flux.<Long>generate(sink -> {

                                     sink.next(lastOffset.get());
                                     sink.complete();
                                 })
                                 .flatMap(this::pollUpdates)
                                 .doOnNext(update -> {
                                     long nextOffset = lastOffset.updateAndGet(currentVal -> {
                                         if (update.updateId() >= currentVal) {
                                             return update.updateId() + 1;
                                         }

                                         return currentVal;
                                     });

                                     log.debug("Received update id={}, next offset={}", update.updateId(), nextOffset);
                                 })
                                 .doOnError(e -> log.error("Error during update polling", e))
                                 .retryWhen(Retry.backoff(Integer.MAX_VALUE, pollingInterval))
                                 .repeatWhen(it -> it.delayElements(pollingInterval))
                                 .subscribeOn(scheduler)
                                 .subscribe();

        disposables.add(polling);


        log.info("TelegramUpdatePoller started with interval={}", pollingInterval);
    }

    @VisibleForTesting
    Flux<Update> pollUpdates(long offset) {
        return botClient.getUpdates(offset, limit, timeout, allowedUpdates)
                        .flatMap(update -> inboundQueue.publish(update)
                                                       .doOnSuccess(v -> log.debug("Published update {} to inbound queue", update.updateId()))
                                                       .thenReturn(update))
                        .doOnError(e -> {
                            log.error("Error polling updates at offset={}", offset, e);
                        });
    }


    @Override
    public void close() {
        disposables.dispose();

        log.info("TelegramUpdatePoller stopped");
    }
}
