package com.scrabble.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.*;
import java.util.stream.IntStream;

/**
 * Represents a 15x15 Scrabble game board with premium squares and placed tiles.
 */
@Data
public class Board {
    public static final int SIZE = 15;
    public static final int CENTER = 7;
    
    public enum PremiumType {
        NONE,
        DOUBLE_LETTER,
        TRIPLE_LETTER,
        DOUBLE_WORD,
        TRIPLE_WORD,
        CENTER
    }
    
    private final Tile[][] tiles;
    private final PremiumType[][] premiums;
    private final Set<Position> usedPremiums;
    
    @JsonCreator
    public Board(@JsonProperty("tiles") Tile[][] tiles, 
                 @JsonProperty("premiums") PremiumType[][] premiums,
                 @JsonProperty("usedPremiums") Set<Position> usedPremiums) {
        this.tiles = tiles != null ? tiles : new Tile[SIZE][SIZE];
        this.premiums = premiums != null ? premiums : createStandardPremiums();
        this.usedPremiums = usedPremiums != null ? usedPremiums : new HashSet<>();
    }
    
    /**
     * Creates a new empty board with standard premium squares
     */
    public static Board createStandard() {
        return new Board(new Tile[SIZE][SIZE], createStandardPremiums(), new HashSet<>());
    }
    
    /**
     * Creates standard Scrabble premium square layout
     */
    private static PremiumType[][] createStandardPremiums() {
        PremiumType[][] premiums = new PremiumType[SIZE][SIZE];
        
        // Initialize all squares to NONE
        for (int row = 0; row < SIZE; row++) {
            Arrays.fill(premiums[row], PremiumType.NONE);
        }
        
        // Triple Word Score
        int[][] tripleWord = {{0,0}, {0,7}, {0,14}, {7,0}, {7,14}, {14,0}, {14,7}, {14,14}};
        for (int[] pos : tripleWord) {
            premiums[pos[0]][pos[1]] = PremiumType.TRIPLE_WORD;
        }
        
        // Double Word Score
        int[][] doubleWord = {{1,1}, {2,2}, {3,3}, {4,4}, {1,13}, {2,12}, {3,11}, {4,10},
                             {13,1}, {12,2}, {11,3}, {10,4}, {13,13}, {12,12}, {11,11}, {10,10}};
        for (int[] pos : doubleWord) {
            premiums[pos[0]][pos[1]] = PremiumType.DOUBLE_WORD;
        }
        
        // Triple Letter Score
        int[][] tripleLetter = {{1,5}, {1,9}, {5,1}, {5,5}, {5,9}, {5,13}, {9,1}, {9,5}, 
                               {9,9}, {9,13}, {13,5}, {13,9}};
        for (int[] pos : tripleLetter) {
            premiums[pos[0]][pos[1]] = PremiumType.TRIPLE_LETTER;
        }
        
        // Double Letter Score
        int[][] doubleLetter = {{0,3}, {0,11}, {2,6}, {2,8}, {3,0}, {3,7}, {3,14}, {6,2},
                               {6,6}, {6,8}, {6,12}, {7,3}, {7,11}, {8,2}, {8,6}, {8,8},
                               {8,12}, {11,0}, {11,7}, {11,14}, {12,6}, {12,8}, {14,3}, {14,11}};
        for (int[] pos : doubleLetter) {
            premiums[pos[0]][pos[1]] = PremiumType.DOUBLE_LETTER;
        }
        
        // Center star
        premiums[CENTER][CENTER] = PremiumType.CENTER;
        
        return premiums;
    }
    
    /**
     * Places a tile at the specified position
     */
    public void placeTile(Position position, Tile tile) {
        if (!isValidPosition(position)) {
            throw new IllegalArgumentException("Invalid position: " + position);
        }
        if (tiles[position.getRow()][position.getCol()] != null) {
            throw new IllegalStateException("Position already occupied: " + position);
        }
        tiles[position.getRow()][position.getCol()] = tile;
        usedPremiums.add(position);
    }
    
    /**
     * Gets the tile at the specified position
     */
    public Tile getTile(Position position) {
        if (!isValidPosition(position)) {
            return null;
        }
        return tiles[position.getRow()][position.getCol()];
    }
    
    /**
     * Gets the premium type at the specified position
     */
    public PremiumType getPremium(Position position) {
        if (!isValidPosition(position)) {
            return PremiumType.NONE;
        }
        return premiums[position.getRow()][position.getCol()];
    }
    
    /**
     * Checks if a position is valid on the board
     */
    public boolean isValidPosition(Position position) {
        return position.getRow() >= 0 && position.getRow() < SIZE &&
               position.getCol() >= 0 && position.getCol() < SIZE;
    }
    
    /**
     * Checks if a position is empty
     */
    public boolean isEmpty(Position position) {
        return getTile(position) == null;
    }
    
    /**
     * Checks if a position is occupied
     */
    public boolean isOccupied(Position position) {
        return !isEmpty(position);
    }
    
    /**
     * Gets all occupied positions on the board
     */
    public Set<Position> getOccupiedPositions() {
        Set<Position> occupied = new HashSet<>();
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (tiles[row][col] != null) {
                    occupied.add(new Position(row, col));
                }
            }
        }
        return occupied;
    }
    
    /**
     * Gets all empty positions on the board
     */
    public Set<Position> getEmptyPositions() {
        Set<Position> empty = new HashSet<>();
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (tiles[row][col] == null) {
                    empty.add(new Position(row, col));
                }
            }
        }
        return empty;
    }
    
    /**
     * Checks if the board is empty
     */
    public boolean isEmpty() {
        return getOccupiedPositions().isEmpty();
    }
    
    /**
     * Gets adjacent positions (up, down, left, right)
     */
    public List<Position> getAdjacentPositions(Position position) {
        List<Position> adjacent = new ArrayList<>();
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        
        for (int[] dir : directions) {
            Position newPos = new Position(position.getRow() + dir[0], position.getCol() + dir[1]);
            if (isValidPosition(newPos)) {
                adjacent.add(newPos);
            }
        }
        return adjacent;
    }
    
    /**
     * Checks if a premium square has been used
     */
    public boolean isPremiumUsed(Position position) {
        return usedPremiums.contains(position);
    }
    
    /**
     * Creates a copy of this board
     */
    public Board copy() {
        Tile[][] newTiles = new Tile[SIZE][SIZE];
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (tiles[row][col] != null) {
                    newTiles[row][col] = tiles[row][col].copy();
                }
            }
        }
        
        PremiumType[][] newPremiums = new PremiumType[SIZE][SIZE];
        for (int row = 0; row < SIZE; row++) {
            System.arraycopy(premiums[row], 0, newPremiums[row], 0, SIZE);
        }
        
        return new Board(newTiles, newPremiums, new HashSet<>(usedPremiums));
    }
    
    /**
     * Returns a string representation of the board
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("   ");
        for (int col = 0; col < SIZE; col++) {
            sb.append(String.format("%2d ", col));
        }
        sb.append("\n");
        
        for (int row = 0; row < SIZE; row++) {
            sb.append(String.format("%2d ", row));
            for (int col = 0; col < SIZE; col++) {
                Tile tile = tiles[row][col];
                if (tile != null) {
                    sb.append(" ").append(tile.getDisplayLetter()).append(" ");
                } else {
                    PremiumType premium = premiums[row][col];
                    sb.append(switch (premium) {
                        case TRIPLE_WORD -> " * ";
                        case DOUBLE_WORD -> " # ";
                        case TRIPLE_LETTER -> " ^ ";
                        case DOUBLE_LETTER -> " + ";
                        case CENTER -> " ★ ";
                        default -> " . ";
                    });
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}