package dev.alimov.telegram.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record InputMediaPhoto(
        @JsonProperty("type") String type,
        @JsonProperty("media") String media,
        @JsonProperty("caption") String caption,
        @JsonProperty("parse_mode") String parseMode,
        @JsonProperty("caption_entities") List<MessageEntity> captionEntities,
        @JsonProperty("show_caption_above_media") Boolean showCaptionAboveMedia,
        @JsonProperty("has_spoiler") Boolean hasSpoiler
) implements InputMedia {

    public InputMediaPhoto(String media) {
        this("photo", media, null, null, null, null, null);
    }

    public InputMediaPhoto(String media, String caption) {
        this("photo", media, caption, null, null, null, null);
    }
}
