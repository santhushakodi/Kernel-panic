package com.scrabble.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Objects;

/**
 * Represents a position on the Scrabble board (row, column).
 */
@Data
public class Position {
    private final int row;
    private final int col;
    
    @JsonCreator
    public Position(@JsonProperty("row") int row, @JsonProperty("col") int col) {
        this.row = row;
        this.col = col;
    }
    
    /**
     * Creates a position from string notation (e.g., "H8" -> row=7, col=7)
     */
    public static Position fromNotation(String notation) {
        if (notation == null || notation.length() < 2 || notation.length() > 3) {
            throw new IllegalArgumentException("Invalid position notation: " + notation);
        }
        
        char colChar = Character.toUpperCase(notation.charAt(0));
        if (colChar < 'A' || colChar > 'O') {
            throw new IllegalArgumentException("Invalid column: " + colChar);
        }
        
        int rowNum;
        try {
            rowNum = Integer.parseInt(notation.substring(1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid row: " + notation.substring(1));
        }
        
        if (rowNum < 1 || rowNum > 15) {
            throw new IllegalArgumentException("Invalid row: " + rowNum);
        }
        
        return new Position(rowNum - 1, colChar - 'A');
    }
    
    /**
     * Converts position to string notation (e.g., row=7, col=7 -> "H8")
     */
    public String toNotation() {
        if (row < 0 || row >= Board.SIZE || col < 0 || col >= Board.SIZE) {
            throw new IllegalStateException("Invalid position: " + this);
        }
        char colChar = (char) ('A' + col);
        int rowNum = row + 1;
        return "" + colChar + rowNum;
    }
    
    /**
     * Returns the position to the right
     */
    public Position right() {
        return new Position(row, col + 1);
    }
    
    /**
     * Returns the position to the left
     */
    public Position left() {
        return new Position(row, col - 1);
    }
    
    /**
     * Returns the position above
     */
    public Position up() {
        return new Position(row - 1, col);
    }
    
    /**
     * Returns the position below
     */
    public Position down() {
        return new Position(row + 1, col);
    }
    
    /**
     * Returns the position moved in the given direction
     */
    public Position move(Direction direction) {
        return switch (direction) {
            case HORIZONTAL -> right();
            case VERTICAL -> down();
        };
    }
    
    /**
     * Calculates the distance between this position and another
     */
    public int distance(Position other) {
        return Math.abs(row - other.row) + Math.abs(col - other.col);
    }
    
    /**
     * Checks if this position is adjacent to another (horizontally or vertically)
     */
    public boolean isAdjacentTo(Position other) {
        return distance(other) == 1;
    }
    
    /**
     * Checks if this position is in the same row as another
     */
    public boolean isSameRow(Position other) {
        return row == other.row;
    }
    
    /**
     * Checks if this position is in the same column as another
     */
    public boolean isSameColumn(Position other) {
        return col == other.col;
    }
    
    /**
     * Checks if this position is aligned (same row or column) with another
     */
    public boolean isAlignedWith(Position other) {
        return isSameRow(other) || isSameColumn(other);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Position position = (Position) obj;
        return row == position.row && col == position.col;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }
    
    @Override
    public String toString() {
        try {
            return toNotation();
        } catch (IllegalStateException e) {
            return String.format("(%d, %d)", row, col);
        }
    }
}