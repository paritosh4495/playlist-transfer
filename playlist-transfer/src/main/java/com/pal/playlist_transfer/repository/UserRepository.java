package com.pal.playlist_transfer.repository;

import com.pal.playlist_transfer.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
//    Optional<User> findByUsername(String username);
    Optional<User> findBySpotifyId(String spotifyId);
    Optional<User> findByGoogleId(String googleId);
    Optional<User> findByEmail(String email);
}
