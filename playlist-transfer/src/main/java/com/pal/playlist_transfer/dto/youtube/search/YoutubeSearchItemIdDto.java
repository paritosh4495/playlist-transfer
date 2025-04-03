package com.pal.playlist_transfer.dto.youtube.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YoutubeSearchItemIdDto {

    private String kind;
    private String videoId;
}

