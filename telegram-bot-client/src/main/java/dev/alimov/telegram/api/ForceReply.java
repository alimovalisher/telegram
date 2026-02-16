package dev.alimov.telegram.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ForceReply {
    @JsonProperty("force_reply")
    private Boolean forceReply;
    private Boolean selective;
    @JsonProperty("input_field_placeholder")
    private String inputFieldPlaceholder;

    public Boolean getForceReply() { return forceReply; }
    public void setForceReply(Boolean forceReply) { this.forceReply = forceReply; }
    public Boolean getSelective() { return selective; }
    public void setSelective(Boolean selective) { this.selective = selective; }
    public String getInputFieldPlaceholder() { return inputFieldPlaceholder; }
    public void setInputFieldPlaceholder(String inputFieldPlaceholder) { this.inputFieldPlaceholder = inputFieldPlaceholder; }
}
