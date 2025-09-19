package com.scrabble.common.service;

import com.scrabble.common.model.*;

import java.util.*;

/**
 * Game rules engine that validates moves and calculates scores according to Scrabble rules.
 */
public class GameRules {
    
    private final Dictionary dictionary;
    
    /**
     * Bonus points for using all 7 tiles in one move (bingo)
     */
    public static final int BINGO_BONUS = 50;
    
    /**
     * Minimum word length in Scrabble
     */
    public static final int MIN_WORD_LENGTH = 2;
    
    public GameRules(Dictionary dictionary) {
        this.dictionary = dictionary;
    }
    
    /**
     * Validates a move and calculates the score
     */
    public MoveValidationResult validateMove(Move move, Board board, Player player, boolean isFirstMove) {
        if (move == null || board == null || player == null) {
            return MoveValidationResult.invalid("Invalid parameters");
        }
        
        switch (move.getType()) {
            case PLACE_TILES:
                return validateTilePlacement(move, board, player, isFirstMove);
            case PASS:
                return MoveValidationResult.valid(0, Collections.emptyList());
            case EXCHANGE:
                return validateExchange(move, player);
            default:
                return MoveValidationResult.invalid("Unknown move type");
        }
    }
    
    /**
     * Validates a tile placement move
     */
    private MoveValidationResult validateTilePlacement(Move move, Board board, Player player, boolean isFirstMove) {
        Map<Position, Tile> placements = move.getTilePlacements();
        
        if (placements.isEmpty()) {
            return MoveValidationResult.invalid("No tiles placed");
        }
        
        // Check if player has all the tiles
        if (!playerHasTiles(player, placements.values())) {
            return MoveValidationResult.invalid("Player doesn't have the required tiles");
        }
        
        // Check if all positions are valid and empty
        for (Position pos : placements.keySet()) {
            if (!board.isValidPosition(pos)) {
                return MoveValidationResult.invalid("Invalid position: " + pos);
            }
            if (!board.isEmpty(pos)) {
                return MoveValidationResult.invalid("Position already occupied: " + pos);
            }
        }
        
        // Check if tiles are placed in a single line (horizontal or vertical)
        if (!tilesInSingleLine(placements.keySet())) {
            return MoveValidationResult.invalid("Tiles must be placed in a single line");
        }
        
        // For first move, must cover the center square
        if (isFirstMove) {
            Position center = new Position(Board.CENTER, Board.CENTER);
            if (!placements.containsKey(center)) {
                return MoveValidationResult.invalid("First move must cover the center square");
            }
        } else {
            // For subsequent moves, must connect to existing tiles
            if (!connectsToExistingTiles(placements.keySet(), board)) {
                return MoveValidationResult.invalid("Move must connect to existing tiles on the board");
            }
        }
        
        // Create temporary board with new tiles
        Board tempBoard = board.copy();
        for (Map.Entry<Position, Tile> entry : placements.entrySet()) {
            tempBoard.placeTile(entry.getKey(), entry.getValue());
        }
        
        // Find all words formed
        Set<String> wordsFormed = findWordsFormed(placements.keySet(), tempBoard);
        
        // Validate all words
        for (String word : wordsFormed) {
            if (word.length() < MIN_WORD_LENGTH) {
                return MoveValidationResult.invalid("Word too short: " + word);
            }
            if (!dictionary.isValidWord(word)) {
                return MoveValidationResult.invalid("Invalid word: " + word);
            }
        }
        
        // Calculate score
        int score = calculateScore(placements, tempBoard, wordsFormed, board);
        
        // Add bingo bonus if all 7 tiles used
        if (placements.size() == Player.RACK_SIZE) {
            score += BINGO_BONUS;
        }
        
        return MoveValidationResult.valid(score, new ArrayList<>(wordsFormed));
    }
    
    /**
     * Validates an exchange move
     */
    private MoveValidationResult validateExchange(Move move, Player player) {
        List<Tile> exchangeTiles = move.getExchangedTiles();
        
        if (exchangeTiles.isEmpty()) {
            return MoveValidationResult.invalid("No tiles to exchange");
        }
        
        if (!playerHasTiles(player, exchangeTiles)) {
            return MoveValidationResult.invalid("Player doesn't have the tiles to exchange");
        }
        
        return MoveValidationResult.valid(0, Collections.emptyList());
    }
    
    /**
     * Checks if player has all the required tiles
     */
    private boolean playerHasTiles(Player player, Collection<Tile> requiredTiles) {
        Map<Character, Integer> playerTiles = new HashMap<>();
        int blanks = 0;
        
        // Count player's tiles
        for (Tile tile : player.getRack()) {
            if (tile.isBlank()) {
                blanks++;
            } else {
                playerTiles.merge(tile.getDisplayLetter(), 1, Integer::sum);
            }
        }
        
        // Count required tiles
        Map<Character, Integer> requiredCount = new HashMap<>();
        for (Tile tile : requiredTiles) {
            if (tile.isBlank()) {
                // Blank tiles can represent any letter, handled separately
                continue;
            }
            requiredCount.merge(tile.getDisplayLetter(), 1, Integer::sum);
        }
        
        // Check if player has enough of each letter
        int blanksNeeded = 0;
        for (Map.Entry<Character, Integer> entry : requiredCount.entrySet()) {
            char letter = entry.getKey();
            int needed = entry.getValue();
            int available = playerTiles.getOrDefault(letter, 0);
            
            if (available < needed) {
                blanksNeeded += (needed - available);
            }
        }
        
        return blanksNeeded <= blanks;
    }
    
    /**
     * Checks if all positions are in a single line (horizontal or vertical)
     */
    private boolean tilesInSingleLine(Set<Position> positions) {
        if (positions.size() <= 1) {
            return true;
        }
        
        List<Position> sortedPositions = new ArrayList<>(positions);
        sortedPositions.sort(Comparator.comparing(Position::getRow)
                                       .thenComparing(Position::getCol));
        
        boolean horizontal = true;
        boolean vertical = true;
        
        Position first = sortedPositions.get(0);
        
        // Check if all in same row (horizontal)
        for (Position pos : sortedPositions) {
            if (pos.getRow() != first.getRow()) {
                horizontal = false;
                break;
            }
        }
        
        // Check if all in same column (vertical)
        for (Position pos : sortedPositions) {
            if (pos.getCol() != first.getCol()) {
                vertical = false;
                break;
            }
        }
        
        return horizontal || vertical;
    }
    
    /**
     * Checks if the move connects to existing tiles on the board
     */
    private boolean connectsToExistingTiles(Set<Position> newPositions, Board board) {
        for (Position pos : newPositions) {
            List<Position> adjacent = board.getAdjacentPositions(pos);
            for (Position adj : adjacent) {
                if (board.isOccupied(adj)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Finds all words formed by the move
     */
    private Set<String> findWordsFormed(Set<Position> newPositions, Board board) {
        Set<String> wordsFormed = new HashSet<>();
        
        // Get the direction of the main word
        Direction direction = getDirection(newPositions);
        
        // Find the main word
        String mainWord = findMainWord(newPositions, board, direction);
        if (mainWord.length() >= MIN_WORD_LENGTH) {
            wordsFormed.add(mainWord);
        }
        
        // Find cross words (perpendicular to main direction)
        Direction crossDirection = direction.opposite();
        for (Position pos : newPositions) {
            String crossWord = findWordAt(pos, board, crossDirection);
            if (crossWord.length() >= MIN_WORD_LENGTH) {
                wordsFormed.add(crossWord);
            }
        }
        
        return wordsFormed;
    }
    
    /**
     * Determines the direction of tile placement
     */
    private Direction getDirection(Set<Position> positions) {
        if (positions.size() <= 1) {
            return Direction.HORIZONTAL; // Default
        }
        
        List<Position> sortedPositions = new ArrayList<>(positions);
        sortedPositions.sort(Comparator.comparing(Position::getRow)
                                       .thenComparing(Position::getCol));
        
        Position first = sortedPositions.get(0);
        Position last = sortedPositions.get(sortedPositions.size() - 1);
        
        if (first.getRow() == last.getRow()) {
            return Direction.HORIZONTAL;
        } else {
            return Direction.VERTICAL;
        }
    }
    
    /**
     * Finds the main word formed by the placement
     */
    private String findMainWord(Set<Position> newPositions, Board board, Direction direction) {
        if (newPositions.isEmpty()) {
            return "";
        }
        
        // Find the bounds of the word
        Position start = Collections.min(newPositions, 
            direction.isHorizontal() ? 
                Comparator.comparing(Position::getCol) : 
                Comparator.comparing(Position::getRow));
        
        Position end = Collections.max(newPositions,
            direction.isHorizontal() ? 
                Comparator.comparing(Position::getCol) : 
                Comparator.comparing(Position::getRow));
        
        // Extend backwards to find the complete word
        while (true) {
            Position prev = direction.isHorizontal() ? start.left() : start.up();
            if (!board.isValidPosition(prev) || board.isEmpty(prev)) {
                break;
            }
            start = prev;
        }
        
        // Extend forwards to find the complete word
        while (true) {
            Position next = direction.isHorizontal() ? end.right() : end.down();
            if (!board.isValidPosition(next) || board.isEmpty(next)) {
                break;
            }
            end = next;
        }
        
        // Build the word
        return buildWord(start, end, board, direction);
    }
    
    /**
     * Finds a word at a specific position in a given direction
     */
    private String findWordAt(Position position, Board board, Direction direction) {
        // Find the start of the word
        Position start = position;
        while (true) {
            Position prev = direction.isHorizontal() ? start.left() : start.up();
            if (!board.isValidPosition(prev) || board.isEmpty(prev)) {
                break;
            }
            start = prev;
        }
        
        // Find the end of the word
        Position end = position;
        while (true) {
            Position next = direction.isHorizontal() ? end.right() : end.down();
            if (!board.isValidPosition(next) || board.isEmpty(next)) {
                break;
            }
            end = next;
        }
        
        return buildWord(start, end, board, direction);
    }
    
    /**
     * Builds a word from start to end position
     */
    private String buildWord(Position start, Position end, Board board, Direction direction) {
        StringBuilder word = new StringBuilder();
        Position current = start;
        
        while (board.isValidPosition(current)) {
            Tile tile = board.getTile(current);
            if (tile != null) {
                word.append(tile.getDisplayLetter());
            }
            
            if (current.equals(end)) {
                break;
            }
            
            current = current.move(direction);
        }
        
        return word.toString();
    }
    
    /**
     * Calculates the score for a move
     */
    private int calculateScore(Map<Position, Tile> placements, Board tempBoard, 
                              Set<String> wordsFormed, Board originalBoard) {
        int totalScore = 0;
        
        for (String word : wordsFormed) {
            int wordScore = calculateWordScore(word, placements, tempBoard, originalBoard);
            totalScore += wordScore;
        }
        
        return totalScore;
    }
    
    /**
     * Calculates the score for a single word
     */
    private int calculateWordScore(String word, Map<Position, Tile> placements, 
                                  Board tempBoard, Board originalBoard) {
        // Find the positions of this word
        List<Position> wordPositions = findWordPositions(word, tempBoard);
        if (wordPositions.isEmpty()) {
            return 0;
        }
        
        int letterScore = 0;
        int wordMultiplier = 1;
        
        for (Position pos : wordPositions) {
            Tile tile = tempBoard.getTile(pos);
            if (tile == null) continue;
            
            int tileScore = tile.getScoreValue();
            
            // Apply premium squares only for newly placed tiles
            if (placements.containsKey(pos)) {
                Board.PremiumType premium = tempBoard.getPremium(pos);
                switch (premium) {
                    case DOUBLE_LETTER:
                        tileScore *= 2;
                        break;
                    case TRIPLE_LETTER:
                        tileScore *= 3;
                        break;
                    case DOUBLE_WORD:
                    case CENTER:
                        wordMultiplier *= 2;
                        break;
                    case TRIPLE_WORD:
                        wordMultiplier *= 3;
                        break;
                }
            }
            
            letterScore += tileScore;
        }
        
        return letterScore * wordMultiplier;
    }
    
    /**
     * Finds the positions of a word on the board
     */
    private List<Position> findWordPositions(String word, Board board) {
        // This is a simplified implementation
        // In a full implementation, you'd need to find the exact positions
        // For now, we'll return empty list
        return new ArrayList<>();
    }
    
    /**
     * Result of move validation
     */
    public static class MoveValidationResult {
        private final boolean valid;
        private final int score;
        private final List<String> wordsFormed;
        private final String errorMessage;
        
        private MoveValidationResult(boolean valid, int score, List<String> wordsFormed, String errorMessage) {
            this.valid = valid;
            this.score = score;
            this.wordsFormed = wordsFormed;
            this.errorMessage = errorMessage;
        }
        
        public static MoveValidationResult valid(int score, List<String> wordsFormed) {
            return new MoveValidationResult(true, score, wordsFormed, null);
        }
        
        public static MoveValidationResult invalid(String errorMessage) {
            return new MoveValidationResult(false, 0, Collections.emptyList(), errorMessage);
        }
        
        // Getters
        public boolean isValid() { return valid; }
        public int getScore() { return score; }
        public List<String> getWordsFormed() { return wordsFormed; }
        public String getErrorMessage() { return errorMessage; }
        
        @Override
        public String toString() {
            if (valid) {
                return String.format("Valid move: %d points, words: %s", score, wordsFormed);
            } else {
                return String.format("Invalid move: %s", errorMessage);
            }
        }
    }
}