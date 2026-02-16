package dev.alimov.telegram.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Sticker {
    @JsonProperty("file_id")
    private String fileId;
    @JsonProperty("file_unique_id")
    private String fileUniqueId;
    private Integer width;
    private Integer height;
    private Boolean isAnimated;
    private Boolean isVideo;
    private PhotoSize thumb;
    private String emoji;
    private String setName;
    @JsonProperty("file_size")
    private Integer fileSize;

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }
    public String getFileUniqueId() { return fileUniqueId; }
    public void setFileUniqueId(String fileUniqueId) { this.fileUniqueId = fileUniqueId; }
    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
    public Boolean getAnimated() { return isAnimated; }
    public void setAnimated(Boolean animated) { isAnimated = animated; }
    public Boolean getVideo() { return isVideo; }
    public void setVideo(Boolean video) { isVideo = video; }
    public PhotoSize getThumb() { return thumb; }
    public void setThumb(PhotoSize thumb) { this.thumb = thumb; }
    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }
    public String getSetName() { return setName; }
    public void setSetName(String setName) { this.setName = setName; }
    public Integer getFileSize() { return fileSize; }
    public void setFileSize(Integer fileSize) { this.fileSize = fileSize; }
}
