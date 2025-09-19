package com.scrabble.client.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Game controls panel with action buttons for gameplay.
 */
public class GameControlsPanel extends VBox {
    private static final Logger logger = LoggerFactory.getLogger(GameControlsPanel.class);
    
    // Action buttons
    private final Button submitMoveButton;
    private final Button passTurnButton;
    private final Button exchangeTilesButton;
    private final Button shuffleRackButton;
    private final Button recallTilesButton;
    
    // Status display
    private final Label statusLabel;
    private final Label previewLabel;
    
    // Event handlers
    private Runnable onSubmitMove;
    private Runnable onPassTurn;
    private Runnable onExchangeTiles;
    private Runnable onShuffleRack;
    private Runnable onRecallTiles;
    private Consumer<String> onStatusUpdate;
    
    // State
    private boolean isCurrentPlayerTurn = false;
    private boolean isExchangeMode = false;
    private int tilesPlacedCount = 0;
    private int tilesSelectedCount = 0;
    
    public GameControlsPanel() {
        // Create status labels
        statusLabel = new Label("Waiting for game to start...");
        statusLabel.getStyleClass().addAll("status-label", "controls-label");
        statusLabel.setWrapText(true);
        
        previewLabel = new Label("");
        previewLabel.getStyleClass().addAll("preview-label", "controls-label");
        previewLabel.setWrapText(true);
        previewLabel.setVisible(false);
        
        // Create action buttons
        submitMoveButton = new Button("Submit Move");
        submitMoveButton.getStyleClass().addAll("control-button", "submit-button");
        submitMoveButton.setOnAction(e -> {
            if (onSubmitMove != null) {
                onSubmitMove.run();
            }
        });
        
        passTurnButton = new Button("Pass Turn");
        passTurnButton.getStyleClass().addAll("control-button", "pass-button");
        passTurnButton.setOnAction(e -> {
            if (onPassTurn != null) {
                onPassTurn.run();
            }
        });
        
        exchangeTilesButton = new Button("Exchange Tiles");
        exchangeTilesButton.getStyleClass().addAll("control-button", "exchange-button");
        exchangeTilesButton.setOnAction(e -> {
            if (onExchangeTiles != null) {
                onExchangeTiles.run();
            }
        });
        
        shuffleRackButton = new Button("Shuffle Rack");
        shuffleRackButton.getStyleClass().addAll("control-button", "utility-button");
        shuffleRackButton.setOnAction(e -> {
            if (onShuffleRack != null) {
                onShuffleRack.run();
            }
        });
        
        recallTilesButton = new Button("Recall Tiles");
        recallTilesButton.getStyleClass().addAll("control-button", "utility-button");
        recallTilesButton.setOnAction(e -> {
            if (onRecallTiles != null) {
                onRecallTiles.run();
            }
        });
        
        setupLayout();
        updateButtonStates();
        
        logger.debug("GameControlsPanel created");
    }
    
    private void setupLayout() {
        setAlignment(Pos.TOP_CENTER);
        setSpacing(10);
        setPadding(new Insets(10));
        getStyleClass().add("game-controls");
        
        // Status section
        VBox statusSection = new VBox();
        statusSection.setAlignment(Pos.CENTER);
        statusSection.setSpacing(5);
        statusSection.getChildren().addAll(statusLabel, previewLabel);
        
        // Main action buttons
        VBox mainActions = new VBox();
        mainActions.setAlignment(Pos.CENTER);
        mainActions.setSpacing(8);
        mainActions.getChildren().addAll(submitMoveButton, passTurnButton, exchangeTilesButton);
        
        // Utility buttons
        HBox utilityActions = new HBox();
        utilityActions.setAlignment(Pos.CENTER);
        utilityActions.setSpacing(8);
        utilityActions.getChildren().addAll(shuffleRackButton, recallTilesButton);
        
        // Add all sections
        getChildren().addAll(statusSection, mainActions, utilityActions);
    }
    
    /**
     * Updates the current player turn state
     */
    public void setCurrentPlayerTurn(boolean isCurrentTurn) {
        this.isCurrentPlayerTurn = isCurrentTurn;
        
        if (isCurrentTurn) {
            statusLabel.setText("Your turn - place tiles or choose an action");
            getStyleClass().add("my-turn");
        } else {
            statusLabel.setText("Opponent's turn...");
            getStyleClass().remove("my-turn");
        }
        
        updateButtonStates();
        logger.debug("Set current player turn: {}", isCurrentTurn);
    }
    
    /**
     * Updates exchange mode state
     */
    public void setExchangeMode(boolean exchangeMode) {
        this.isExchangeMode = exchangeMode;
        
        if (exchangeMode) {
            statusLabel.setText("Exchange mode - select tiles to exchange");
            exchangeTilesButton.setText("Confirm Exchange");
            getStyleClass().add("exchange-mode");
        } else {
            statusLabel.setText(isCurrentPlayerTurn ? "Your turn - place tiles or choose an action" : "Opponent's turn...");
            exchangeTilesButton.setText("Exchange Tiles");
            getStyleClass().remove("exchange-mode");
        }
        
        updateButtonStates();
        logger.debug("Set exchange mode: {}", exchangeMode);
    }
    
    /**
     * Updates the count of tiles placed on board
     */
    public void updateTilesPlaced(int tilesPlaced) {
        this.tilesPlacedCount = tilesPlaced;
        
        if (tilesPlaced > 0) {
            statusLabel.setText(String.format("You have placed %d tile%s", 
                               tilesPlaced, tilesPlaced == 1 ? "" : "s"));
        } else if (isCurrentPlayerTurn && !isExchangeMode) {
            statusLabel.setText("Your turn - place tiles or choose an action");
        }
        
        updateButtonStates();
        logger.debug("Updated tiles placed count: {}", tilesPlaced);
    }
    
    /**
     * Updates the count of tiles selected for exchange
     */
    public void updateTilesSelected(int tilesSelected) {
        this.tilesSelectedCount = tilesSelected;
        
        if (isExchangeMode && tilesSelected > 0) {
            statusLabel.setText(String.format("Exchange mode - %d tile%s selected", 
                               tilesSelected, tilesSelected == 1 ? "" : "s"));
        } else if (isExchangeMode) {
            statusLabel.setText("Exchange mode - select tiles to exchange");
        }
        
        updateButtonStates();
        logger.debug("Updated tiles selected count: {}", tilesSelected);
    }
    
    /**
     * Shows a move preview with score calculation
     */
    public void showMovePreview(String wordsFormed, int score) {
        if (wordsFormed != null && !wordsFormed.isEmpty() && score > 0) {
            previewLabel.setText(String.format("Words: %s | Score: %d points", wordsFormed, score));
            previewLabel.setVisible(true);
            previewLabel.getStyleClass().add("valid-move");
        } else {
            hideMovePreview();
        }
        
        logger.debug("Showing move preview: words={}, score={}", wordsFormed, score);
    }
    
    /**
     * Shows an error in move preview
     */
    public void showMoveError(String errorMessage) {
        previewLabel.setText("Error: " + errorMessage);
        previewLabel.setVisible(true);
        previewLabel.getStyleClass().remove("valid-move");
        previewLabel.getStyleClass().add("invalid-move");
        
        logger.debug("Showing move error: {}", errorMessage);
    }
    
    /**
     * Hides the move preview
     */
    public void hideMovePreview() {
        previewLabel.setVisible(false);
        previewLabel.getStyleClass().removeAll("valid-move", "invalid-move");
    }
    
    /**
     * Updates button enabled/disabled states based on current game state
     */
    private void updateButtonStates() {
        // Submit move - only enabled when tiles are placed and it's player's turn
        submitMoveButton.setDisable(!isCurrentPlayerTurn || isExchangeMode || tilesPlacedCount == 0);
        
        // Pass turn - enabled when it's player's turn and no exchange mode
        passTurnButton.setDisable(!isCurrentPlayerTurn || isExchangeMode);
        
        // Exchange tiles - enabled when it's player's turn
        if (isExchangeMode) {
            // In exchange mode, only enabled when tiles are selected
            exchangeTilesButton.setDisable(tilesSelectedCount == 0);
        } else {
            // Not in exchange mode, enabled when no tiles placed
            exchangeTilesButton.setDisable(!isCurrentPlayerTurn || tilesPlacedCount > 0);
        }
        
        // Shuffle rack - always enabled when it's player's turn
        shuffleRackButton.setDisable(!isCurrentPlayerTurn || isExchangeMode);
        
        // Recall tiles - only enabled when tiles are placed
        recallTilesButton.setDisable(!isCurrentPlayerTurn || isExchangeMode || tilesPlacedCount == 0);
        
        // Update button text for exchange mode
        if (isExchangeMode && tilesSelectedCount > 0) {
            exchangeTilesButton.setText(String.format("Exchange %d Tiles", tilesSelectedCount));
        } else if (isExchangeMode) {
            exchangeTilesButton.setText("Cancel Exchange");
        } else {
            exchangeTilesButton.setText("Exchange Tiles");
        }
    }
    
    /**
     * Disables all controls (e.g., when game is over)
     */
    public void disableAllControls() {
        submitMoveButton.setDisable(true);
        passTurnButton.setDisable(true);
        exchangeTilesButton.setDisable(true);
        shuffleRackButton.setDisable(true);
        recallTilesButton.setDisable(true);
        
        statusLabel.setText("Game Over");
        hideMovePreview();
        
        logger.debug("All controls disabled");
    }
    
    /**
     * Shows waiting state
     */
    public void showWaitingState(String message) {
        statusLabel.setText(message != null ? message : "Waiting...");
        submitMoveButton.setDisable(true);
        passTurnButton.setDisable(true);
        exchangeTilesButton.setDisable(true);
        shuffleRackButton.setDisable(true);
        recallTilesButton.setDisable(true);
        hideMovePreview();
    }
    
    // Event handler setters
    public void setOnSubmitMove(Runnable handler) {
        this.onSubmitMove = handler;
    }
    
    public void setOnPassTurn(Runnable handler) {
        this.onPassTurn = handler;
    }
    
    public void setOnExchangeTiles(Runnable handler) {
        this.onExchangeTiles = handler;
    }
    
    public void setOnShuffleRack(Runnable handler) {
        this.onShuffleRack = handler;
    }
    
    public void setOnRecallTiles(Runnable handler) {
        this.onRecallTiles = handler;
    }
    
    public void setOnStatusUpdate(Consumer<String> handler) {
        this.onStatusUpdate = handler;
    }
    
    // Additional compatibility methods for GameViewController
    
    /**
     * Sets whether there are pending moves (compatibility method)
     */
    public void setHasPendingMove(boolean hasPendingMove) {
        updateTilesPlaced(hasPendingMove ? 1 : 0); // Delegate to existing method
    }
    
    /**
     * Sets the move score preview (compatibility method)
     */
    public void setMoveScore(int score) {
        if (score > 0) {
            showMovePreview("Move", score); // Delegate to existing method
        } else {
            hideMovePreview();
        }
    }
    
    // Getters
    public boolean isCurrentPlayerTurn() { return isCurrentPlayerTurn; }
    public boolean isExchangeMode() { return isExchangeMode; }
    public int getTilesPlacedCount() { return tilesPlacedCount; }
    public int getTilesSelectedCount() { return tilesSelectedCount; }
}
