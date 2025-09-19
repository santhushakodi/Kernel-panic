package com.scrabble.common.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

class TileTest {
    
    @Test
    @DisplayName("Create regular tile with correct letter and value")
    void testCreateRegularTile() {
        Tile tile = Tile.of('A');
        
        assertEquals('A', tile.getLetter());
        assertEquals(1, tile.getValue());
        assertEquals('A', tile.getDisplayLetter());
        assertFalse(tile.isBlank());
        assertEquals(1, tile.getScoreValue());
    }
    
    @Test
    @DisplayName("Create blank tile")
    void testCreateBlankTile() {
        Tile blankTile = Tile.blank();
        
        assertEquals('?', blankTile.getLetter());
        assertEquals(0, blankTile.getValue());
        assertTrue(blankTile.isBlank());
        assertEquals(0, blankTile.getScoreValue());
    }
    
    @Test
    @DisplayName("Assign letter to blank tile")
    void testAssignLetterToBlank() {
        Tile blankTile = Tile.blank();
        blankTile.assignLetter('S');
        
        assertEquals('?', blankTile.getLetter()); // Original letter unchanged
        assertEquals('S', blankTile.getDisplayLetter()); // Display letter changed
        assertEquals(0, blankTile.getScoreValue()); // Still worth 0 points
        assertTrue(blankTile.isBlank());
    }
    
    @Test
    @DisplayName("Cannot assign letter to non-blank tile")
    void testCannotAssignLetterToNonBlank() {
        Tile regularTile = Tile.of('A');
        
        assertThrows(IllegalStateException.class, () -> {
            regularTile.assignLetter('B');
        });
    }
    
    @Test
    @DisplayName("Standard letter values are correct")
    void testStandardLetterValues() {
        assertEquals(1, Tile.getStandardValue('A'));
        assertEquals(1, Tile.getStandardValue('E'));
        assertEquals(2, Tile.getStandardValue('D'));
        assertEquals(3, Tile.getStandardValue('B'));
        assertEquals(4, Tile.getStandardValue('F'));
        assertEquals(5, Tile.getStandardValue('K'));
        assertEquals(8, Tile.getStandardValue('J'));
        assertEquals(10, Tile.getStandardValue('Q'));
        assertEquals(0, Tile.getStandardValue('?'));
    }
    
    @Test
    @DisplayName("Standard tile counts are correct")
    void testStandardTileCounts() {
        assertEquals(12, Tile.getStandardCount('E'));
        assertEquals(9, Tile.getStandardCount('A'));
        assertEquals(8, Tile.getStandardCount('O'));
        assertEquals(6, Tile.getStandardCount('R'));
        assertEquals(4, Tile.getStandardCount('L'));
        assertEquals(2, Tile.getStandardCount('F'));
        assertEquals(1, Tile.getStandardCount('Q'));
        assertEquals(2, Tile.getStandardCount('?'));
    }
    
    @Test
    @DisplayName("Invalid letter throws exception")
    void testInvalidLetter() {
        assertThrows(IllegalArgumentException.class, () -> {
            Tile.getStandardValue('1');
        });
    }
    
    @Test
    @DisplayName("Tile equality works correctly")
    void testTileEquality() {
        Tile tile1 = Tile.of('A');
        Tile tile2 = Tile.of('A');
        Tile tile3 = Tile.of('B');
        
        assertEquals(tile1, tile2);
        assertNotEquals(tile1, tile3);
        
        // Test with blank tiles
        Tile blank1 = Tile.blank();
        Tile blank2 = Tile.blank();
        assertEquals(blank1, blank2);
        
        blank1.assignLetter('A');
        blank2.assignLetter('A');
        assertEquals(blank1, blank2);
        
        blank2.assignLetter('B');
        assertNotEquals(blank1, blank2);
    }
    
    @Test
    @DisplayName("Tile copy works correctly")
    void testTileCopy() {
        Tile original = Tile.of('A');
        Tile copy = original.copy();
        
        assertEquals(original, copy);
        assertNotSame(original, copy);
        
        // Test with blank tile
        Tile blankOriginal = Tile.blank();
        blankOriginal.assignLetter('X');
        Tile blankCopy = blankOriginal.copy();
        
        assertEquals(blankOriginal, blankCopy);
        assertNotSame(blankOriginal, blankCopy);
        assertEquals('X', blankCopy.getDisplayLetter());
    }
    
    @Test
    @DisplayName("Tile toString works correctly")
    void testTileToString() {
        Tile regularTile = Tile.of('A');
        assertEquals("A", regularTile.toString());
        
        Tile blankTile = Tile.blank();
        assertEquals("?", blankTile.toString());
        
        blankTile.assignLetter('S');
        assertEquals("S*", blankTile.toString());
    }
    
    @Test
    @DisplayName("Case insensitive tile creation")
    void testCaseInsensitiveCreation() {
        Tile upperTile = Tile.of('a');
        assertEquals('A', upperTile.getLetter());
        assertEquals('A', upperTile.getDisplayLetter());
        
        Tile blankTile = Tile.blank();
        blankTile.assignLetter('x');
        assertEquals('X', blankTile.getDisplayLetter());
    }
}