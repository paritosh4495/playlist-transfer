package com.pal.playlist_transfer.service;

import com.pal.playlist_transfer.dto.youtube.insertPlaylist.YoutubePlaylistItemRequestDto;
import com.pal.playlist_transfer.dto.youtube.insertPlaylist.YoutubePlaylistItemResponseDto;
import com.pal.playlist_transfer.dto.youtube.insertPlaylist.YoutubePlaylistItemSnippetDto;
import com.pal.playlist_transfer.dto.youtube.insertPlaylist.YoutubeResourceIdDto;
import com.pal.playlist_transfer.dto.youtube.playlist.YoutubePlaylistRequestDto;
import com.pal.playlist_transfer.dto.youtube.playlist.YoutubePlaylistResponseDto;
import com.pal.playlist_transfer.dto.youtube.playlist.YoutubePlaylistSnippetDto;
import com.pal.playlist_transfer.dto.youtube.playlist.YoutubePlaylistStatusDto;
import com.pal.playlist_transfer.dto.youtube.search.YoutubeSearchListResponseDto;
import com.pal.playlist_transfer.dto.youtube.search.YoutubeSearchResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class YoutubeApiService {

    private final WebClient webClient;
    private final String YOUTUBE_API_BASE_URL = "https://www.googleapis.com/youtube/v3";


    /**
     * Creates a new YouTube playlist for the authenticated user.
     * @param title The desired title for the playlist.
     * @param description A description for the playlist.
     * @param privacyStatus "public", "private", or "unlisted".
     * @return YoutubePlaylistResponseDto containing details of the created playlist (including its ID).
     */

    public YoutubePlaylistResponseDto createPlaylist(String title, String description, String privacyStatus) {

        log.info("Attempting to create youtube playlist with titel : {} ", title);

        // Construct the request body
        YoutubePlaylistSnippetDto snippet = new YoutubePlaylistSnippetDto(title, description);
        YoutubePlaylistStatusDto status = new YoutubePlaylistStatusDto(privacyStatus);
        YoutubePlaylistRequestDto requestBody = new YoutubePlaylistRequestDto(snippet, status);

        try {
            // MAKE THE POST requeest using the configured WebClient
            // The filter funtion automatically adds the Google token

            YoutubePlaylistResponseDto response = this.webClient.post()
                    .uri(YOUTUBE_API_BASE_URL+"/playlists?part=snippet,status")
                    .bodyValue(requestBody)
            // Explicitly tell the WebClient filter to use the "google" registration
            // This is good practice if the default isn't guaranteed or if multiple clients exist.
                    .attributes(ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId("google"))
                    .retrieve()
                    .bodyToMono(YoutubePlaylistResponseDto.class)
                    .block();
            if (response != null && response.getId() != null) {
                log.info("Successfully created YouTube playlist '{}' with ID: {}", response.getSnippet().getTitle(), response.getId());
                return response;
            } else {
                log.error("Failed to create YouTube playlist '{}'. Response or ID was null.", title);
                throw new RuntimeException("Failed to create YouTube playlist '" + title + "'. Response or ID was null.");
            }
        } catch (Exception e) {
            log.error("Error creating YouTube playlist '{}': {}", title, e.getMessage(), e);
            // Handle potential errors like quota limits (403), invalid request (400), etc.
            throw new RuntimeException("Error creating YouTube playlist '" + title + "': " + e.getMessage(), e);
        }


    }


    /**
     * Searches YouTube for videos matching the query.
     * Aims to find the best match for a given song.
     * @param query The search query (e.g., "Track Name Artist Name")
     * @return The video ID (String) of the top search result, or null if no results.
     */

    public String searchVideo(String query){
        log.info("Searching Youtube for query : {} ",query);

        try {
            // Call the search.list endpoint

            YoutubeSearchListResponseDto response = this.webClient.get()
                    .uri(YOUTUBE_API_BASE_URL +"/search", uriBuilder -> uriBuilder

                            .queryParam("part","snippet") // Basic Details
                            .queryParam("q",query) // The Search query itself
                            .queryParam("type","video") // WE ONly want videos
                            .queryParam("maxResults",1) // Get only the top result for now
                            .queryParam("videoCategoryId","10") //"10" is the category ID for Music (optional, but helps)
                            .build())
                    .attributes(ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId("google")) //Use Google token
                    .retrieve()
                    .bodyToMono(YoutubeSearchListResponseDto.class)
                    .block();

            if(response!=null && response.getItems()!=null && response.getItems().size() > 0){
                // Get the first search result
                YoutubeSearchResultDto topResult = response.getItems().get(0);
                if(topResult.getId()!=null && "youtube#video".equals(topResult.getId().getKind())){
                    String videoId = topResult.getId().getVideoId();
                    log.info("Found vido for query {}, Video ID: {}, Title: {}", query, videoId,topResult.getSnippet().getTitle());
                    return videoId;
                }
                else{
                    log.warn("Top search result for query {} was not a video or had no ID", query);
                }

            }
            else {
                log.warn("No Youtube results found for query {}", query);
            }
        }
        catch (Exception e) {
            log.error("Error searching Youtube for query {} : {}", query, e.getMessage(), e);
            // Don't necessarily throw exception here, failing to find a song is expected sometimes
            // throw new RuntimeException("Error searching YouTube for query '" + query + "': " + e.getMessage(), e);
        }
        return null;
    }


    /**
     * Adds a video to a specific YouTube playlist.
     * @param playlistId The ID of the target YouTube playlist.
     * @param videoId The ID of the YouTube video to add.
     * @return YoutubePlaylistItemResponseDto representing the added item.
     */


    public YoutubePlaylistItemResponseDto addVideoToPlaylist(String playlistId, String videoId) {
        log.info("Attempting to add video ID: {} to YouTube playlist ID: {}", videoId, playlistId);

        // Construct the request body
        YoutubeResourceIdDto resourceId = new YoutubeResourceIdDto("youtube#video", videoId);
        YoutubePlaylistItemSnippetDto snippet = new YoutubePlaylistItemSnippetDto(playlistId, resourceId);
        YoutubePlaylistItemRequestDto requestBody = new YoutubePlaylistItemRequestDto(snippet);

        try {
            // Make the POST request to playlistItems.insert
            YoutubePlaylistItemResponseDto response = this.webClient.post()
                    .uri(YOUTUBE_API_BASE_URL + "/playlistItems?part=snippet") // API endpoint and parts
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .attributes(ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId("google")) // Use Google token
                    .retrieve()
                    .bodyToMono(YoutubePlaylistItemResponseDto.class) // Expect response DTO
                    .block(); // Synchronous call

            if (response != null && response.getId() != null) {
                log.info("Successfully added video ID: {} to playlist ID: {}. New item ID: {}", videoId, playlistId, response.getId());
                return response;
            } else {
                log.error("Failed to add video ID: {} to playlist ID: {}. Response or ID was null.", videoId, playlistId);
                // This could happen for various reasons, e.g., duplicate video?
                throw new RuntimeException("Failed to add video " + videoId + " to playlist " + playlistId + ". Response or ID was null.");
            }

        } catch (Exception e) {
            // Handle potential errors:
            // 400 Bad Request (invalid videoId/playlistId format?)
            // 403 Forbidden (quota exceeded? user doesn't own playlist?)
            // 404 Not Found (playlistId or videoId doesn't exist?)
            // 409 Conflict (Video already in playlist?) - Need to check actual error code/reason from Google
            log.error("Error adding video ID: {} to playlist ID: {}: {}", videoId, playlistId, e.getMessage(), e);

            // Check if it's a known "duplicate" error (often a 409 Conflict)
            // if (e instanceof WebClientResponseException && ((WebClientResponseException) e).getStatusCode().value() == 409) {
            //    log.warn("Video {} might already be in playlist {}. Skipping.", videoId, playlistId);
            //    return null; // Or return a specific status/object indicating duplication
            // }

            throw new RuntimeException("Error adding video " + videoId + " to playlist " + playlistId + ": " + e.getMessage(), e);
        }
    }




}
