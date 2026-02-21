package dev.alimov.telegram.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record InputMediaVideo(
        @JsonProperty("type") String type,
        @JsonProperty("media") String media,
        @JsonProperty("caption") String caption,
        @JsonProperty("parse_mode") String parseMode,
        @JsonProperty("caption_entities") List<MessageEntity> captionEntities,
        @JsonProperty("width") Integer width,
        @JsonProperty("height") Integer height,
        @JsonProperty("duration") Integer duration,
        @JsonProperty("supports_streaming") Boolean supportsStreaming,
        @JsonProperty("has_spoiler") Boolean hasSpoiler,
        @JsonProperty("show_caption_above_media") Boolean showCaptionAboveMedia
) implements InputMedia {

    public InputMediaVideo(String media) {
        this("video", media, null, null, null, null, null, null, null, null, null);
    }

    public InputMediaVideo(String media, String caption) {
        this("video", media, caption, null, null, null, null, null, null, null, null);
    }
}
