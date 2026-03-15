package dev.alimov.telegram.poller;

import dev.alimov.telegram.api.TelegramBotClient;
import dev.alimov.telegram.api.Update;
import dev.alimov.telegram.core.BotResponse;
import dev.alimov.telegram.core.ReactiveQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Polls updates from the Telegram Bot API on a scheduled interval and publishes them
 * to an inbound {@link ReactiveQueue}. Also subscribes to an outbound {@link ReactiveQueue}
 * to forward {@link BotResponse} messages back through the Telegram API.
 */
public class TelegramUpdatePoller implements Disposable {

    private static final Logger log = LoggerFactory.getLogger(TelegramUpdatePoller.class);

    private final TelegramBotClient botClient;
    private final ReactiveQueue<Update> inboundQueue;
    private final ReactiveQueue<BotResponse> outboundQueue;
    private final Duration pollingInterval;
    private final int limit;
    private final int timeout;
    private final List<String> allowedUpdates;
    private final Scheduler scheduler;

    private final AtomicInteger lastOffset = new AtomicInteger(0);
    private final Disposable.Composite disposables = Disposables.composite();

    public TelegramUpdatePoller(
            TelegramBotClient botClient,
            ReactiveQueue<Update> inboundQueue,
            ReactiveQueue<BotResponse> outboundQueue,
            Duration pollingInterval,
            int limit,
            int timeout,
            List<String> allowedUpdates
    ) {
        this(botClient, inboundQueue, outboundQueue, pollingInterval, limit, timeout, allowedUpdates,
             Schedulers.boundedElastic());
    }

    public TelegramUpdatePoller(
            TelegramBotClient botClient,
            ReactiveQueue<Update> inboundQueue,
            ReactiveQueue<BotResponse> outboundQueue,
            Duration pollingInterval,
            int limit,
            int timeout,
            List<String> allowedUpdates,
            Scheduler scheduler
    ) {
        this.botClient = botClient;
        this.inboundQueue = inboundQueue;
        this.outboundQueue = outboundQueue;
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
        Disposable polling = Flux.interval(pollingInterval, scheduler)
                                 .onBackpressureDrop()
                                 .concatMap(tick -> pollUpdates())
                                 .doOnError(e -> log.error("Error during update polling", e))
                                 .retry()
                                 .subscribe();
        disposables.add(polling);

        Disposable outbound = outboundQueue.subscribe()
                                           .flatMap(this::dispatch)
                                           .doOnError(e -> log.error("Error sending outbound response", e))
                                           .retry()
                                           .subscribe();
        disposables.add(outbound);

        log.info("TelegramUpdatePoller started with interval={}", pollingInterval);
    }

    Flux<Update> pollUpdates() {
        Integer offset = lastOffset.get() == 0 ? null : lastOffset.get();
        return botClient.getUpdates(offset, limit, timeout, allowedUpdates)
                        .doOnNext(update -> {
                            int newOffset = update.updateId().intValue() + 1;
                            lastOffset.set(newOffset);
                            log.debug("Received update id={}, next offset={}", update.updateId(), newOffset);
                        })
                        .flatMap(update -> inboundQueue.publish(update)
                                                       .doOnSuccess(v -> log.debug("Published update {} to inbound queue", update.updateId()))
                                                       .thenReturn(update))
                        .onErrorResume(e -> {
                            log.error("Error polling updates at offset={}", offset, e);
                            return Mono.empty();
                        });
    }

    /**
     * Dispatch a {@link BotResponse} to the appropriate {@link TelegramBotClient} method.
     */
    Mono<?> dispatch(BotResponse response) {
        return switch (response) {

            // Text
            case BotResponse.SendMessage r -> botClient.sendMessage(r.chatId(), r.text(), r.parseMode(), r.replyMarkup(),
                                                                    r.entities(), r.disableNotification(), r.protectContent());

            // Media
            case BotResponse.SendPhoto r -> botClient.sendPhoto(r.chatId(), r.photo(), null, null,
                                                                r.caption(), r.parseMode(), r.captionEntities(),
                                                                r.showCaptionAboveMedia(), r.hasSpoiler(),
                                                                r.disableNotification(), r.protectContent(),
                                                                null, null, null, r.replyMarkup());

            case BotResponse.SendDocument r -> botClient.sendDocument(r.chatId(), r.document(), r.caption(), r.parseMode(),
                                                                      r.replyMarkup(), r.disableNotification(), r.protectContent());

            case BotResponse.SendVideo r -> botClient.sendVideo(r.chatId(), r.video(), r.caption(), r.parseMode(),
                                                                r.duration(), r.width(), r.height(), r.supportsStreaming(),
                                                                r.hasSpoiler(), r.replyMarkup(),
                                                                r.disableNotification(), r.protectContent());

            case BotResponse.SendAudio r -> botClient.sendAudio(r.chatId(), r.audio(), r.caption(), r.parseMode(),
                                                                r.duration(), r.performer(), r.title(), r.replyMarkup(),
                                                                r.disableNotification(), r.protectContent());

            case BotResponse.SendVoice r -> botClient.sendVoice(r.chatId(), r.voice(), r.caption(), r.parseMode(),
                                                                r.duration(), r.replyMarkup(),
                                                                r.disableNotification(), r.protectContent());

            case BotResponse.SendSticker r -> botClient.sendSticker(r.chatId(), r.sticker(), r.emoji(), r.replyMarkup(),
                                                                    r.disableNotification(), r.protectContent());

            case BotResponse.SendLocation r -> botClient.sendLocation(r.chatId(), r.latitude(), r.longitude(),
                                                                      r.livePeriod(), r.heading(), r.proximityAlertRadius(),
                                                                      r.replyMarkup(), r.disableNotification(), r.protectContent());

            case BotResponse.SendContact r -> botClient.sendContact(r.chatId(), r.phoneNumber(), r.firstName(),
                                                                    r.lastName(), r.vcard(), r.replyMarkup(),
                                                                    r.disableNotification(), r.protectContent());

            case BotResponse.SendMediaGroup r -> botClient.sendMediaGroup(r.chatId(), r.media(), null, null,
                                                                          r.disableNotification(), r.protectContent(),
                                                                          null, null, null);

            // Actions
            case BotResponse.SendChatAction r -> botClient.sendChatAction(r.chatId(), r.action(), null, null);

            // Payments
            case BotResponse.SendInvoice r -> botClient.setInvoice(r.chatId(), r.title(), r.description(),
                                                                   r.payload(), r.price(), r.currency());

            case BotResponse.AnswerPreCheckoutQuery r -> botClient.answerPreCheckoutQuery(r.preCheckoutQueryId(), r.ok(), r.errorMessage());

            // Forward / Copy
            case BotResponse.ForwardMessage r -> botClient.forwardMessage(r.chatId(), r.fromChatId(), r.messageId(),
                                                                          r.disableNotification(), r.protectContent());

            case BotResponse.CopyMessage r -> botClient.copyMessage(r.chatId(), r.fromChatId(), r.messageId(),
                                                                    r.caption(), r.parseMode(), r.replyMarkup(),
                                                                    r.disableNotification(), r.protectContent());

            // Edit
            case BotResponse.EditMessageText r -> botClient.editMessageText(r.chatId(), r.messageId(), r.text(),
                                                                            r.parseMode(), r.entities(), r.replyMarkup());

            case BotResponse.EditMessageCaption r -> botClient.editMessageCaption(r.chatId(), r.messageId(), r.caption(),
                                                                                  r.parseMode(), r.captionEntities(), r.replyMarkup());

            case BotResponse.EditMessageReplyMarkup r -> botClient.editMessageReplyMarkup(r.chatId(), r.messageId(), r.replyMarkup());

            // Delete
            case BotResponse.DeleteMessage r -> botClient.deleteMessage(r.chatId(), r.messageId());

            // Callback
            case BotResponse.AnswerCallbackQuery r -> botClient.answerCallbackQuery(r.callbackQueryId(), r.text(), r.showAlert(),
                                                                                    r.url(), r.cacheTime());
        };
    }

    @Override
    public void dispose() {
        disposables.dispose();
        log.info("TelegramUpdatePoller stopped");
    }

    @Override
    public boolean isDisposed() {
        return disposables.isDisposed();
    }
}
