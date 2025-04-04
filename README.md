# Spotify to YouTube Playlist Transfer (Backend) 🎧➡️🎬

A Spring Boot backend application designed to transfer user playlists from Spotify to YouTube Music. This application handles authentication with both services using OAuth 2.0 and provides API endpoints to manage the transfer process.

## Overview ✨

This project allows users to authenticate their Spotify and Google (YouTube) accounts and then initiate a transfer of a selected Spotify playlist. The application fetches track details from Spotify, searches for corresponding music videos on YouTube, creates a new playlist on YouTube with the same name, and adds the found videos. The core transfer logic runs asynchronously.

**Core Technologies:**

* Java 21
* Spring Boot (v3.3.x)
* Spring Security (OAuth 2.0 Client, Session Management)
* Spring Data JPA / Hibernate
* Spring Web / WebClient
* PostgreSQL Database
* Lombok
* Spotify Web API
* YouTube Data API v3

## Features 🚀

* **OAuth 2.0 Authentication:** Securely connect to Spotify and Google (for YouTube) accounts.
* **Account Linking:** Automatically links Spotify and Google logins based on matching email addresses (if available and matching). Creates separate internal user accounts if emails differ.
* **Token Management:** Persistently stores OAuth access and refresh tokens in the database (using Spring Security's default JDBC storage). Handles token refresh automatically where possible via `OAuth2AuthorizedClientManager`.
* **List Spotify Playlists:** API endpoint to fetch the authenticated user's Spotify playlists.
* **Get Spotify Playlist Tracks:** API endpoint to retrieve detailed track information (name, artists, album, duration) for a specific Spotify playlist, filtering out local tracks.
* **Create YouTube Playlist:** Creates a new private playlist on the authenticated user's YouTube account.
* **Search YouTube Videos:** Searches YouTube for music videos based on Spotify track name and artist.
* **Add Videos to YouTube Playlist:** Adds found YouTube videos to the newly created YouTube playlist.
* **Asynchronous Transfer:** The main playlist transfer process runs in a background thread (`@Async`) so the initial API request returns quickly.
* **Basic API Testing Support:** Works with tools like Postman using browser-based login and `JSESSIONID` cookies.

## Technologies Used 🛠️

* **Framework:** Spring Boot 3.3.x
* **Language:** Java 21
* **Build Tool:** Maven 
* **Security:** Spring Security 6.x (OAuth 2.0 Client, Web Security)
* **Data:** Spring Data JPA, Hibernate, Spring JDBC
* **Database:** PostgreSQL
* **HTTP Client:** Spring WebClient (from `spring-webflux` module)
* **Utility:** Lombok
* **External APIs:**
    * Spotify Web API
    * YouTube Data API v3
* **Async:** Spring `@Async` with custom `ThreadPoolTaskExecutor` and `DelegatingSecurityContextAsyncTaskExecutor`.

## Prerequisites 📋

* **Java Development Kit (JDK):** Version 21 or higher.
* **Build Tool:** Apache Maven 3.8+ (or compatible Gradle version).
* **Database:** PostgreSQL server running locally or accessible.
* **Spotify Developer Account:** To register an application and get API credentials.
* **Google Cloud Platform Account:** To enable the YouTube Data API v3 and get OAuth 2.0 credentials.

## Setup & Configuration ⚙️

1.  **Clone the Repository:**
    ```bash
    git clone <https://github.com/paritosh4495/playlist-transfer>
    cd <playlist-transfer>
    ```

2.  **Database Setup:**
    * Ensure your PostgreSQL server is running.
    * Create a database for the application (e.g., `playlist_transfer_db`).
    * The necessary tables (`app_users`, `oauth2_authorized_client`) will be created/updated automatically by Hibernate based on the `ddl-auto` setting (currently configured as `update`).
    * *(Note: During development, we encountered an issue where `oauth2_authorized_client` wasn't created automatically. If this occurs, manually create it using the SQL DDL found in Spring Security documentation or provided during troubleshooting, and set `ddl-auto: validate`.)*

3.  **API Credentials:**

    * **Spotify:**
        * Go to the [Spotify Developer Dashboard](https://developer.spotify.com/dashboard/).
        * Create a new application.
        * Note down your `Client ID` and `Client Secret`.
        * Go to "Edit Settings" -> "Redirect URIs". Add `http://localhost:8080/login/oauth2/code/spotify`.
        * Ensure you have requested the necessary **scopes**: `playlist-read-private`, `playlist-read-collaborative`, `user-read-email`.

    * **Google (YouTube):**
        * Go to the [Google Cloud Console](https://console.cloud.google.com/).
        * Create a new project or select an existing one.
        * Enable the **"YouTube Data API v3"**.
        * Go to "APIs & Services" -> "Credentials".
        * Click "Create Credentials" -> "OAuth client ID".
        * Select "Web application" as the application type.
        * Under "Authorized redirect URIs", add `http://localhost:8080/login/oauth2/code/google`.
        * Click "Create" and note down your `Client ID` and `Client Secret`.
        * Ensure you have requested the necessary **scopes**: `openid`, `profile`, `email`, `https://www.googleapis.com/auth/youtube`.

4.  **Application Configuration (`application.yml`):**

    Configure your database connection and API credentials. It's **highly recommended** to use environment variables for secrets rather than hardcoding them. The application uses `${VAR_NAME:default_value}` syntax.

    ```yaml
    # src/main/resources/application.yml

    spring:
      datasource:
        url: jdbc:postgresql://localhost:5000/db # Adjust port and DB name if needed
        username: ${DB_USERNAME:your_db_user} # Use env var or replace default
        password: ${DB_PASSWORD:your_db_password} # Use env var or replace default
        driver-class-name: org.postgresql.Driver
      jpa:
        hibernate:
          ddl-auto: update # Use 'update' or 'validate'. 'create' drops data on restart.
        properties:
          hibernate:
            dialect: org.hibernate.dialect.PostgreSQLDialect # Optional, usually auto-detected
        show-sql: true # Set to false in production

      security:
        oauth2:
          client:
            registration:
              spotify:
                client-id: ${SPOTIFY_CLIENT_ID:YOUR_SPOTIFY_CLIENT_ID} # Use env var or replace default
                client-secret: ${SPOTIFY_CLIENT_SECRET:YOUR_SPOTIFY_CLIENT_SECRET} # Use env var or replace default
                scope:
                  - playlist-read-private
                  - playlist-read-collaborative
                  - user-read-email
                authorization-grant-type: authorization_code
                redirect-uri: http://localhost:8080/login/oauth2/code/spotify
                # provider: spotify # Usually not needed if registration ID matches common provider

              google:
                client-id: ${GOOGLE_CLIENT_ID:YOUR_GOOGLE_CLIENT_ID} # Use env var or replace default
                client-secret: ${GOOGLE_CLIENT_SECRET:YOUR_GOOGLE_CLIENT_SECRET} # Use env var or replace default
                scope:
                  - openid
                  - profile
                  - email
                  - [https://www.googleapis.com/auth/youtube](https://www.googleapis.com/auth/youtube)
                authorization-grant-type: authorization_code
                redirect-uri: http://localhost:8080/login/oauth2/code/google

            provider: # Provider details usually only needed if not standard (like Spotify)
              spotify:
                authorization-uri: [https://accounts.spotify.com/authorize](https://accounts.spotify.com/authorize)
                token-uri: [https://accounts.spotify.com/api/token](https://accounts.spotify.com/api/token)
                user-info-uri: [https://api.spotify.com/v1/me](https://api.spotify.com/v1/me)
                user-name-attribute: id

    logging:
      level:
        root: INFO
        com.pal.playlist_transfer: DEBUG # Your base package to DEBUG
        org.springframework.security: INFO # Set to DEBUG for verbose security logs
        org.hibernate.SQL: DEBUG # If show-sql: false, logs SQL via logger
        org.hibernate.type.descriptor.sql: TRACE # Logs PreparedStatement parameters

    # Ensure you have set the environment variables (DB_USERNAME, DB_PASSWORD, SPOTIFY_CLIENT_ID, etc.)
    # OR replace the default values above before running.
    # DO NOT COMMIT YOUR SECRETS TO GIT!
    ```

## Running the Application 🚀

1.  **Build (Optional):**
    ```bash
    mvn clean package
    ```
2.  **Run:**
    * Using Maven:
        ```bash
        mvn spring-boot:run
        ```
    * Or run the main application class (`PlaylistTransferApplication.java`) directly from your IDE.

The application will start on `http://localhost:8080`.

## Usage / API Endpoints 🧭

This is a backend-only application. Interaction happens via API endpoints, typically after authenticating through a browser flow first.

### 1. Authentication Flow (Browser Required)

* **Initiate Spotify Login:** Navigate your browser to `http://localhost:8080/oauth2/authorization/spotify`. Complete the login/authorization on Spotify. You'll be redirected back.
* **Initiate Google/YouTube Login:** Navigate your browser to `http://localhost:8080/oauth2/authorization/google`. Complete the login/authorization with Google, ensuring YouTube permissions are granted. You'll be redirected back.

* **Important:** For the transfer to work, you must log in via **both** services at least once within the same browser session to ensure both sets of tokens are stored and associated with your linked user account in the backend.

### 2. Getting a Session Token for API Tools (e.g., Postman)

* After successfully logging in via the browser (preferably completing **both** Spotify and Google logins in the same session), open your browser's developer tools (F12).
* Go to the "Application" (Chrome) or "Storage" (Firefox) tab.
* Find Cookies for `http://localhost:8080`.
* Copy the value of the `JSESSIONID` cookie.
* In Postman (or `curl`), add a `Cookie` header to your requests: `Cookie: JSESSIONID=<PASTED_VALUE>`

### 3. API Endpoints (Requires `JSESSIONID` Cookie Header)

* **List Spotify Playlists:**
    * `GET /api/spotify/playlists`
    * Requires prior Spotify login.
    * Returns: JSON array of `SpotifyPlaylistDto` (`id`, `name`).

* **Get Spotify Playlist Tracks:**
    * `GET /api/spotify/playlists/{playlistId}/tracks`
    * Replace `{playlistId}` with a valid Spotify playlist ID obtained from the previous endpoint.
    * Requires prior Spotify login.
    * Returns: JSON array of `SpotifyTrackDto` (detailed track info).

* **Create YouTube Playlist (Test Endpoint):**
    * `POST /api/youtube/playlists`
    * Requires prior Google login.
    * Requires request parameters (e.g., `title=MyTest`, `description=Optional`, `privacy=private`). Use `x-www-form-urlencoded` or `form-data` in Postman.
    * Returns: JSON object `YoutubePlaylistResponseDto` with the details (including the new `id`) of the created playlist.

* **Search YouTube Video (Test Endpoint):**
    * `GET /api/Youtube`
    * Requires prior Google login.
    * Requires query parameter, e.g., `?query=Song+Name+Artist`.
    * Returns: The YouTube Video ID (String) of the top result, or 404 Not Found.

* **Initiate Spotify -> YouTube Transfer:**
    * `POST /api/transfers/spotify/{spotifyPlaylistId}`
    * Replace `{spotifyPlaylistId}` with the ID of the Spotify playlist to transfer.
    * Requires **prior successful login via BOTH Spotify and Google** in the browser session corresponding to the `JSESSIONID`.
    * **Response:** Immediately returns `202 Accepted` with a message indicating the transfer has started in the background.
    * **Check Progress:** Monitor the application's console logs to see the transfer progress (playlist creation, track searching, adding).
    * **Verify Result:** Check your YouTube account for the newly created playlist and its contents after the logs indicate completion.

## Project Structure 📁 (Simplified)
![image](https://github.com/user-attachments/assets/bf35c854-88da-4300-bdd5-af5c8211915f)

## Current Limitations & Future Improvements 📈

* **Backend Only:** No user interface is provided. Interaction requires API calls (e.g., via Postman, curl).
* **Basic Youtube:** Uses a simple "Track Name Artist Name" query and takes the top result. More sophisticated matching (checking duration, album, official channels) could improve accuracy.
* **No Transfer Status API:** Progress can only be monitored via application logs. A dedicated status endpoint (`/api/transfers/{jobId}/status`) could be added.
* **Error Handling:** Basic error handling exists, but could be more robust (e.g., handling specific API error codes like 409 Conflict for duplicate videos, providing clearer feedback).
* **Rate Limiting:** No explicit handling for Spotify or YouTube API rate limits (a basic `Thread.sleep` is commented out). Hitting limits could cause failures on very large playlists.
* **Token Expiry During Transfer:** The current "pass token to async" approach might fail if a token expires during a very long transfer (longer than 1 hour). Implementing manual refresh within the async task or switching to custom token persistence with refresh logic would be needed.
* **No YouTube -> Spotify:** Transfer is currently one-way.
* **Default Token Storage:** Uses Spring's default `oauth2_authorized_client` table. Could be refactored to use custom `SpotifyToken`/`YoutubeToken` entities for finer control.



## License 📄

( MIT License).
