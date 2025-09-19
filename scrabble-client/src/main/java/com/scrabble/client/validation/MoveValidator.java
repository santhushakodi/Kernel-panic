package com.scrabble.client.validation;

import com.scrabble.common.model.*;
import com.scrabble.client.model.TilePlacement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Validates Scrabble moves according to official game rules.
 * Checks word formation, connectivity, and calculates scores.
 */
public class MoveValidator {
    private static final Logger logger = LoggerFactory.getLogger(MoveValidator.class);
    
    private static final int CENTER_ROW = 7;
    private static final int CENTER_COL = 7;
    
    // Standard Scrabble letter scores
    private static final Map<Character, Integer> LETTER_SCORES = createLetterScores();
    
    // Basic dictionary for validation (in a real implementation, this would be loaded from a file)
    private final Set<String> dictionary;
    
    public MoveValidator() {
        // Initialize with basic words for validation
        dictionary = initializeDictionary();
        logger.debug("MoveValidator initialized with {} words", dictionary.size());
    }
    
    /**
     * Validates a move and returns the validation result with score
     */
    public MoveValidationResult validateMove(Board board, List<TilePlacement> placements, boolean isFirstMove) {
        logger.debug("Validating move with {} placements, first move: {}", placements.size(), isFirstMove);
        
        try {
            // Basic checks
            if (placements.isEmpty()) {
                return MoveValidationResult.invalid("No tiles placed");
            }
            
            // Check if all positions are valid and empty
            for (TilePlacement placement : placements) {
                Position pos = placement.getPosition();
                if (!board.isValidPosition(pos)) {
                    return MoveValidationResult.invalid("Invalid position: " + pos);
                }
                if (board.isOccupied(pos)) {
                    return MoveValidationResult.invalid("Position already occupied: " + pos);
                }
            }
            
            // Create a copy of the board with new tiles placed
            Board testBoard = board.copy();
            for (TilePlacement placement : placements) {
                testBoard.placeTile(placement.getPosition(), placement.getTile());
            }
            
            // Check if move forms a line (all tiles in same row or column)
            if (!formsLine(placements)) {
                return MoveValidationResult.invalid("All tiles must be placed in the same row or column");
            }
            
            // Check if tiles form a continuous line (no gaps)
            if (!isContinuous(placements, testBoard)) {
                return MoveValidationResult.invalid("Tiles must form a continuous line");
            }
            
            // First move validation
            if (isFirstMove) {
                if (!coversCenter(placements)) {
                    return MoveValidationResult.invalid("First word must cover the center star");
                }
            } else {
                // Check if new tiles connect to existing tiles
                if (!connectsToExistingTiles(placements, board)) {
                    return MoveValidationResult.invalid("New tiles must connect to existing tiles on the board");
                }
            }
            
            // Find all words formed by this move
            List<Word> formedWords = findFormedWords(testBoard, placements);
            if (formedWords.isEmpty()) {
                return MoveValidationResult.invalid("No valid words formed");
            }
            
            // Validate all words against dictionary
            List<String> invalidWords = new ArrayList<>();
            for (Word word : formedWords) {
                if (!isValidWord(word.getText())) {
                    invalidWords.add(word.getText());
                }
            }
            
            if (!invalidWords.isEmpty()) {
                return MoveValidationResult.invalid("Invalid words: " + String.join(", ", invalidWords));
            }
            
            // Calculate score
            int totalScore = calculateScore(testBoard, placements, formedWords);
            
            logger.debug("Move validation successful, score: {}, words: {}", 
                        totalScore, formedWords.stream().map(Word::getText).collect(Collectors.joining(", ")));
            
            return MoveValidationResult.valid(totalScore, formedWords);
            
        } catch (Exception e) {
            logger.error("Error validating move", e);
            return MoveValidationResult.invalid("Validation error: " + e.getMessage());
        }
    }
    
    private boolean formsLine(List<TilePlacement> placements) {
        if (placements.size() <= 1) return true;
        
        Set<Integer> rows = placements.stream()
            .map(p -> p.getPosition().getRow())
            .collect(Collectors.toSet());
            
        Set<Integer> cols = placements.stream()
            .map(p -> p.getPosition().getCol())
            .collect(Collectors.toSet());
        
        return rows.size() == 1 || cols.size() == 1;
    }
    
    private boolean isContinuous(List<TilePlacement> placements, Board board) {
        if (placements.size() <= 1) return true;
        
        // Sort placements by position
        List<Position> positions = placements.stream()
            .map(TilePlacement::getPosition)
            .sorted((p1, p2) -> {
                if (p1.getRow() != p2.getRow()) {
                    return Integer.compare(p1.getRow(), p2.getRow());
                }
                return Integer.compare(p1.getCol(), p2.getCol());
            })
            .collect(Collectors.toList());
        
        // Check if positions are consecutive (allowing for existing tiles in between)
        boolean isHorizontal = positions.stream()
            .map(Position::getRow)
            .distinct()
            .count() == 1;
        
        if (isHorizontal) {
            int row = positions.get(0).getRow();
            int startCol = positions.get(0).getCol();
            int endCol = positions.get(positions.size() - 1).getCol();
            
            // Check if all positions between start and end are occupied
            for (int col = startCol; col <= endCol; col++) {
                Position pos = new Position(row, col);
                if (board.isEmpty(pos) && placements.stream().noneMatch(p -> p.getPosition().equals(pos))) {
                    return false;
                }
            }
        } else {
            int col = positions.get(0).getCol();
            int startRow = positions.get(0).getRow();
            int endRow = positions.get(positions.size() - 1).getRow();
            
            // Check if all positions between start and end are occupied
            for (int row = startRow; row <= endRow; row++) {
                Position pos = new Position(row, col);
                if (board.isEmpty(pos) && placements.stream().noneMatch(p -> p.getPosition().equals(pos))) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    private boolean coversCenter(List<TilePlacement> placements) {
        Position center = new Position(CENTER_ROW, CENTER_COL);
        return placements.stream()
            .anyMatch(p -> p.getPosition().equals(center));
    }
    
    private boolean connectsToExistingTiles(List<TilePlacement> placements, Board board) {
        for (TilePlacement placement : placements) {
            Position pos = placement.getPosition();
            List<Position> adjacent = board.getAdjacentPositions(pos);
            
            // Check if any adjacent position has an existing tile
            for (Position adjPos : adjacent) {
                if (board.isOccupied(adjPos)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private List<Word> findFormedWords(Board board, List<TilePlacement> placements) {
        List<Word> words = new ArrayList<>();
        Set<Position> newPositions = placements.stream()
            .map(TilePlacement::getPosition)
            .collect(Collectors.toSet());
        
        // Find the main word (containing all new tiles)
        Word mainWord = findMainWord(board, placements);
        if (mainWord != null && mainWord.getText().length() > 1) {
            words.add(mainWord);
        }
        
        // Find perpendicular words formed by each new tile
        for (TilePlacement placement : placements) {
            Word perpWord = findPerpendicularWord(board, placement.getPosition(), newPositions);
            if (perpWord != null && perpWord.getText().length() > 1) {
                words.add(perpWord);
            }
        }
        
        return words;
    }
    
    private Word findMainWord(Board board, List<TilePlacement> placements) {
        if (placements.isEmpty()) return null;
        
        // Determine if the word is horizontal or vertical
        boolean isHorizontal = placements.stream()
            .map(p -> p.getPosition().getRow())
            .distinct()
            .count() == 1;
        
        List<Position> sortedPositions = placements.stream()
            .map(TilePlacement::getPosition)
            .sorted((p1, p2) -> isHorizontal ? 
                Integer.compare(p1.getCol(), p2.getCol()) :
                Integer.compare(p1.getRow(), p2.getRow()))
            .collect(Collectors.toList());
        
        Position start = sortedPositions.get(0);
        Position end = sortedPositions.get(sortedPositions.size() - 1);
        
        // Extend to include existing tiles
        if (isHorizontal) {
            int row = start.getRow();
            
            // Extend left
            while (start.getCol() > 0) {
                Position left = new Position(row, start.getCol() - 1);
                if (board.isOccupied(left)) {
                    start = left;
                } else {
                    break;
                }
            }
            
            // Extend right
            while (end.getCol() < Board.SIZE - 1) {
                Position right = new Position(row, end.getCol() + 1);
                if (board.isOccupied(right)) {
                    end = right;
                } else {
                    break;
                }
            }
            
            // Build word
            StringBuilder word = new StringBuilder();
            List<Position> wordPositions = new ArrayList<>();
            for (int col = start.getCol(); col <= end.getCol(); col++) {
                Position pos = new Position(row, col);
                Tile tile = board.getTile(pos);
                if (tile != null) {
                    word.append(tile.getDisplayLetter());
                    wordPositions.add(pos);
                }
            }
            
            return new Word(word.toString(), wordPositions);
            
        } else {
            int col = start.getCol();
            
            // Extend up
            while (start.getRow() > 0) {
                Position up = new Position(start.getRow() - 1, col);
                if (board.isOccupied(up)) {
                    start = up;
                } else {
                    break;
                }
            }
            
            // Extend down
            while (end.getRow() < Board.SIZE - 1) {
                Position down = new Position(end.getRow() + 1, col);
                if (board.isOccupied(down)) {
                    end = down;
                } else {
                    break;
                }
            }
            
            // Build word
            StringBuilder word = new StringBuilder();
            List<Position> wordPositions = new ArrayList<>();
            for (int row = start.getRow(); row <= end.getRow(); row++) {
                Position pos = new Position(row, col);
                Tile tile = board.getTile(pos);
                if (tile != null) {
                    word.append(tile.getDisplayLetter());
                    wordPositions.add(pos);
                }
            }
            
            return new Word(word.toString(), wordPositions);
        }
    }
    
    private Word findPerpendicularWord(Board board, Position center, Set<Position> newPositions) {
        // Try vertical word if main word is horizontal, and vice versa
        List<Position> wordPositions = new ArrayList<>();
        StringBuilder word = new StringBuilder();
        
        // Check vertical word
        Position start = center;
        Position end = center;
        
        // Extend up
        while (start.getRow() > 0) {
            Position up = new Position(start.getRow() - 1, start.getCol());
            if (board.isOccupied(up)) {
                start = up;
            } else {
                break;
            }
        }
        
        // Extend down
        while (end.getRow() < Board.SIZE - 1) {
            Position down = new Position(end.getRow() + 1, end.getCol());
            if (board.isOccupied(down)) {
                end = down;
            } else {
                break;
            }
        }
        
        // Build vertical word
        for (int row = start.getRow(); row <= end.getRow(); row++) {
            Position pos = new Position(row, center.getCol());
            Tile tile = board.getTile(pos);
            if (tile != null) {
                word.append(tile.getDisplayLetter());
                wordPositions.add(pos);
            }
        }
        
        if (word.length() > 1) {
            return new Word(word.toString(), wordPositions);
        }
        
        // Try horizontal word
        word = new StringBuilder();
        wordPositions = new ArrayList<>();
        start = center;
        end = center;
        
        // Extend left
        while (start.getCol() > 0) {
            Position left = new Position(start.getRow(), start.getCol() - 1);
            if (board.isOccupied(left)) {
                start = left;
            } else {
                break;
            }
        }
        
        // Extend right
        while (end.getCol() < Board.SIZE - 1) {
            Position right = new Position(end.getRow(), end.getCol() + 1);
            if (board.isOccupied(right)) {
                end = right;
            } else {
                break;
            }
        }
        
        // Build horizontal word
        for (int col = start.getCol(); col <= end.getCol(); col++) {
            Position pos = new Position(center.getRow(), col);
            Tile tile = board.getTile(pos);
            if (tile != null) {
                word.append(tile.getDisplayLetter());
                wordPositions.add(pos);
            }
        }
        
        return word.length() > 1 ? new Word(word.toString(), wordPositions) : null;
    }
    
    private boolean isValidWord(String word) {
        return dictionary.contains(word.toUpperCase());
    }
    
    private int calculateScore(Board board, List<TilePlacement> placements, List<Word> words) {
        int totalScore = 0;
        boolean hasWordMultiplier = false;
        
        for (Word word : words) {
            int wordScore = 0;
            int wordMultiplier = 1;
            
            for (Position pos : word.getPositions()) {
                Tile tile = board.getTile(pos);
                if (tile == null) continue;
                
                int letterScore = LETTER_SCORES.getOrDefault(tile.getDisplayLetter(), 0);
                
                // Apply premium only for newly placed tiles
                if (placements.stream().anyMatch(p -> p.getPosition().equals(pos))) {
                    Board.PremiumType premium = board.getPremium(pos);
                    if (!board.isPremiumUsed(pos)) {
                        switch (premium) {
                            case DOUBLE_LETTER:
                                letterScore *= 2;
                                break;
                            case TRIPLE_LETTER:
                                letterScore *= 3;
                                break;
                            case DOUBLE_WORD:
                            case CENTER:
                                wordMultiplier *= 2;
                                hasWordMultiplier = true;
                                break;
                            case TRIPLE_WORD:
                                wordMultiplier *= 3;
                                hasWordMultiplier = true;
                                break;
                        }
                    }
                }
                
                wordScore += letterScore;
            }
            
            totalScore += wordScore * wordMultiplier;
        }
        
        // Bonus for using all 7 tiles
        if (placements.size() == 7) {
            totalScore += 50;
            logger.debug("Bingo bonus applied: +50 points");
        }
        
        return totalScore;
    }
    
    private static Map<Character, Integer> createLetterScores() {
        Map<Character, Integer> scores = new HashMap<>();
        scores.put('A', 1); scores.put('B', 3); scores.put('C', 3); scores.put('D', 2);
        scores.put('E', 1); scores.put('F', 4); scores.put('G', 2); scores.put('H', 4);
        scores.put('I', 1); scores.put('J', 8); scores.put('K', 5); scores.put('L', 1);
        scores.put('M', 3); scores.put('N', 1); scores.put('O', 1); scores.put('P', 3);
        scores.put('Q', 10); scores.put('R', 1); scores.put('S', 1); scores.put('T', 1);
        scores.put('U', 1); scores.put('V', 4); scores.put('W', 4); scores.put('X', 8);
        scores.put('Y', 4); scores.put('Z', 10);
        return scores;
    }
    
    private Set<String> initializeDictionary() {
        // Basic dictionary for validation
        // In a real implementation, this would be loaded from a comprehensive word list
        Set<String> dict = new HashSet<>();
        
        // Add some common words for testing
        String[] commonWords = {
            "THE", "AND", "FOR", "ARE", "BUT", "NOT", "YOU", "ALL", "CAN", "HER", "WAS", "ONE", "OUR", "HAD",
            "BY", "HOT", "WORD", "WHAT", "SOME", "WE", "IT", "OF", "TO", "IN", "IS", "YOUR", "THAT", "IT",
            "HE", "WAS", "FOR", "ON", "ARE", "AS", "WITH", "HIS", "THEY", "I", "AT", "BE", "THIS", "HAVE",
            "FROM", "OR", "ONE", "HAD", "BY", "WORD", "BUT", "NOT", "WHAT", "ALL", "WERE", "WE", "WHEN",
            "YOUR", "CAN", "SAID", "THERE", "USE", "AN", "EACH", "WHICH", "SHE", "DO", "HOW", "THEIR",
            "IF", "WILL", "UP", "OTHER", "ABOUT", "OUT", "MANY", "THEN", "THEM", "THESE", "SO", "SOME",
            "HER", "WOULD", "MAKE", "LIKE", "INTO", "HIM", "TIME", "HAS", "TWO", "MORE", "GO", "NO", "WAY",
            "COULD", "MY", "THAN", "FIRST", "WATER", "BEEN", "CALL", "WHO", "ITS", "NOW", "FIND", "LONG",
            "DOWN", "DAY", "DID", "GET", "COME", "MADE", "MAY", "PART", "OVER", "NEW", "SOUND", "TAKE",
            "ONLY", "LITTLE", "WORK", "KNOW", "PLACE", "YEAR", "LIVE", "ME", "BACK", "GIVE", "MOST", "VERY",
            "AFTER", "THING", "OUR", "JUST", "NAME", "GOOD", "SENTENCE", "MAN", "THINK", "SAY", "GREAT",
            "WHERE", "HELP", "THROUGH", "MUCH", "BEFORE", "LINE", "RIGHT", "TOO", "MEAN", "OLD", "ANY",
            "SAME", "TELL", "BOY", "FOLLOW", "CAME", "WANT", "SHOW", "ALSO", "AROUND", "FARM", "THREE",
            "SMALL", "SET", "PUT", "END", "WHY", "AGAIN", "TURN", "HERE", "OFF", "WENT", "OLD", "NUMBER",
            "GREAT", "TELL", "MEN", "SAY", "SMALL", "EVERY", "FOUND", "STILL", "BETWEEN", "MANE", "SHOULD",
            "HOME", "BIG", "GIVE", "AIR", "LINE", "SET", "OWN", "UNDER", "READ", "LAST", "NEVER", "US",
            "LEFT", "END", "ALONG", "WHILE", "MIGHT", "NEXT", "SOUND", "BELOW", "SAW", "SOMETHING", "THOUGHT",
            "BOTH", "FEW", "THOSE", "ALWAYS", "LOOKED", "SHOW", "LARGE", "OFTEN", "TOGETHER", "ASKED",
            "HOUSE", "DON'T", "WORLD", "GOING", "WANT", "SCHOOL", "IMPORTANT", "UNTIL", "FORM", "FOOD",
            "KEEP", "CHILDREN", "FEET", "LAND", "SIDE", "WITHOUT", "BOY", "ONCE", "ANIMAL", "LIFE", "ENOUGH",
            "TOOK", "FOUR", "HEAD", "ABOVE", "KIND", "BEGAN", "ALMOST", "LIVE", "PAGE", "GOT", "EARTH",
            "NEED", "FAR", "HAND", "HIGH", "YEAR", "MOTHER", "LIGHT", "COUNTRY", "FATHER", "LET", "NIGHT",
            "PICTURE", "BEING", "STUDY", "SECOND", "BOOK", "CARRY", "TOOK", "SCIENCE", "EAT", "ROOM",
            "FRIEND", "BEGAN", "IDEA", "FISH", "MOUNTAIN", "NORTH", "ONCE", "BASE", "HEAR", "HORSE", "CUT",
            "SURE", "WATCH", "COLOR", "FACE", "WOOD", "MAIN", "OPEN", "SEEM", "TOGETHER", "NEXT", "WHITE",
            "CHILDREN", "BEGIN", "GOT", "WALK", "EXAMPLE", "EASE", "PAPER", "GROUP", "ALWAYS", "MUSIC",
            "THOSE", "BOTH", "MARK", "OFTEN", "LETTER", "UNTIL", "MILE", "RIVER", "CAR", "FEET", "CARE",
            "SECOND", "ENOUGH", "PLAIN", "GIRL", "USUAL", "YOUNG", "READY", "ABOVE", "EVER", "RED", "LIST",
            "THOUGH", "FEEL", "TALK", "BIRD", "SOON", "BODY", "DOG", "FAMILY", "DIRECT", "LEAVE", "SONG",
            "MEASURE", "DOOR", "PRODUCT", "BLACK", "SHORT", "NUMERAL", "CLASS", "WIND", "QUESTION", "HAPPEN",
            "COMPLETE", "SHIP", "AREA", "HALF", "ROCK", "ORDER", "FIRE", "SOUTH", "PROBLEM", "PIECE",
            "TOLD", "KNEW", "PASS", "SINCE", "TOP", "WHOLE", "KING", "SPACE", "HEARD", "BEST", "HOUR",
            "BETTER", "DURING", "HUNDRED", "FIVE", "REMEMBER", "STEP", "EARLY", "HOLD", "WEST", "GROUND",
            "INTEREST", "REACH", "FAST", "VERB", "SING", "LISTEN", "SIX", "TABLE", "TRAVEL", "LESS",
            "MORNING", "TEN", "SIMPLE", "SEVERAL", "VOWEL", "TOWARD", "WAR", "LAY", "AGAINST", "PATTERN",
            "SLOW", "CENTER", "LOVE", "PERSON", "MONEY", "SERVE", "APPEAR", "ROAD", "MAP", "RAIN", "RULE",
            "GOVERN", "PULL", "COLD", "NOTICE", "VOICE", "UNIT", "POWER", "TOWN", "FINE", "CERTAIN", "FLY",
            "FALL", "LEAD", "CRY", "DARK", "MACHINE", "NOTE", "WAIT", "PLAN", "FIGURE", "STAR", "BOX",
            "NOUN", "FIELD", "REST", "CORRECT", "ABLE", "POUND", "DONE", "BEAUTY", "DRIVE", "STOOD",
            "CONTAIN", "FRONT", "TEACH", "WEEK", "FINAL", "GAVE", "GREEN", "OH", "QUICK", "DEVELOP",
            "OCEAN", "WARM", "FREE", "MINUTE", "STRONG", "SPECIAL", "MIND", "BEHIND", "CLEAR", "TAIL",
            "PRODUCE", "FACT", "STREET", "INCH", "MULTIPLY", "NOTHING", "COURSE", "STAY", "WHEEL", "FULL",
            "FORCE", "BLUE", "OBJECT", "DECIDE", "SURFACE", "DEEP", "MOON", "ISLAND", "FOOT", "SYSTEM",
            "BUSY", "TEST", "RECORD", "BOAT", "COMMON", "GOLD", "POSSIBLE", "PLANE", "STEAD", "DRY",
            "WONDER", "LAUGH", "THOUSANDS", "AGO", "RAN", "CHECK", "GAME", "SHAPE", "EQUATE", "MISS",
            "BROUGHT", "HEAT", "SNOW", "TIRE", "BRING", "YES", "DISTANT", "FILL", "EAST", "PAINT",
            "LANGUAGE", "AMONG"
        };
        
        for (String word : commonWords) {
            dict.add(word.toUpperCase());
        }
        
        return dict;
    }
    
    /**
     * Represents a word formed on the board
     */
    public static class Word {
        private final String text;
        private final List<Position> positions;
        
        public Word(String text, List<Position> positions) {
            this.text = text;
            this.positions = new ArrayList<>(positions);
        }
        
        public String getText() { return text; }
        public List<Position> getPositions() { return new ArrayList<>(positions); }
        
        @Override
        public String toString() {
            return text;
        }
    }
}