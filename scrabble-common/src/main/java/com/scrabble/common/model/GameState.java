package com.scrabble.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents the complete state of a Scrabble game.
 */
@Data
public class GameState {
    
    public enum Status {
        WAITING_FOR_PLAYERS,
        IN_PROGRESS,
        FINISHED,
        ABANDONED
    }
    
    private final String gameId;
    private final List<Player> players;
    private final Board board;
    private final TileBag tileBag;
    private final List<Move> moveHistory;
    private Status status;
    private int currentPlayerIndex;
    private LocalDateTime gameStartTime;
    private LocalDateTime lastMoveTime;
    private int consecutivePasses;
    private String winnerId;
    private String gameEndReason;
    
    @JsonCreator
    public GameState(@JsonProperty("gameId") String gameId,
                     @JsonProperty("players") List<Player> players,
                     @JsonProperty("board") Board board,
                     @JsonProperty("tileBag") TileBag tileBag,
                     @JsonProperty("moveHistory") List<Move> moveHistory,
                     @JsonProperty("status") Status status,
                     @JsonProperty("currentPlayerIndex") int currentPlayerIndex,
                     @JsonProperty("gameStartTime") LocalDateTime gameStartTime,
                     @JsonProperty("lastMoveTime") LocalDateTime lastMoveTime,
                     @JsonProperty("consecutivePasses") int consecutivePasses,
                     @JsonProperty("winnerId") String winnerId,
                     @JsonProperty("gameEndReason") String gameEndReason) {
        this.gameId = gameId;
        this.players = players != null ? players : new ArrayList<>();
        this.board = board != null ? board : Board.createStandard();
        this.tileBag = tileBag != null ? tileBag : TileBag.createStandard();
        this.moveHistory = moveHistory != null ? moveHistory : new ArrayList<>();
        this.status = status != null ? status : Status.WAITING_FOR_PLAYERS;
        this.currentPlayerIndex = currentPlayerIndex;
        this.gameStartTime = gameStartTime;
        this.lastMoveTime = lastMoveTime;
        this.consecutivePasses = consecutivePasses;
        this.winnerId = winnerId;
        this.gameEndReason = gameEndReason;
    }
    
    /**
     * Creates a new game with the given players
     */
    public static GameState createNew(String gameId, List<Player> players) {
        if (players.size() != 2) {
            throw new IllegalArgumentException("Scrabble requires exactly 2 players");
        }
        
        GameState game = new GameState(gameId, new ArrayList<>(players), Board.createStandard(),
                                     TileBag.createStandard(), new ArrayList<>(),
                                     Status.WAITING_FOR_PLAYERS, 0, null, null, 0, null, null);
        
        // Fill initial racks
        for (Player player : game.players) {
            List<Tile> initialTiles = game.tileBag.drawTiles(Player.RACK_SIZE);
            for (Tile tile : initialTiles) {
                player.addTileToRack(tile);
            }
        }
        
        return game;
    }
    
    /**
     * Starts the game
     */
    public void startGame() {
        if (status != Status.WAITING_FOR_PLAYERS) {
            throw new IllegalStateException("Game already started");
        }
        if (players.size() != 2) {
            throw new IllegalStateException("Need exactly 2 players to start");
        }
        
        status = Status.IN_PROGRESS;
        gameStartTime = LocalDateTime.now();
        
        // Randomly choose starting player
        currentPlayerIndex = new Random().nextInt(2);
        
        // Set player statuses
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            player.setStatus(i == currentPlayerIndex ? Player.Status.PLAYING : Player.Status.WAITING);
        }
    }
    
    /**
     * Gets the current player
     */
    public Player getCurrentPlayer() {
        if (players.isEmpty()) {
            return null;
        }
        return players.get(currentPlayerIndex);
    }
    
    /**
     * Gets the waiting player (opponent)
     */
    public Player getWaitingPlayer() {
        if (players.size() != 2) {
            return null;
        }
        int waitingIndex = (currentPlayerIndex + 1) % 2;
        return players.get(waitingIndex);
    }
    
    /**
     * Gets a player by ID
     */
    public Player getPlayerById(String playerId) {
        return players.stream()
                     .filter(p -> p.getId().equals(playerId))
                     .findFirst()
                     .orElse(null);
    }
    
    /**
     * Switches to the next player
     */
    public void switchTurns() {
        if (players.size() != 2) {
            return;
        }
        
        // Update current player status
        getCurrentPlayer().setStatus(Player.Status.WAITING);
        
        // Switch to next player
        currentPlayerIndex = (currentPlayerIndex + 1) % 2;
        
        // Update new current player status
        getCurrentPlayer().setStatus(Player.Status.PLAYING);
        
        lastMoveTime = LocalDateTime.now();
    }
    
    /**
     * Adds a move to the history
     */
    public void addMove(Move move) {
        moveHistory.add(move);
        lastMoveTime = LocalDateTime.now();
        
        if (move.getType() == Move.Type.PASS) {
            consecutivePasses++;
        } else {
            consecutivePasses = 0;
        }
    }
    
    /**
     * Checks if the game is over
     */
    public boolean isGameOver() {
        if (status == Status.FINISHED || status == Status.ABANDONED) {
            return true;
        }
        
        // Game ends if 6 consecutive passes (3 per player)
        if (consecutivePasses >= 6) {
            return true;
        }
        
        // Game ends if a player's rack is empty and tile bag is empty
        for (Player player : players) {
            if (player.isRackEmpty() && tileBag.isEmpty()) {
                return true;
            }
        }
        
        // Game ends if both players have timed out
        boolean allTimedOut = players.stream()
                                    .allMatch(p -> p.getStatus() == Player.Status.TIMEOUT);
        if (allTimedOut) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Ends the game and determines the winner
     */
    public void endGame(String reason) {
        if (status == Status.FINISHED) {
            return;
        }
        
        status = Status.FINISHED;
        gameEndReason = reason;
        
        // Set all player statuses to finished
        players.forEach(player -> player.setStatus(Player.Status.FINISHED));
        
        // Determine winner
        Player winner = determineWinner();
        if (winner != null) {
            winnerId = winner.getId();
        }
    }
    
    /**
     * Determines the winner based on scores and remaining tiles
     */
    private Player determineWinner() {
        if (players.size() != 2) {
            return null;
        }
        
        Player player1 = players.get(0);
        Player player2 = players.get(1);
        
        // Deduct remaining tile values from scores
        int finalScore1 = player1.getScore() - player1.getRemainingTileValue();
        int finalScore2 = player2.getScore() - player2.getRemainingTileValue();
        
        // If a player used all tiles, add opponent's remaining tile value
        if (player1.isRackEmpty()) {
            finalScore1 += player2.getRemainingTileValue();
        } else if (player2.isRackEmpty()) {
            finalScore2 += player1.getRemainingTileValue();
        }
        
        // Update final scores
        player1.setScore(finalScore1);
        player2.setScore(finalScore2);
        
        // Return winner (higher score wins)
        if (finalScore1 > finalScore2) {
            return player1;
        } else if (finalScore2 > finalScore1) {
            return player2;
        } else {
            return null; // Tie
        }
    }
    
    /**
     * Gets the game duration
     */
    public long getGameDurationMinutes() {
        if (gameStartTime == null) {
            return 0;
        }
        
        LocalDateTime endTime = status == Status.FINISHED ? lastMoveTime : LocalDateTime.now();
        if (endTime == null) {
            endTime = LocalDateTime.now();
        }
        
        return java.time.Duration.between(gameStartTime, endTime).toMinutes();
    }
    
    /**
     * Gets the number of moves played
     */
    public int getMoveCount() {
        return moveHistory.size();
    }
    
    /**
     * Gets the last move
     */
    public Move getLastMove() {
        if (moveHistory.isEmpty()) {
            return null;
        }
        return moveHistory.get(moveHistory.size() - 1);
    }
    
    /**
     * Checks if this is the first move of the game
     */
    public boolean isFirstMove() {
        return moveHistory.stream()
                         .noneMatch(move -> move.getType() == Move.Type.PLACE_TILES);
    }
    
    /**
     * Gets moves by a specific player
     */
    public List<Move> getMovesByPlayer(String playerId) {
        return moveHistory.stream()
                         .filter(move -> move.getPlayerId().equals(playerId))
                         .toList();
    }
    
    /**
     * Gets the tiles remaining in the bag
     */
    public int getTilesRemainingInBag() {
        return tileBag.size();
    }
    
    /**
     * Creates a copy of the game state
     */
    public GameState copy() {
        List<Player> newPlayers = players.stream()
                                        .map(Player::copy)
                                        .toList();
        
        List<Move> newMoveHistory = moveHistory.stream()
                                              .map(Move::copy)
                                              .toList();
        
        return new GameState(gameId, new ArrayList<>(newPlayers), board.copy(),
                           tileBag.copy(), new ArrayList<>(newMoveHistory), status,
                           currentPlayerIndex, gameStartTime, lastMoveTime,
                           consecutivePasses, winnerId, gameEndReason);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Game ").append(gameId).append(" [").append(status).append("]\n");
        sb.append("Players:\n");
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            sb.append("  ").append(i == currentPlayerIndex ? "* " : "  ")
              .append(player.toString()).append("\n");
        }
        sb.append("Moves: ").append(moveHistory.size());
        sb.append(", Tiles in bag: ").append(tileBag.size());
        if (winnerId != null) {
            Player winner = getPlayerById(winnerId);
            sb.append(", Winner: ").append(winner != null ? winner.getName() : winnerId);
        }
        return sb.toString();
    }
}