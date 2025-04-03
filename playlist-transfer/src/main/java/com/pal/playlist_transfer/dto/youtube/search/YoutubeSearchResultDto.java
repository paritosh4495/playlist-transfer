package com.pal.playlist_transfer.dto.youtube.search;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

// Represents a single item in the search results list

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YoutubeSearchResultDto {

    private String kind;
    private String etag;
    private YoutubeSearchItemIdDto id;
    private YoutubeSearchItemSnippetDto snippet;
}
