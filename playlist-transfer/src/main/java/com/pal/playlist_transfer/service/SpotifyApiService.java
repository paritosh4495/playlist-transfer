package com.pal.playlist_transfer.service;

import com.pal.playlist_transfer.dto.spotify.SpotifyPagingObject;
import com.pal.playlist_transfer.dto.spotify.SpotifyPlaylistDto;
import com.pal.playlist_transfer.dto.spotify.SpotifyTrackDto;
import com.pal.playlist_transfer.dto.spotify.SpotifyTrackItemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpotifyApiService {

    private final WebClient webClient;
    private final String SPOTIFY_API_BASE_URL = "https://api.spotify.com/v1";


    public List<SpotifyPlaylistDto> getCurrentUserPlaylists() {

        // The WebClient configured with ServletOAuth2AuthorizedClientExchangeFilterFunction
        // needs context about the current request/user, which it gets implicitly
        // when called within a request thread managed by Spring Security.
        // It will automatically use the token associated with the principal
        // and the "spotify" client registration.
        log.info("Fetching playlists from Spotify for current user...");
        List<SpotifyPlaylistDto> allPlaylists = new ArrayList<>();
        String url = SPOTIFY_API_BASE_URL + "/me/playlists?limit=50";

        while (url!=null){
            log.debug("Requesting playlist from URL : {} ",url);
            try {
                // Define the expected response type (a PagingObject containing Playlist DTOs)
                ParameterizedTypeReference<SpotifyPagingObject<SpotifyPlaylistDto>> responseType =
                        new ParameterizedTypeReference<>() {};
                // Make the blocking call using the configured WebClient

                SpotifyPagingObject<SpotifyPlaylistDto> page = this.webClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(responseType)
                        .block();

                if(page!=null && page.getItems()!=null){
                    allPlaylists.addAll(page.getItems());
                    url = page.getNext();
                    log.debug("Fetched {} playlists, next page URL: {}", page.getItems().size(), url);
                }
                else{
                    log.warn("Recieved null page or null items from spotify at url : {} ",url);
                    url = null;
                }
            } catch (Exception e) {
                // Handle WebClient exceptions (e.g., 4xx/5xx errors, network issues)
                log.error("Error fetching playlists from Spotify URL {}: {}", url, e.getMessage(), e);
                // Depending on the error (e.g., 401 Unauthorized), token refresh might have failed
                // or the token might be invalid. Re-authentication might be needed.
                throw new RuntimeException("Failed to fetch playlists from Spotify: " + e.getMessage(), e);
                // Or return empty list / throw custom exception
            }
        }

        log.info("Successfully fetched {} playlists in total.", allPlaylists.size());
        return allPlaylists;

    }

    public List<SpotifyTrackDto> getPlaylistTracks(String playlistId) {
        log.info("Fetching playlist tracks from Spotify playlist id: {}", playlistId);
        List<SpotifyTrackDto> allTracks = new ArrayList<>();
        // Note: Spotify endpoint includes market param, but often works without for owned playlists
        String url = SPOTIFY_API_BASE_URL + "/playlists/" + playlistId + "/tracks?limit=100"; // Max limit is 100
        while (url != null) {
            log.debug("Requesting tracks from URL: {}", url);
            try {
                // Define the expected response type: PagingObject containing TrackItemDTOs
                ParameterizedTypeReference<SpotifyPagingObject<SpotifyTrackItemDto>> responseType =
                        new ParameterizedTypeReference<>() {};

                // Make the blocking call
                SpotifyPagingObject<SpotifyTrackItemDto> page = this.webClient.get()
                        .uri(url) // Use the full URL directly
                        .retrieve()
                        .bodyToMono(responseType)
                        .block(); // Make synchronous call

                if (page != null && page.getItems() != null) {
                    // Extract the actual Track DTO from the TrackItem DTO
                    // and filter out local tracks which cannot be transferred
                    List<SpotifyTrackDto> tracksInPage = page.getItems().stream()
                            .map(SpotifyTrackItemDto::getTrack)
                            .filter(track -> track != null && !track.isLocal() && track.getId() != null) // Ensure track exists, is not local, and has an ID
                            .toList();

                    allTracks.addAll(tracksInPage);
                    url = page.getNext(); // Get URL for the next page
                    log.debug("Fetched {} valid tracks, next page URL: {}", tracksInPage.size(), url);
                } else {
                    log.warn("Received null page or null items from Spotify for playlist tracks at URL: {}", url);
                    url = null; // Stop looping
                }
            } catch (Exception e) {
                log.error("Error fetching tracks for playlist {} from URL {}: {}", playlistId, url, e.getMessage(), e);
                // Consider specific error handling (e.g., 404 Not Found for playlistId)
                throw new RuntimeException("Failed to fetch tracks for playlist " + playlistId + ": " + e.getMessage(), e);
            }
        }
        log.info("Successfully fetched {} valid, non-local tracks for playlist ID: {}.", allTracks.size(), playlistId);
        return allTracks;
    }
}
