package dev.alimov.telegram.core;

import dev.alimov.telegram.api.Update;
import reactor.core.publisher.Flux;

/**
 * Functional interface for processing Telegram updates and producing bot responses.
 */
@FunctionalInterface
public interface UpdateHandler {

    /**
     * Handle an incoming update and produce zero or more responses.
     *
     * @param update the incoming Telegram update
     * @return a {@link Flux} of responses to send back
     */
    Flux<BotResponse> handle(Update update);
}
