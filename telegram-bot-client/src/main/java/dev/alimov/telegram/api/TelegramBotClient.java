package dev.alimov.telegram.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public class TelegramBotClient {

    public static final String TELEGRAM_API_ENDPOINT = "https://api.telegram.org";
    private static final Logger log = LoggerFactory.getLogger(TelegramBotClient.class);

    private final String endpoint;
    private final String token;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;


    public TelegramBotClient(String token) {
        this(TELEGRAM_API_ENDPOINT, token, new ObjectMapper(), WebClient.builder().build());
    }

    public TelegramBotClient(String token, ObjectMapper objectMapper, WebClient webClient) {
        this(TELEGRAM_API_ENDPOINT, token, objectMapper, webClient);
    }

    public TelegramBotClient(String endpoint, String token, ObjectMapper objectMapper, WebClient webClient) {
        this.objectMapper = objectMapper;
        this.endpoint = endpoint;
        this.token = token;

        this.webClient = webClient;
    }

    /**
     * Extracts and processes the response from a {@link ClientResponse}, converting it into a {@link Mono} of {@link Response}.
     * Handles successful (2xx), client error (4xx), and other error responses appropriately.
     *
     * @param clientResponse the {@link ClientResponse} object representing the response from the client
     * @param <T>            the type of the expected response body
     * @return a {@link Mono} containing the extracted and processed {@link Response} object
     * or an error signal for client/server errors
     */
    private static <T> Mono<Response<T>> extractResponse(ClientResponse clientResponse, ParameterizedTypeReference<Response<T>> typeReference) {
        return clientResponse.bodyToMono(typeReference)
                             .flatMap(response -> {
                                 if (response.isOk()) {
                                     return Mono.just(response);
                                 } else {
                                     return Mono.error(new TelegramApiException(response.getErrorCode(), response.getDescription()));
                                 }
                             });
    }

    /**
     * Retrieves updates from the Telegram Bot API based on the provided parameters.
     *
     * @param offset         the offset value used to identify the first update to be returned. Updates with an ID less than
     *                       this value will be ignored.
     * @param limit          the maximum number of updates to retrieve. Acceptable values range from 1 to 100.
     * @param timeout        the timeout in seconds for long polling. A higher timeout will keep the connection open until
     *                       an update is received or the timeout expires.
     * @param allowedUpdates a list of update types that should be received. If null or empty, all update types will
     *                       be returned.
     * @return a Flux containing the updates retrieved from the Telegram Bot API.
     */
    public Flux<Update> getUpdates(long offset, int limit, int timeout, List<String> allowedUpdates) {
        log.info("getUpdates offset={} limit={} timeout={} allowedUpdates={}", offset, limit, timeout, allowedUpdates);

        return webClient.get()
                        .uri(endpoint + "/bot" + token, uriBuilder -> {
                            return uriBuilder
                                    .pathSegment("getUpdates")
                                    .queryParam("offset", offset)
                                    .queryParam("limit", limit)
                                    .queryParam("timeout", timeout)
                                    .queryParam("allowed_updates", allowedUpdates)
                                    .build();
                        })
                        .exchangeToMono(clientResponse -> extractResponse(clientResponse, new ParameterizedTypeReference<Response<List<Update>>>() {
                        }))
                        .flatMapMany(response -> Flux.fromIterable(response.getResult()))
                        .doOnComplete(() -> {
                            log.info("getUpdates complete {}", offset);
                        })
                        .doOnError(e -> {
                            log.warn("getUpdates error", e);
                        });
    }

    public Mono<Response<Message>> sendMessage(Long chatId, String text, @Nullable ParseMode parseMode, @Nullable ReplyKeyboardMarkup replyKeyboardMarkups) {


        BodyInserters.FormInserter<String> inserter = BodyInserters.fromFormData("chat_id", String.valueOf(chatId))
                                                                   .with("text", text);

        if (replyKeyboardMarkups != null) {
            String value = toJson(replyKeyboardMarkups);
            inserter = inserter.with("reply_markup", value);
        }

        if (parseMode != null) {
            inserter = inserter.with("parse_mode", parseMode.name());
        }

        return webClient.post()
                        .uri(endpoint + "/bot" + token, uriBuilder -> {
                            return uriBuilder.pathSegment("sendMessage")
                                             .build();
                        })
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(inserter
                        )
                        .exchangeToMono(clientResponse -> extractResponse(clientResponse, new ParameterizedTypeReference<Response<Message>>() {
                        }));
    }

    public Mono<Response<Message>> setInvoice(long chatId, String title, String description, String payload, int price, String currency) {
        BodyInserters.FormInserter<String> inserter = BodyInserters.fromFormData("chat_id", String.valueOf(chatId))
                                                                   .with("title", title)
                                                                   .with("description", description)
                                                                   .with("payload", payload)
                                                                   .with("prices", toJson(List.of(new LabeledPrice("product price", price))))
                                                                   .with("currency", currency);


        return webClient.post()
                        .uri(endpoint + "/bot" + token, uriBuilder -> {
                            return uriBuilder.pathSegment("sendInvoice")
                                             .build();
                        })
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(inserter
                        )
                        .exchangeToMono(clientResponse -> extractResponse(clientResponse, new ParameterizedTypeReference<Response<Message>>() {
                        }));
    }

    public Mono<Response<Boolean>> answerPreCheckoutQuery(String preCheckoutQueryId, boolean ok, String errorMessage) {
        BodyInserters.FormInserter<String> inserter = BodyInserters.fromFormData("pre_checkout_query_id", String.valueOf(preCheckoutQueryId))
                                                                   .with("ok", String.valueOf(ok))
                                                                   .with("errorMessage", errorMessage);


        return webClient.post()
                        .uri(endpoint + "/bot" + token, uriBuilder -> {
                            return uriBuilder.pathSegment("answerPreCheckoutQuery")
                                             .build();
                        })
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(inserter
                        )
                        .exchangeToMono(clientResponse -> extractResponse(clientResponse, new ParameterizedTypeReference<Response<Boolean>>() {
                        }));
    }

    /**
     * Use this method to send photos. On success, the sent Message is returned.
     *
     * @see <a href="https://core.telegram.org/bots/api#sendphoto">Telegram Bot API - sendPhoto</a>
     */
    public Mono<Response<Message>> sendPhoto(
            long chatId,
            String photo,
            @Nullable String businessConnectionId,
            @Nullable Integer messageThreadId,
            @Nullable String caption,
            @Nullable ParseMode parseMode,
            @Nullable List<MessageEntity> captionEntities,
            @Nullable Boolean showCaptionAboveMedia,
            @Nullable Boolean hasSpoiler,
            @Nullable Boolean disableNotification,
            @Nullable Boolean protectContent,
            @Nullable Boolean allowPaidBroadcast,
            @Nullable String messageEffectId,
            @Nullable ReplyParameters replyParameters,
            @Nullable Object replyMarkup
    ) {
        BodyInserters.FormInserter<String> inserter = BodyInserters.fromFormData("chat_id", String.valueOf(chatId))
                                                                   .with("photo", photo);

        if (businessConnectionId != null) {
            inserter = inserter.with("business_connection_id", businessConnectionId);
        }
        if (messageThreadId != null) {
            inserter = inserter.with("message_thread_id", String.valueOf(messageThreadId));
        }
        if (caption != null) {
            inserter = inserter.with("caption", caption);
        }
        if (parseMode != null) {
            inserter = inserter.with("parse_mode", parseMode.name());
        }
        if (captionEntities != null) {
            inserter = inserter.with("caption_entities", toJson(captionEntities));
        }
        if (showCaptionAboveMedia != null) {
            inserter = inserter.with("show_caption_above_media", String.valueOf(showCaptionAboveMedia));
        }
        if (hasSpoiler != null) {
            inserter = inserter.with("has_spoiler", String.valueOf(hasSpoiler));
        }
        if (disableNotification != null) {
            inserter = inserter.with("disable_notification", String.valueOf(disableNotification));
        }
        if (protectContent != null) {
            inserter = inserter.with("protect_content", String.valueOf(protectContent));
        }
        if (allowPaidBroadcast != null) {
            inserter = inserter.with("allow_paid_broadcast", String.valueOf(allowPaidBroadcast));
        }
        if (messageEffectId != null) {
            inserter = inserter.with("message_effect_id", messageEffectId);
        }
        if (replyParameters != null) {
            inserter = inserter.with("reply_parameters", toJson(replyParameters));
        }
        if (replyMarkup != null) {
            inserter = inserter.with("reply_markup", toJson(replyMarkup));
        }

        return webClient.post()
                        .uri(endpoint + "/bot" + token, uriBuilder -> {
                            return uriBuilder.pathSegment("sendPhoto")
                                             .build();
                        })
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(inserter)
                        .exchangeToMono(clientResponse -> extractResponse(clientResponse, new ParameterizedTypeReference<Response<Message>>() {
                        }));
    }

    /**
     * Use this method to send a group of photos, videos, documents or audios as an album.
     * On success, an array of Messages that were sent is returned.
     *
     * @see <a href="https://core.telegram.org/bots/api#sendmediagroup">Telegram Bot API - sendMediaGroup</a>
     */
    public Mono<Response<List<Message>>> sendMediaGroup(
            long chatId,
            List<InputMedia> media,
            @Nullable String businessConnectionId,
            @Nullable Integer messageThreadId,
            @Nullable Boolean disableNotification,
            @Nullable Boolean protectContent,
            @Nullable Boolean allowPaidBroadcast,
            @Nullable String messageEffectId,
            @Nullable ReplyParameters replyParameters
    ) {
        BodyInserters.FormInserter<String> inserter = BodyInserters.fromFormData("chat_id", String.valueOf(chatId))
                                                                   .with("media", toJson(media));

        if (businessConnectionId != null) {
            inserter = inserter.with("business_connection_id", businessConnectionId);
        }
        if (messageThreadId != null) {
            inserter = inserter.with("message_thread_id", String.valueOf(messageThreadId));
        }
        if (disableNotification != null) {
            inserter = inserter.with("disable_notification", String.valueOf(disableNotification));
        }
        if (protectContent != null) {
            inserter = inserter.with("protect_content", String.valueOf(protectContent));
        }
        if (allowPaidBroadcast != null) {
            inserter = inserter.with("allow_paid_broadcast", String.valueOf(allowPaidBroadcast));
        }
        if (messageEffectId != null) {
            inserter = inserter.with("message_effect_id", messageEffectId);
        }
        if (replyParameters != null) {
            inserter = inserter.with("reply_parameters", toJson(replyParameters));
        }

        return webClient.post()
                        .uri(endpoint + "/bot" + token, uriBuilder -> {
                            return uriBuilder.pathSegment("sendMediaGroup")
                                             .build();
                        })
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(inserter)
                        .exchangeToMono(clientResponse -> extractResponse(clientResponse, new ParameterizedTypeReference<Response<List<Message>>>() {
                        }));
    }

    /**
     * Use this method when you need to tell the user that something is happening on the bot's side.
     * Returns True on success.
     *
     * @see <a href="https://core.telegram.org/bots/api#sendchataction">Telegram Bot API - sendChatAction</a>
     */
    public Mono<Response<Boolean>> sendChatAction(
            long chatId,
            ChatAction action,
            @Nullable String businessConnectionId,
            @Nullable Integer messageThreadId
    ) {
        BodyInserters.FormInserter<String> inserter = BodyInserters.fromFormData("chat_id", String.valueOf(chatId))
                                                                   .with("action", action.getValue());

        if (businessConnectionId != null) {
            inserter = inserter.with("business_connection_id", businessConnectionId);
        }
        if (messageThreadId != null) {
            inserter = inserter.with("message_thread_id", String.valueOf(messageThreadId));
        }

        return webClient.post()
                        .uri(endpoint + "/bot" + token, uriBuilder -> {
                            return uriBuilder.pathSegment("sendChatAction")
                                             .build();
                        })
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(inserter)
                        .exchangeToMono(clientResponse -> extractResponse(clientResponse, new ParameterizedTypeReference<Response<Boolean>>() {
                        }));
    }

    /**
     * Use this method to send text messages with any reply markup type.
     *
     * @see <a href="https://core.telegram.org/bots/api#sendmessage">Telegram Bot API - sendMessage</a>
     */
    public Mono<Response<Message>> sendMessage(
            long chatId,
            String text,
            @Nullable ParseMode parseMode,
            @Nullable Object replyMarkup,
            @Nullable List<MessageEntity> entities,
            @Nullable Boolean disableNotification,
            @Nullable Boolean protectContent
    ) {
        BodyInserters.FormInserter<String> inserter = BodyInserters.fromFormData("chat_id", String.valueOf(chatId))
                                                                   .with("text", text);
        if (parseMode != null) {
            inserter = inserter.with("parse_mode", parseMode.name());
        }
        if (replyMarkup != null) {
            inserter = inserter.with("reply_markup", toJson(replyMarkup));
        }
        if (entities != null) {
            inserter = inserter.with("entities", toJson(entities));
        }
        if (disableNotification != null) {
            inserter = inserter.with("disable_notification", String.valueOf(disableNotification));
        }
        if (protectContent != null) {
            inserter = inserter.with("protect_content", String.valueOf(protectContent));
        }

        return webClient.post()
                        .uri(endpoint + "/bot" + token, uriBuilder -> uriBuilder.pathSegment("sendMessage").build())
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(inserter)
                        .exchangeToMono(clientResponse -> extractResponse(clientResponse, new ParameterizedTypeReference<Response<Message>>() {
                        }));
    }

    /**
     * Use this method to send general files.
     *
     * @see <a href="https://core.telegram.org/bots/api#senddocument">Telegram Bot API - sendDocument</a>
     */
    public Mono<Response<Message>> sendDocument(
            long chatId,
            String document,
            @Nullable String caption,
            @Nullable ParseMode parseMode,
            @Nullable Object replyMarkup,
            @Nullable Boolean disableNotification,
            @Nullable Boolean protectContent
    ) {
        BodyInserters.FormInserter<String> inserter = BodyInserters.fromFormData("chat_id", String.valueOf(chatId))
                                                                   .with("document", document);
        if (caption != null) {
            inserter = inserter.with("caption", caption);
        }
        if (parseMode != null) {
            inserter = inserter.with("parse_mode", parseMode.name());
        }
        if (replyMarkup != null) {
            inserter = inserter.with("reply_markup", toJson(replyMarkup));
        }
        if (disableNotification != null) {
            inserter = inserter.with("disable_notification", String.valueOf(disableNotification));
        }
        if (protectContent != null) {
            inserter = inserter.with("protect_content", String.valueOf(protectContent));
        }

        return webClient.post()
                        .uri(endpoint + "/bot" + token, uriBuilder -> uriBuilder.pathSegment("sendDocument").build())
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(inserter)
                        .exchangeToMono(clientResponse -> extractResponse(clientResponse, new ParameterizedTypeReference<Response<Message>>() {
                        }));
    }

    /**
     * Use this method to send video files.
     *
     * @see <a href="https://core.telegram.org/bots/api#sendvideo">Telegram Bot API - sendVideo</a>
     */
    public Mono<Response<Message>> sendVideo(
            long chatId,
            String video,
            @Nullable String caption,
            @Nullable ParseMode parseMode,
            @Nullable Integer duration,
            @Nullable Integer width,
            @Nullable Integer height,
            @Nullable Boolean supportsStreaming,
            @Nullable Boolean hasSpoiler,
            @Nullable Object replyMarkup,
            @Nullable Boolean disableNotification,
            @Nullable Boolean protectContent
    ) {
        BodyInserters.FormInserter<String> inserter = BodyInserters.fromFormData("chat_id", String.valueOf(chatId))
                                                                   .with("video", video);
        if (caption != null) {
            inserter = inserter.with("caption", caption);
        }
        if (parseMode != null) {
            inserter = inserter.with("parse_mode", parseMode.name());
        }
        if (duration != null) {
            inserter = inserter.with("duration", String.valueOf(duration));
        }
        if (width != null) {
            inserter = inserter.with("width", String.valueOf(width));
        }
        if (height != null) {
            inserter = inserter.with("height", String.valueOf(height));
        }
        if (supportsStreaming != null) {
            inserter = inserter.with("supports_streaming", String.valueOf(supportsStreaming));
        }
        if (hasSpoiler != null) {
            inserter = inserter.with("has_spoiler", String.valueOf(hasSpoiler));
        }
        if (replyMarkup != null) {
            inserter = inserter.with("reply_markup", toJson(replyMarkup));
        }
        if (disableNotification != null) {
            inserter = inserter.with("disable_notification", String.valueOf(disableNotification));
        }
        if (protectContent != null) {
            inserter = inserter.with("protect_content", String.valueOf(protectContent));
        }

        return webClient.post()
                        .uri(endpoint + "/bot" + token, uriBuilder -> uriBuilder.pathSegment("sendVideo").build())
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(inserter)
                        .exchangeToMono(clientResponse -> extractResponse(clientResponse, new ParameterizedTypeReference<Response<Message>>() {
                        }));
    }

    /**
     * Use this method to send audio files.
     *
     * @see <a href="https://core.telegram.org/bots/api#sendaudio">Telegram Bot API - sendAudio</a>
     */
    public Mono<Response<Message>> sendAudio(
            long chatId,
            String audio,
            @Nullable String caption,
            @Nullable ParseMode parseMode,
            @Nullable Integer duration,
            @Nullable String performer,
            @Nullable String title,
            @Nullable Object replyMarkup,
            @Nullable Boolean disableNotification,
            @Nullable Boolean protectContent
    ) {
        BodyInserters.FormInserter<String> inserter = BodyInserters.fromFormData("chat_id", String.valueOf(chatId))
                                                                   .with("audio", audio);
        if (caption != null) {
            inserter = inserter.with("caption", caption);
        }
        if (parseMode != null) {
            inserter = inserter.with("parse_mode", parseMode.name());
        }
        if (duration != null) {
            inserter = inserter.with("duration", String.valueOf(duration));
        }
        if (performer != null) {
            inserter = inserter.with("performer", performer);
        }
        if (title != null) {
            inserter = inserter.with("title", title);
        }
        if (replyMarkup != null) {
            inserter = inserter.with("reply_markup", toJson(replyMarkup));
        }
        if (disableNotification != null) {
            inserter = inserter.with("disable_notification", String.valueOf(disableNotification));
        }
        if (protectContent != null) {
            inserter = inserter.with("protect_content", String.valueOf(protectContent));
        }

        return webClient.post()
                        .uri(endpoint + "/bot" + token, uriBuilder -> uriBuilder.pathSegment("sendAudio").build())
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(inserter)
                        .exchangeToMono(clientResponse -> extractResponse(clientResponse, new ParameterizedTypeReference<Response<Message>>() {
                        }));
    }

    /**
     * Use this method to send audio files that the client should display as a playable voice message.
     *
     * @see <a href="https://core.telegram.org/bots/api#sendvoice">Telegram Bot API - sendVoice</a>
     */
    public Mono<Response<Message>> sendVoice(
            long chatId,
            String voice,
            @Nullable String caption,
            @Nullable ParseMode parseMode,
            @Nullable Integer duration,
            @Nullable Object replyMarkup,
            @Nullable Boolean disableNotification,
            @Nullable Boolean protectContent
    ) {
        BodyInserters.FormInserter<String> inserter = BodyInserters.fromFormData("chat_id", String.valueOf(chatId))
                                                                   .with("voice", voice);
        if (caption != null) {
            inserter = inserter.with("caption", caption);
        }
        if (parseMode != null) {
            inserter = inserter.with("parse_mode", parseMode.name());
        }
        if (duration != null) {
            inserter = inserter.with("duration", String.valueOf(duration));
        }
        if (replyMarkup != null) {
            inserter = inserter.with("reply_markup", toJson(replyMarkup));
        }
        if (disableNotification != null) {
            inserter = inserter.with("disable_notification", String.valueOf(disableNotification));
        }
        if (protectContent != null) {
            inserter = inserter.with("protect_content", String.valueOf(protectContent));
        }

        return webClient.post()
                        .uri(endpoint + "/bot" + token, uriBuilder -> uriBuilder.pathSegment("sendVoice").build())
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(inserter)
                        .exchangeToMono(clientResponse -> extractResponse(clientResponse, new ParameterizedTypeReference<Response<Message>>() {
                        }));
    }

    /**
     * Use this method to send static .WEBP, animated .TGS, or video .WEBM stickers.
     *
     * @see <a href="https://core.telegram.org/bots/api#sendsticker">Telegram Bot API - sendSticker</a>
     */
    public Mono<Response<Message>> sendSticker(
            long chatId,
            String sticker,
            @Nullable String emoji,
            @Nullable Object replyMarkup,
            @Nullable Boolean disableNotification,
            @Nullable Boolean protectContent
    ) {
        BodyInserters.FormInserter<String> inserter = BodyInserters.fromFormData("chat_id", String.valueOf(chatId))
                                                                   .with("sticker", sticker);
        if (emoji != null) {
            inserter = inserter.with("emoji", emoji);
        }
        if (replyMarkup != null) {
            inserter = inserter.with("reply_markup", toJson(replyMarkup));
        }
        if (disableNotification != null) {
            inserter = inserter.with("disable_notification", String.valueOf(disableNotification));
        }
        if (protectContent != null) {
            inserter = inserter.with("protect_content", String.valueOf(protectContent));
        }

        return webClient.post()
                        .uri(endpoint + "/bot" + token, uriBuilder -> uriBuilder.pathSegment("sendSticker").build())
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(inserter)
                        .exchangeToMono(clientResponse -> extractResponse(clientResponse, new ParameterizedTypeReference<Response<Message>>() {
                        }));
    }

    /**
     * Use this method to send point on the map.
     *
     * @see <a href="https://core.telegram.org/bots/api#sendlocation">Telegram Bot API - sendLocation</a>
     */
    public Mono<Response<Message>> sendLocation(
            long chatId,
            double latitude,
            double longitude,
            @Nullable Integer livePeriod,
            @Nullable Integer heading,
            @Nullable Integer proximityAlertRadius,
            @Nullable Object replyMarkup,
            @Nullable Boolean disableNotification,
            @Nullable Boolean protectContent
    ) {
        BodyInserters.FormInserter<String> inserter = BodyInserters.fromFormData("chat_id", String.valueOf(chatId))
                                                                   .with("latitude", String.valueOf(latitude))
                                                                   .with("longitude", String.valueOf(longitude));
        if (livePeriod != null) {
            inserter = inserter.with("live_period", String.valueOf(livePeriod));
        }
        if (heading != null) {
            inserter = inserter.with("heading", String.valueOf(heading));
        }
        if (proximityAlertRadius != null) {
            inserter = inserter.with("proximity_alert_radius", String.valueOf(proximityAlertRadius));
        }
        if (replyMarkup != null) {
            inserter = inserter.with("reply_markup", toJson(replyMarkup));
        }
        if (disableNotification != null) {
            inserter = inserter.with("disable_notification", String.valueOf(disableNotification));
        }
        if (protectContent != null) {
            inserter = inserter.with("protect_content", String.valueOf(protectContent));
        }

        return webClient.post()
                        .uri(endpoint + "/bot" + token, uriBuilder -> uriBuilder.pathSegment("sendLocation").build())
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(inserter)
                        .exchangeToMono(clientResponse -> extractResponse(clientResponse, new ParameterizedTypeReference<Response<Message>>() {
                        }));
    }

    /**
     * Use this method to send phone contacts.
     *
     * @see <a href="https://core.telegram.org/bots/api#sendcontact">Telegram Bot API - sendContact</a>
     */
    public Mono<Response<Message>> sendContact(
            long chatId,
            String phoneNumber,
            String firstName,
            @Nullable String lastName,
            @Nullable String vcard,
            @Nullable Object replyMarkup,
            @Nullable Boolean disableNotification,
            @Nullable Boolean protectContent
    ) {
        BodyInserters.FormInserter<String> inserter = BodyInserters.fromFormData("chat_id", String.valueOf(chatId))
                                                                   .with("phone_number", phoneNumber)
                                                                   .with("first_name", firstName);
        if (lastName != null) {
            inserter = inserter.with("last_name", lastName);
        }
        if (vcard != null) {
            inserter = inserter.with("vcard", vcard);
        }
        if (replyMarkup != null) {
            inserter = inserter.with("reply_markup", toJson(replyMarkup));
        }
        if (disableNotification != null) {
            inserter = inserter.with("disable_notification", String.valueOf(disableNotification));
        }
        if (protectContent != null) {
            inserter = inserter.with("protect_content", String.valueOf(protectContent));
        }

        return webClient.post()
                        .uri(endpoint + "/bot" + token, uriBuilder -> uriBuilder.pathSegment("sendContact").build())
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(inserter)
                        .exchangeToMono(clientResponse -> extractResponse(clientResponse, new ParameterizedTypeReference<Response<Message>>() {
                        }));
    }

    /**
     * Use this method to forward messages of any kind.
     *
     * @see <a href="https://core.telegram.org/bots/api#forwardmessage">Telegram Bot API - forwardMessage</a>
     */
    public Mono<Response<Message>> forwardMessage(
            long chatId,
            long fromChatId,
            long messageId,
            @Nullable Boolean disableNotification,
            @Nullable Boolean protectContent
    ) {
        BodyInserters.FormInserter<String> inserter = BodyInserters.fromFormData("chat_id", String.valueOf(chatId))
                                                                   .with("from_chat_id", String.valueOf(fromChatId))
                                                                   .with("message_id", String.valueOf(messageId));
        if (disableNotification != null) {
            inserter = inserter.with("disable_notification", String.valueOf(disableNotification));
        }
        if (protectContent != null) {
            inserter = inserter.with("protect_content", String.valueOf(protectContent));
        }

        return webClient.post()
                        .uri(endpoint + "/bot" + token, uriBuilder -> uriBuilder.pathSegment("forwardMessage").build())
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(inserter)
                        .exchangeToMono(clientResponse -> extractResponse(clientResponse, new ParameterizedTypeReference<Response<Message>>() {
                        }));
    }

    /**
     * Use this method to copy messages of any kind.
     *
     * @see <a href="https://core.telegram.org/bots/api#copymessage">Telegram Bot API - copyMessage</a>
     */
    public Mono<Response<MessageId>> copyMessage(
            long chatId,
            long fromChatId,
            long messageId,
            @Nullable String caption,
            @Nullable ParseMode parseMode,
            @Nullable Object replyMarkup,
            @Nullable Boolean disableNotification,
            @Nullable Boolean protectContent
    ) {
        BodyInserters.FormInserter<String> inserter = BodyInserters.fromFormData("chat_id", String.valueOf(chatId))
                                                                   .with("from_chat_id", String.valueOf(fromChatId))
                                                                   .with("message_id", String.valueOf(messageId));
        if (caption != null) {
            inserter = inserter.with("caption", caption);
        }
        if (parseMode != null) {
            inserter = inserter.with("parse_mode", parseMode.name());
        }
        if (replyMarkup != null) {
            inserter = inserter.with("reply_markup", toJson(replyMarkup));
        }
        if (disableNotification != null) {
            inserter = inserter.with("disable_notification", String.valueOf(disableNotification));
        }
        if (protectContent != null) {
            inserter = inserter.with("protect_content", String.valueOf(protectContent));
        }

        return webClient.post()
                        .uri(endpoint + "/bot" + token, uriBuilder -> uriBuilder.pathSegment("copyMessage").build())
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(inserter)
                        .exchangeToMono(clientResponse -> extractResponse(clientResponse, new ParameterizedTypeReference<Response<MessageId>>() {
                        }));
    }

    /**
     * Use this method to edit text and game messages.
     *
     * @see <a href="https://core.telegram.org/bots/api#editmessagetext">Telegram Bot API - editMessageText</a>
     */
    public Mono<Response<Message>> editMessageText(
            long chatId,
            long messageId,
            String text,
            @Nullable ParseMode parseMode,
            @Nullable List<MessageEntity> entities,
            @Nullable Object replyMarkup
    ) {
        BodyInserters.FormInserter<String> inserter = BodyInserters.fromFormData("chat_id", String.valueOf(chatId))
                                                                   .with("message_id", String.valueOf(messageId))
                                                                   .with("text", text);
        if (parseMode != null) {
            inserter = inserter.with("parse_mode", parseMode.name());
        }
        if (entities != null) {
            inserter = inserter.with("entities", toJson(entities));
        }
        if (replyMarkup != null) {
            inserter = inserter.with("reply_markup", toJson(replyMarkup));
        }

        return webClient.post()
                        .uri(endpoint + "/bot" + token, uriBuilder -> uriBuilder.pathSegment("editMessageText").build())
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(inserter)
                        .exchangeToMono(clientResponse -> extractResponse(clientResponse, new ParameterizedTypeReference<Response<Message>>() {
                        }));
    }

    /**
     * Use this method to edit captions of messages.
     *
     * @see <a href="https://core.telegram.org/bots/api#editmessagecaption">Telegram Bot API - editMessageCaption</a>
     */
    public Mono<Response<Message>> editMessageCaption(
            long chatId,
            long messageId,
            @Nullable String caption,
            @Nullable ParseMode parseMode,
            @Nullable List<MessageEntity> captionEntities,
            @Nullable Object replyMarkup
    ) {
        BodyInserters.FormInserter<String> inserter = BodyInserters.fromFormData("chat_id", String.valueOf(chatId))
                                                                   .with("message_id", String.valueOf(messageId));
        if (caption != null) {
            inserter = inserter.with("caption", caption);
        }
        if (parseMode != null) {
            inserter = inserter.with("parse_mode", parseMode.name());
        }
        if (captionEntities != null) {
            inserter = inserter.with("caption_entities", toJson(captionEntities));
        }
        if (replyMarkup != null) {
            inserter = inserter.with("reply_markup", toJson(replyMarkup));
        }

        return webClient.post()
                        .uri(endpoint + "/bot" + token, uriBuilder -> uriBuilder.pathSegment("editMessageCaption").build())
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(inserter)
                        .exchangeToMono(clientResponse -> extractResponse(clientResponse, new ParameterizedTypeReference<Response<Message>>() {
                        }));
    }

    /**
     * Use this method to edit only the reply markup of messages.
     *
     * @see <a href="https://core.telegram.org/bots/api#editmessagereplymarkup">Telegram Bot API - editMessageReplyMarkup</a>
     */
    public Mono<Response<Message>> editMessageReplyMarkup(
            long chatId,
            long messageId,
            @Nullable Object replyMarkup
    ) {
        BodyInserters.FormInserter<String> inserter = BodyInserters.fromFormData("chat_id", String.valueOf(chatId))
                                                                   .with("message_id", String.valueOf(messageId));
        if (replyMarkup != null) {
            inserter = inserter.with("reply_markup", toJson(replyMarkup));
        }

        return webClient.post()
                        .uri(endpoint + "/bot" + token, uriBuilder -> uriBuilder.pathSegment("editMessageReplyMarkup").build())
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(inserter)
                        .exchangeToMono(clientResponse -> extractResponse(clientResponse, new ParameterizedTypeReference<Response<Message>>() {
                        }));
    }

    /**
     * Use this method to delete a message.
     *
     * @see <a href="https://core.telegram.org/bots/api#deletemessage">Telegram Bot API - deleteMessage</a>
     */
    public Mono<Response<Boolean>> deleteMessage(long chatId, long messageId) {
        BodyInserters.FormInserter<String> inserter = BodyInserters.fromFormData("chat_id", String.valueOf(chatId))
                                                                   .with("message_id", String.valueOf(messageId));

        return webClient.post()
                        .uri(endpoint + "/bot" + token, uriBuilder -> uriBuilder.pathSegment("deleteMessage").build())
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(inserter)
                        .exchangeToMono(clientResponse -> extractResponse(clientResponse, new ParameterizedTypeReference<Response<Boolean>>() {
                        }));
    }

    /**
     * Use this method to send a message draft, allowing partial messages to be streamed to a user while being generated.
     * On success, the sent Message is returned.
     *
     * @see <a href="https://core.telegram.org/bots/api#sendmessagedraft">Telegram Bot API - sendMessageDraft</a>
     */
    public Mono<Response<Boolean>> sendMessageDraft(
            long chatId,
            @Nullable Integer messageThreadId,
            int draftId,
            String text,
            @Nullable ParseMode parseMode,
            @Nullable List<MessageEntity> entities
    ) {
        BodyInserters.FormInserter<String> inserter = BodyInserters.fromFormData("chat_id", String.valueOf(chatId))
                                                                   .with("draft_id", String.valueOf(draftId))
                                                                   .with("text", text);

        if (messageThreadId != null) {
            inserter = inserter.with("message_thread_id", String.valueOf(messageThreadId));
        }

        if (parseMode != null) {
            inserter = inserter.with("parse_mode", parseMode.name());
        }
        if (entities != null) {
            inserter = inserter.with("entities", toJson(entities));
        }


        return webClient.post()
                        .uri(endpoint + "/bot" + token, uriBuilder -> uriBuilder.pathSegment("sendMessageDraft").build())
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(inserter)
                        .exchangeToMono(clientResponse -> extractResponse(clientResponse, new ParameterizedTypeReference<Response<Boolean>>() {
                        }));

    }

    /**
     * Use this method to send answers to callback queries sent from inline keyboards.
     *
     * @see <a href="https://core.telegram.org/bots/api#answercallbackquery">Telegram Bot API - answerCallbackQuery</a>
     */
    public Mono<Response<Boolean>> answerCallbackQuery(
            String callbackQueryId,
            @Nullable String text,
            @Nullable Boolean showAlert,
            @Nullable String url,
            @Nullable Integer cacheTime
    ) {
        BodyInserters.FormInserter<String> inserter = BodyInserters.fromFormData("callback_query_id", callbackQueryId);
        if (text != null) {
            inserter = inserter.with("text", text);
        }
        if (showAlert != null) {
            inserter = inserter.with("show_alert", String.valueOf(showAlert));
        }
        if (url != null) {
            inserter = inserter.with("url", url);
        }
        if (cacheTime != null) {
            inserter = inserter.with("cache_time", String.valueOf(cacheTime));
        }

        return webClient.post()
                        .uri(endpoint + "/bot" + token, uriBuilder -> uriBuilder.pathSegment("answerCallbackQuery").build())
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(inserter)
                        .exchangeToMono(clientResponse -> extractResponse(clientResponse, new ParameterizedTypeReference<Response<Boolean>>() {
                        }));
    }

    private <T> String toJson(T object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


}
