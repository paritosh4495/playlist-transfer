package com.pal.playlist_transfer.dto.spotify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpotifyAlbumDto {
    private String name;
    private String id;
    private List<SpotifyArtistDto> artists;
}
