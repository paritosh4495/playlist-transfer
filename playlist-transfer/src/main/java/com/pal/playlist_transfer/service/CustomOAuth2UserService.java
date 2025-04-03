package com.pal.playlist_transfer.service;

import com.pal.playlist_transfer.model.User;
import com.pal.playlist_transfer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest; // Import OidcUserRequest
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService; // Import OidcUserService
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser; // Import OidcUser
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Use Spring's Transactional

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);
    private final UserRepository userRepository;

    // --- Standard OAuth2 Handling ---
    @Override
    @Transactional // Use Spring's annotation
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("LOAD USER (OAuth2 Standard) TRIGGERED for registrationId: {}", userRequest.getClientRegistration().getRegistrationId());

        // 1. Delegate to the default implementation to get the OAuth2User
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oauth2User = delegate.loadUser(userRequest);

        // 2. Process the user information
        processOAuthUser(userRequest.getClientRegistration().getRegistrationId(), oauth2User);

        return oauth2User;
    }

    // --- OIDC Handling ---
    // We need a method that matches the OidcUserService functional interface
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("LOAD USER (OIDC) TRIGGERED for registrationId: {}", userRequest.getClientRegistration().getRegistrationId());

        // 1. Delegate to the default OIDC service to get the OidcUser
        OidcUserService delegate = new OidcUserService();
        OidcUser oidcUser = delegate.loadUser(userRequest);

        // 2. Process the user information (OidcUser extends OAuth2User)
        processOAuthUser(userRequest.getClientRegistration().getRegistrationId(), oidcUser);

        return oidcUser;
    }

    // --- Common Processing Logic ---
    private void processOAuthUser(String registrationId, OAuth2User oauth2User) {
        Map<String, Object> attributes = oauth2User.getAttributes();
        String providerUserId = null;
        String displayName = null;
        String email = null;

        log.debug("Processing user for registrationId: {}", registrationId); // Add debug log

        if ("spotify".equalsIgnoreCase(registrationId)) {
            providerUserId = oauth2User.getAttribute("id");
            displayName = oauth2User.getAttribute("display_name");
            email = oauth2User.getAttribute("email");
            log.info("Spotify User Details: id={}, displayName={}, email={}", providerUserId, displayName, email);

        } else if ("google".equalsIgnoreCase(registrationId)) {
            // **** IMPORTANT: Use 'sub' for Google's unique ID ****
            providerUserId = oauth2User.getAttribute("sub");
            displayName = oauth2User.getAttribute("name");
            email = oauth2User.getAttribute("email");
            log.info("Google User Details (OIDC): sub={}, name={}, email={}", providerUserId, displayName, email);

        } else {
            log.warn("Unsupported OAuth2 provider encountered: {}", registrationId);
            // Decide how to handle - skip DB update or throw exception
            return; // Skip DB processing for unknown providers
        }

        if (providerUserId == null || providerUserId.trim().isEmpty()) {
            log.error("Could not extract valid provider user ID for registrationId: {}. Attributes: {}", registrationId, attributes);
            // Decide how to handle - skip DB update or throw exception
            return; // Skip DB processing if ID is missing
        }

        // Call the database update logic
        updateUserDatabase(providerUserId, registrationId, displayName, email);
    }

    // --- Database Update Logic ---
    // (Keep your existing updateUserDatabase method, ensuring case-insensitive checks for "google" and "spotify")
    private User updateUserDatabase(String providerUserId, String provider, String displayName, String email) {
        Optional<User> userOptional;

        log.debug("Attempting DB update for provider: {}, providerUserId: {}", provider, providerUserId);

        // Use equalsIgnoreCase consistently
        if ("spotify".equalsIgnoreCase(provider)) {
            userOptional = userRepository.findBySpotifyId(providerUserId);
        } else if ("google".equalsIgnoreCase(provider)) {
            userOptional = userRepository.findByGoogleId(providerUserId);
        } else {
            log.warn("Provider '{}' not recognized in updateUserDatabase.", provider);
            return null; // Should not happen if processOAuthUser filters first
        }

        User appUser;
        if (userOptional.isPresent()) {
            // User exists for this specific provider ID
            appUser = userOptional.get();
            log.info("Found existing user by providerId. User ID: {}, Provider: {}", appUser.getId(), provider);
            boolean updated = false;
            // Update logic (ensure null checks and avoid overwriting with nulls unnecessarily)
            if (displayName != null && !displayName.equals(appUser.getDisplayName())) {
                appUser.setDisplayName(displayName);
                updated = true;
            }
            if (email != null && email.length() > 0 && (appUser.getEmail() == null || !email.equals(appUser.getEmail()))) {
                // Consider policy: Only update email if current one is null? Or always update?
                // Let's update if different and not null/empty for now.
                appUser.setEmail(email);
                updated = true;
            }
            // Update provider IDs if somehow they were null before (shouldn't normally happen here)
            if ("spotify".equalsIgnoreCase(provider) && appUser.getSpotifyId() == null) {
                appUser.setSpotifyId(providerUserId);
                updated = true;
            } else if ("google".equalsIgnoreCase(provider) && appUser.getGoogleId() == null) {
                appUser.setGoogleId(providerUserId);
                updated = true;
            }

            if (updated) {
                log.info("Updating existing user ID {} for provider {}", appUser.getId(), provider);
                appUser = userRepository.save(appUser);
            } else {
                log.info("No updates needed for existing user ID {} with provider {}", appUser.getId(), provider);
            }
        } else {
            // No user found for this specific provider ID. Check by email for account linking.
            log.info("No user found for providerId: {}. Checking by email: {}", providerUserId, email);
            Optional<User> existingUserByEmail = (email != null && email.length() > 0) ? userRepository.findByEmail(email) : Optional.empty();

            if (existingUserByEmail.isPresent()) {
                // Found user with same email via different provider -> Link accounts
                appUser = existingUserByEmail.get();
                log.info("Found existing user ID {} via email {}. Linking provider {}", appUser.getId(), email, provider);

                // Add the missing provider ID
                if ("spotify".equalsIgnoreCase(provider)) {
                    if(appUser.getSpotifyId() == null) { // Only set if not already set
                        appUser.setSpotifyId(providerUserId);
                    } else if (!providerUserId.equals(appUser.getSpotifyId())) {
                        log.warn("Attempting to link Spotify ID {} but user {} already has Spotify ID {}", providerUserId, appUser.getId(), appUser.getSpotifyId());
                        // Handle this conflict case as needed - maybe throw error or ignore
                    }
                } else if ("google".equalsIgnoreCase(provider)) {
                    if(appUser.getGoogleId() == null) { // Only set if not already set
                        appUser.setGoogleId(providerUserId);
                    } else if (!providerUserId.equals(appUser.getGoogleId())) {
                        log.warn("Attempting to link Google ID {} but user {} already has Google ID {}", providerUserId, appUser.getId(), appUser.getGoogleId());
                        // Handle this conflict case as needed
                    }
                }

                // Update display name if the new provider's one is more complete? (Optional logic)
                if (displayName != null && (appUser.getDisplayName() == null || appUser.getDisplayName().isEmpty() || !displayName.equals(appUser.getDisplayName()))) {
                    appUser.setDisplayName(displayName);
                }
                appUser = userRepository.save(appUser);
            } else {
                // Completely new user - neither provider ID nor email matched
                log.info("Creating completely new user for provider {}: providerId={}, displayName={}, email={}", provider, providerUserId, displayName, email);
                appUser = new User(); // Use default constructor
                appUser.setEmail(email);
                appUser.setDisplayName(displayName);
                if ("spotify".equalsIgnoreCase(provider)) {
                    appUser.setSpotifyId(providerUserId);
                } else if ("google".equalsIgnoreCase(provider)) {
                    appUser.setGoogleId(providerUserId);
                }
                appUser = userRepository.save(appUser);
            }
        }
        return appUser;
    }
}