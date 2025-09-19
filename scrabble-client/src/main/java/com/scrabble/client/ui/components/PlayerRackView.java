package com.scrabble.client.ui.components;

import com.scrabble.common.model.Tile;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Player tile rack component that displays and manages a player's tiles.
 * Supports drag-and-drop operations, tile selection, and shuffling.
 */
public class PlayerRackView extends VBox implements TileDropTarget {
    private static final Logger logger = LoggerFactory.getLogger(PlayerRackView.class);
    
    public static final int MAX_RACK_SIZE = 7;
    private static final double RACK_HEIGHT = 60.0;
    
    private final String playerName;
    private final Label playerLabel;
    private final HBox rackContainer;
    private final ObservableList<TileView> tileViews;
    private final List<TileView> selectedTiles;
    
    private boolean isCurrentPlayerTurn = false;
    private boolean isExchangeMode = false;
    
    // Event handlers
    private Consumer<TileView> onTileSelected;
    private Consumer<TileView> onTileDeselected;
    private Consumer<TileView> onTileRemoved;
    private Runnable onTileShuffled;
    
    public PlayerRackView(String playerName) {
        this.playerName = playerName;
        this.tileViews = FXCollections.observableArrayList();
        this.selectedTiles = new ArrayList<>();
        
        // Create player label
        this.playerLabel = new Label(playerName);
        playerLabel.getStyleClass().addAll("player-label", "rack-label");
        
        // Create rack container
        this.rackContainer = new HBox();
        rackContainer.setAlignment(Pos.CENTER_LEFT);
        rackContainer.setSpacing(5);
        rackContainer.setPadding(new Insets(5, 10, 5, 10));
        rackContainer.getStyleClass().add("player-rack");
        rackContainer.setMinHeight(RACK_HEIGHT);
        rackContainer.setMaxHeight(RACK_HEIGHT);
        
        // Layout
        getChildren().addAll(playerLabel, rackContainer);
        VBox.setVgrow(rackContainer, Priority.ALWAYS);
        
        setupDragAndDrop();
        setupStyling();
        
        logger.debug("Created PlayerRackView for player: {}", playerName);
    }
    
    private void setupStyling() {
        getStyleClass().add("player-rack-view");
        setSpacing(5);
        setPadding(new Insets(5));
    }
    
    private void setupDragAndDrop() {
        // Accept tiles dropped back to rack
        rackContainer.setOnDragOver(event -> {
            if (event.getDragboard().hasString() && canAcceptTileFromBoard()) {
                event.acceptTransferModes(TransferMode.MOVE);
                rackContainer.getStyleClass().add("drop-target");
            }
            event.consume();
        });
        
        rackContainer.setOnDragExited(event -> {
            rackContainer.getStyleClass().remove("drop-target");
            event.consume();
        });
        
        rackContainer.setOnDragDropped(event -> {
            boolean success = false;
            
            if (event.getDragboard().hasString() && canAcceptTileFromBoard()) {
                // Handle tile return to rack - will be implemented properly later
                success = true;
            }
            
            event.setDropCompleted(success);
            rackContainer.getStyleClass().remove("drop-target");
            event.consume();
        });
    }
    
    private boolean canAcceptTileFromBoard() {
        return isCurrentPlayerTurn && tileViews.size() < MAX_RACK_SIZE;
    }
    
    /**
     * Updates the rack with new tiles
     */
    public void updateTiles(List<Tile> tiles) {
        logger.debug("Updating rack for {} with {} tiles", playerName, tiles.size());
        
        clearTiles();
        for (Tile tile : tiles) {
            addTile(tile);
        }
        updateRackVisuals();
    }
    
    /**
     * Adds a tile to the rack
     */
    public void addTile(Tile tile) {
        if (tileViews.size() >= MAX_RACK_SIZE) {
            logger.warn("Cannot add tile to full rack for player: {}", playerName);
            return;
        }
        
        TileView tileView = new TileView(tile);
        tileView.setDraggable(isCurrentPlayerTurn);
        tileView.setOriginalParent(this);
        
        // Set up tile selection callbacks
        tileView.setOnTileSelected(() -> {
            selectedTiles.add(tileView);
            updateTileSelectionVisuals();
            
            if (onTileSelected != null) {
                onTileSelected.accept(tileView);
            }
        });
        
        tileView.setOnTileDeselected(() -> {
            selectedTiles.remove(tileView);
            updateTileSelectionVisuals();
            
            if (onTileDeselected != null) {
                onTileDeselected.accept(tileView);
            }
        });
        
        tileViews.add(tileView);
        rackContainer.getChildren().add(tileView);
        
        logger.debug("Added tile {} to rack for {}", tile.getDisplayLetter(), playerName);
    }
    
    /**
     * Removes a tile from the rack
     */
    public void removeTile(TileView tileView) {
        if (tileViews.remove(tileView)) {
            rackContainer.getChildren().remove(tileView);
            selectedTiles.remove(tileView);
            
            if (onTileRemoved != null) {
                onTileRemoved.accept(tileView);
            }
            
            updateRackVisuals();
        }
    }
    
    /**
     * Clears all tiles from the rack
     */
    public void clearTiles() {
        selectedTiles.clear();
        tileViews.clear();
        rackContainer.getChildren().clear();
        updateRackVisuals();
    }
    
    /**
     * Shuffles tiles in the rack
     */
    public void shuffleTiles() {
        if (tileViews.isEmpty()) {
            return;
        }
        
        List<TileView> shuffledTiles = new ArrayList<>(tileViews);
        Collections.shuffle(shuffledTiles);
        
        rackContainer.getChildren().clear();
        rackContainer.getChildren().addAll(shuffledTiles);
        
        if (onTileShuffled != null) {
            onTileShuffled.run();
        }
    }
    
    /**
     * Sets whether it's this player's turn
     */
    public void setCurrentPlayerTurn(boolean isCurrentTurn) {
        this.isCurrentPlayerTurn = isCurrentTurn;
        
        for (TileView tileView : tileViews) {
            tileView.setDraggable(isCurrentTurn);
        }
        
        if (isCurrentTurn) {
            playerLabel.getStyleClass().add("current-turn");
            rackContainer.getStyleClass().add("current-turn");
        } else {
            playerLabel.getStyleClass().remove("current-turn");
            rackContainer.getStyleClass().remove("current-turn");
        }
    }
    
    /**
     * Sets exchange mode for tile selection
     */
    public void setExchangeMode(boolean exchangeMode) {
        this.isExchangeMode = exchangeMode;
        
        if (exchangeMode) {
            playerLabel.getStyleClass().add("exchange-mode");
            rackContainer.getStyleClass().add("exchange-mode");
        } else {
            playerLabel.getStyleClass().remove("exchange-mode");
            rackContainer.getStyleClass().remove("exchange-mode");
            deselectAllTiles();
        }
    }
    
    public void deselectAllTiles() {
        for (TileView tileView : new ArrayList<>(selectedTiles)) {
            tileView.deselect();
        }
    }
    
    private void updateRackVisuals() {
        double fillPercentage = (double) tileViews.size() / MAX_RACK_SIZE;
        
        rackContainer.getStyleClass().removeAll("rack-low", "rack-medium", "rack-full");
        
        if (fillPercentage < 0.3) {
            rackContainer.getStyleClass().add("rack-low");
        } else if (fillPercentage < 0.8) {
            rackContainer.getStyleClass().add("rack-medium");
        } else {
            rackContainer.getStyleClass().add("rack-full");
        }
    }
    
    private void updateTileSelectionVisuals() {
        int selectedCount = selectedTiles.size();
        
        if (selectedCount > 0) {
            playerLabel.setText(String.format("%s (%d selected)", playerName, selectedCount));
        } else {
            playerLabel.setText(playerName);
        }
    }
    
    // TileDropTarget implementation
    @Override
    public void onTileDropped(TileView tileView) {
        if (!tileViews.contains(tileView) && canAcceptTileFromBoard()) {
            tileView.setOnBoard(false);
            tileView.setDraggable(isCurrentPlayerTurn);
            
            tileViews.add(tileView);
            rackContainer.getChildren().add(tileView);
            
            updateRackVisuals();
        }
    }
    
    @Override
    public void onTileRemoved(TileView tileView) {
        removeTile(tileView);
    }
    
    @Override
    public boolean canAcceptTile(TileView tileView) {
        return canAcceptTileFromBoard() && !tileView.isOnBoard();
    }
    
    // Getters
    public String getPlayerName() { return playerName; }
    public int getTileCount() { return tileViews.size(); }
    public int getSelectedTileCount() { return selectedTiles.size(); }
    public List<TileView> getSelectedTiles() { return new ArrayList<>(selectedTiles); }
    public List<TileView> getAllTiles() { return new ArrayList<>(tileViews); }
    public boolean isCurrentPlayerTurn() { return isCurrentPlayerTurn; }
    public boolean isExchangeMode() { return isExchangeMode; }
    
    // Event handler setters
    public void setOnTileSelected(Consumer<TileView> handler) { this.onTileSelected = handler; }
    public void setOnTileDeselected(Consumer<TileView> handler) { this.onTileDeselected = handler; }
    public void setOnTileRemoved(Consumer<TileView> handler) { this.onTileRemoved = handler; }
    public void setOnTileShuffled(Runnable handler) { this.onTileShuffled = handler; }
}