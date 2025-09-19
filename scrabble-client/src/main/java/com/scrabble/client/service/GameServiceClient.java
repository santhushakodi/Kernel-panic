package com.scrabble.client.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.scrabble.common.model.Player;
import com.scrabble.client.dto.PlayerDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP client service for REST API communication with the Scrabble game server.
 * Handles lobby operations, game creation, and server status checks.
 */
public class GameServiceClient {
    private static final Logger logger = LoggerFactory.getLogger(GameServiceClient.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public GameServiceClient(String serverUrl) {
        this.baseUrl = serverUrl + "/api";
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        logger.debug("GameServiceClient created for server: {}", serverUrl);
    }
    
    /**
     * Check server health and get lobby statistics.
     */
    public CompletableFuture<ServerStatus> getServerHealth() {
        String url = baseUrl + "/health";
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(REQUEST_TIMEOUT)
            .GET()
            .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                try {
                    if (response.statusCode() == 200) {
                        JsonNode json = objectMapper.readTree(response.body());
                        return new ServerStatus(true, json.toString());
                    } else {
                        logger.warn("Health check failed with status: {}", response.statusCode());
                        return new ServerStatus(false, "Server returned status: " + response.statusCode());
                    }
                } catch (Exception e) {
                    logger.error("Failed to parse health check response", e);
                    return new ServerStatus(false, "Failed to parse server response: " + e.getMessage());
                }
            })
            .exceptionally(throwable -> {
                logger.error("Health check request failed", throwable);
                return new ServerStatus(false, "Connection failed: " + throwable.getMessage());
            });
    }
    
    /**
     * Join the lobby for human vs human play.
     */
    public CompletableFuture<LobbyResult> joinLobby(Player player) {
        String url = baseUrl + "/lobby/join";
        
        try {
            PlayerDTO playerDTO = PlayerDTO.from(player);
            String requestBody = objectMapper.writeValueAsString(playerDTO);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
            
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    try {
                        if (response.statusCode() == 200) {
                            JsonNode json = objectMapper.readTree(response.body());
                            String gameId = json.get("gameId").asText();
                            String message = json.has("message") ? json.get("message").asText() : "Joined lobby successfully";
                            
                            logger.info("Successfully joined lobby - Game ID: {}", gameId);
                            return new LobbyResult(true, gameId, message);
                        } else {
                            logger.warn("Join lobby failed with status: {}", response.statusCode());
                            return new LobbyResult(false, null, "Server returned status: " + response.statusCode());
                        }
                    } catch (Exception e) {
                        logger.error("Failed to parse join lobby response", e);
                        return new LobbyResult(false, null, "Failed to parse server response: " + e.getMessage());
                    }
                })
                .exceptionally(throwable -> {
                    logger.error("Join lobby request failed", throwable);
                    return new LobbyResult(false, null, "Connection failed: " + throwable.getMessage());
                });
        } catch (Exception e) {
            logger.error("Failed to create join lobby request", e);
            return CompletableFuture.completedFuture(
                new LobbyResult(false, null, "Request creation failed: " + e.getMessage()));
        }
    }
    
    /**
     * Request a game against a bot opponent.
     */
    public CompletableFuture<LobbyResult> playAgainstBot(Player player) {
        String url = baseUrl + "/lobby/bot";
        
        try {
            PlayerDTO playerDTO = PlayerDTO.from(player);
            String requestBody = objectMapper.writeValueAsString(playerDTO);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
            
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    try {
                        if (response.statusCode() == 200) {
                            JsonNode json = objectMapper.readTree(response.body());
                            String gameId = json.get("gameId").asText();
                            String message = json.has("message") ? json.get("message").asText() : "Game with bot created successfully";
                            
                            logger.info("Successfully created game with bot - Game ID: {}", gameId);
                            return new LobbyResult(true, gameId, message);
                        } else {
                            logger.warn("Bot game creation failed with status: {}", response.statusCode());
                            return new LobbyResult(false, null, "Server returned status: " + response.statusCode());
                        }
                    } catch (Exception e) {
                        logger.error("Failed to parse bot game response", e);
                        return new LobbyResult(false, null, "Failed to parse server response: " + e.getMessage());
                    }
                })
                .exceptionally(throwable -> {
                    logger.error("Bot game request failed", throwable);
                    return new LobbyResult(false, null, "Connection failed: " + throwable.getMessage());
                });
        } catch (Exception e) {
            logger.error("Failed to create bot game request", e);
            return CompletableFuture.completedFuture(
                new LobbyResult(false, null, "Request creation failed: " + e.getMessage()));
        }
    }
    
    /**
     * Get lobby statistics.
     */
    public CompletableFuture<LobbyStats> getLobbyStats() {
        String url = baseUrl + "/lobby/stats";
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(REQUEST_TIMEOUT)
            .GET()
            .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                try {
                    if (response.statusCode() == 200) {
                        JsonNode json = objectMapper.readTree(response.body());
                        
                        int waitingPlayers = json.has("waitingPlayers") ? json.get("waitingPlayers").asInt() : 0;
                        int activeGames = json.has("activeGames") ? json.get("activeGames").asInt() : 0;
                        
                        return new LobbyStats(waitingPlayers, activeGames);
                    } else {
                        logger.warn("Lobby stats request failed with status: {}", response.statusCode());
                        return new LobbyStats(0, 0);
                    }
                } catch (Exception e) {
                    logger.error("Failed to parse lobby stats response", e);
                    return new LobbyStats(0, 0);
                }
            })
            .exceptionally(throwable -> {
                logger.error("Lobby stats request failed", throwable);
                return new LobbyStats(0, 0);
            });
    }
    
    /**
     * Result classes for API responses
     */
    public static class ServerStatus {
        private final boolean healthy;
        private final String details;
        
        public ServerStatus(boolean healthy, String details) {
            this.healthy = healthy;
            this.details = details;
        }
        
        public boolean isHealthy() { return healthy; }
        public String getDetails() { return details; }
    }
    
    public static class LobbyResult {
        private final boolean success;
        private final String gameId;
        private final String message;
        
        public LobbyResult(boolean success, String gameId, String message) {
            this.success = success;
            this.gameId = gameId;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public String getGameId() { return gameId; }
        public String getMessage() { return message; }
    }
    
    public static class LobbyStats {
        private final int waitingPlayers;
        private final int activeGames;
        
        public LobbyStats(int waitingPlayers, int activeGames) {
            this.waitingPlayers = waitingPlayers;
            this.activeGames = activeGames;
        }
        
        public int getWaitingPlayers() { return waitingPlayers; }
        public int getActiveGames() { return activeGames; }
    }
}