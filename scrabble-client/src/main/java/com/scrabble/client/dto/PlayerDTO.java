package com.scrabble.client.dto;

import com.scrabble.common.model.Player;

/**
 * Data Transfer Object for Player API requests.
 * Contains only the essential fields needed for lobby operations.
 */
public class PlayerDTO {
    private String id;
    private String name;
    private boolean isBot;
    
    public PlayerDTO() {}
    
    public PlayerDTO(String id, String name, boolean isBot) {
        this.id = id;
        this.name = name;
        this.isBot = isBot;
    }
    
    /**
     * Create DTO from Player object
     */
    public static PlayerDTO from(Player player) {
        return new PlayerDTO(player.getId(), player.getName(), player.isBot());
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public boolean isBot() {
        return isBot;
    }
    
    public void setBot(boolean bot) {
        isBot = bot;
    }
}