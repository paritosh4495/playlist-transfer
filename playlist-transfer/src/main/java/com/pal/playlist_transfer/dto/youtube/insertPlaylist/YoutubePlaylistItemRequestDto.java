package com.pal.playlist_transfer.dto.youtube.insertPlaylist;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YoutubePlaylistItemRequestDto {
    private YoutubePlaylistItemSnippetDto snippet;
}
