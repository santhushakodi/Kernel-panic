package com.scrabble.client.model;

import com.scrabble.common.model.Position;
import com.scrabble.common.model.Tile;

/**
 * Represents a tile placement on the board, containing both the tile and its position.
 */
public class TilePlacement {
    private final Position position;
    private final Tile tile;

    public TilePlacement(Position position, Tile tile) {
        if (position == null) {
            throw new IllegalArgumentException("Position cannot be null");
        }
        if (tile == null) {
            throw new IllegalArgumentException("Tile cannot be null");
        }
        
        this.position = position;
        this.tile = tile;
    }

    public Position getPosition() {
        return position;
    }

    public Tile getTile() {
        return tile;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        TilePlacement that = (TilePlacement) obj;
        return position.equals(that.position) && tile.equals(that.tile);
    }

    @Override
    public int hashCode() {
        return position.hashCode() * 31 + tile.hashCode();
    }

    @Override
    public String toString() {
        return String.format("TilePlacement{%s at %s}", tile.getDisplayLetter(), position);
    }
}