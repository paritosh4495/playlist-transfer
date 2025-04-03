package com.pal.playlist_transfer.dto.youtube.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YoutubeSearchListResponseDto {

    private String kind;
    private String etag;
    private String nextPageToken;
    private String previousPageToken;
    private Map<String,Object> pageInfo; /// Contains totalResutls, resultsPerPage
    private List<YoutubeSearchResultDto> items;
}
