package com.pal.playlist_transfer.dto.spotify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpotifyPagingObject<T> {
    private List<T> items;
    private int limit;
    private String next; // URL for the next page, null if none
    private int offset;
    private String previous; // URL for the previous page, null if none
    private int total;
}



