package com.scrabble.client.controller;

import com.scrabble.client.service.GameService;
import com.scrabble.client.ui.components.*;
import com.scrabble.client.websocket.GameWebSocketClient;
import com.scrabble.client.model.TilePlacement;
import com.scrabble.client.dto.GameRequestDTOs.*;
import com.scrabble.client.validation.MoveValidator;
import com.scrabble.client.validation.MoveValidationResult;
import com.scrabble.common.model.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.BorderPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Main controller for the game view, managing all game interactions,
 * UI updates, and communication with the server.
 */
public class GameViewController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(GameViewController.class);

    // FXML injected components
    @FXML private BorderPane gamePane;
    @FXML private VBox leftPanel;
    @FXML private VBox rightPanel;
    @FXML private HBox bottomPanel;

    // Game UI components
    private GameBoardView gameBoardView;
    private PlayerRackView playerRackView;
    private ScoreDisplayPanel scoreDisplayPanel;
    private GameControlsPanel gameControlsPanel;

    // Services
    private GameService gameService;
    private GameWebSocketClient webSocketClient;

    // Game state
    private GameState currentGameState;
    private Player currentPlayer;
    private List<TilePlacement> pendingMove;
    private boolean gameInProgress;
    private String gameId;

    // Move validation
    private MoveValidator moveValidator;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing GameViewController");
        
        // Initialize collections
        pendingMove = new ArrayList<>();
        
        // Initialize validator
        moveValidator = new MoveValidator();
        
        // Create UI components
        initializeComponents();
        
        // Setup layout
        setupLayout();
        
        // Initialize services (will be injected later)
        // gameService and webSocketClient will be set via setters
        
        logger.info("GameViewController initialized successfully");
    }

    private void initializeComponents() {
        // Create game board
        gameBoardView = new GameBoardView();
        gameBoardView.setOnTilePlaced(this::handleTilePlaced);
        gameBoardView.setOnTileRemoved(this::handleTileRemoved);
        gameBoardView.setOnSquareClicked(this::handleSquareClicked);

        // Create player rack
        playerRackView = new PlayerRackView("Player");
        playerRackView.setOnTileSelected(this::handleRackTileSelected);
        playerRackView.setOnTileDeselected(this::handleRackTileDeselected);

        // Create score display
        scoreDisplayPanel = new ScoreDisplayPanel();

        // Create game controls
        gameControlsPanel = new GameControlsPanel();
        gameControlsPanel.setOnSubmitMove(this::handleSubmitMove);
        gameControlsPanel.setOnPassTurn(this::handlePassTurn);
        gameControlsPanel.setOnExchangeTiles(() -> handleExchangeTiles(null)); // Will be handled by dialog
        gameControlsPanel.setOnShuffleRack(this::handleShuffleRack);
        gameControlsPanel.setOnRecallTiles(this::handleRecallTiles);

        logger.debug("All game components initialized");
    }

    private void setupLayout() {
        // Center: Game board
        gamePane.setCenter(gameBoardView);

        // Left panel: Score display
        leftPanel.getChildren().add(scoreDisplayPanel);

        // Right panel: Could be used for chat or other features
        // rightPanel.getChildren().add(/* future components */);

        // Bottom: Player rack and controls
        bottomPanel.getChildren().addAll(playerRackView, gameControlsPanel);

        logger.debug("Game layout setup completed");
    }

    /**
     * Starts a new game with the given game state
     */
    public void startGame(GameState gameState) {
        logger.info("Starting game with ID: {}", gameState.getGameId());
        
        this.currentGameState = gameState;
        this.gameId = gameState.getGameId();
        this.gameInProgress = true;
        
        // Find current player
        String playerId = getCurrentPlayerId(); // Will be implemented
        this.currentPlayer = gameState.getPlayers().stream()
            .filter(p -> p.getId().equals(playerId))
            .findFirst()
            .orElse(null);
        
        if (currentPlayer == null) {
            logger.error("Current player not found in game state");
            showError("Player not found in game");
            return;
        }
        
        // Update UI
        updateGameDisplay();
        
        logger.info("Game started successfully for player: {}", currentPlayer.getName());
    }

    /**
     * Updates the game state and refreshes the UI
     */
    public void updateGameState(GameState gameState) {
        logger.debug("Updating game state");
        
        Platform.runLater(() -> {
            this.currentGameState = gameState;
            
            // Update current player info
            String playerId = getCurrentPlayerId();
            this.currentPlayer = gameState.getPlayers().stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElse(null);
            
            // Update UI components
            updateGameDisplay();
            
            // Clear pending move if it's not our turn
            if (!isCurrentPlayerTurn()) {
                clearPendingMove();
            }
        });
    }

    private void updateGameDisplay() {
        if (currentGameState == null) return;
        
        // Update board
        gameBoardView.updateBoard(currentGameState.getBoard());
        
        // Update player rack
        if (currentPlayer != null) {
            // playerRackView.setTiles(currentPlayer.getRack()); // Will be implemented
        }
        
        // Update scores - temporary implementation
        if (!currentGameState.getPlayers().isEmpty()) {
            // For now, just update with first two players' scores
            Player player1 = currentGameState.getPlayers().get(0);
            Player player2 = currentGameState.getPlayers().size() > 1 ? 
                currentGameState.getPlayers().get(1) : null;
            
            scoreDisplayPanel.updateScores(
                player1.getScore(), 
                player2 != null ? player2.getScore() : 0
            );
        }
        
        // Update game info - temporary implementation  
        // scoreDisplayPanel.setTilesRemaining(currentGameState.getTilesRemaining());
        // scoreDisplayPanel.setMoveCount(currentGameState.getMoveCount());
        
        // Update controls - will be implemented
        // boolean isPlayerTurn = isCurrentPlayerTurn();
        // gameControlsPanel.setTurnActive(isPlayerTurn);
        // gameControlsPanel.setHasPendingMove(!pendingMove.isEmpty());
        
        // Update turn indicator - will be implemented
        // if (isPlayerTurn) {
        //     scoreDisplayPanel.setCurrentTurn("Your Turn");
        // } else {
        //     Player currentTurnPlayer = getCurrentTurnPlayer();
        //     if (currentTurnPlayer != null) {
        //         scoreDisplayPanel.setCurrentTurn(currentTurnPlayer.getName() + "'s Turn");
        //     }
        // }
        
        logger.debug("Game display updated");
    }

    private boolean isCurrentPlayerTurn() {
        return currentGameState != null && currentPlayer != null;
        // Note: GameState needs getCurrentPlayerId() method
        // return currentGameState.getCurrentPlayerId().equals(currentPlayer.getId());
    }

    private Player getCurrentTurnPlayer() {
        if (currentGameState == null || currentGameState.getPlayers().isEmpty()) return null;
        // For now, return first player as current turn player
        return currentGameState.getPlayers().get(0);
        // Note: GameState needs getCurrentPlayerId() method for proper implementation
        // return currentGameState.getPlayers().stream()
        //     .filter(p -> p.getId().equals(currentGameState.getCurrentPlayerId()))
        //     .findFirst()
        //     .orElse(null);
    }

    // Event handlers for UI components

    private void handleTilePlaced(com.scrabble.client.ui.components.GameBoardView.TilePlacement placement) {
        logger.debug("Tile placed at position: {}", placement.getPosition());
        
        if (!isCurrentPlayerTurn()) {
            logger.warn("Attempted to place tile when not player's turn");
            return;
        }
        
        // Convert GameBoardView.TilePlacement to client.model.TilePlacement
        com.scrabble.client.model.TilePlacement modelPlacement = 
            new com.scrabble.client.model.TilePlacement(placement.getPosition(), placement.getTileView().getTile());
        
        // Add to pending move
        pendingMove.add(modelPlacement);
        
        // Validate current move
        validateCurrentMove();
        
        // Update controls - will be implemented
        // gameControlsPanel.setHasPendingMove(true);
    }

    private void handleTileRemoved(Position position) {
        logger.debug("Tile removed from position: {}", position);
        
        // Remove from pending move
        pendingMove.removeIf(placement -> placement.getPosition().equals(position));
        
        // Update controls
        gameControlsPanel.setHasPendingMove(!pendingMove.isEmpty());
        
        // Re-validate move
        validateCurrentMove();
    }

    private void handleSquareClicked(Position position) {
        logger.debug("Square clicked at position: {}", position);
        // Could be used for tile selection or other interactions
    }

    private void handleRackTileSelected(TileView tileView) {
        logger.debug("Rack tile selected: {}", tileView.getTile().getDisplayLetter());
        // Enable drag or click-to-place mode
    }

    private void handleRackTileDeselected(TileView tileView) {
        logger.debug("Rack tile deselected: {}", tileView.getTile().getDisplayLetter());
        // Disable drag or click-to-place mode
    }

    // Game action handlers

    private void handleSubmitMove() {
        logger.info("Submitting move with {} tiles", pendingMove.size());
        
        if (pendingMove.isEmpty()) {
            showError("No tiles placed to submit");
            return;
        }
        
        if (!validateCurrentMove()) {
            showError("Invalid move. Please check tile placement.");
            return;
        }
        
        // Create move request
        MoveRequest moveRequest = new MoveRequest();
        moveRequest.setGameId(gameId);
        moveRequest.setPlayerId(currentPlayer.getId());
        moveRequest.setPlacements(pendingMove);
        
        // Submit via service
        gameService.submitMove(moveRequest)
            .thenAccept(response -> {
                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        logger.info("Move submitted successfully");
                        clearPendingMove();
                    } else {
                        logger.warn("Move submission failed: {}", response.getErrorMessage());
                        showError("Move failed: " + response.getErrorMessage());
                    }
                });
            })
            .exceptionally(throwable -> {
                Platform.runLater(() -> {
                    logger.error("Error submitting move", throwable);
                    showError("Failed to submit move: " + throwable.getMessage());
                });
                return null;
            });
    }

    private void handlePassTurn() {
        logger.info("Passing turn");
        
        // Confirm action
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Pass Turn");
        alert.setHeaderText("Are you sure you want to pass your turn?");
        alert.setContentText("You will not place any tiles this turn.");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                PassTurnRequest request = new PassTurnRequest();
                request.setGameId(gameId);
                request.setPlayerId(currentPlayer.getId());
                
                gameService.passTurn(request)
                    .thenAccept(result -> {
                        Platform.runLater(() -> {
                            if (result.isSuccess()) {
                                logger.info("Turn passed successfully");
                                clearPendingMove();
                            } else {
                                showError("Failed to pass turn: " + result.getErrorMessage());
                            }
                        });
                    })
                    .exceptionally(throwable -> {
                        Platform.runLater(() -> {
                            logger.error("Error passing turn", throwable);
                            showError("Failed to pass turn: " + throwable.getMessage());
                        });
                        return null;
                    });
            }
        });
    }

    private void handleExchangeTiles(List<Tile> tilesToExchange) {
        logger.info("Exchanging {} tiles", tilesToExchange.size());
        
        ExchangeTilesRequest request = new ExchangeTilesRequest();
        request.setGameId(gameId);
        request.setPlayerId(currentPlayer.getId());
        request.setTilesToExchange(tilesToExchange);
        
        gameService.exchangeTiles(request)
            .thenAccept(response -> {
                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        logger.info("Tiles exchanged successfully");
                        // UI will be updated via WebSocket game state update
                    } else {
                        showError("Failed to exchange tiles: " + response.getErrorMessage());
                    }
                });
            })
            .exceptionally(throwable -> {
                Platform.runLater(() -> {
                    logger.error("Error exchanging tiles", throwable);
                    showError("Failed to exchange tiles: " + throwable.getMessage());
                });
                return null;
            });
    }

    private void handleShuffleRack() {
        logger.debug("Shuffling player rack");
        playerRackView.shuffleTiles();
    }

    private void handleRecallTiles() {
        logger.debug("Recalling pending tiles");
        clearPendingMove();
        gameBoardView.clearPendingTiles();
        gameControlsPanel.setHasPendingMove(false);
    }

    // Move validation

    private boolean validateCurrentMove() {
        if (pendingMove.isEmpty()) {
            return true; // No move to validate
        }
        
        try {
            // Basic validation
            MoveValidationResult result = moveValidator.validateMove(
                currentGameState.getBoard(), 
                pendingMove, 
                currentGameState.getMoveCount() == 0
            );
            
            // Update UI with validation result
            gameBoardView.showMoveValidation(result);
            
            if (result.isValid()) {
                // Show score preview
                gameControlsPanel.setMoveScore(result.getScore());
                logger.debug("Move validation successful, score: {}", result.getScore());
            } else {
                gameControlsPanel.setMoveScore(0);
                logger.debug("Move validation failed: {}", result.getErrorMessage());
            }
            
            return result.isValid();
            
        } catch (Exception e) {
            logger.error("Error validating move", e);
            return false;
        }
    }

    // Utility methods

    private void clearPendingMove() {
        pendingMove.clear();
        gameBoardView.clearPendingTiles();
        gameControlsPanel.setHasPendingMove(false);
        gameControlsPanel.setMoveScore(0);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Game Error");
        alert.setHeaderText("An error occurred");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String getCurrentPlayerId() {
        // This would typically come from a user session or authentication service
        // For now, return a placeholder - this should be injected
        return "current-player-id"; // TODO: Implement proper player ID retrieval
    }

    // Setters for dependency injection

    public void setGameService(GameService gameService) {
        this.gameService = gameService;
        logger.debug("GameService injected");
    }

    public void setWebSocketClient(GameWebSocketClient webSocketClient) {
        this.webSocketClient = webSocketClient;
        logger.debug("WebSocketClient injected");
    }

    /**
     * Cleanup when leaving the game
     */
    public void cleanup() {
        logger.info("Cleaning up GameViewController");
        gameInProgress = false;
        clearPendingMove();
        
        if (webSocketClient != null && webSocketClient.isConnected()) {
            webSocketClient.disconnect();
        }
        
        logger.info("GameViewController cleanup completed");
    }

    // Getters for testing and external access

    public boolean isGameInProgress() {
        return gameInProgress;
    }

    public GameState getCurrentGameState() {
        return currentGameState;
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public List<TilePlacement> getPendingMove() {
        return new ArrayList<>(pendingMove);
    }
}