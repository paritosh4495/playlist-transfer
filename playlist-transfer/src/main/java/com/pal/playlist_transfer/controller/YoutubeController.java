package com.pal.playlist_transfer.controller;

import com.pal.playlist_transfer.dto.youtube.playlist.YoutubePlaylistResponseDto;
import com.pal.playlist_transfer.service.YoutubeApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/youtube")
@RequiredArgsConstructor
public class YoutubeController {
    private final YoutubeApiService youtubeApiService;

    @PostMapping("/playlists")
    public ResponseEntity<YoutubePlaylistResponseDto> createYoutubePlaylist(
            @RequestParam String title,
            @RequestParam(required = false, defaultValue = "") String description,
            @RequestParam(required = false, defaultValue = "private") String privacy // "private", "public", "unlisted"
    ) {
        try {
            YoutubePlaylistResponseDto createdPlaylist = youtubeApiService.createPlaylist(title, description, privacy);
            return ResponseEntity.ok(createdPlaylist);
        } catch (Exception e) {
            // Log error maybe
            return ResponseEntity.internalServerError().body(null); // Or proper error DTO
        }
    }



    @GetMapping("/search")
    public ResponseEntity<String> searchYoutubeVideo(@RequestParam String query) {
        try {
            String videoId = youtubeApiService.searchVideo(query);
            if (videoId != null) {
                return ResponseEntity.ok(videoId);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error during search: " + e.getMessage());
        }
    }
}

