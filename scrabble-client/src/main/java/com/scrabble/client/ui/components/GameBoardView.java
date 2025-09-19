package com.scrabble.client.ui.components;

import com.scrabble.common.model.Board;
import com.scrabble.common.model.Position;
import com.scrabble.common.model.Tile;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Interactive 15x15 Scrabble game board component.
 * Handles tile placement, premium squares display, and drag-and-drop operations.
 */
public class GameBoardView extends GridPane {
    private static final Logger logger = LoggerFactory.getLogger(GameBoardView.class);
    
    public static final int BOARD_SIZE = 15;
    public static final double SQUARE_SIZE = 35.0;
    
    private final Board gameBoard;
    private final BoardSquareView[][] boardSquares;
    private final Map<Position, TileView> placedTiles;
    private final Map<Position, TileView> pendingTiles; // Tiles placed but not confirmed
    
    // Event handlers
    private Consumer<Position> onSquareClicked;
    private Consumer<TilePlacement> onTilePlaced;
    private Consumer<Position> onTileRemoved;
    
    public GameBoardView() {
        this.gameBoard = Board.createStandard();
        this.boardSquares = new BoardSquareView[BOARD_SIZE][BOARD_SIZE];
        this.placedTiles = new HashMap<>();
        this.pendingTiles = new HashMap<>();
        
        setupBoard();
        setupStyling();
        
        logger.info("GameBoardView created with {}x{} squares", BOARD_SIZE, BOARD_SIZE);
    }
    
    private void setupBoard() {
        // Create all board squares
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                Position position = new Position(row, col);
                BoardSquareView square = new BoardSquareView(position);
                
                boardSquares[row][col] = square;
                add(square, col, row);
                
                // Set grid positioning
                GridPane.setHalignment(square, HPos.CENTER);
                GridPane.setValignment(square, VPos.CENTER);
            }
        }
        
        // Set gap between squares
        setHgap(1);
        setVgap(1);
        setAlignment(Pos.CENTER);
    }
    
    private void setupStyling() {
        getStyleClass().add("game-board");
        setStyle("-fx-background-color: #8b7355; -fx-padding: 10;");
    }
    
    /**
     * Updates the board with tiles from the game state
     */
    public void updateBoard(Board board) {
        logger.debug("Updating board with new game state");
        
        // Clear pending tiles
        clearPendingTiles();
        
        // Update placed tiles
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                Position position = new Position(row, col);
                Tile tile = board.getTile(position);
                
                if (tile != null) {
                    // Place confirmed tile
                    TileView existingTileView = placedTiles.get(position);
                    if (existingTileView == null) {
                        TileView tileView = new TileView(tile);
                        placeTileAt(position, tileView, true);
                    }
                } else {
                    // Remove tile if it was removed from board
                    removeTileAt(position, true);
                }
            }
        }
    }
    
    /**
     * Places a tile at the specified position
     */
    public void placeTileAt(Position position, TileView tileView, boolean confirmed) {
        if (position.getRow() < 0 || position.getRow() >= BOARD_SIZE || 
            position.getCol() < 0 || position.getCol() >= BOARD_SIZE) {
            logger.warn("Attempted to place tile at invalid position: {}", position);
            return;
        }
        
        BoardSquareView square = boardSquares[position.getRow()][position.getCol()];
        
        // Remove existing tile if any
        TileView existingTile = confirmed ? placedTiles.get(position) : pendingTiles.get(position);
        if (existingTile != null) {
            square.removeTile();
        }
        
        // Place new tile
        square.placeTile(tileView);
        tileView.setOnBoard(true);
        
        if (confirmed) {
            placedTiles.put(position, tileView);
            pendingTiles.remove(position);
        } else {
            pendingTiles.put(position, tileView);
        }
        
        logger.debug("Placed tile {} at position {} (confirmed: {})", 
                    tileView.getTile().getDisplayLetter(), position, confirmed);
    }
    
    /**
     * Removes a tile from the specified position
     */
    public void removeTileAt(Position position, boolean confirmed) {
        if (position.getRow() < 0 || position.getRow() >= BOARD_SIZE || 
            position.getCol() < 0 || position.getCol() >= BOARD_SIZE) {
            return;
        }
        
        BoardSquareView square = boardSquares[position.getRow()][position.getCol()];
        TileView tileView = confirmed ? placedTiles.remove(position) : pendingTiles.remove(position);
        
        if (tileView != null) {
            square.removeTile();
            tileView.setOnBoard(false);
            
            logger.debug("Removed tile from position {} (confirmed: {})", position, confirmed);
            
            if (onTileRemoved != null) {
                onTileRemoved.accept(position);
            }
        }
    }
    
    /**
     * Gets the position for a board square
     */
    public Position getPositionForSquare(BoardSquareView square) {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if (boardSquares[row][col] == square) {
                    return new Position(row, col);
                }
            }
        }
        return null;
    }
    
    /**
     * Clears all pending (unconfirmed) tile placements
     */
    public void clearPendingTiles() {
        logger.debug("Clearing {} pending tiles", pendingTiles.size());
        
        for (Map.Entry<Position, TileView> entry : pendingTiles.entrySet()) {
            Position position = entry.getKey();
            TileView tileView = entry.getValue();
            
            BoardSquareView square = boardSquares[position.getRow()][position.getCol()];
            square.removeTile();
            tileView.setOnBoard(false);
        }
        
        pendingTiles.clear();
    }
    
    /**
     * Confirms all pending tile placements
     */
    public void confirmPendingTiles() {
        logger.debug("Confirming {} pending tiles", pendingTiles.size());
        
        for (Map.Entry<Position, TileView> entry : pendingTiles.entrySet()) {
            Position position = entry.getKey();
            TileView tileView = entry.getValue();
            
            placedTiles.put(position, tileView);
        }
        
        pendingTiles.clear();
    }
    
    /**
     * Gets all pending tile placements
     */
    public Map<Position, TileView> getPendingTiles() {
        return new HashMap<>(pendingTiles);
    }
    
    /**
     * Gets all placed tiles
     */
    public Map<Position, TileView> getPlacedTiles() {
        return new HashMap<>(placedTiles);
    }
    
    // Event handler setters
    public void setOnSquareClicked(Consumer<Position> handler) {
        this.onSquareClicked = handler;
    }
    
    public void setOnTilePlaced(Consumer<TilePlacement> handler) {
        this.onTilePlaced = handler;
    }
    
    public void setOnTileRemoved(Consumer<Position> handler) {
        this.onTileRemoved = handler;
    }
    
    /**
     * Shows move validation results on the board (compatibility method)
     */
    public void showMoveValidation(com.scrabble.client.validation.MoveValidationResult result) {
        // For now, just log the validation result
        // In a full implementation, this would highlight valid/invalid positions
        if (result.isValid()) {
            logger.debug("Move validation successful: score={}", result.getScore());
        } else {
            logger.debug("Move validation failed: {}", result.getErrorMessage());
        }
        
        // Future: Add visual indicators on the board for valid/invalid moves
    }
    
    /**
     * Individual board square that can accept tile drops
     */
    private class BoardSquareView extends StackPane implements TileDropTarget {
        private final Position position;
        private final Label premiumLabel;
        private TileView currentTile;
        
        public BoardSquareView(Position position) {
            this.position = position;
            this.premiumLabel = new Label();
            
            setupSquare();
            setupDragAndDrop();
        }
        
        private void setupSquare() {
            // Set size constraints
            setMinSize(SQUARE_SIZE, SQUARE_SIZE);
            setMaxSize(SQUARE_SIZE, SQUARE_SIZE);
            setPrefSize(SQUARE_SIZE, SQUARE_SIZE);
            
            // Determine square type and styling
            String squareType = determineSquareType(position);
            getStyleClass().addAll("board-square", squareType);
            
            // Set premium square label
            String labelText = getPremiumLabel(squareType);
            if (!labelText.isEmpty()) {
                premiumLabel.setText(labelText);
                premiumLabel.getStyleClass().add("premium-label");
                premiumLabel.setMouseTransparent(true);
                getChildren().add(premiumLabel);
            }
            
            // Click handler
            setOnMouseClicked(event -> {
                if (onSquareClicked != null) {
                    onSquareClicked.accept(position);
                }
            });
        }
        
        private void setupDragAndDrop() {
            // Accept drag over
            setOnDragOver(event -> {
                if (event.getDragboard().hasString() && canAcceptDrop()) {
                    event.acceptTransferModes(TransferMode.MOVE);
                    getStyleClass().add("drop-target");
                }
                event.consume();
            });
            
            // Remove drop target styling
            setOnDragExited(event -> {
                getStyleClass().remove("drop-target");
                event.consume();
            });
            
            // Handle tile drop
            setOnDragDropped(event -> {
                Dragboard dragboard = event.getDragboard();
                boolean success = false;
                
                if (dragboard.hasString() && canAcceptDrop()) {
                    // Find the source tile view
                    TileView sourceTile = findDraggedTile(event);
                    if (sourceTile != null) {
                        // Place tile on this square
                        onTileDropped(sourceTile);
                        success = true;
                    }
                }
                
                event.setDropCompleted(success);
                getStyleClass().remove("drop-target");
                event.consume();
            });
        }
        
        private boolean canAcceptDrop() {
            return currentTile == null; // Can only drop on empty squares
        }
        
        private TileView findDraggedTile(DragEvent event) {
            // This is a bit of a hack - in a real implementation, you'd want
            // a more robust way to find the source tile
            Object source = event.getSource();
            if (source instanceof TileView) {
                return (TileView) source;
            }
            
            // Look for the tile being dragged in the scene
            return null; // For now, will be handled by proper drag/drop implementation
        }
        
        public void placeTile(TileView tileView) {
            if (currentTile != null) {
                getChildren().remove(currentTile);
            }
            
            currentTile = tileView;
            getChildren().add(tileView);
            
            // Hide premium label when tile is placed
            if (premiumLabel != null) {
                premiumLabel.setVisible(false);
            }
        }
        
        public void removeTile() {
            if (currentTile != null) {
                getChildren().remove(currentTile);
                currentTile = null;
                
                // Show premium label when tile is removed
                if (premiumLabel != null) {
                    premiumLabel.setVisible(true);
                }
            }
        }
        
        @Override
        public void onTileDropped(TileView tileView) {
            placeTile(tileView);
            
            if (onTilePlaced != null) {
                onTilePlaced.accept(new TilePlacement(position, tileView));
            }
        }
        
        @Override
        public void onTileRemoved(TileView tileView) {
            removeTile();
        }
        
        @Override
        public boolean canAcceptTile(TileView tileView) {
            return currentTile == null;
        }
    }
    
    /**
     * Determines the premium square type based on position
     */
    private String determineSquareType(Position position) {
        int row = position.getRow();
        int col = position.getCol();
        
        // Center star
        if (row == 7 && col == 7) {
            return "center-star";
        }
        
        // Triple word scores (corners and middle edges)
        if ((row == 0 || row == 14) && (col == 0 || col == 7 || col == 14) ||
            (row == 7) && (col == 0 || col == 14)) {
            return "triple-word";
        }
        
        // Double word scores
        if (row == col && row >= 1 && row <= 4 ||
            row == col && row >= 10 && row <= 13 ||
            row + col == 14 && row >= 1 && row <= 4 ||
            row + col == 14 && row >= 10 && row <= 13) {
            return "double-word";
        }
        
        // Triple letter scores
        if ((row == 1 || row == 13) && (col == 5 || col == 9) ||
            (row == 5 || row == 9) && (col == 1 || col == 5 || col == 9 || col == 13)) {
            return "triple-letter";
        }
        
        // Double letter scores
        if ((row == 0 || row == 14) && (col == 3 || col == 11) ||
            (row == 2 || row == 12) && (col == 6 || col == 8) ||
            (row == 3 || row == 11) && (col == 0 || col == 7 || col == 14) ||
            (row == 6 || row == 8) && (col == 2 || col == 6 || col == 8 || col == 12) ||
            (row == 7) && (col == 3 || col == 11)) {
            return "double-letter";
        }
        
        return "normal";
    }
    
    /**
     * Gets the display label for premium squares
     */
    private String getPremiumLabel(String squareType) {
        return switch (squareType) {
            case "center-star" -> "★";
            case "triple-word" -> "3W";
            case "double-word" -> "2W";
            case "triple-letter" -> "3L";
            case "double-letter" -> "2L";
            default -> "";
        };
    }
    
    /**
     * Data class for tile placement events
     */
    public static class TilePlacement {
        private final Position position;
        private final TileView tileView;
        
        public TilePlacement(Position position, TileView tileView) {
            this.position = position;
            this.tileView = tileView;
        }
        
        public Position getPosition() { return position; }
        public TileView getTileView() { return tileView; }
    }
}