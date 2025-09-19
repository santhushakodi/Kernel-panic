package com.scrabble.client;

import com.scrabble.client.controller.AppController;
import com.scrabble.client.service.GameServiceClient;
import com.scrabble.client.websocket.GameWebSocketClient;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main JavaFX application for the Scrabble game client.
 * Handles application lifecycle, scene management, and service initialization.
 */
public class MainApplication extends Application {
    private static final Logger logger = LoggerFactory.getLogger(MainApplication.class);
    
    private static final String TITLE = "Scrabble Game";
    private static final int MIN_WIDTH = 1200;
    private static final int MIN_HEIGHT = 800;
    private static final String SERVER_URL = "http://localhost:8081";
    private static final String WEBSOCKET_URL = "ws://localhost:8081/game";
    
    private Stage primaryStage;
    private AppController appController;
    private GameServiceClient gameService;
    private GameWebSocketClient webSocketClient;
    
    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        
        try {
            initializeServices();
            setupPrimaryStage();
            loadMainScene();
            
            logger.info("Scrabble client application started successfully");
        } catch (Exception e) {
            logger.error("Failed to start application", e);
            showErrorDialog("Application Startup Error", 
                "Failed to start the Scrabble game client: " + e.getMessage());
            Platform.exit();
        }
    }
    
    @Override
    public void stop() {
        logger.info("Shutting down Scrabble client application");
        
        try {
            if (webSocketClient != null) {
                webSocketClient.disconnect();
            }
            if (appController != null) {
                appController.shutdown();
            }
        } catch (Exception e) {
            logger.warn("Error during application shutdown", e);
        }
    }
    
    private void initializeServices() {
        logger.debug("Initializing client services");
        
        this.gameService = new GameServiceClient(SERVER_URL);
        this.webSocketClient = new GameWebSocketClient(WEBSOCKET_URL);
        
        // Set up WebSocket connection callbacks
        this.webSocketClient.setOnOpen(() -> {
            logger.info("Connected to game server");
            Platform.runLater(() -> {
                if (appController != null) {
                    appController.onServerConnected();
                }
            });
        });
        
        this.webSocketClient.setOnClose(() -> {
            logger.warn("Disconnected from game server");
            Platform.runLater(() -> {
                if (appController != null) {
                    appController.onServerDisconnected();
                }
            });
        });
        
        this.webSocketClient.setOnError((error) -> {
            logger.error("WebSocket error: " + error);
            Platform.runLater(() -> {
                if (appController != null) {
                    appController.onServerError(error);
                }
            });
        });
    }
    
    private void setupPrimaryStage() {
        primaryStage.setTitle(TITLE);
        primaryStage.setMinWidth(MIN_WIDTH);
        primaryStage.setMinHeight(MIN_HEIGHT);
        
        // Set application icon if available
        try {
            Image icon = new Image(getClass().getResourceAsStream("/images/scrabble-icon.png"));
            primaryStage.getIcons().add(icon);
        } catch (Exception e) {
            logger.debug("Application icon not found, using default");
        }
        
        // Handle window close event
        primaryStage.setOnCloseRequest(event -> {
            logger.info("User requested to close application");
            stop();
            Platform.exit();
        });
    }
    
    private void loadMainScene() throws Exception {
        logger.debug("Loading main application scene");
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        Scene scene = new Scene(loader.load(), MIN_WIDTH, MIN_HEIGHT);
        
        // Load CSS styling
        try {
            scene.getStylesheets().add(getClass().getResource("/css/scrabble.css").toExternalForm());
        } catch (Exception e) {
            logger.warn("Could not load CSS stylesheet", e);
        }
        
        // Get the controller and inject dependencies
        this.appController = loader.getController();
        this.appController.initialize(gameService, webSocketClient, primaryStage);
        
        primaryStage.setScene(scene);
        primaryStage.show();
        
        logger.debug("Main scene loaded and displayed");
    }
    
    private void showErrorDialog(String title, String message) {
        Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    public static void main(String[] args) {
        // Set system properties for better JavaFX experience
        System.setProperty("prism.lcdtext", "false");
        
        logger.info("Starting Scrabble Game Client");
        launch(args);
    }
}