package com.pal.playlist_transfer.dto.youtube.playlist;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YoutubePlaylistRequestDto {
    private YoutubePlaylistSnippetDto snippet;
    private YoutubePlaylistStatusDto status;
}
