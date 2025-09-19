package com.scrabble.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;

import java.util.Objects;

/**
 * Represents a Scrabble tile with a letter and point value.
 * Blank tiles are represented with the letter '?' and have a value of 0.
 */
@Data
public class Tile {
    public static final char BLANK_LETTER = '?';
    public static final int BLANK_VALUE = 0;
    
    private final char letter;
    private final int value;
    private char assignedLetter; // For blank tiles
    
    @JsonCreator
    public Tile(@JsonProperty("letter") char letter, @JsonProperty("value") int value) {
        this.letter = Character.toUpperCase(letter);
        this.value = value;
        this.assignedLetter = this.letter;
    }
    
    /**
     * Creates a tile with standard Scrabble letter values
     */
    public static Tile of(char letter) {
        char upperLetter = Character.toUpperCase(letter);
        return new Tile(upperLetter, getStandardValue(upperLetter));
    }
    
    /**
     * Creates a blank tile
     */
    public static Tile blank() {
        return new Tile(BLANK_LETTER, BLANK_VALUE);
    }
    
    /**
     * Creates a copy of this tile
     */
    public Tile copy() {
        Tile tile = new Tile(this.letter, this.value);
        tile.assignedLetter = this.assignedLetter;
        return tile;
    }
    
    /**
     * Returns true if this is a blank tile
     */
    public boolean isBlank() {
        return letter == BLANK_LETTER;
    }
    
    /**
     * Assigns a letter to a blank tile
     */
    public void assignLetter(char letter) {
        if (!isBlank()) {
            throw new IllegalStateException("Cannot assign letter to non-blank tile");
        }
        this.assignedLetter = Character.toUpperCase(letter);
    }
    
    /**
     * Gets the display letter (assigned letter for blanks, original letter otherwise)
     */
    public char getDisplayLetter() {
        return assignedLetter;
    }
    
    /**
     * Gets the point value for scoring (0 for blank tiles regardless of assignment)
     */
    public int getScoreValue() {
        return value;
    }
    
    /**
     * Returns standard Scrabble letter values
     */
    public static int getStandardValue(char letter) {
        return switch (Character.toUpperCase(letter)) {
            case 'A', 'E', 'I', 'L', 'N', 'O', 'R', 'S', 'T', 'U' -> 1;
            case 'D', 'G' -> 2;
            case 'B', 'C', 'M', 'P' -> 3;
            case 'F', 'H', 'V', 'W', 'Y' -> 4;
            case 'K' -> 5;
            case 'J', 'X' -> 8;
            case 'Q', 'Z' -> 10;
            case BLANK_LETTER -> BLANK_VALUE;
            default -> throw new IllegalArgumentException("Invalid letter: " + letter);
        };
    }
    
    /**
     * Returns the standard distribution of tiles in Scrabble
     */
    public static int getStandardCount(char letter) {
        return switch (Character.toUpperCase(letter)) {
            case 'A', 'I' -> 9;
            case 'E' -> 12;
            case 'O' -> 8;
            case 'R', 'N', 'T' -> 6;
            case 'L', 'S', 'U' -> 4;
            case 'D' -> 4;
            case 'G' -> 3;
            case 'B', 'C', 'M', 'P', 'F', 'H', 'V', 'W', 'Y' -> 2;
            case 'K', 'J', 'X', 'Q', 'Z' -> 1;
            case BLANK_LETTER -> 2;
            default -> 0;
        };
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Tile tile = (Tile) obj;
        return letter == tile.letter && 
               value == tile.value && 
               assignedLetter == tile.assignedLetter;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(letter, value, assignedLetter);
    }
    
    @Override
    public String toString() {
        if (isBlank()) {
            return assignedLetter == BLANK_LETTER ? "?" : assignedLetter + "*";
        }
        return String.valueOf(letter);
    }
}