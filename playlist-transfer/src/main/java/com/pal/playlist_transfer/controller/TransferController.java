package com.pal.playlist_transfer.controller;


import com.pal.playlist_transfer.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {
    private static final Logger log = LoggerFactory.getLogger(TransferController.class);
    private final TransferService transferService;

    @PostMapping("/spotify/{spotifyPlaylistId}")
    public ResponseEntity<String> initiateTransfer(@PathVariable String spotifyPlaylistId) {
        log.info("Received request to initiate transfer for Spotify playlist ID: {}", spotifyPlaylistId);

        // Basic validation
        if (spotifyPlaylistId == null || spotifyPlaylistId.isBlank()) {
            return ResponseEntity.badRequest().body("Spotify Playlist ID must be provided.");
        }

        try {
            // Call the asynchronous service method
            // IMPORTANT: Ensure user is authenticated with BOTH services before calling this
            // We are currently relying on the presence of tokens in the database.
            // A check could be added here to verify both tokens exist for the user before starting.
            transferService.transferPlaylistAsync(spotifyPlaylistId);

            // Return immediately with 202 Accepted
            String message = "Transfer initiated for Spotify playlist ID: " + spotifyPlaylistId + ". Processing will occur in the background.";
            log.info(message);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(message);

        } catch (Exception e) {
            // Catch potential immediate errors (e.g., service bean not found),
            // though errors during the async process won't be caught here.
            log.error("Failed to initiate transfer for Spotify playlist ID {}: {}", spotifyPlaylistId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to initiate transfer: " + e.getMessage());
        }
    }

}
