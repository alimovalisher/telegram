package dev.alimov.telegram.poller;

import dev.alimov.telegram.api.Response;
import dev.alimov.telegram.api.TelegramBotClient;
import dev.alimov.telegram.core.BotResponse;
import dev.alimov.telegram.core.ReactiveChannel;
import dev.alimov.telegram.core.ReadableReactiveChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.Duration;

/**
 * Polls updates from the Telegram Bot API on a scheduled interval and publishes them
 * to an inbound {@link ReactiveChannel}. Also subscribes to an outbound {@link ReactiveChannel}
 * to forward {@link BotResponse} messages back through the Telegram API.
 */
public class TelegramBotReplier implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotReplier.class);

    private final TelegramBotClient botClient;


    private final Disposable.Composite disposables = Disposables.composite();

    public TelegramBotReplier(TelegramBotClient botClient) {
        this.botClient = botClient;
    }


    /**
     * Dispatch a {@link BotResponse} to the appropriate {@link TelegramBotClient} method.
     */
    Mono<Response> dispatch(BotResponse response) {
        return switch (response) {

            // Text
            case BotResponse.SendMessage r ->
                    botClient.sendMessage(r.chatId(), r.text(), r.parseMode(), r.replyMarkup(), r.entities(), r.disableNotification(), r.protectContent())
                             .cast(Response.class);

            // Media
            case BotResponse.SendPhoto r -> botClient.sendPhoto(r.chatId(), r.photo(), null, null,
                                                                r.caption(), r.parseMode(), r.captionEntities(),
                                                                r.showCaptionAboveMedia(), r.hasSpoiler(),
                                                                r.disableNotification(), r.protectContent(),
                                                                null, null, null, r.replyMarkup())
                                                     .cast(Response.class);

            case BotResponse.SendDocument r -> botClient.sendDocument(r.chatId(), r.document(), r.caption(), r.parseMode(),
                                                                      r.replyMarkup(), r.disableNotification(), r.protectContent())
                                                        .cast(Response.class);

            case BotResponse.SendVideo r -> botClient.sendVideo(r.chatId(), r.video(), r.caption(), r.parseMode(),
                                                                r.duration(), r.width(), r.height(), r.supportsStreaming(),
                                                                r.hasSpoiler(), r.replyMarkup(),
                                                                r.disableNotification(), r.protectContent())
                                                     .cast(Response.class);

            case BotResponse.SendAudio r -> botClient.sendAudio(r.chatId(), r.audio(), r.caption(), r.parseMode(),
                                                                r.duration(), r.performer(), r.title(), r.replyMarkup(),
                                                                r.disableNotification(), r.protectContent())
                                                     .cast(Response.class);

            case BotResponse.SendVoice r -> botClient.sendVoice(r.chatId(), r.voice(), r.caption(), r.parseMode(),
                                                                r.duration(), r.replyMarkup(),
                                                                r.disableNotification(), r.protectContent())
                                                     .cast(Response.class);

            case BotResponse.SendSticker r -> botClient.sendSticker(r.chatId(), r.sticker(), r.emoji(), r.replyMarkup(),
                                                                    r.disableNotification(), r.protectContent())
                                                       .cast(Response.class);

            case BotResponse.SendLocation r -> botClient.sendLocation(r.chatId(), r.latitude(), r.longitude(),
                                                                      r.livePeriod(), r.heading(), r.proximityAlertRadius(),
                                                                      r.replyMarkup(), r.disableNotification(), r.protectContent())
                                                        .cast(Response.class);

            case BotResponse.SendContact r -> botClient.sendContact(r.chatId(), r.phoneNumber(), r.firstName(),
                                                                    r.lastName(), r.vcard(), r.replyMarkup(),
                                                                    r.disableNotification(), r.protectContent())
                                                       .cast(Response.class);

            case BotResponse.SendMediaGroup r -> botClient.sendMediaGroup(r.chatId(), r.media(), null, null,
                                                                          r.disableNotification(), r.protectContent(),
                                                                          null, null, null)
                                                          .cast(Response.class);

            // Actions
            case BotResponse.SendChatAction r -> botClient.sendChatAction(r.chatId(), r.action(), null, null)
                                                          .cast(Response.class);

            // Payments
            case BotResponse.SendInvoice r -> botClient.setInvoice(r.chatId(), r.title(), r.description(),
                                                                   r.payload(), r.price(), r.currency())
                                                       .cast(Response.class);

            case BotResponse.AnswerPreCheckoutQuery r -> botClient.answerPreCheckoutQuery(r.preCheckoutQueryId(), r.ok(), r.errorMessage())
                                                                  .cast(Response.class);

            // Forward / Copy
            case BotResponse.ForwardMessage r -> botClient.forwardMessage(r.chatId(), r.fromChatId(), r.messageId(),
                                                                          r.disableNotification(), r.protectContent())
                                                          .cast(Response.class);

            case BotResponse.CopyMessage r -> botClient.copyMessage(r.chatId(), r.fromChatId(), r.messageId(),
                                                                    r.caption(), r.parseMode(), r.replyMarkup(),
                                                                    r.disableNotification(), r.protectContent())
                                                       .cast(Response.class);

            // Edit
            case BotResponse.EditMessageText r -> botClient.editMessageText(r.chatId(), r.messageId(), r.text(),
                                                                            r.parseMode(), r.entities(), r.replyMarkup())
                                                           .cast(Response.class);

            case BotResponse.EditMessageCaption r -> botClient.editMessageCaption(r.chatId(), r.messageId(), r.caption(),
                                                                                  r.parseMode(), r.captionEntities(), r.replyMarkup())
                                                              .cast(Response.class);

            case BotResponse.EditMessageReplyMarkup r -> botClient.editMessageReplyMarkup(r.chatId(), r.messageId(), r.replyMarkup())
                                                                  .cast(Response.class);

            // Delete
            case BotResponse.DeleteMessage r -> botClient.deleteMessage(r.chatId(), r.messageId())
                                                         .cast(Response.class);

            // Callback
            case BotResponse.AnswerCallbackQuery r -> botClient.answerCallbackQuery(r.callbackQueryId(), r.text(), r.showAlert(),
                                                                                    r.url(), r.cacheTime())
                                                               .cast(Response.class);
        };
    }


    @Override
    public void close() throws Exception {
        disposables.dispose();

        log.info("TelegramUpdatePoller stopped");
    }
}
