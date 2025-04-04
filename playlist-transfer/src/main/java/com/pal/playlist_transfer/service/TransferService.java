package com.pal.playlist_transfer.service;

import com.pal.playlist_transfer.dto.spotify.SpotifyFullPlaylistDto;
import com.pal.playlist_transfer.dto.spotify.SpotifyTrackDto;
import com.pal.playlist_transfer.dto.youtube.playlist.YoutubePlaylistResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final SpotifyApiService spotifyApiService;
    private final YoutubeApiService youtubeApiService;

    // Mark this method to run async

    @Async
    public void transferPlaylistAsync(String spotifyPlaylistId) {

        log.info("Starting transfer for Spotify Playlist ID : {} ", spotifyPlaylistId);

        try {
            // 1. Get Spotify Playlist Details (for name/ description)
            SpotifyFullPlaylistDto spotifyPlaylist = spotifyApiService.getPlaylistDetails(spotifyPlaylistId);
            if(spotifyPlaylist == null) {
                log.error("Cannot proceed with transfer: Failed to get details for Spotify playlist {}", spotifyPlaylistId);
                return;
            }
            String playlistTitle = spotifyPlaylist.getName();
            String playlistDescription = spotifyPlaylist.getDescription() != null ? spotifyPlaylist.getDescription() : "Transferred from Spotify";

            // 2. Create Youtube Playlist
            // Using private for now, could be configuable
            YoutubePlaylistResponseDto youtubPlaylist = youtubeApiService.createPlaylist(playlistTitle,playlistDescription,"private");
            if(youtubPlaylist == null) {
                log.error("Cannot proceed with transfer: Failed to create youtube playlist {}", playlistTitle);
                return;
            }

            String youtubePlaylistId = youtubPlaylist.getId();
            log.info("Created Youtube playlist : {} with id : {}", youtubePlaylistId, youtubePlaylistId);

            // 3. Get Spotify Tracks

            List<SpotifyTrackDto> spotifyTracks = spotifyApiService.getPlaylistTracks(spotifyPlaylistId);
            int totalTracks = spotifyTracks.size();
            log.info("Found {} valid tracks in Spotify playlist : {}", totalTracks, spotifyPlaylistId);

            // 4. LOOP -> SEARCH -> ADD

            int successCount = 0;
            int failCount = 0;
            for(int i = 0; i < totalTracks; i++) {
                SpotifyTrackDto track  = spotifyTracks.get(i);
                log.info("[Track {}/{}] Processing: {} by {}", (i+1), totalTracks, track.getName(), track.getArtists().stream().map(a->a.getName()).collect(Collectors.joining(", ")));

                // COnstruct search query ( SIMPLE VERSION )

                String artistName = track.getArtists().isEmpty() ? "" : track.getArtists().get(0).getName();
                String query = track.getName()+" "+artistName;

                try {
                    // 4 a -> Search on yt
                    String videoId = youtubeApiService.searchVideo(query);

                    if(videoId == null) {
                        // 4b Add to Youtube Playlist
                        youtubeApiService.addVideoToPlaylist(youtubePlaylistId,videoId);
                        log.info("[Track {}/{}] Successfully searched and added '{}' (Video ID :{})", (i+1), totalTracks, track.getName(), videoId);
                        successCount++;
                    }
                    else {
                        log.warn("[Track {}/{}] Could not find YouTube video for '{}'. Skipping.", (i + 1), totalTracks, track.getName());
                        failCount++;
                    }
                    // Optional: Add a small delay to avoid hitting rate limits too quickly
                     Thread.sleep(500); // Sleep for 5
                }catch (Exception e) {
                    log.error("[Track {}/{}] Failed to process track '{}': {}", (i + 1), totalTracks, track.getName(), e.getMessage());
                    failCount++;
                    // Decide whether to continue or stop on error - continuing for now

                }
            }
            log.info("Transfer completed for Spotify Playlist ID: {}. Success: {}, Failed/Skipped: {}",
                    spotifyPlaylistId, successCount, failCount);
        } catch (Exception e) {
            log.error("Transfer failed catastrophically for Spotify Playlist ID {}: {}", spotifyPlaylistId, e.getMessage(), e);
            // Handle overall failure - maybe update a status if we had status tracking
        }
    }
}
