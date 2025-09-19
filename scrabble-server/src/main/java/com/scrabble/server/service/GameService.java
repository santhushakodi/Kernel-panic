package com.scrabble.server.service;

import com.scrabble.common.model.*;
import com.scrabble.common.service.Dictionary;
import com.scrabble.common.service.GameRules;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing game instances and game logic.
 */
@Service
public class GameService {
    
    private final Dictionary dictionary;
    private final GameRules gameRules;
    private final Map<String, GameState> games = new ConcurrentHashMap<>();
    private com.scrabble.server.websocket.GameWebSocketHandler webSocketHandler;
    
    public GameService(Dictionary dictionary) {
        this.dictionary = dictionary;
        this.gameRules = new GameRules(dictionary);
    }
    
    /**
     * Sets the WebSocket handler for bot move notifications
     */
    public void setWebSocketHandler(com.scrabble.server.websocket.GameWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }
    
    /**
     * Creates a new game with the given players
     */
    public GameState createGame(List<Player> players) {
        if (players.size() != 2) {
            throw new IllegalArgumentException("Scrabble requires exactly 2 players");
        }
        
        String gameId = UUID.randomUUID().toString();
        GameState game = GameState.createNew(gameId, players);
        games.put(gameId, game);
        
        System.out.println("Created new game: " + gameId);
        return game;
    }
    
    /**
     * Gets a game by ID
     */
    public GameState getGame(String gameId) {
        return games.get(gameId);
    }
    
    /**
     * Gets all active games
     */
    public Collection<GameState> getAllGames() {
        return games.values();
    }
    
    /**
     * Starts a game
     */
    public GameState startGame(String gameId) {
        GameState game = games.get(gameId);
        if (game == null) {
            throw new IllegalArgumentException("Game not found: " + gameId);
        }
        
        game.startGame();
        System.out.println("Started game: " + gameId);
        
        // Check if bot should make the first move
        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer != null && currentPlayer.isBot()) {
            System.out.println("DEBUG: Bot is the starting player, processing first bot move for: " + currentPlayer.getId());
            processBotMove(game);
            
            // Notify WebSocket clients about the bot's first move
            if (webSocketHandler != null) {
                webSocketHandler.notifyBotMove(game.getGameId());
            }
        }
        
        return game;
    }
    
    /**
     * Processes a move and returns the result
     */
    public GameRules.MoveValidationResult processMove(String gameId, String playerId, Move move) {
        GameState game = games.get(gameId);
        if (game == null) {
            return GameRules.MoveValidationResult.invalid("Game not found");
        }
        
        // Validate it's the player's turn
        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer == null || !currentPlayer.getId().equals(playerId)) {
            return GameRules.MoveValidationResult.invalid("Not your turn");
        }
        
        // Validate the move
        GameRules.MoveValidationResult result = gameRules.validateMove(
            move, game.getBoard(), currentPlayer, game.isFirstMove());
        
        if (!result.isValid()) {
            return result;
        }
        
        // Apply the move
        applyMove(game, move, result);
        
        // Check if game is over
        if (game.isGameOver()) {
            endGame(game);
        } else {
            // Switch turns
            game.switchTurns();
            
            // Process bot move if it's now a bot's turn
            Player newCurrentPlayer = game.getCurrentPlayer();
            if (newCurrentPlayer != null && newCurrentPlayer.isBot()) {
                System.out.println("DEBUG: Bot's turn, processing bot move for player: " + newCurrentPlayer.getId());
                processBotMove(game);
            }
        }
        
        return result;
    }
    
    /**
     * Applies a validated move to the game
     */
    private void applyMove(GameState game, Move move, GameRules.MoveValidationResult result) {
        Player currentPlayer = game.getCurrentPlayer();
        
        switch (move.getType()) {
            case PLACE_TILES:
                // Place tiles on board
                for (Map.Entry<Position, Tile> entry : move.getTilePlacements().entrySet()) {
                    game.getBoard().placeTile(entry.getKey(), entry.getValue());
                    currentPlayer.removeTileFromRack(entry.getValue());
                }
                
                // Add score
                currentPlayer.addScore(result.getScore());
                
                // Draw new tiles
                List<Tile> newTiles = game.getTileBag().drawToFillRack(
                    currentPlayer.getRackSize(), Player.RACK_SIZE);
                for (Tile tile : newTiles) {
                    currentPlayer.addTileToRack(tile);
                }
                
                currentPlayer.recordMove();
                break;
                
            case PASS:
                currentPlayer.recordPass();
                break;
                
            case EXCHANGE:
                // Exchange tiles
                List<Tile> exchangedTiles = move.getExchangedTiles();
                for (Tile tile : exchangedTiles) {
                    currentPlayer.removeTileFromRack(tile);
                }
                
                List<Tile> newExchangedTiles = game.getTileBag().exchangeTiles(exchangedTiles);
                for (Tile tile : newExchangedTiles) {
                    currentPlayer.addTileToRack(tile);
                }
                
                currentPlayer.recordMove();
                break;
        }
        
        // Create move with validation results
        Move validatedMove = Move.createValidated(
            move.getPlayerId(), move.getType(), move.getTilePlacements(),
            move.getExchangedTiles(), result.getScore(), result.getWordsFormed());
        
        game.addMove(validatedMove);
    }
    
    /**
     * Ends a game
     */
    private void endGame(GameState game) {
        String reason = determineEndReason(game);
        game.endGame(reason);
        System.out.println("Game ended: " + game.getGameId() + " - " + reason);
    }
    
    /**
     * Determines why the game ended
     */
    private String determineEndReason(GameState game) {
        if (game.getConsecutivePasses() >= 6) {
            return "Six consecutive passes";
        }
        
        for (Player player : game.getPlayers()) {
            if (player.isRackEmpty() && game.getTileBag().isEmpty()) {
                return player.getName() + " used all tiles";
            }
        }
        
        boolean allTimedOut = game.getPlayers().stream()
                                  .allMatch(p -> p.getStatus() == Player.Status.TIMEOUT);
        if (allTimedOut) {
            return "All players timed out";
        }
        
        return "Game completed";
    }
    
    /**
     * Removes a game
     */
    public void removeGame(String gameId) {
        games.remove(gameId);
        System.out.println("Removed game: " + gameId);
    }
    
    /**
     * Gets the number of active games
     */
    public int getGameCount() {
        return games.size();
    }
    
    /**
     * Abandons a game (when a player leaves)
     */
    public void abandonGame(String gameId, String playerId) {
        GameState game = games.get(gameId);
        if (game != null) {
            Player leavingPlayer = game.getPlayerById(playerId);
            if (leavingPlayer != null) {
                leavingPlayer.setStatus(Player.Status.DISCONNECTED);
                
                // If both players are disconnected, remove the game
                boolean allDisconnected = game.getPlayers().stream()
                    .allMatch(p -> p.getStatus() == Player.Status.DISCONNECTED);
                
                if (allDisconnected || game.getStatus() == GameState.Status.WAITING_FOR_PLAYERS) {
                    removeGame(gameId);
                } else {
                    game.setStatus(GameState.Status.ABANDONED);
                    game.endGame("Player left the game");
                }
            }
        }
    }
    
    /**
     * Processes a bot move using simple AI
     */
    private void processBotMove(GameState game) {
        Player bot = game.getCurrentPlayer();
        if (bot == null || !bot.isBot()) {
            return;
        }
        
        System.out.println("DEBUG: Processing bot move for " + bot.getName());
        
        try {
            // Simple bot AI: Try to place a word, otherwise pass
            Move botMove = generateBotMove(game, bot);
            
            if (botMove != null) {
                System.out.println("DEBUG: Bot generated move: " + botMove);
                
                // Validate and apply the bot move
                GameRules.MoveValidationResult result = gameRules.validateMove(
                    botMove, game.getBoard(), bot, game.isFirstMove());
                
                if (result.isValid()) {
                    System.out.println("DEBUG: Bot move is valid, applying it");
                    applyMove(game, botMove, result);
                    
                    // Check if game is over after bot move
                    if (game.isGameOver()) {
                        endGame(game);
                    } else {
                        // Switch turns back to human player
                        game.switchTurns();
                    }
                } else {
                    System.out.println("DEBUG: Bot move is invalid: " + result.getErrorMessage());
                    // If bot move is invalid, make the bot pass
                    Move passMove = Move.createPass(bot.getId());
                    applyMove(game, passMove, GameRules.MoveValidationResult.valid(0, new ArrayList<>()));
                    game.switchTurns();
                }
                
                // Notify WebSocket clients about the bot move
                if (webSocketHandler != null) {
                    webSocketHandler.notifyBotMove(game.getGameId());
                }
            } else {
                System.out.println("DEBUG: Bot could not generate a move, passing");
                // Bot passes
                Move passMove = Move.createPass(bot.getId());
                applyMove(game, passMove, GameRules.MoveValidationResult.valid(0, new ArrayList<>()));
                game.switchTurns();
                
                // Notify WebSocket clients about the bot pass
                if (webSocketHandler != null) {
                    webSocketHandler.notifyBotMove(game.getGameId());
                }
            }
        } catch (Exception e) {
            System.err.println("ERROR: Exception during bot move processing: " + e.getMessage());
            e.printStackTrace();
            
            // Fallback: make bot pass
            try {
                Move passMove = Move.createPass(bot.getId());
                applyMove(game, passMove, GameRules.MoveValidationResult.valid(0, new ArrayList<>()));
                game.switchTurns();
                
                // Notify WebSocket clients about the bot fallback pass
                if (webSocketHandler != null) {
                    webSocketHandler.notifyBotMove(game.getGameId());
                }
            } catch (Exception fallbackError) {
                System.err.println("ERROR: Even bot pass failed: " + fallbackError.getMessage());
            }
        }
    }
    
    /**
     * Generates a simple bot move (basic AI)
     */
    private Move generateBotMove(GameState game, Player bot) {
        // Very simple bot AI: Try to place first available tile in center or pass
        
        if (bot.getRackSize() == 0) {
            return null; // No tiles to play
        }
        
        // For the first move, try to place a tile in the center
        if (game.isFirstMove()) {
            Tile firstTile = bot.getRack().get(0);
            Map<Position, Tile> placements = new HashMap<>();
            placements.put(new Position(7, 7), firstTile); // Center position
            return Move.createTilePlacement(bot.getId(), placements);
        }
        
        // For subsequent moves, try to find a simple placement
        // This is a very basic AI - in a real game, you'd want more sophisticated logic
        Tile tileToPlay = findBestTileToPlay(bot, game.getBoard());
        Position bestPosition = findBestPosition(game.getBoard(), tileToPlay);
        
        if (tileToPlay != null && bestPosition != null) {
            Map<Position, Tile> placements = new HashMap<>();
            placements.put(bestPosition, tileToPlay);
            return Move.createTilePlacement(bot.getId(), placements);
        }
        
        return null; // Can't make a move, will pass
    }
    
    /**
     * Simple method to find the best tile to play
     */
    private Tile findBestTileToPlay(Player bot, Board board) {
        // Simple logic: return first tile with highest value
        return bot.getRack().stream()
                 .max(Comparator.comparingInt(Tile::getValue))
                 .orElse(null);
    }
    
    /**
     * Simple method to find a valid position to place a tile
     */
    private Position findBestPosition(Board board, Tile tile) {
        // Very simple logic: find first empty position adjacent to existing tiles
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                Position pos = new Position(row, col);
                
                // Skip if position is occupied
                if (board.getTile(pos) != null) {
                    continue;
                }
                
                // Check if position is adjacent to an existing tile
                if (isAdjacentToExistingTile(board, pos)) {
                    return pos;
                }
            }
        }
        
        return null; // No valid position found
    }
    
    /**
     * Checks if a position is adjacent to an existing tile on the board
     */
    private boolean isAdjacentToExistingTile(Board board, Position pos) {
        // Check all four directions
        Position[] adjacent = {
            new Position(pos.getRow() - 1, pos.getCol()), // up
            new Position(pos.getRow() + 1, pos.getCol()), // down
            new Position(pos.getRow(), pos.getCol() - 1), // left
            new Position(pos.getRow(), pos.getCol() + 1)  // right
        };
        
        for (Position adjPos : adjacent) {
            if (board.isValidPosition(adjPos) && board.getTile(adjPos) != null) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Cleans up finished games
     */
    public void cleanupFinishedGames() {
        games.entrySet().removeIf(entry -> {
            GameState game = entry.getValue();
            return game.getStatus() == GameState.Status.FINISHED ||
                   game.getStatus() == GameState.Status.ABANDONED;
        });
    }
}