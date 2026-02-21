package dev.alimov.telegram.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
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


    public Flux<Update> getUpdates(Integer offset, int limit, int timeout, List<String> allowedUpdates) {
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
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Response<List<Update>>>() {
                        })
                        .flatMapMany(response -> {
                            if (response.isOk()) {
                                return Flux.fromIterable(response.getResult());
                            } else {
                                return Mono.error(new RuntimeException(response.getDescription()));
                            }
                        })
                        .doOnComplete(() -> {
                            log.info("getUpdates complete {}", offset);
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
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Response<Message>>() {
                        });
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
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Response<Message>>() {
                        });
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
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Response<Boolean>>() {
                        });
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
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Response<Message>>() {
                        });
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
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Response<List<Message>>>() {
                        });
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
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Response<Boolean>>() {
                        });
    }

    private <T> String toJson(T object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


}
