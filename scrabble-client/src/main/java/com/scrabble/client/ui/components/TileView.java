package com.scrabble.client.ui.components;

import com.scrabble.common.model.Tile;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.input.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UI component representing a draggable Scrabble tile.
 * Handles drag-and-drop operations, visual states, and tile information display.
 */
public class TileView extends StackPane {
    private static final Logger logger = LoggerFactory.getLogger(TileView.class);
    
    public static final double TILE_SIZE = 40.0;
    public static final String DRAG_FORMAT = "application/scrabble-tile";
    
    private final Tile tile;
    private final Label letterLabel;
    private final Label pointLabel;
    private final VBox contentBox;
    
    private boolean isDraggable = true;
    private boolean isSelected = false;
    private boolean isOnBoard = false;
    private TileDropTarget originalParent;
    
    // Event handlers
    private Runnable onTileSelected;
    private Runnable onTileDeselected;
    
    public TileView(Tile tile) {
        this.tile = tile;
        
        // Create main content container
        this.contentBox = new VBox();
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setSpacing(-2);
        contentBox.setPadding(new Insets(2, 4, 2, 4));
        
        // Create letter label
        this.letterLabel = new Label(String.valueOf(tile.getDisplayLetter()));
        letterLabel.getStyleClass().add("tile-letter");
        letterLabel.setMouseTransparent(true);
        
        // Create point value label (smaller, bottom-right)
        this.pointLabel = new Label(String.valueOf(tile.getValue()));
        pointLabel.getStyleClass().add("tile-points");
        pointLabel.setMouseTransparent(true);
        
        // Add labels to content
        contentBox.getChildren().addAll(letterLabel, pointLabel);
        
        // Add content to tile
        getChildren().add(contentBox);
        
        // Set default styling
        getStyleClass().addAll("tile");
        if (tile.isBlank()) {
            getStyleClass().add("blank");
        }
        
        // Set size constraints
        setMinSize(TILE_SIZE, TILE_SIZE);
        setMaxSize(TILE_SIZE, TILE_SIZE);
        setPrefSize(TILE_SIZE, TILE_SIZE);
        
        // Setup interactions
        setupDragAndDrop();
        setupClickHandlers();
        
        logger.debug("Created TileView for tile: {} ({})", tile.getDisplayLetter(), tile.getValue());
    }
    
    private void setupDragAndDrop() {
        // Drag detected - start drag operation
        setOnDragDetected(event -> {
            if (!isDraggable) {
                return;
            }
            
            logger.debug("Drag detected for tile: {}", tile.getDisplayLetter());
            
            Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.put(DataFormat.PLAIN_TEXT, tile.getDisplayLetter() + ":" + tile.getValue() + ":" + tile.isBlank());
            content.putString(DRAG_FORMAT);
            dragboard.setContent(content);
            
            // Create drag view
            dragboard.setDragView(snapshot(null, null));
            
            // Visual feedback
            setOpacity(0.7);
            getStyleClass().add("dragging");
            
            event.consume();
        });
        
        // Drag done - cleanup
        setOnDragDone(event -> {
            logger.debug("Drag done for tile: {}, transfer mode: {}", tile.getDisplayLetter(), event.getTransferMode());
            
            // Reset visual state
            setOpacity(1.0);
            getStyleClass().remove("dragging");
            
            // If move was successful, the tile will be handled by the drop target
            if (event.getTransferMode() == TransferMode.MOVE) {
                // The tile has been moved successfully
                logger.debug("Tile {} moved successfully", tile.getDisplayLetter());
            }
            
            event.consume();
        });
        
        // Mouse entered - show hand cursor for draggable tiles
        setOnMouseEntered(event -> {
            if (isDraggable) {
                setCursor(Cursor.HAND);
                if (!isSelected) {
                    getStyleClass().add("hover");
                }
            }
        });
        
        // Mouse exited - reset cursor
        setOnMouseExited(event -> {
            setCursor(Cursor.DEFAULT);
            getStyleClass().remove("hover");
        });
    }
    
    private void setupClickHandlers() {
        // Click to select/deselect tile
        setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && isDraggable) {
                if (isSelected) {
                    deselect();
                } else {
                    select();
                }
                event.consume();
            }
        });
    }
    
    public void select() {
        if (!isSelected) {
            isSelected = true;
            getStyleClass().add("selected");
            logger.debug("Tile {} selected", tile.getDisplayLetter());
            
            if (onTileSelected != null) {
                onTileSelected.run();
            }
        }
    }
    
    public void deselect() {
        if (isSelected) {
            isSelected = false;
            getStyleClass().remove("selected");
            logger.debug("Tile {} deselected", tile.getDisplayLetter());
            
            if (onTileDeselected != null) {
                onTileDeselected.run();
            }
        }
    }
    
    public void setDraggable(boolean draggable) {
        this.isDraggable = draggable;
        
        if (!draggable) {
            setCursor(Cursor.DEFAULT);
            getStyleClass().add("fixed");
            deselect();
        } else {
            getStyleClass().remove("fixed");
        }
        
        logger.debug("Tile {} draggable set to: {}", tile.getDisplayLetter(), draggable);
    }
    
    public void setOnBoard(boolean onBoard) {
        this.isOnBoard = onBoard;
        
        if (onBoard) {
            getStyleClass().add("on-board");
            setDraggable(false);
        } else {
            getStyleClass().remove("on-board");
        }
        
        logger.debug("Tile {} on board set to: {}", tile.getDisplayLetter(), onBoard);
    }
    
    public void updateDisplayLetter(char letter) {
        letterLabel.setText(String.valueOf(Character.toUpperCase(letter)));
        logger.debug("Updated tile display letter to: {}", letter);
    }
    
    public void showError() {
        getStyleClass().add("error");
        // Remove error styling after a delay
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
        pause.setOnFinished(e -> getStyleClass().remove("error"));
        pause.play();
    }
    
    public void showSuccess() {
        getStyleClass().add("success");
        // Remove success styling after a delay
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1));
        pause.setOnFinished(e -> getStyleClass().remove("success"));
        pause.play();
    }
    
    // Getters and setters
    public Tile getTile() {
        return tile;
    }
    
    public boolean isSelected() {
        return isSelected;
    }
    
    public boolean isDraggable() {
        return isDraggable;
    }
    
    public boolean isOnBoard() {
        return isOnBoard;
    }
    
    public void setOriginalParent(TileDropTarget parent) {
        this.originalParent = parent;
    }
    
    public TileDropTarget getOriginalParent() {
        return originalParent;
    }
    
    public void setOnTileSelected(Runnable handler) {
        this.onTileSelected = handler;
    }
    
    public void setOnTileDeselected(Runnable handler) {
        this.onTileDeselected = handler;
    }
    
    @Override
    public String toString() {
        return String.format("TileView{tile=%s, selected=%s, draggable=%s, onBoard=%s}", 
                           tile.getDisplayLetter(), isSelected, isDraggable, isOnBoard);
    }
}

/**
 * Interface for components that can accept tile drops
 */
interface TileDropTarget {
    void onTileDropped(TileView tileView);
    void onTileRemoved(TileView tileView);
    boolean canAcceptTile(TileView tileView);
}