package com.scrabble.client.controller;

import com.scrabble.client.service.GameServiceClient;
import com.scrabble.client.websocket.GameWebSocketClient;
import com.scrabble.common.dto.GameMessage;
import com.scrabble.common.model.Player;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

/**
 * Main application controller for the Scrabble client.
 * Manages scene transitions, server communication, and user interactions.
 */
public class AppController {
    private static final Logger logger = LoggerFactory.getLogger(AppController.class);
    
    // FXML Controls
    @FXML private StackPane contentPane;
    @FXML private VBox welcomePane;
    @FXML private VBox lobbyPane;
    @FXML private BorderPane gamePane;
    
    @FXML private Button playHumanButton;
    @FXML private Button playBotButton;
    @FXML private Button watchGameButton;
    @FXML private Button connectButton;
    @FXML private Button cancelLobbyButton;
    
    @FXML private TextField playerNameField;
    @FXML private ProgressIndicator lobbyProgress;
    
    @FXML private Label connectionStatus;
    @FXML private Label statusLabel;
    @FXML private Label serverStatusLabel;
    
    @FXML private MenuItem newGameMenuItem;
    @FXML private MenuItem joinGameMenuItem;
    @FXML private MenuItem disconnectMenuItem;
    @FXML private CheckMenuItem showChatMenuItem;
    @FXML private CheckMenuItem showScoresMenuItem;
    
    // Services
    private GameServiceClient gameService;
    private GameWebSocketClient webSocketClient;
    private Stage primaryStage;
    
    // State
    private Player currentPlayer;
    private String currentGameId;
    private boolean isInLobby = false;
    
    /**
     * Initialize the controller with required services.
     */
    public void initialize(GameServiceClient gameService, GameWebSocketClient webSocketClient, Stage primaryStage) {
        this.gameService = gameService;
        this.webSocketClient = webSocketClient;
        this.primaryStage = primaryStage;
        
        setupWebSocketHandlers();
        setupDefaultPlayerName();
        updateUIState();
        
        logger.info("AppController initialized");
    }
    
    private void setupWebSocketHandlers() {
        // Register message handlers
        webSocketClient.registerMessageHandler("GAME_STARTED", this::handleGameStarted);
        webSocketClient.registerMessageHandler("PLAYER_JOINED", this::handlePlayerJoined);
        webSocketClient.registerMessageHandler("GAME_STATE_UPDATE", this::handleGameStateUpdate);
        webSocketClient.registerMessageHandler("ERROR", this::handleError);
    }
    
    private void setupDefaultPlayerName() {
        String defaultName = "Player_" + UUID.randomUUID().toString().substring(0, 8);
        playerNameField.setText(defaultName);
    }
    
    // FXML Event Handlers
    @FXML
    private void onConnect() {
        if (webSocketClient.isConnected()) {
            return;
        }
        
        setStatus("Connecting to server...");
        connectButton.setDisable(true);
        connectionStatus.setText("Connecting...");
        connectionStatus.setStyle("-fx-text-fill: orange;");
        
        Task<Void> connectTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                webSocketClient.connectAsync().get();
                return null;
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    setStatus("Connected to server");
                    updateConnectionStatus(true);
                    connectButton.setDisable(false);
                    enableGameOptions(true);
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    setStatus("Failed to connect to server");
                    updateConnectionStatus(false);
                    connectButton.setDisable(false);
                    showError("Connection Failed", "Unable to connect to the game server. Please try again.");
                });
            }
        };
        
        new Thread(connectTask).start();
    }
    
    @FXML
    private void onPlayAgainstHuman() {
        if (!validateConnection()) return;
        
        String playerName = playerNameField.getText().trim();
        if (playerName.isEmpty()) {
            showError("Invalid Name", "Please enter a player name.");
            return;
        }
        
        currentPlayer = Player.createHuman(UUID.randomUUID().toString(), playerName);
        showLobbyScreen();
        joinHumanLobby();
    }
    
    @FXML
    private void onPlayAgainstBot() {
        if (!validateConnection()) return;
        
        String playerName = playerNameField.getText().trim();
        if (playerName.isEmpty()) {
            showError("Invalid Name", "Please enter a player name.");
            return;
        }
        
        currentPlayer = Player.createHuman(UUID.randomUUID().toString(), playerName);
        showLobbyScreen();
        createBotGame();
    }
    
    @FXML
    private void onWatchGame() {
        showInfo("Watch Game", "Game spectating feature will be implemented in a future version.");
    }
    
    @FXML
    private void onCancelLobby() {
        if (isInLobby) {
            setStatus("Cancelling...");
            isInLobby = false;
            currentGameId = null;
            showWelcomeScreen();
            setStatus("Ready");
        }
    }
    
    @FXML
    private void onNewGame() {
        onPlayAgainstHuman();
    }
    
    @FXML
    private void onJoinGame() {
        // Show dialog to enter game ID
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Join Game");
        dialog.setHeaderText("Enter Game ID");
        dialog.setContentText("Game ID:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(gameId -> {
            // TODO: Implement joining existing game
            showInfo("Join Game", "Joining existing games will be implemented in a future version.");
        });
    }
    
    @FXML
    private void onDisconnect() {
        webSocketClient.disconnect();
        updateConnectionStatus(false);
        enableGameOptions(false);
        showWelcomeScreen();
        setStatus("Disconnected");
    }
    
    @FXML
    private void onExit() {
        Platform.exit();
    }
    
    @FXML
    private void onRules() {
        showInfo("Game Rules", 
            "Scrabble Rules:\\n\\n" +
            "• Place tiles to form words on the board\\n" +
            "• First word must cover the center star\\n" +
            "• All subsequent words must connect to existing tiles\\n" +
            "• Use premium squares for bonus points\\n" +
            "• Use all 7 tiles for a 50-point bingo bonus\\n" +
            "• Game ends when tile bag is empty and one player uses all tiles"
        );
    }
    
    @FXML
    private void onAbout() {
        showInfo("About Scrabble", 
            "Scrabble Game Client\\n" +
            "Version 1.0.0\\n\\n" +
            "A JavaFX implementation of the classic word game.\\n" +
            "Features real-time multiplayer gameplay and AI opponents."
        );
    }
    
    // Server Event Handlers
    public void onServerConnected() {
        updateConnectionStatus(true);
        enableGameOptions(true);
        setStatus("Connected to server");
    }
    
    public void onServerDisconnected() {
        updateConnectionStatus(false);
        enableGameOptions(false);
        setStatus("Disconnected from server");
        
        if (isInLobby) {
            showWelcomeScreen();
            isInLobby = false;
        }
    }
    
    public void onServerError(String error) {
        setStatus("Server error: " + error);
        showError("Server Error", error);
    }
    
    // WebSocket Message Handlers
    private void handleGameStarted(GameMessage message) {
        Platform.runLater(() -> {
            logger.info("Game started: {}", message.getGameId());
            currentGameId = message.getGameId();
            showGameScreen();
            setStatus("Game in progress");
        });
    }
    
    private void handlePlayerJoined(GameMessage message) {
        Platform.runLater(() -> {
            logger.info("Player joined game: {}", message.getGameId());
            setStatus("Opponent found! Starting game...");
        });
    }
    
    private void handleGameStateUpdate(GameMessage message) {
        Platform.runLater(() -> {
            logger.info("Game state updated: {}", message.getGameId());
            
            // If we're in lobby and receive game state, transition to game screen
            if (isInLobby && message.getGameId() != null) {
                currentGameId = message.getGameId();
                showGameScreen();
                setStatus("Game in progress");
            }
            
            // TODO: Update game UI with new state
            logger.debug("Game state processing completed");
        });
    }
    
    private void handleError(GameMessage message) {
        Platform.runLater(() -> {
            logger.error("Server error: {}", message.getPayload());
            showError("Game Error", message.getPayload().toString());
        });
    }
    
    // Private Helper Methods
    private boolean validateConnection() {
        if (!webSocketClient.isConnected()) {
            showError("Not Connected", "Please connect to the server first.");
            return false;
        }
        return true;
    }
    
    private void joinHumanLobby() {
        isInLobby = true;
        setStatus("Joining lobby...");
        
        gameService.joinLobby(currentPlayer).thenAccept(result -> {
            Platform.runLater(() -> {
                if (result.isSuccess()) {
                    currentGameId = result.getGameId();
                    setStatus("Waiting for opponent...");
                    
                    // Send join message via WebSocket
                    GameMessage joinMessage = GameMessage.joinGame(currentGameId, currentPlayer.getId());
                    webSocketClient.sendMessage(joinMessage);
                } else {
                    setStatus("Failed to join lobby: " + result.getMessage());
                    showError("Lobby Error", result.getMessage());
                    showWelcomeScreen();
                    isInLobby = false;
                }
            });
        });
    }
    
    private void createBotGame() {
        isInLobby = true;
        setStatus("Creating game with bot...");
        
        gameService.playAgainstBot(currentPlayer).thenAccept(result -> {
            Platform.runLater(() -> {
                if (result.isSuccess()) {
                    currentGameId = result.getGameId();
                    setStatus("Bot opponent found! Starting game...");
                    
                    // Send join message via WebSocket
                    GameMessage joinMessage = GameMessage.joinGame(currentGameId, currentPlayer.getId());
                    webSocketClient.sendMessage(joinMessage);
                } else {
                    setStatus("Failed to create bot game: " + result.getMessage());
                    showError("Bot Game Error", result.getMessage());
                    showWelcomeScreen();
                    isInLobby = false;
                }
            });
        });
    }
    
    private void showWelcomeScreen() {
        welcomePane.setVisible(true);
        lobbyPane.setVisible(false);
        gamePane.setVisible(false);
    }
    
    private void showLobbyScreen() {
        welcomePane.setVisible(false);
        lobbyPane.setVisible(true);
        gamePane.setVisible(false);
    }
    
    private void showGameScreen() {
        welcomePane.setVisible(false);
        lobbyPane.setVisible(false);
        gamePane.setVisible(true);
        isInLobby = false;
    }
    
    private void updateConnectionStatus(boolean connected) {
        if (connected) {
            connectionStatus.setText("Connected");
            connectionStatus.setStyle("-fx-text-fill: green;");
            serverStatusLabel.setText("Server: Connected");
            connectButton.setText("Connected");
            disconnectMenuItem.setDisable(false);
        } else {
            connectionStatus.setText("Disconnected");
            connectionStatus.setStyle("-fx-text-fill: red;");
            serverStatusLabel.setText("Server: Disconnected");
            connectButton.setText("Connect");
            disconnectMenuItem.setDisable(true);
        }
    }
    
    private void enableGameOptions(boolean enabled) {
        playHumanButton.setDisable(!enabled);
        playBotButton.setDisable(!enabled);
        watchGameButton.setDisable(!enabled);
        newGameMenuItem.setDisable(!enabled);
        joinGameMenuItem.setDisable(!enabled);
    }
    
    private void updateUIState() {
        updateConnectionStatus(false);
        enableGameOptions(false);
        showWelcomeScreen();
    }
    
    private void setStatus(String message) {
        statusLabel.setText(message);
        logger.debug("Status: {}", message);
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public void shutdown() {
        logger.info("Shutting down AppController");
        if (webSocketClient != null) {
            webSocketClient.disconnect();
        }
    }
}