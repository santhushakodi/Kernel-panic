package com.scrabble.server.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.scrabble.common.dto.GameMessage;
import com.scrabble.common.model.GameState;
import com.scrabble.common.model.Move;
import com.scrabble.common.model.Position;
import com.scrabble.common.model.Tile;
import com.scrabble.common.service.GameRules;
import com.scrabble.server.service.GameService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for real-time game communication.
 */
@Component
public class GameWebSocketHandler extends TextWebSocketHandler {
    
    private final GameService gameService;
    private final ObjectMapper objectMapper;
    
    // Maps to track sessions and their associated games/players
    private final Map<String, WebSocketSession> playerSessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToPlayer = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToGame = new ConcurrentHashMap<>();
    
    public GameWebSocketHandler(GameService gameService) {
        this.gameService = gameService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules(); // For LocalDateTime support
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Register this handler with the GameService for bot move notifications
        gameService.setWebSocketHandler(this);
    }
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("WebSocket connection established: " + session.getId());
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            GameMessage gameMessage = objectMapper.readValue(message.getPayload(), GameMessage.class);
            handleGameMessage(session, gameMessage);
        } catch (Exception e) {
            System.err.println("Error handling WebSocket message: " + e.getMessage());
            sendError(session, null, null, "Invalid message format");
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("WebSocket connection closed: " + session.getId());
        
        String playerId = sessionToPlayer.remove(session.getId());
        String gameId = sessionToGame.remove(session.getId());
        
        if (playerId != null) {
            playerSessions.remove(playerId);
            
            // Notify other players in the game
            if (gameId != null) {
                broadcastToGame(gameId, GameMessage.playerLeft(gameId, playerId), playerId);
            }
        }
    }
    
    /**
     * Handles incoming game messages
     */
    private void handleGameMessage(WebSocketSession session, GameMessage message) {
        try {
            switch (message.getType()) {
                case JOIN_GAME:
                    handleJoinGame(session, message);
                    break;
                    
                case LEAVE_GAME:
                    handleLeaveGame(session, message);
                    break;
                    
                case MAKE_MOVE:
                    handleMakeMove(session, message);
                    break;
                    
                case REQUEST_GAME_STATE:
                    handleGameStateRequest(session, message);
                    break;
                    
                case CHAT_MESSAGE:
                    handleChatMessage(session, message);
                    break;
                    
                default:
                    sendError(session, message.getGameId(), message.getPlayerId(),
                            "Unsupported message type: " + message.getType());
            }
        } catch (Exception e) {
            System.err.println("Error handling game message: " + e.getMessage());
            sendError(session, message.getGameId(), message.getPlayerId(), e.getMessage());
        }
    }
    
    /**
     * Handles player joining a game
     */
    private void handleJoinGame(WebSocketSession session, GameMessage message) {
        String gameId = message.getGameId();
        String playerId = message.getPlayerId();
        
        GameState game = gameService.getGame(gameId);
        if (game == null) {
            sendError(session, gameId, playerId, "Game not found");
            return;
        }
        
        // Register the session
        playerSessions.put(playerId, session);
        sessionToPlayer.put(session.getId(), playerId);
        sessionToGame.put(session.getId(), gameId);
        
        // Send current game state to the joining player
        sendMessage(session, GameMessage.gameStateUpdate(gameId, game));
        
        // Notify other players
        broadcastToGame(gameId, GameMessage.playerJoined(gameId, game.getPlayerById(playerId)), playerId);
        
        System.out.println("Player " + playerId + " joined game " + gameId);
    }
    
    /**
     * Handles player leaving a game
     */
    private void handleLeaveGame(WebSocketSession session, GameMessage message) {
        String gameId = message.getGameId();
        String playerId = message.getPlayerId();
        
        // Clean up session tracking
        playerSessions.remove(playerId);
        sessionToPlayer.remove(session.getId());
        sessionToGame.remove(session.getId());
        
        // Notify other players
        broadcastToGame(gameId, GameMessage.playerLeft(gameId, playerId), playerId);
        
        System.out.println("Player " + playerId + " left game " + gameId);
    }
    
    /**
     * Handles move attempts
     */
    private void handleMakeMove(WebSocketSession session, GameMessage message) {
        String gameId = message.getGameId();
        String playerId = message.getPlayerId();
        
        System.out.println("DEBUG: Received MAKE_MOVE message from player " + playerId);
        System.out.println("DEBUG: Message payload type: " + (message.getPayload() != null ? message.getPayload().getClass() : "null"));
        System.out.println("DEBUG: Message payload: " + message.getPayload());
        
        Move move = null;
        try {
            // Try to deserialize the payload as Move using custom approach
            if (message.getPayload() != null) {
                String payloadJson = objectMapper.writeValueAsString(message.getPayload());
                System.out.println("DEBUG: Payload JSON: " + payloadJson);
                
                // Parse as a generic object first to handle the custom structure
                @SuppressWarnings("unchecked")
                Map<String, Object> moveData = objectMapper.readValue(payloadJson, Map.class);
                
                move = createMoveFromMap(moveData);
                System.out.println("DEBUG: Successfully created Move: " + move);
            }
        } catch (Exception e) {
            System.err.println("DEBUG: Error deserializing Move: " + e.getMessage());
            e.printStackTrace();
        }
        
        if (move == null) {
            sendError(session, gameId, playerId, "Invalid move data - could not parse move object");
            return;
        }
        
        // Process the move
        GameRules.MoveValidationResult result = gameService.processMove(gameId, playerId, move);
        
        // Send result back to the player
        GameMessage.MoveResult moveResult = result.isValid() ?
                GameMessage.MoveResult.success(result.getScore(), result.getWordsFormed(), false) :
                GameMessage.MoveResult.failure(result.getErrorMessage());
        
        sendMessage(session, GameMessage.moveResult(gameId, playerId, moveResult));
        
        if (result.isValid()) {
            // Broadcast updated game state to all players
            GameState updatedGame = gameService.getGame(gameId);
            if (updatedGame != null) {
                System.out.println("DEBUG: Broadcasting game state update after move");
                broadcastToGame(gameId, GameMessage.gameStateUpdate(gameId, updatedGame), null);
                
                // Check if game ended
                if (updatedGame.isGameOver()) {
                    broadcastToGame(gameId, GameMessage.gameEnded(gameId,
                            updatedGame.getGameEndReason(), updatedGame.getWinnerId()), null);
                }
            }
        }
    }
    
    /**
     * Handles game state requests
     */
    private void handleGameStateRequest(WebSocketSession session, GameMessage message) {
        String gameId = message.getGameId();
        GameState game = gameService.getGame(gameId);
        
        if (game != null) {
            sendMessage(session, GameMessage.gameStateUpdate(gameId, game));
        } else {
            sendError(session, gameId, message.getPlayerId(), "Game not found");
        }
    }
    
    /**
     * Handles chat messages
     */
    private void handleChatMessage(WebSocketSession session, GameMessage message) {
        String gameId = message.getGameId();
        
        // Broadcast chat message to all players in the game
        broadcastToGame(gameId, message, null);
    }
    
    /**
     * Sends a message to a specific session
     */
    private void sendMessage(WebSocketSession session, GameMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            System.err.println("Error sending message: " + e.getMessage());
        }
    }
    
    /**
     * Sends an error message to a session
     */
    private void sendError(WebSocketSession session, String gameId, String playerId, String error) {
        sendMessage(session, GameMessage.error(gameId, playerId, error));
    }
    
    /**
     * Broadcasts a message to all players in a game
     */
    private void broadcastToGame(String gameId, GameMessage message, String excludePlayerId) {
        GameState game = gameService.getGame(gameId);
        if (game == null) return;
        
        for (var player : game.getPlayers()) {
            if (excludePlayerId != null && excludePlayerId.equals(player.getId())) {
                continue; // Skip the excluded player
            }
            
            WebSocketSession session = playerSessions.get(player.getId());
            if (session != null && session.isOpen()) {
                sendMessage(session, message);
            }
        }
    }
    
    /**
     * Creates a Move object from a Map representation (for custom deserialization)
     */
    private Move createMoveFromMap(Map<String, Object> moveData) throws Exception {
        String playerId = (String) moveData.get("playerId");
        String typeStr = (String) moveData.get("type");
        
        Move.Type moveType;
        try {
            moveType = Move.Type.valueOf(typeStr);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid move type: " + typeStr);
        }
        
        System.out.println("DEBUG: Creating move with type: " + moveType);
        
        Map<Position, Tile> tilePlacements = new HashMap<>();
        List<Tile> exchangedTiles = new ArrayList<>();
        
        // Handle tile placements for PLACE_TILES moves
        if (moveType == Move.Type.PLACE_TILES) {
            Object placementsObj = moveData.get("tilePlacements");
            if (placementsObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> placementsMap = (Map<String, Object>) placementsObj;
                
                for (Map.Entry<String, Object> entry : placementsMap.entrySet()) {
                    // Parse position from key (format: "row,col")
                    String[] coords = entry.getKey().split(",");
                    if (coords.length == 2) {
                        int row = Integer.parseInt(coords[0]);
                        int col = Integer.parseInt(coords[1]);
                        Position position = new Position(row, col);
                        
                        // Parse tile from value
                        @SuppressWarnings("unchecked")
                        Map<String, Object> tileData = (Map<String, Object>) entry.getValue();
                        Tile tile = createTileFromMap(tileData);
                        
                        tilePlacements.put(position, tile);
                    }
                }
            }
        }
        
        // Handle exchanged tiles for EXCHANGE moves
        if (moveType == Move.Type.EXCHANGE) {
            Object exchangedObj = moveData.get("exchangedTiles");
            if (exchangedObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> exchangedList = (List<Map<String, Object>>) exchangedObj;
                
                for (Map<String, Object> tileData : exchangedList) {
                    exchangedTiles.add(createTileFromMap(tileData));
                }
            }
        }
        
        // Create the Move object
        return new Move(
            playerId,
            moveType,
            tilePlacements,
            exchangedTiles,
            0, // score - will be calculated
            new ArrayList<>(), // words formed - will be calculated
            java.time.LocalDateTime.now(),
            true, // assume valid initially
            null // no invalid reason initially
        );
    }
    
    /**
     * Creates a Tile object from a Map representation
     */
    private Tile createTileFromMap(Map<String, Object> tileData) {
        String letter = (String) tileData.get("letter");
        Integer value = (Integer) tileData.get("value");
        String assignedLetter = (String) tileData.get("assignedLetter");
        
        if (letter == null || letter.isEmpty()) {
            throw new IllegalArgumentException("Tile letter cannot be null or empty");
        }
        
        Tile tile = new Tile(letter.charAt(0), value != null ? value : 0);
        
        // Set assigned letter if different from original letter (for blank tiles)
        if (assignedLetter != null && !assignedLetter.isEmpty() && 
            !assignedLetter.equals(letter)) {
            tile.setAssignedLetter(assignedLetter.charAt(0));
        }
        
        return tile;
    }
    
    /**
     * Gets the number of active WebSocket connections
     */
    public int getActiveConnections() {
        return playerSessions.size();
    }
    
    /**
     * Broadcasts a message to a specific player
     */
    public void sendToPlayer(String playerId, GameMessage message) {
        WebSocketSession session = playerSessions.get(playerId);
        if (session != null && session.isOpen()) {
            sendMessage(session, message);
        }
    }
    
    /**
     * Notifies all players about a bot move and updated game state
     */
    public void notifyBotMove(String gameId) {
        System.out.println("DEBUG: Notifying about bot move in game: " + gameId);
        GameState updatedGame = gameService.getGame(gameId);
        if (updatedGame != null) {
            broadcastToGame(gameId, GameMessage.gameStateUpdate(gameId, updatedGame), null);
            
            // Check if game ended after bot move
            if (updatedGame.isGameOver()) {
                broadcastToGame(gameId, GameMessage.gameEnded(gameId,
                        updatedGame.getGameEndReason(), updatedGame.getWinnerId()), null);
            }
        }
    }
}