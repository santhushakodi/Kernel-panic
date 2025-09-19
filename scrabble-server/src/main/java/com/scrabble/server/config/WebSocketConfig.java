package com.scrabble.server.config;

import com.scrabble.server.websocket.GameWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket configuration for game communication.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Autowired
    private GameWebSocketHandler gameWebSocketHandler;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Plain WebSocket endpoint for Java clients
        registry.addHandler(gameWebSocketHandler, "/game")
                .setAllowedOrigins("*"); // For development - restrict in production
                
        // SockJS endpoint for web browsers
        registry.addHandler(gameWebSocketHandler, "/game-sockjs")
                .setAllowedOrigins("*") // For development - restrict in production
                .withSockJS(); // Enable SockJS fallback for browsers that don't support WebSocket
    }
}