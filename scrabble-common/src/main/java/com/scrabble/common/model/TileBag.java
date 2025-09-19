package com.scrabble.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.*;
import java.util.random.RandomGenerator;

/**
 * Represents the tile bag containing all available tiles for drawing.
 */
@Data
public class TileBag {
    private final List<Tile> tiles;
    private final RandomGenerator random;
    
    @JsonCreator
    public TileBag(@JsonProperty("tiles") List<Tile> tiles) {
        this.tiles = tiles != null ? tiles : new ArrayList<>();
        this.random = RandomGenerator.getDefault();
    }
    
    /**
     * Creates a standard Scrabble tile bag with proper distribution
     */
    public static TileBag createStandard() {
        List<Tile> tiles = new ArrayList<>();
        
        // Add tiles according to standard Scrabble distribution
        for (char letter = 'A'; letter <= 'Z'; letter++) {
            int count = Tile.getStandardCount(letter);
            for (int i = 0; i < count; i++) {
                tiles.add(Tile.of(letter));
            }
        }
        
        // Add blank tiles
        int blankCount = Tile.getStandardCount(Tile.BLANK_LETTER);
        for (int i = 0; i < blankCount; i++) {
            tiles.add(Tile.blank());
        }
        
        Collections.shuffle(tiles);
        return new TileBag(tiles);
    }
    
    /**
     * Creates a custom tile bag with specified tiles
     */
    public static TileBag create(List<Tile> tiles) {
        List<Tile> shuffledTiles = new ArrayList<>(tiles);
        Collections.shuffle(shuffledTiles);
        return new TileBag(shuffledTiles);
    }
    
    /**
     * Draws a single tile from the bag
     * 
     * @return The drawn tile, or null if bag is empty
     */
    public Tile drawTile() {
        if (isEmpty()) {
            return null;
        }
        return tiles.remove(random.nextInt(tiles.size()));
    }
    
    /**
     * Draws multiple tiles from the bag
     * 
     * @param count Number of tiles to draw
     * @return List of drawn tiles (may be fewer than requested if bag doesn't have enough)
     */
    public List<Tile> drawTiles(int count) {
        List<Tile> drawn = new ArrayList<>();
        for (int i = 0; i < count && !isEmpty(); i++) {
            drawn.add(drawTile());
        }
        return drawn;
    }
    
    /**
     * Draws tiles to fill a player's rack to the specified size
     */
    public List<Tile> drawToFillRack(int currentRackSize, int targetSize) {
        int needed = Math.max(0, targetSize - currentRackSize);
        return drawTiles(needed);
    }
    
    /**
     * Returns tiles to the bag and shuffles
     */
    public void returnTiles(List<Tile> returnedTiles) {
        if (returnedTiles != null && !returnedTiles.isEmpty()) {
            tiles.addAll(returnedTiles);
            Collections.shuffle(tiles);
        }
    }
    
    /**
     * Exchanges tiles: takes tiles from player and returns new ones
     * 
     * @param tilesToExchange Tiles to put back in bag
     * @return New tiles drawn from bag (same count as exchanged)
     */
    public List<Tile> exchangeTiles(List<Tile> tilesToExchange) {
        if (tilesToExchange == null || tilesToExchange.isEmpty()) {
            return new ArrayList<>();
        }
        
        int exchangeCount = tilesToExchange.size();
        if (tiles.size() < exchangeCount) {
            // Not enough tiles in bag for exchange
            return new ArrayList<>();
        }
        
        // Draw new tiles first
        List<Tile> newTiles = drawTiles(exchangeCount);
        
        // Return the exchanged tiles to the bag
        returnTiles(tilesToExchange);
        
        return newTiles;
    }
    
    /**
     * Peeks at a tile without removing it (for testing purposes)
     */
    public Tile peekTile(int index) {
        if (index < 0 || index >= tiles.size()) {
            return null;
        }
        return tiles.get(index);
    }
    
    /**
     * Gets the number of tiles remaining in the bag
     */
    public int size() {
        return tiles.size();
    }
    
    /**
     * Checks if the bag is empty
     */
    public boolean isEmpty() {
        return tiles.isEmpty();
    }
    
    /**
     * Gets count of specific letters remaining in bag
     */
    public int getLetterCount(char letter) {
        char upperLetter = Character.toUpperCase(letter);
        return (int) tiles.stream()
                         .filter(tile -> !tile.isBlank() && tile.getDisplayLetter() == upperLetter)
                         .count();
    }
    
    /**
     * Gets count of blank tiles remaining in bag
     */
    public int getBlankCount() {
        return (int) tiles.stream()
                         .filter(Tile::isBlank)
                         .count();
    }
    
    /**
     * Gets a summary of tiles remaining in the bag
     */
    public Map<Character, Integer> getTileSummary() {
        Map<Character, Integer> summary = new HashMap<>();
        
        for (Tile tile : tiles) {
            char letter = tile.isBlank() ? Tile.BLANK_LETTER : tile.getDisplayLetter();
            summary.merge(letter, 1, Integer::sum);
        }
        
        return summary;
    }
    
    /**
     * Shuffles the tiles in the bag
     */
    public void shuffle() {
        Collections.shuffle(tiles);
    }
    
    /**
     * Creates a copy of this tile bag
     */
    public TileBag copy() {
        List<Tile> newTiles = tiles.stream()
                                  .map(Tile::copy)
                                  .toList();
        return new TileBag(new ArrayList<>(newTiles));
    }
    
    /**
     * Resets the bag to standard distribution
     */
    public void reset() {
        tiles.clear();
        TileBag standard = createStandard();
        tiles.addAll(standard.getTiles());
    }
    
    @Override
    public String toString() {
        Map<Character, Integer> summary = getTileSummary();
        StringBuilder sb = new StringBuilder();
        sb.append("TileBag(").append(size()).append(" tiles): ");
        
        summary.entrySet().stream()
               .sorted(Map.Entry.comparingByKey())
               .forEach(entry -> {
                   char letter = entry.getKey();
                   int count = entry.getValue();
                   sb.append(letter == Tile.BLANK_LETTER ? "?" : letter)
                     .append(":").append(count).append(" ");
               });
        
        return sb.toString().trim();
    }
}