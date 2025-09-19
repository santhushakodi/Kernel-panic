package com.scrabble.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents a player's move in Scrabble.
 */
@Data
public class Move {
    
    public enum Type {
        PLACE_TILES,
        PASS,
        EXCHANGE
    }
    
    private final String playerId;
    private final Type type;
    private final Map<Position, Tile> tilePlacements;
    private final List<Tile> exchangedTiles;
    private final int score;
    private final List<String> wordsFormed;
    private final LocalDateTime timestamp;
    private final boolean isValid;
    private final String invalidReason;
    
    @JsonCreator
    public Move(@JsonProperty("playerId") String playerId,
                @JsonProperty("type") Type type,
                @JsonProperty("tilePlacements") Map<Position, Tile> tilePlacements,
                @JsonProperty("exchangedTiles") List<Tile> exchangedTiles,
                @JsonProperty("score") int score,
                @JsonProperty("wordsFormed") List<String> wordsFormed,
                @JsonProperty("timestamp") LocalDateTime timestamp,
                @JsonProperty("isValid") boolean isValid,
                @JsonProperty("invalidReason") String invalidReason) {
        this.playerId = playerId;
        this.type = type;
        this.tilePlacements = tilePlacements != null ? tilePlacements : new HashMap<>();
        this.exchangedTiles = exchangedTiles != null ? exchangedTiles : new ArrayList<>();
        this.score = score;
        this.wordsFormed = wordsFormed != null ? wordsFormed : new ArrayList<>();
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.isValid = isValid;
        this.invalidReason = invalidReason;
    }
    
    /**
     * Creates a tile placement move
     */
    public static Move createTilePlacement(String playerId, Map<Position, Tile> placements) {
        return new Move(playerId, Type.PLACE_TILES, placements, null, 0, null, 
                       LocalDateTime.now(), true, null);
    }
    
    /**
     * Creates a pass move
     */
    public static Move createPass(String playerId) {
        return new Move(playerId, Type.PASS, null, null, 0, null,
                       LocalDateTime.now(), true, null);
    }
    
    /**
     * Creates an exchange move
     */
    public static Move createExchange(String playerId, List<Tile> tilesToExchange) {
        return new Move(playerId, Type.EXCHANGE, null, tilesToExchange, 0, null,
                       LocalDateTime.now(), true, null);
    }
    
    /**
     * Creates an invalid move with error message
     */
    public static Move createInvalid(String playerId, Type type, String reason) {
        return new Move(playerId, type, null, null, 0, null,
                       LocalDateTime.now(), false, reason);
    }
    
    /**
     * Creates a validated move with score and words
     */
    public static Move createValidated(String playerId, Type type,
                                     Map<Position, Tile> placements,
                                     List<Tile> exchangedTiles,
                                     int score, List<String> wordsFormed) {
        return new Move(playerId, type, placements, exchangedTiles, score, wordsFormed,
                       LocalDateTime.now(), true, null);
    }
    
    /**
     * Gets the starting position of the move (for tile placements)
     */
    public Position getStartPosition() {
        if (type != Type.PLACE_TILES || tilePlacements.isEmpty()) {
            return null;
        }
        
        return tilePlacements.keySet().stream()
                            .min(Comparator.comparing(Position::getRow)
                                          .thenComparing(Position::getCol))
                            .orElse(null);
    }
    
    /**
     * Gets the ending position of the move (for tile placements)
     */
    public Position getEndPosition() {
        if (type != Type.PLACE_TILES || tilePlacements.isEmpty()) {
            return null;
        }
        
        return tilePlacements.keySet().stream()
                            .max(Comparator.comparing(Position::getRow)
                                          .thenComparing(Position::getCol))
                            .orElse(null);
    }
    
    /**
     * Determines the direction of the move
     */
    public Direction getDirection() {
        if (type != Type.PLACE_TILES || tilePlacements.size() <= 1) {
            return Direction.HORIZONTAL; // Default direction
        }
        
        List<Position> positions = new ArrayList<>(tilePlacements.keySet());
        positions.sort(Comparator.comparing(Position::getRow)
                                 .thenComparing(Position::getCol));
        
        Position first = positions.get(0);
        Position last = positions.get(positions.size() - 1);
        
        if (first.getRow() == last.getRow()) {
            return Direction.HORIZONTAL;
        } else if (first.getCol() == last.getCol()) {
            return Direction.VERTICAL;
        } else {
            // Diagonal placement - invalid but we'll return horizontal as default
            return Direction.HORIZONTAL;
        }
    }
    
    /**
     * Gets all positions involved in the move (sorted)
     */
    public List<Position> getPositions() {
        if (type != Type.PLACE_TILES) {
            return new ArrayList<>();
        }
        
        List<Position> positions = new ArrayList<>(tilePlacements.keySet());
        positions.sort(Comparator.comparing(Position::getRow)
                                 .thenComparing(Position::getCol));
        return positions;
    }
    
    /**
     * Gets the word formed by the main placement (not cross-words)
     */
    public String getMainWord(Board board) {
        if (type != Type.PLACE_TILES || tilePlacements.isEmpty()) {
            return "";
        }
        
        List<Position> positions = getPositions();
        Direction direction = getDirection();
        
        // Find the complete word span including existing tiles
        Position start = positions.get(0);
        Position end = positions.get(positions.size() - 1);
        
        // Extend backwards
        while (true) {
            Position prev = direction.isHorizontal() ? start.left() : start.up();
            if (!board.isValidPosition(prev) || board.isEmpty(prev)) {
                break;
            }
            start = prev;
        }
        
        // Extend forwards
        while (true) {
            Position next = direction.isHorizontal() ? end.right() : end.down();
            if (!board.isValidPosition(next) || board.isEmpty(next)) {
                break;
            }
            end = next;
        }
        
        // Build the word
        StringBuilder word = new StringBuilder();
        Position current = start;
        
        while (board.isValidPosition(current) && 
               (direction.isHorizontal() ? current.getCol() <= end.getCol() : 
                                          current.getRow() <= end.getRow())) {
            
            Tile tile = tilePlacements.get(current);
            if (tile == null) {
                tile = board.getTile(current);
            }
            
            if (tile != null) {
                word.append(tile.getDisplayLetter());
            }
            
            current = current.move(direction);
        }
        
        return word.toString();
    }
    
    /**
     * Gets the number of tiles placed
     */
    public int getTileCount() {
        return switch (type) {
            case PLACE_TILES -> tilePlacements.size();
            case EXCHANGE -> exchangedTiles.size();
            default -> 0;
        };
    }
    
    /**
     * Checks if this is a bingo (using all 7 tiles)
     */
    public boolean isBingo() {
        return type == Type.PLACE_TILES && tilePlacements.size() == Player.RACK_SIZE;
    }
    
    /**
     * Creates a copy of this move
     */
    public Move copy() {
        Map<Position, Tile> newPlacements = new HashMap<>();
        tilePlacements.forEach((pos, tile) -> newPlacements.put(pos, tile.copy()));
        
        List<Tile> newExchanged = exchangedTiles.stream()
                                               .map(Tile::copy)
                                               .toList();
        
        return new Move(playerId, type, newPlacements, new ArrayList<>(newExchanged),
                       score, new ArrayList<>(wordsFormed), timestamp, isValid, invalidReason);
    }
    
    @Override
    public String toString() {
        return switch (type) {
            case PLACE_TILES -> String.format("PLACE: %s (%d points) - %s", 
                                             tilePlacements.keySet(), score, wordsFormed);
            case PASS -> String.format("PASS by %s", playerId);
            case EXCHANGE -> String.format("EXCHANGE: %d tiles by %s", 
                                          exchangedTiles.size(), playerId);
        };
    }
}