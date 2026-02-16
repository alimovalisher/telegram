package dev.alimov.telegram.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ReplyKeyboardRemove {
    @JsonProperty("remove_keyboard")
    private Boolean removeKeyboard;
    private Boolean selective;

    public Boolean getRemoveKeyboard() { return removeKeyboard; }
    public void setRemoveKeyboard(Boolean removeKeyboard) { this.removeKeyboard = removeKeyboard; }
    public Boolean getSelective() { return selective; }
    public void setSelective(Boolean selective) { this.selective = selective; }
}
