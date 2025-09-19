package com.scrabble.client.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.scrabble.common.dto.GameMessage;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * WebSocket client for real-time communication with the Scrabble game server.
 * Handles connection management, message serialization, and callback management.
 */
public class GameWebSocketClient extends WebSocketClient {
    private static final Logger logger = LoggerFactory.getLogger(GameWebSocketClient.class);
    private static final int RECONNECT_DELAY_SECONDS = 5;
    private static final int MAX_RECONNECT_ATTEMPTS = 3;
    
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Consumer<GameMessage>> messageHandlers;
    
    private Runnable onOpenCallback;
    private Runnable onCloseCallback;
    private Consumer<String> onErrorCallback;
    private Consumer<GameMessage> onMessageCallback;
    
    private boolean shouldReconnect = true;
    private int reconnectAttempts = 0;
    private CompletableFuture<Void> connectionFuture;
    
    public GameWebSocketClient(String serverUrl) {
        super(URI.create(serverUrl));
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.messageHandlers = new ConcurrentHashMap<>();
        this.connectionFuture = new CompletableFuture<>();
        
        logger.debug("GameWebSocketClient created for server: {}", serverUrl);
    }
    
    @Override
    public void onOpen(ServerHandshake handshake) {
        logger.info("WebSocket connection opened to game server");
        reconnectAttempts = 0;
        
        if (!connectionFuture.isDone()) {
            connectionFuture.complete(null);
        }
        
        if (onOpenCallback != null) {
            onOpenCallback.run();
        }
    }
    
    @Override
    public void onMessage(String message) {
        logger.debug("Received WebSocket message: {}", message);
        
        try {
            GameMessage gameMessage = objectMapper.readValue(message, GameMessage.class);
            handleGameMessage(gameMessage);
        } catch (Exception e) {
            logger.error("Failed to parse WebSocket message: {}", message, e);
        }
    }
    
    @Override
    public void onClose(int code, String reason, boolean remote) {
        logger.warn("WebSocket connection closed - Code: {}, Reason: {}, Remote: {}", code, reason, remote);
        
        if (!connectionFuture.isDone()) {
            connectionFuture.completeExceptionally(new RuntimeException("Connection failed: " + reason));
        }
        
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
        
        // Attempt reconnection if appropriate
        if (shouldReconnect && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            scheduleReconnect();
        }
    }
    
    @Override
    public void onError(Exception ex) {
        logger.error("WebSocket error occurred", ex);
        
        if (!connectionFuture.isDone()) {
            connectionFuture.completeExceptionally(ex);
        }
        
        if (onErrorCallback != null) {
            onErrorCallback.accept(ex.getMessage());
        }
    }
    
    /**
     * Connect to the server asynchronously.
     */
    public CompletableFuture<Void> connectAsync() {
        logger.debug("Attempting to connect to game server");
        
        this.connectionFuture = new CompletableFuture<>();
        this.shouldReconnect = true;
        this.reconnectAttempts = 0;
        
        try {
            this.connect();
            return connectionFuture.orTimeout(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("Failed to initiate WebSocket connection", e);
            connectionFuture.completeExceptionally(e);
            return connectionFuture;
        }
    }
    
    /**
     * Disconnect from the server.
     */
    public void disconnect() {
        logger.info("Disconnecting from game server");
        
        this.shouldReconnect = false;
        if (this.isOpen()) {
            this.close();
        }
    }
    
    /**
     * Send a game message to the server.
     */
    public boolean sendMessage(GameMessage message) {
        if (!isOpen()) {
            logger.warn("Cannot send message - WebSocket not connected");
            return false;
        }
        
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            this.send(jsonMessage);
            logger.debug("Sent WebSocket message: {}", jsonMessage);
            return true;
        } catch (Exception e) {
            logger.error("Failed to send WebSocket message", e);
            return false;
        }
    }
    
    /**
     * Register a handler for specific message types.
     */
    public void registerMessageHandler(String messageType, Consumer<GameMessage> handler) {
        messageHandlers.put(messageType, handler);
        logger.debug("Registered message handler for type: {}", messageType);
    }
    
    /**
     * Remove a message handler.
     */
    public void removeMessageHandler(String messageType) {
        messageHandlers.remove(messageType);
        logger.debug("Removed message handler for type: {}", messageType);
    }
    
    private void handleGameMessage(GameMessage message) {
        logger.debug("Handling game message of type: {}", message.getType());
        
        // Call specific message handler if registered
        Consumer<GameMessage> handler = messageHandlers.get(message.getType());
        if (handler != null) {
            try {
                handler.accept(message);
            } catch (Exception e) {
                logger.error("Error in message handler for type: {}", message.getType(), e);
            }
        }
        
        // Call general message callback
        if (onMessageCallback != null) {
            try {
                onMessageCallback.accept(message);
            } catch (Exception e) {
                logger.error("Error in general message callback", e);
            }
        }
    }
    
    private void scheduleReconnect() {
        reconnectAttempts++;
        logger.info("Scheduling reconnection attempt {} of {} in {} seconds", 
                   reconnectAttempts, MAX_RECONNECT_ATTEMPTS, RECONNECT_DELAY_SECONDS);
        
        CompletableFuture.delayedExecutor(RECONNECT_DELAY_SECONDS, TimeUnit.SECONDS)
            .execute(() -> {
                if (shouldReconnect) {
                    try {
                        this.reconnect();
                    } catch (Exception e) {
                        logger.error("Reconnection attempt failed", e);
                    }
                }
            });
    }
    
    // Callback setters
    public void setOnOpen(Runnable callback) {
        this.onOpenCallback = callback;
    }
    
    public void setOnClose(Runnable callback) {
        this.onCloseCallback = callback;
    }
    
    public void setOnError(Consumer<String> callback) {
        this.onErrorCallback = callback;
    }
    
    public void setOnMessage(Consumer<GameMessage> callback) {
        this.onMessageCallback = callback;
    }
    
    // Status getters
    public boolean isConnected() {
        return isOpen();
    }
    
    public int getReconnectAttempts() {
        return reconnectAttempts;
    }
}