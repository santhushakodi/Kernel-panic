package com.scrabble.server.service;

import com.scrabble.common.model.GameState;
import com.scrabble.common.model.Player;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Service for managing the game lobby and matchmaking.
 */
@Service
public class LobbyService {
    
    private final GameService gameService;
    private final Queue<Player> waitingPlayers = new ConcurrentLinkedQueue<>();
    private final Map<String, String> playerToGameMap = new ConcurrentHashMap<>();
    
    public LobbyService(GameService gameService) {
        this.gameService = gameService;
    }
    
    /**
     * Adds a player to the lobby queue
     */
    public synchronized MatchmakingResult joinLobby(Player player) {
        // Check if player is already in a game
        String existingGameId = playerToGameMap.get(player.getId());
        if (existingGameId != null) {
            GameState existingGame = gameService.getGame(existingGameId);
            if (existingGame != null && existingGame.getStatus() != GameState.Status.FINISHED) {
                return MatchmakingResult.existingGame(existingGameId);
            } else {
                // Clean up stale mapping
                playerToGameMap.remove(player.getId());
            }
        }
        
        // Try to match with waiting player
        Player waitingPlayer = waitingPlayers.poll();
        if (waitingPlayer != null) {
            // Create game with two players
            List<Player> players = Arrays.asList(waitingPlayer, player);
            GameState game = gameService.createGame(players);
            
            // Track player to game mappings
            playerToGameMap.put(waitingPlayer.getId(), game.getGameId());
            playerToGameMap.put(player.getId(), game.getGameId());
            
            // Start the game automatically
            gameService.startGame(game.getGameId());
            
            return MatchmakingResult.gameCreated(game.getGameId());
        } else {
            // Add to waiting queue
            waitingPlayers.offer(player);
            return MatchmakingResult.waiting();
        }
    }
    
    /**
     * Creates a game against the bot
     */
    public MatchmakingResult playAgainstBot(Player player) {
        // Create a simple bot player
        Player botPlayer = Player.createBot("bot-" + UUID.randomUUID().toString(), "ScrabbleBot");
        
        List<Player> players = Arrays.asList(player, botPlayer);
        GameState game = gameService.createGame(players);
        
        // Track player to game mapping
        playerToGameMap.put(player.getId(), game.getGameId());
        
        // Start the game automatically
        gameService.startGame(game.getGameId());
        
        return MatchmakingResult.gameCreated(game.getGameId());
    }
    
    /**
     * Removes a player from the lobby
     */
    public synchronized void leaveLobby(String playerId) {
        // Remove from waiting queue
        waitingPlayers.removeIf(player -> player.getId().equals(playerId));
        
        // Check if player is in an active game
        String gameId = playerToGameMap.get(playerId);
        if (gameId != null) {
            gameService.abandonGame(gameId, playerId);
            playerToGameMap.remove(playerId);
        }
    }
    
    /**
     * Gets the game ID for a player
     */
    public String getPlayerGameId(String playerId) {
        return playerToGameMap.get(playerId);
    }
    
    /**
     * Gets the number of players waiting in lobby
     */
    public int getWaitingPlayersCount() {
        return waitingPlayers.size();
    }
    
    /**
     * Gets lobby statistics
     */
    public LobbyStats getStats() {
        return new LobbyStats(
            waitingPlayers.size(),
            playerToGameMap.size(),
            gameService.getGameCount()
        );
    }
    
    /**
     * Cleans up finished games and stale player mappings
     */
    public void cleanup() {
        // Clean up finished games
        gameService.cleanupFinishedGames();
        
        // Remove stale player mappings
        playerToGameMap.entrySet().removeIf(entry -> {
            GameState game = gameService.getGame(entry.getValue());
            return game == null || 
                   game.getStatus() == GameState.Status.FINISHED ||
                   game.getStatus() == GameState.Status.ABANDONED;
        });
    }
    
    /**
     * Result of matchmaking attempt
     */
    public static class MatchmakingResult {
        public enum Type {
            WAITING,
            GAME_CREATED,
            EXISTING_GAME,
            ERROR
        }
        
        private final Type type;
        private final String gameId;
        private final String message;
        
        private MatchmakingResult(Type type, String gameId, String message) {
            this.type = type;
            this.gameId = gameId;
            this.message = message;
        }
        
        public static MatchmakingResult waiting() {
            return new MatchmakingResult(Type.WAITING, null, "Waiting for opponent");
        }
        
        public static MatchmakingResult gameCreated(String gameId) {
            return new MatchmakingResult(Type.GAME_CREATED, gameId, "Game created");
        }
        
        public static MatchmakingResult existingGame(String gameId) {
            return new MatchmakingResult(Type.EXISTING_GAME, gameId, "Rejoining existing game");
        }
        
        public static MatchmakingResult error(String message) {
            return new MatchmakingResult(Type.ERROR, null, message);
        }
        
        // Getters
        public Type getType() { return type; }
        public String getGameId() { return gameId; }
        public String getMessage() { return message; }
        
        public boolean isSuccess() {
            return type == Type.GAME_CREATED || type == Type.EXISTING_GAME;
        }
        
        @Override
        public String toString() {
            return String.format("MatchmakingResult{type=%s, gameId=%s, message=%s}",
                               type, gameId, message);
        }
    }
    
    /**
     * Lobby statistics
     */
    public record LobbyStats(int waitingPlayers, int activePlayers, int activeGames) {
        @Override
        public String toString() {
            return String.format("LobbyStats{waiting=%d, active=%d, games=%d}",
                               waitingPlayers, activePlayers, activeGames);
        }
    }
}