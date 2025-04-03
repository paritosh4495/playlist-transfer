package com.pal.playlist_transfer.dto.youtube.playlist;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YoutubePlaylistResponseDto {

    private String kind;
    private String etag;
    private String id;
    private YoutubePlaylistSnippetDto snippet;
    private YoutubePlaylistStatusDto status;
}
