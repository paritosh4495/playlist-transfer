package com.pal.playlist_transfer.dto.youtube.insertPlaylist;


// Represents the snippet part for playlist item insertion


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YoutubePlaylistItemSnippetDto {

    private String playlistId;
    private YoutubeResourceIdDto resourceId;
}
