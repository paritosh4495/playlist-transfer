package com.pal.playlist_transfer.dto.spotify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpotifyTrackItemDto {

    private SpotifyTrackDto track;

    // Note: The /playlists/{id}/tracks endpoint returns a PagingObject
// containing SpotifyTrackItemDto objects in its "items" field.
// So we'll reuse SpotifyPagingObject<SpotifyTrackItemDto>.

}
