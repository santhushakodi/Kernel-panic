package com.scrabble.client.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Score display panel showing current scores, turn indicator, and game statistics.
 */
public class ScoreDisplayPanel extends VBox {
    private static final Logger logger = LoggerFactory.getLogger(ScoreDisplayPanel.class);
    
    // Player score components
    private final Label player1NameLabel;
    private final Label player1ScoreLabel;
    private final Label player2NameLabel;
    private final Label player2ScoreLabel;
    private final Label currentTurnLabel;
    
    // Game statistics
    private final Label tilesRemainingLabel;
    private final Label moveCountLabel;
    private final Label timeRemainingLabel;
    
    // Layout containers
    private final VBox player1Container;
    private final VBox player2Container;
    private final VBox statsContainer;
    
    public ScoreDisplayPanel() {
        // Create player score displays
        player1NameLabel = new Label("Player 1");
        player1NameLabel.getStyleClass().addAll("player-name", "score-label");
        
        player1ScoreLabel = new Label("0");
        player1ScoreLabel.getStyleClass().addAll("player-score", "score-value");
        
        player2NameLabel = new Label("Player 2");
        player2NameLabel.getStyleClass().addAll("player-name", "score-label");
        
        player2ScoreLabel = new Label("0");
        player2ScoreLabel.getStyleClass().addAll("player-score", "score-value");
        
        currentTurnLabel = new Label("Game Starting...");
        currentTurnLabel.getStyleClass().addAll("current-turn-label", "score-label");
        
        // Create game statistics
        tilesRemainingLabel = new Label("Tiles: 100");
        tilesRemainingLabel.getStyleClass().add("stat-label");
        
        moveCountLabel = new Label("Move: 0");
        moveCountLabel.getStyleClass().add("stat-label");
        
        timeRemainingLabel = new Label("--:--");
        timeRemainingLabel.getStyleClass().add("stat-label");
        
        // Create layout containers
        player1Container = new VBox();
        player1Container.setAlignment(Pos.CENTER);
        player1Container.setSpacing(5);
        player1Container.getStyleClass().add("player-score-container");
        player1Container.getChildren().addAll(player1NameLabel, player1ScoreLabel);
        
        player2Container = new VBox();
        player2Container.setAlignment(Pos.CENTER);
        player2Container.setSpacing(5);
        player2Container.getStyleClass().add("player-score-container");
        player2Container.getChildren().addAll(player2NameLabel, player2ScoreLabel);
        
        statsContainer = new VBox();
        statsContainer.setAlignment(Pos.CENTER);
        statsContainer.setSpacing(8);
        statsContainer.getStyleClass().add("stats-container");
        statsContainer.getChildren().addAll(tilesRemainingLabel, moveCountLabel, timeRemainingLabel);
        
        // Main layout
        setupLayout();
        
        logger.debug("ScoreDisplayPanel created");
    }
    
    private void setupLayout() {
        setAlignment(Pos.TOP_CENTER);
        setSpacing(15);
        setPadding(new Insets(10));
        getStyleClass().add("score-display");
        
        // Create players section
        HBox playersSection = new HBox();
        playersSection.setAlignment(Pos.CENTER);
        playersSection.setSpacing(20);
        playersSection.getChildren().addAll(player1Container, player2Container);
        
        // Add all sections
        getChildren().addAll(
            playersSection,
            currentTurnLabel,
            statsContainer
        );
        
        VBox.setVgrow(statsContainer, Priority.ALWAYS);
    }
    
    /**
     * Updates player names
     */
    public void updatePlayerNames(String player1Name, String player2Name) {
        player1NameLabel.setText(player1Name != null ? player1Name : "Player 1");
        player2NameLabel.setText(player2Name != null ? player2Name : "Player 2");
        
        logger.debug("Updated player names: {} vs {}", player1Name, player2Name);
    }
    
    /**
     * Updates player scores
     */
    public void updateScores(int player1Score, int player2Score) {
        player1ScoreLabel.setText(String.valueOf(player1Score));
        player2ScoreLabel.setText(String.valueOf(player2Score));
        
        // Highlight leading player
        if (player1Score > player2Score) {
            player1Container.getStyleClass().add("leading-player");
            player2Container.getStyleClass().remove("leading-player");
        } else if (player2Score > player1Score) {
            player2Container.getStyleClass().add("leading-player");
            player1Container.getStyleClass().remove("leading-player");
        } else {
            // Tie game
            player1Container.getStyleClass().remove("leading-player");
            player2Container.getStyleClass().remove("leading-player");
        }
        
        logger.debug("Updated scores: {} - {}", player1Score, player2Score);
    }
    
    /**
     * Updates current turn indicator
     */
    public void updateCurrentTurn(String playerName, boolean isCurrentPlayer) {
        if (playerName != null) {
            String turnText = playerName + "'s Turn";
            if (isCurrentPlayer) {
                turnText = "Your Turn";
            }
            currentTurnLabel.setText(turnText);
            
            // Highlight current turn
            if (isCurrentPlayer) {
                currentTurnLabel.getStyleClass().add("my-turn");
            } else {
                currentTurnLabel.getStyleClass().remove("my-turn");
            }
            
            // Highlight the appropriate player container
            String player1Name = player1NameLabel.getText();
            String player2Name = player2NameLabel.getText();
            
            if (playerName.equals(player1Name)) {
                player1Container.getStyleClass().add("current-turn");
                player2Container.getStyleClass().remove("current-turn");
            } else if (playerName.equals(player2Name)) {
                player2Container.getStyleClass().add("current-turn");
                player1Container.getStyleClass().remove("current-turn");
            }
        } else {
            currentTurnLabel.setText("Game Starting...");
            currentTurnLabel.getStyleClass().remove("my-turn");
            player1Container.getStyleClass().remove("current-turn");
            player2Container.getStyleClass().remove("current-turn");
        }
        
        logger.debug("Updated current turn: {} (isCurrentPlayer: {})", playerName, isCurrentPlayer);
    }
    
    /**
     * Updates tiles remaining count
     */
    public void updateTilesRemaining(int tilesRemaining) {
        tilesRemainingLabel.setText("Tiles: " + tilesRemaining);
        
        // Visual indicator for low tile count
        if (tilesRemaining <= 10) {
            tilesRemainingLabel.getStyleClass().add("low-tiles");
        } else {
            tilesRemainingLabel.getStyleClass().remove("low-tiles");
        }
        
        logger.debug("Updated tiles remaining: {}", tilesRemaining);
    }
    
    /**
     * Updates move count
     */
    public void updateMoveCount(int moveCount) {
        moveCountLabel.setText("Move: " + moveCount);
        logger.debug("Updated move count: {}", moveCount);
    }
    
    /**
     * Updates time remaining for current player
     */
    public void updateTimeRemaining(long secondsRemaining) {
        if (secondsRemaining >= 0) {
            long minutes = secondsRemaining / 60;
            long seconds = secondsRemaining % 60;
            String timeText = String.format("%02d:%02d", minutes, seconds);
            timeRemainingLabel.setText(timeText);
            
            // Visual warning for low time
            if (secondsRemaining <= 60) {
                timeRemainingLabel.getStyleClass().add("low-time");
            } else if (secondsRemaining <= 300) { // 5 minutes
                timeRemainingLabel.getStyleClass().add("medium-time");
            } else {
                timeRemainingLabel.getStyleClass().removeAll("low-time", "medium-time");
            }
        } else {
            timeRemainingLabel.setText("--:--");
            timeRemainingLabel.getStyleClass().removeAll("low-time", "medium-time");
        }
        
        logger.debug("Updated time remaining: {} seconds", secondsRemaining);
    }
    
    /**
     * Shows game over state
     */
    public void showGameOver(String winnerName, String reason) {
        if (winnerName != null) {
            currentTurnLabel.setText("Game Over - " + winnerName + " Wins!");
        } else {
            currentTurnLabel.setText("Game Over - Draw!");
        }
        
        currentTurnLabel.getStyleClass().add("game-over");
        
        // Stop time display
        timeRemainingLabel.setText("FINAL");
        timeRemainingLabel.getStyleClass().removeAll("low-time", "medium-time");
        timeRemainingLabel.getStyleClass().add("game-over");
        
        logger.info("Game over: winner={}, reason={}", winnerName, reason);
    }
    
    /**
     * Resets the display for a new game
     */
    public void resetForNewGame() {
        updatePlayerNames("Player 1", "Player 2");
        updateScores(0, 0);
        updateCurrentTurn(null, false);
        updateTilesRemaining(100);
        updateMoveCount(0);
        updateTimeRemaining(-1);
        
        // Remove all state classes
        currentTurnLabel.getStyleClass().remove("game-over");
        timeRemainingLabel.getStyleClass().removeAll("low-time", "medium-time", "game-over");
        tilesRemainingLabel.getStyleClass().remove("low-tiles");
        player1Container.getStyleClass().removeAll("leading-player", "current-turn");
        player2Container.getStyleClass().removeAll("leading-player", "current-turn");
        
        logger.debug("Reset score display for new game");
    }
    
    /**
     * Updates all game state information at once
     */
    public void updateGameState(String player1Name, String player2Name,
                               int player1Score, int player2Score,
                               String currentPlayerName, boolean isCurrentPlayer,
                               int tilesRemaining, int moveCount, long timeRemaining) {
        updatePlayerNames(player1Name, player2Name);
        updateScores(player1Score, player2Score);
        updateCurrentTurn(currentPlayerName, isCurrentPlayer);
        updateTilesRemaining(tilesRemaining);
        updateMoveCount(moveCount);
        updateTimeRemaining(timeRemaining);
    }
}