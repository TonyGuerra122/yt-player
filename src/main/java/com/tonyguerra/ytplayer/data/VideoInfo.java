package com.tonyguerra.ytplayer.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VideoInfo(
        String url,
        String title,
        @JsonProperty("uploader") String author,
        float duration,
        String description,
        String thumbnail) {

    // Removed invalid instance field declaration
}
