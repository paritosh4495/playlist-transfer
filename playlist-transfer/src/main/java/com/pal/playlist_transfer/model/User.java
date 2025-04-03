package com.pal.playlist_transfer.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "app_users")
@Data
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = true)
    private String spotifyId;

    @Column(unique = true, nullable = true)
    private String googleId;

    private String email; // email, cannot get from OAuth because will be different for spoitfy and Yotube

    @Column(nullable = false)
    private String displayName;


    public User(String providerId,String provider, String displayName, String email) {
        this.displayName = displayName;
        this.email = email;
        if("spotify".equalsIgnoreCase(provider)){
            this.spotifyId = providerId;
        } else if("Google".equalsIgnoreCase(provider)){
            this.googleId = provider;
        }
    }


    // We Will add @OneToOne mappings here later if we switch to custom token entities ( we will switch to custom token entities)
    // @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    // private SpotifyToken spotifyToken;
    //
    // @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    // private YoutubeToken youtubeToken;
}
