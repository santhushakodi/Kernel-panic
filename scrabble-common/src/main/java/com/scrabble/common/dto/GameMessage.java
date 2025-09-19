package com.scrabble.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.scrabble.common.model.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * WebSocket message for game communication between client and server.
 */
@Data
public class GameMessage {
    
    public enum Type {
        // Client -> Server
        JOIN_GAME,
        LEAVE_GAME,
        MAKE_MOVE,
        REQUEST_GAME_STATE,
        
        // Server -> Client
        GAME_STATE_UPDATE,
        MOVE_RESULT,
        PLAYER_JOINED,
        PLAYER_LEFT,
        GAME_STARTED,
        GAME_ENDED,
        ERROR,
        
        // Bidirectional
        CHAT_MESSAGE,
        TIMER_UPDATE
    }
    
    private final Type type;
    private final String gameId;
    private final String playerId;
    private final Object payload;
    private final LocalDateTime timestamp;
    private final String error;
    
    @JsonCreator
    public GameMessage(@JsonProperty("type") Type type,
                      @JsonProperty("gameId") String gameId,
                      @JsonProperty("playerId") String playerId,
                      @JsonProperty("payload") Object payload,
                      @JsonProperty("timestamp") LocalDateTime timestamp,
                      @JsonProperty("error") String error) {
        this.type = type;
        this.gameId = gameId;
        this.playerId = playerId;
        this.payload = payload;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.error = error;
    }
    
    // Factory methods for common message types
    
    public static GameMessage joinGame(String gameId, String playerId) {
        return new GameMessage(Type.JOIN_GAME, gameId, playerId, null, LocalDateTime.now(), null);
    }
    
    public static GameMessage leaveGame(String gameId, String playerId) {
        return new GameMessage(Type.LEAVE_GAME, gameId, playerId, null, LocalDateTime.now(), null);
    }
    
    public static GameMessage makeMove(String gameId, String playerId, Move move) {
        return new GameMessage(Type.MAKE_MOVE, gameId, playerId, move, LocalDateTime.now(), null);
    }
    
    public static GameMessage requestGameState(String gameId, String playerId) {
        return new GameMessage(Type.REQUEST_GAME_STATE, gameId, playerId, null, LocalDateTime.now(), null);
    }
    
    public static GameMessage gameStateUpdate(String gameId, GameState gameState) {
        return new GameMessage(Type.GAME_STATE_UPDATE, gameId, null, gameState, LocalDateTime.now(), null);
    }
    
    public static GameMessage moveResult(String gameId, String playerId, MoveResult result) {
        return new GameMessage(Type.MOVE_RESULT, gameId, playerId, result, LocalDateTime.now(), null);
    }
    
    public static GameMessage playerJoined(String gameId, Player player) {
        return new GameMessage(Type.PLAYER_JOINED, gameId, player.getId(), player, LocalDateTime.now(), null);
    }
    
    public static GameMessage playerLeft(String gameId, String playerId) {
        return new GameMessage(Type.PLAYER_LEFT, gameId, playerId, null, LocalDateTime.now(), null);
    }
    
    public static GameMessage gameStarted(String gameId) {
        return new GameMessage(Type.GAME_STARTED, gameId, null, null, LocalDateTime.now(), null);
    }
    
    public static GameMessage gameEnded(String gameId, String reason, String winnerId) {
        Map<String, String> endInfo = Map.of(
            "reason", reason,
            "winnerId", winnerId != null ? winnerId : ""
        );
        return new GameMessage(Type.GAME_ENDED, gameId, null, endInfo, LocalDateTime.now(), null);
    }
    
    public static GameMessage error(String gameId, String playerId, String errorMessage) {
        return new GameMessage(Type.ERROR, gameId, playerId, null, LocalDateTime.now(), errorMessage);
    }
    
    public static GameMessage chatMessage(String gameId, String playerId, String message) {
        return new GameMessage(Type.CHAT_MESSAGE, gameId, playerId, message, LocalDateTime.now(), null);
    }
    
    public static GameMessage timerUpdate(String gameId, TimerInfo timerInfo) {
        return new GameMessage(Type.TIMER_UPDATE, gameId, null, timerInfo, LocalDateTime.now(), null);
    }
    
    /**
     * Gets the payload as a specific type
     */
    @SuppressWarnings("unchecked")
    public <T> T getPayloadAs(Class<T> clazz) {
        if (payload == null) {
            return null;
        }
        
        if (clazz.isInstance(payload)) {
            return (T) payload;
        }
        
        return null;
    }
    
    @Override
    public String toString() {
        return String.format("GameMessage{type=%s, gameId=%s, playerId=%s, timestamp=%s%s}",
                           type, gameId, playerId, timestamp,
                           error != null ? ", error=" + error : "");
    }
    
    /**
     * Result of a move attempt
     */
    @Data
    public static class MoveResult {
        private final boolean success;
        private final String errorMessage;
        private final int score;
        private final java.util.List<String> wordsFormed;
        private final boolean gameEnded;
        
        @JsonCreator
        public MoveResult(@JsonProperty("success") boolean success,
                         @JsonProperty("errorMessage") String errorMessage,
                         @JsonProperty("score") int score,
                         @JsonProperty("wordsFormed") java.util.List<String> wordsFormed,
                         @JsonProperty("gameEnded") boolean gameEnded) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.score = score;
            this.wordsFormed = wordsFormed;
            this.gameEnded = gameEnded;
        }
        
        public static MoveResult success(int score, java.util.List<String> wordsFormed, boolean gameEnded) {
            return new MoveResult(true, null, score, wordsFormed, gameEnded);
        }
        
        public static MoveResult failure(String errorMessage) {
            return new MoveResult(false, errorMessage, 0, java.util.Collections.emptyList(), false);
        }
    }
    
    /**
     * Timer information for players
     */
    @Data
    public static class TimerInfo {
        private final String currentPlayerId;
        private final long currentPlayerTimeRemainingSeconds;
        private final long opponentTimeRemainingSeconds;
        
        @JsonCreator
        public TimerInfo(@JsonProperty("currentPlayerId") String currentPlayerId,
                        @JsonProperty("currentPlayerTimeRemainingSeconds") long currentPlayerTimeRemainingSeconds,
                        @JsonProperty("opponentTimeRemainingSeconds") long opponentTimeRemainingSeconds) {
            this.currentPlayerId = currentPlayerId;
            this.currentPlayerTimeRemainingSeconds = currentPlayerTimeRemainingSeconds;
            this.opponentTimeRemainingSeconds = opponentTimeRemainingSeconds;
        }
    }
}