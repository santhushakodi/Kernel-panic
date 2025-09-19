package com.scrabble.client.validation;

import com.scrabble.client.validation.MoveValidator.Word;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of move validation containing validity status, score, formed words, and error messages.
 */
public class MoveValidationResult {
    private final boolean valid;
    private final int score;
    private final List<Word> formedWords;
    private final String errorMessage;

    private MoveValidationResult(boolean valid, int score, List<Word> formedWords, String errorMessage) {
        this.valid = valid;
        this.score = score;
        this.formedWords = formedWords != null ? new ArrayList<>(formedWords) : new ArrayList<>();
        this.errorMessage = errorMessage;
    }

    /**
     * Creates a successful validation result
     */
    public static MoveValidationResult valid(int score, List<Word> formedWords) {
        return new MoveValidationResult(true, score, formedWords, null);
    }

    /**
     * Creates a failed validation result
     */
    public static MoveValidationResult invalid(String errorMessage) {
        return new MoveValidationResult(false, 0, null, errorMessage);
    }

    /**
     * @return true if the move is valid
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * @return the calculated score for the move (0 if invalid)
     */
    public int getScore() {
        return score;
    }

    /**
     * @return list of words formed by the move
     */
    public List<Word> getFormedWords() {
        return new ArrayList<>(formedWords);
    }

    /**
     * @return error message if invalid, null if valid
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        if (valid) {
            return String.format("Valid move: score=%d, words=%s", score, formedWords);
        } else {
            return String.format("Invalid move: %s", errorMessage);
        }
    }
}