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

import java.lang.reflect.GenericDeclaration;
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

    private <T> String toJson(T object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


}
