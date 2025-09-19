package com.scrabble.server.controller;

import com.scrabble.common.model.GameState;
import com.scrabble.common.model.Player;
import com.scrabble.server.service.GameService;
import com.scrabble.server.service.LobbyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

/**
 * REST controller for game and lobby management.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // For development - restrict in production
public class GameController {
    
    private final GameService gameService;
    private final LobbyService lobbyService;
    
    public GameController(GameService gameService, LobbyService lobbyService) {
        this.gameService = gameService;
        this.lobbyService = lobbyService;
    }
    
    /**
     * Join the lobby to find a game
     */
    @PostMapping("/lobby/join")
    public ResponseEntity<?> joinLobby(@RequestBody Player player) {
        try {
            LobbyService.MatchmakingResult result = lobbyService.joinLobby(player);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Play against the bot
     */
    @PostMapping("/lobby/bot")
    public ResponseEntity<?> playAgainstBot(@RequestBody Player player) {
        try {
            LobbyService.MatchmakingResult result = lobbyService.playAgainstBot(player);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Leave the lobby
     */
    @PostMapping("/lobby/leave")
    public ResponseEntity<?> leaveLobby(@RequestParam String playerId) {
        try {
            lobbyService.leaveLobby(playerId);
            return ResponseEntity.ok(Map.of("message", "Left lobby successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get lobby statistics
     */
    @GetMapping("/lobby/stats")
    public ResponseEntity<LobbyService.LobbyStats> getLobbyStats() {
        return ResponseEntity.ok(lobbyService.getStats());
    }
    
    /**
     * Get game state
     */
    @GetMapping("/game/{gameId}")
    public ResponseEntity<?> getGame(@PathVariable String gameId) {
        GameState game = gameService.getGame(gameId);
        if (game == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(game);
    }
    
    /**
     * Get all active games (for admin/debug purposes)
     */
    @GetMapping("/games")
    public ResponseEntity<Collection<GameState>> getAllGames() {
        return ResponseEntity.ok(gameService.getAllGames());
    }
    
    /**
     * Start a game manually (for testing)
     */
    @PostMapping("/game/{gameId}/start")
    public ResponseEntity<?> startGame(@PathVariable String gameId) {
        try {
            GameState game = gameService.startGame(gameId);
            return ResponseEntity.ok(game);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "activeGames", gameService.getGameCount(),
            "lobbyStats", lobbyService.getStats(),
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    /**
     * Clean up finished games
     */
    @PostMapping("/admin/cleanup")
    public ResponseEntity<Map<String, String>> cleanup() {
        lobbyService.cleanup();
        return ResponseEntity.ok(Map.of("message", "Cleanup completed"));
    }
}