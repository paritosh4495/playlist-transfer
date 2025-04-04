package com.pal.playlist_transfer.dto.youtube.insertPlaylist;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YoutubePlaylistItemResponseDto {
    private String kind;
    private String etag;
    private String id;
}
