package com.scrabble.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents a Scrabble player with their rack, score, and game state.
 */
@Data
@JsonIgnoreProperties({"availableLetters", "rackSize", "rackEmpty", "rackFull", "rackCopy", "remainingTileValue", "bot"})
public class Player {
    public static final int RACK_SIZE = 7;
    
    public enum Status {
        WAITING,
        PLAYING,
        FINISHED,
        DISCONNECTED,
        TIMEOUT
    }
    
    private final String id;
    private final String name;
    @JsonProperty("isBot")
    private final boolean isBot;
    private List<Tile> rack;
    private int score;
    private Status status;
    private Duration timeRemaining;
    private LocalDateTime lastMoveTime;
    private int consecutivePasses;
    
    @JsonCreator
    public Player(@JsonProperty("id") String id,
                  @JsonProperty("name") String name,
                  @JsonProperty("isBot") boolean isBot,
                  @JsonProperty("rack") List<Tile> rack,
                  @JsonProperty("score") int score,
                  @JsonProperty("status") Status status,
                  @JsonProperty("timeRemaining") Duration timeRemaining,
                  @JsonProperty("lastMoveTime") LocalDateTime lastMoveTime,
                  @JsonProperty("consecutivePasses") int consecutivePasses) {
        this.id = id;
        this.name = name;
        this.isBot = isBot;
        this.rack = rack != null ? rack : new ArrayList<>();
        this.score = score;
        this.status = status != null ? status : Status.WAITING;
        this.timeRemaining = timeRemaining != null ? timeRemaining : Duration.ofMinutes(10);
        this.lastMoveTime = lastMoveTime;
        this.consecutivePasses = consecutivePasses;
    }
    
    /**
     * Creates a new human player
     */
    public static Player createHuman(String id, String name) {
        return new Player(id, name, false, new ArrayList<>(), 0, Status.WAITING,
                         Duration.ofMinutes(10), null, 0);
    }
    
    /**
     * Creates a new bot player
     */
    public static Player createBot(String id, String name) {
        return new Player(id, name, true, new ArrayList<>(), 0, Status.WAITING,
                         Duration.ofMinutes(10), null, 0);
    }
    
    /**
     * Adds a tile to the player's rack
     */
    public void addTileToRack(Tile tile) {
        if (rack.size() >= RACK_SIZE) {
            throw new IllegalStateException("Rack is already full");
        }
        rack.add(tile);
    }
    
    /**
     * Removes a tile from the player's rack
     */
    public boolean removeTileFromRack(Tile tile) {
        return rack.remove(tile);
    }
    
    /**
     * Removes a tile by letter (handles blank tiles)
     */
    public Tile removeTileByLetter(char letter) {
        char upperLetter = Character.toUpperCase(letter);
        
        // First, try to find exact match
        for (int i = 0; i < rack.size(); i++) {
            Tile tile = rack.get(i);
            if (tile.getDisplayLetter() == upperLetter && !tile.isBlank()) {
                return rack.remove(i);
            }
        }
        
        // If no exact match, look for blank tiles
        for (int i = 0; i < rack.size(); i++) {
            Tile tile = rack.get(i);
            if (tile.isBlank()) {
                Tile blankTile = rack.remove(i);
                blankTile.assignLetter(upperLetter);
                return blankTile;
            }
        }
        
        return null; // Tile not found
    }
    
    /**
     * Checks if the player has a specific letter in their rack
     */
    public boolean hasLetter(char letter) {
        char upperLetter = Character.toUpperCase(letter);
        
        // Check for direct match
        for (Tile tile : rack) {
            if (tile.getDisplayLetter() == upperLetter && !tile.isBlank()) {
                return true;
            }
        }
        
        // Check for blank tiles
        for (Tile tile : rack) {
            if (tile.isBlank()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Gets all letters available in the rack (including blank possibilities)
     */
    public Set<Character> getAvailableLetters() {
        Set<Character> letters = new HashSet<>();
        
        for (Tile tile : rack) {
            if (tile.isBlank()) {
                // Add all possible letters for blank tiles
                for (char c = 'A'; c <= 'Z'; c++) {
                    letters.add(c);
                }
            } else {
                letters.add(tile.getDisplayLetter());
            }
        }
        
        return letters;
    }
    
    /**
     * Adds points to the player's score
     */
    public void addScore(int points) {
        this.score += points;
    }
    
    /**
     * Shuffles the tiles in the rack
     */
    public void shuffleRack() {
        Collections.shuffle(rack);
    }
    
    /**
     * Gets the count of tiles in the rack
     */
    public int getRackSize() {
        return rack.size();
    }
    
    /**
     * Checks if the rack is empty
     */
    public boolean isRackEmpty() {
        return rack.isEmpty();
    }
    
    /**
     * Checks if the rack is full
     */
    public boolean isRackFull() {
        return rack.size() >= RACK_SIZE;
    }
    
    /**
     * Gets a copy of the rack
     */
    public List<Tile> getRackCopy() {
        return rack.stream()
                   .map(Tile::copy)
                   .toList();
    }
    
    /**
     * Records a pass (increases consecutive passes counter)
     */
    public void recordPass() {
        consecutivePasses++;
        lastMoveTime = LocalDateTime.now();
    }
    
    /**
     * Records a move (resets consecutive passes counter)
     */
    public void recordMove() {
        consecutivePasses = 0;
        lastMoveTime = LocalDateTime.now();
    }
    
    /**
     * Calculates the total value of tiles remaining in rack
     */
    public int getRemainingTileValue() {
        return rack.stream()
                   .mapToInt(Tile::getScoreValue)
                   .sum();
    }
    
    /**
     * Creates a copy of this player
     */
    public Player copy() {
        List<Tile> newRack = rack.stream()
                                 .map(Tile::copy)
                                 .toList();
        return new Player(id, name, isBot, new ArrayList<>(newRack), score, status,
                         timeRemaining, lastMoveTime, consecutivePasses);
    }
    
    @Override
    public String toString() {
        return String.format("%s (%s) - Score: %d, Rack: %s, Time: %s",
                           name, isBot ? "Bot" : "Human", score, rack, 
                           timeRemaining != null ? timeRemaining.toMinutes() + "m" : "N/A");
    }
}