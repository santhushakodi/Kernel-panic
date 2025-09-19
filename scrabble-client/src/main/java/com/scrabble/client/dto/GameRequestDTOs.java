package com.scrabble.client.dto;

import com.scrabble.client.model.TilePlacement;
import com.scrabble.common.model.Tile;

import java.util.List;

/**
 * DTO classes for various game requests sent to the server
 */
public class GameRequestDTOs {

    /**
     * Request to submit a move
     */
    public static class MoveRequest {
        private String gameId;
        private String playerId;
        private List<TilePlacement> placements;

        // Default constructor for serialization
        public MoveRequest() {}

        public MoveRequest(String gameId, String playerId, List<TilePlacement> placements) {
            this.gameId = gameId;
            this.playerId = playerId;
            this.placements = placements;
        }

        public String getGameId() { return gameId; }
        public void setGameId(String gameId) { this.gameId = gameId; }

        public String getPlayerId() { return playerId; }
        public void setPlayerId(String playerId) { this.playerId = playerId; }

        public List<TilePlacement> getPlacements() { return placements; }
        public void setPlacements(List<TilePlacement> placements) { this.placements = placements; }
    }

    /**
     * Request to pass turn
     */
    public static class PassTurnRequest {
        private String gameId;
        private String playerId;

        // Default constructor for serialization
        public PassTurnRequest() {}

        public PassTurnRequest(String gameId, String playerId) {
            this.gameId = gameId;
            this.playerId = playerId;
        }

        public String getGameId() { return gameId; }
        public void setGameId(String gameId) { this.gameId = gameId; }

        public String getPlayerId() { return playerId; }
        public void setPlayerId(String playerId) { this.playerId = playerId; }
    }

    /**
     * Request to exchange tiles
     */
    public static class ExchangeTilesRequest {
        private String gameId;
        private String playerId;
        private List<Tile> tilesToExchange;

        // Default constructor for serialization
        public ExchangeTilesRequest() {}

        public ExchangeTilesRequest(String gameId, String playerId, List<Tile> tilesToExchange) {
            this.gameId = gameId;
            this.playerId = playerId;
            this.tilesToExchange = tilesToExchange;
        }

        public String getGameId() { return gameId; }
        public void setGameId(String gameId) { this.gameId = gameId; }

        public String getPlayerId() { return playerId; }
        public void setPlayerId(String playerId) { this.playerId = playerId; }

        public List<Tile> getTilesToExchange() { return tilesToExchange; }
        public void setTilesToExchange(List<Tile> tilesToExchange) { this.tilesToExchange = tilesToExchange; }
    }

    /**
     * Generic response for game operations
     */
    public static class GameResponse {
        private boolean success;
        private String errorMessage;
        private Object data;

        // Default constructor for serialization
        public GameResponse() {}

        public GameResponse(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public GameResponse(boolean success, String errorMessage, Object data) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.data = data;
        }

        public static GameResponse success() {
            return new GameResponse(true, null);
        }

        public static GameResponse success(Object data) {
            return new GameResponse(true, null, data);
        }

        public static GameResponse error(String message) {
            return new GameResponse(false, message);
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
    }
}