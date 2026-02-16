package dev.alimov.telegram.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Audio(
        @JsonProperty("file_id") String fileId,
        @JsonProperty("file_unique_id") String fileUniqueId,
        Integer duration,
        String performer,
        String title,
        String mimeType,
        Integer fileSize
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String fileId;
        private String fileUniqueId;
        private Integer duration;
        private String performer;
        private String title;
        private String mimeType;
        private Integer fileSize;

        public Builder fileId(String fileId) { this.fileId = fileId; return this; }
        public Builder fileUniqueId(String fileUniqueId) { this.fileUniqueId = fileUniqueId; return this; }
        public Builder duration(Integer duration) { this.duration = duration; return this; }
        public Builder performer(String performer) { this.performer = performer; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder mimeType(String mimeType) { this.mimeType = mimeType; return this; }
        public Builder fileSize(Integer fileSize) { this.fileSize = fileSize; return this; }

        public Audio build() { return new Audio(fileId, fileUniqueId, duration, performer, title, mimeType, fileSize); }
    }
}
