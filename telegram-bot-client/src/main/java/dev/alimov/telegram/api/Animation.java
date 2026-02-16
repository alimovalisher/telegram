package dev.alimov.telegram.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Animation(
        @JsonProperty("file_id") String fileId,
        @JsonProperty("file_unique_id") String fileUniqueId,
        Integer width,
        Integer height,
        Integer duration,
        PhotoSize thumb,
        String fileName,
        String mimeType,
        Integer fileSize
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String fileId;
        private String fileUniqueId;
        private Integer width;
        private Integer height;
        private Integer duration;
        private PhotoSize thumb;
        private String fileName;
        private String mimeType;
        private Integer fileSize;

        public Builder fileId(String fileId) { this.fileId = fileId; return this; }
        public Builder fileUniqueId(String fileUniqueId) { this.fileUniqueId = fileUniqueId; return this; }
        public Builder width(Integer width) { this.width = width; return this; }
        public Builder height(Integer height) { this.height = height; return this; }
        public Builder duration(Integer duration) { this.duration = duration; return this; }
        public Builder thumb(PhotoSize thumb) { this.thumb = thumb; return this; }
        public Builder fileName(String fileName) { this.fileName = fileName; return this; }
        public Builder mimeType(String mimeType) { this.mimeType = mimeType; return this; }
        public Builder fileSize(Integer fileSize) { this.fileSize = fileSize; return this; }

        public Animation build() { return new Animation(fileId, fileUniqueId, width, height, duration, thumb, fileName, mimeType, fileSize); }
    }
}
