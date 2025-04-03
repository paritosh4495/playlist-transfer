package com.pal.playlist_transfer.dto.youtube.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

// Represents the snippet part of a search result
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YoutubeSearchItemSnippetDto {

    private String publishedAt;
    private String channelId;
    private String title;
    private String description;
    private String channelTitle;

}
