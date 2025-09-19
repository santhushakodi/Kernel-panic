package com.scrabble.common.model;

/**
 * Represents the direction of word placement on the board.
 */
public enum Direction {
    HORIZONTAL, VERTICAL;
    
    /**
     * Returns the opposite direction
     */
    public Direction opposite() {
        return switch (this) {
            case HORIZONTAL -> VERTICAL;
            case VERTICAL -> HORIZONTAL;
        };
    }
    
    /**
     * Returns true if this direction is horizontal
     */
    public boolean isHorizontal() {
        return this == HORIZONTAL;
    }
    
    /**
     * Returns true if this direction is vertical
     */
    public boolean isVertical() {
        return this == VERTICAL;
    }
}