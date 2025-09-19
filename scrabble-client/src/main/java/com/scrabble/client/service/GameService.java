package com.scrabble.client.service;

import com.scrabble.client.dto.GameRequestDTOs.*;
import com.scrabble.common.model.GameState;

import java.util.concurrent.CompletableFuture;

/**
 * Service interface for handling game operations and server communication.
 * This interface defines all the game-related operations that can be performed
 * such as submitting moves, passing turns, and exchanging tiles.
 */
public interface GameService {

    /**
     * Submits a move to the server
     * @param moveRequest the move request containing placements
     * @return CompletableFuture with the game response
     */
    CompletableFuture<GameResponse> submitMove(MoveRequest moveRequest);

    /**
     * Passes the current turn
     * @param passTurnRequest the pass turn request
     * @return CompletableFuture with the game response
     */
    CompletableFuture<GameResponse> passTurn(PassTurnRequest passTurnRequest);

    /**
     * Exchanges tiles from the player's rack
     * @param exchangeRequest the exchange tiles request
     * @return CompletableFuture with the game response
     */
    CompletableFuture<GameResponse> exchangeTiles(ExchangeTilesRequest exchangeRequest);

    /**
     * Gets the current game state
     * @param gameId the game ID
     * @return CompletableFuture with the current game state
     */
    CompletableFuture<GameState> getGameState(String gameId);

    /**
     * Joins a game
     * @param gameId the game ID to join
     * @param playerId the player ID
     * @return CompletableFuture with the game response
     */
    CompletableFuture<GameResponse> joinGame(String gameId, String playerId);

    /**
     * Leaves a game
     * @param gameId the game ID to leave
     * @param playerId the player ID
     * @return CompletableFuture with the game response
     */
    CompletableFuture<GameResponse> leaveGame(String gameId, String playerId);

    /**
     * Creates a new game
     * @param playerId the creator player ID
     * @param maxPlayers maximum number of players
     * @return CompletableFuture with the created game ID
     */
    CompletableFuture<GameResponse> createGame(String playerId, int maxPlayers);

    /**
     * Starts a game (for game creators)
     * @param gameId the game ID to start
     * @param playerId the player ID
     * @return CompletableFuture with the game response
     */
    CompletableFuture<GameResponse> startGame(String gameId, String playerId);
}