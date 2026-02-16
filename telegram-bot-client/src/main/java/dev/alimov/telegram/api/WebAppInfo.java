package dev.alimov.telegram.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WebAppInfo {
    private String url;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
