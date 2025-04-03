package com.pal.playlist_transfer.controller;

import com.pal.playlist_transfer.dto.spotify.SpotifyPlaylistDto;
import com.pal.playlist_transfer.dto.spotify.SpotifyTrackDto;
import com.pal.playlist_transfer.service.SpotifyApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/spotify")
@RequiredArgsConstructor
public class SpotifyController {

    private final SpotifyApiService spotifyApiService;

    @GetMapping("/playlists")
    public ResponseEntity<List<SpotifyPlaylistDto>> getCurrentUserPlaylists() {
        // Ensure user is authenticated before calling this
        try {
            List<SpotifyPlaylistDto> playlists = spotifyApiService.getCurrentUserPlaylists();
            return ResponseEntity.ok(playlists);
        } catch (Exception e) {
            // Handle exceptions from the service layer (e.g., API call failures)
            // Return an appropriate error response (e.g., 500 Internal Server Error)
            return ResponseEntity.internalServerError().body(null); // Or a proper error DTO
        }
    }

    @GetMapping("/playlists/{playlistId}/tracks")
    public ResponseEntity<List<SpotifyTrackDto>> getPlaylistTracks(
            @PathVariable String playlistId) { // Get playlistId from URL path
        try {
            if (playlistId == null || playlistId.isBlank()) {
                return ResponseEntity.badRequest().body(null); // Basic validation
            }
            List<SpotifyTrackDto> tracks = spotifyApiService.getPlaylistTracks(playlistId);
            return ResponseEntity.ok(tracks);
        } catch (Exception e) {
            // More specific error handling could be added (e.g., PlaylistNotFoundException)
            return ResponseEntity.internalServerError().body(null);
        }
    }
}
