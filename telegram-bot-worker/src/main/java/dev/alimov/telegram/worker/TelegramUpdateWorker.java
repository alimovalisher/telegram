package dev.alimov.telegram.worker;

import dev.alimov.telegram.api.Update;
import dev.alimov.telegram.core.BotResponse;
import dev.alimov.telegram.core.ReactiveQueue;
import dev.alimov.telegram.core.UpdateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Subscribes to the inbound {@link ReactiveQueue} of Telegram updates, processes each
 * update through an {@link UpdateHandler}, and publishes resulting {@link BotResponse}
 * messages to the outbound queue.
 */
public class TelegramUpdateWorker implements Disposable {

    private static final Logger log = LoggerFactory.getLogger(TelegramUpdateWorker.class);

    private final ReactiveQueue<Update> inboundQueue;
    private final ReactiveQueue<BotResponse> outboundQueue;
    private final UpdateHandler updateHandler;
    private final Scheduler scheduler;
    private final int concurrency;

    private volatile Disposable disposable;

    public TelegramUpdateWorker(
            ReactiveQueue<Update> inboundQueue,
            ReactiveQueue<BotResponse> outboundQueue,
            UpdateHandler updateHandler
    ) {
        this(inboundQueue, outboundQueue, updateHandler, Schedulers.boundedElastic(), 256);
    }

    public TelegramUpdateWorker(
            ReactiveQueue<Update> inboundQueue,
            ReactiveQueue<BotResponse> outboundQueue,
            UpdateHandler updateHandler,
            Scheduler scheduler,
            int concurrency
    ) {
        this.inboundQueue = inboundQueue;
        this.outboundQueue = outboundQueue;
        this.updateHandler = updateHandler;
        this.scheduler = scheduler;
        this.concurrency = concurrency;
    }

    /**
     * Start consuming updates from the inbound queue and processing them.
     */
    public void start() {
        disposable = inboundQueue.subscribe()
                                 .publishOn(scheduler)
                                 .flatMap(this::processUpdate, concurrency)
                                 .doOnError(e -> log.error("Error in worker pipeline", e))
                                 .retry()
                                 .subscribe();

        log.info("TelegramUpdateWorker started with concurrency={}", concurrency);
    }

    Flux<BotResponse> processUpdate(Update update) {
        log.debug("Processing update id={}", update.updateId());
        return updateHandler.handle(update)
                            .flatMap(response -> outboundQueue.publish(response)
                                                              .doOnSuccess(v -> log.debug("Published response for update id={}", update.updateId()))
                                                              .thenReturn(response))
                            .onErrorResume(e -> {
                                log.error("Error processing update id={}", update.updateId(), e);
                                return Flux.empty();
                            });
    }

    @Override
    public void dispose() {
        if (disposable != null) {
            disposable.dispose();
        }
        log.info("TelegramUpdateWorker stopped");
    }

    @Override
    public boolean isDisposed() {
        return disposable != null && disposable.isDisposed();
    }
}
