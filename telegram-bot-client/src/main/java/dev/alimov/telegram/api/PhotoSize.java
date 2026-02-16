package dev.alimov.telegram.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PhotoSize(
        String fileId,
        String fileUniqueId,
        Integer width,
        Integer height,
        Integer fileSize
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String fileId;
        private String fileUniqueId;
        private Integer width;
        private Integer height;
        private Integer fileSize;

        public Builder fileId(String fileId) { this.fileId = fileId; return this; }
        public Builder fileUniqueId(String fileUniqueId) { this.fileUniqueId = fileUniqueId; return this; }
        public Builder width(Integer width) { this.width = width; return this; }
        public Builder height(Integer height) { this.height = height; return this; }
        public Builder fileSize(Integer fileSize) { this.fileSize = fileSize; return this; }

        public PhotoSize build() { return new PhotoSize(fileId, fileUniqueId, width, height, fileSize); }
    }
}
