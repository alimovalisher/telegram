package dev.alimov.telegram.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record File(
        @JsonProperty("file_id") String fileId,
        @JsonProperty("file_unique_id") String fileUniqueId,
        @JsonProperty("file_size") Integer fileSize,
        @JsonProperty("file_path") String filePath
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String fileId;
        private String fileUniqueId;
        private Integer fileSize;
        private String filePath;

        public Builder fileId(String fileId) { this.fileId = fileId; return this; }
        public Builder fileUniqueId(String fileUniqueId) { this.fileUniqueId = fileUniqueId; return this; }
        public Builder fileSize(Integer fileSize) { this.fileSize = fileSize; return this; }
        public Builder filePath(String filePath) { this.filePath = filePath; return this; }

        public File build() { return new File(fileId, fileUniqueId, fileSize, filePath); }
    }
}
