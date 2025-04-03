package com.pal.playlist_transfer.dto.spotify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpotifyTrackDto {

    private String id;
    private String name;
    private List<SpotifyArtistDto> artists;
    private SpotifyAlbumDto album;
    @JsonProperty("duration_ms")
    private int durationMs;
    @JsonProperty("is_local")
    private boolean isLocal;
    private String uri;
}
