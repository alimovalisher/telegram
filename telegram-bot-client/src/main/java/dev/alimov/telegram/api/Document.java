package dev.alimov.telegram.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Document(
        @JsonProperty("file_id") String fileId,
        @JsonProperty("file_unique_id") String fileUniqueId,
        PhotoSize thumb,
        String fileName,
        String mimeType,
        Integer fileSize
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String fileId;
        private String fileUniqueId;
        private PhotoSize thumb;
        private String fileName;
        private String mimeType;
        private Integer fileSize;

        public Builder fileId(String fileId) { this.fileId = fileId; return this; }
        public Builder fileUniqueId(String fileUniqueId) { this.fileUniqueId = fileUniqueId; return this; }
        public Builder thumb(PhotoSize thumb) { this.thumb = thumb; return this; }
        public Builder fileName(String fileName) { this.fileName = fileName; return this; }
        public Builder mimeType(String mimeType) { this.mimeType = mimeType; return this; }
        public Builder fileSize(Integer fileSize) { this.fileSize = fileSize; return this; }

        public Document build() { return new Document(fileId, fileUniqueId, thumb, fileName, mimeType, fileSize); }
    }
}
