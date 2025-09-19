package com.scrabble.common.service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * Dictionary service for validating Scrabble words.
 * Loads words from the CSW24.txt file.
 */
public class Dictionary {
    private final Set<String> validWords;
    private final Map<String, String> wordDefinitions;
    private final boolean loaded;
    
    /**
     * Creates a new Dictionary instance
     */
    public Dictionary() {
        this.validWords = new HashSet<>();
        this.wordDefinitions = new HashMap<>();
        this.loaded = loadDictionary();
    }
    
    /**
     * Creates a Dictionary with a custom word set (for testing)
     */
    public Dictionary(Set<String> words) {
        this.validWords = new HashSet<>(words);
        this.wordDefinitions = new HashMap<>();
        this.loaded = true;
    }
    
    /**
     * Loads the dictionary from CSW24.txt file
     */
    private boolean loadDictionary() {
        try {
            // Try to find CSW24.txt in various locations
            Path dictionaryPath = findDictionaryFile();
            if (dictionaryPath == null) {
                System.err.println("CSW24.txt dictionary file not found");
                return false;
            }
            
            System.out.println("Loading dictionary from: " + dictionaryPath);
            
            try (Stream<String> lines = Files.lines(dictionaryPath)) {
                lines.forEach(this::parseDictionaryLine);
            }
            
            System.out.println("Dictionary loaded: " + validWords.size() + " words");
            return true;
            
        } catch (IOException e) {
            System.err.println("Error loading dictionary: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Attempts to find the CSW24.txt file in various locations
     */
    private Path findDictionaryFile() {
        // Try current directory first
        Path currentDir = Paths.get("CSW24.txt");
        if (Files.exists(currentDir)) {
            return currentDir;
        }
        
        // Try parent directory
        Path parentDir = Paths.get("../CSW24.txt");
        if (Files.exists(parentDir)) {
            return parentDir;
        }
        
        // Try resources directory
        Path resourcesDir = Paths.get("src/main/resources/CSW24.txt");
        if (Files.exists(resourcesDir)) {
            return resourcesDir;
        }
        
        // Try common-module resources
        Path commonResources = Paths.get("scrabble-common/src/main/resources/CSW24.txt");
        if (Files.exists(commonResources)) {
            return commonResources;
        }
        
        // Try to load from classpath
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("CSW24.txt")) {
            if (is != null) {
                // Create temporary file
                Path tempFile = Files.createTempFile("CSW24", ".txt");
                Files.copy(is, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                tempFile.toFile().deleteOnExit();
                return tempFile;
            }
        } catch (IOException e) {
            // Could not load dictionary from classpath - this is expected
        }
        
        return null;
    }
    
    /**
     * Parses a single line from the dictionary file
     * Format: "WORD description [part of speech]"
     */
    private void parseDictionaryLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return;
        }
        
        String trimmedLine = line.trim();
        String[] parts = trimmedLine.split("\\s+", 2);
        
        if (parts.length > 0) {
            String word = parts[0].toUpperCase().trim();
            if (isValidWordFormat(word)) {
                validWords.add(word);
                
                // Store definition if available
                if (parts.length > 1) {
                    wordDefinitions.put(word, parts[1].trim());
                }
            }
        }
    }
    
    /**
     * Validates the format of a word (only letters, 2-15 characters)
     */
    private boolean isValidWordFormat(String word) {
        if (word == null || word.length() < 2 || word.length() > 15) {
            return false;
        }
        
        return word.chars().allMatch(Character::isLetter);
    }
    
    /**
     * Checks if a word is valid in the dictionary
     */
    public boolean isValidWord(String word) {
        if (!loaded || word == null || word.trim().isEmpty()) {
            return false;
        }
        
        String upperWord = word.toUpperCase().trim();
        return validWords.contains(upperWord);
    }
    
    /**
     * Validates multiple words at once
     */
    public boolean areValidWords(Collection<String> words) {
        if (!loaded || words == null || words.isEmpty()) {
            return false;
        }
        
        return words.stream().allMatch(this::isValidWord);
    }
    
    /**
     * Gets the definition of a word (if available)
     */
    public String getDefinition(String word) {
        if (!loaded || word == null) {
            return null;
        }
        
        String upperWord = word.toUpperCase().trim();
        return wordDefinitions.get(upperWord);
    }
    
    /**
     * Finds all valid words that can be formed from the given letters
     * This is useful for AI players to find possible words
     */
    public Set<String> findWordsFromLetters(Collection<Character> availableLetters) {
        if (!loaded || availableLetters == null || availableLetters.isEmpty()) {
            return new HashSet<>();
        }
        
        Set<String> possibleWords = new HashSet<>();
        Map<Character, Integer> letterCounts = new HashMap<>();
        
        // Count available letters
        for (char letter : availableLetters) {
            letterCounts.merge(Character.toUpperCase(letter), 1, Integer::sum);
        }
        
        // Check each word in dictionary
        for (String word : validWords) {
            if (canFormWord(word, letterCounts)) {
                possibleWords.add(word);
            }
        }
        
        return possibleWords;
    }
    
    /**
     * Checks if a word can be formed from available letters
     */
    private boolean canFormWord(String word, Map<Character, Integer> availableLetters) {
        Map<Character, Integer> needed = new HashMap<>();
        
        // Count letters needed for the word
        for (char c : word.toCharArray()) {
            needed.merge(c, 1, Integer::sum);
        }
        
        // Check if we have enough of each letter
        for (Map.Entry<Character, Integer> entry : needed.entrySet()) {
            char letter = entry.getKey();
            int requiredCount = entry.getValue();
            int availableCount = availableLetters.getOrDefault(letter, 0);
            
            if (availableCount < requiredCount) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Gets words that start with a specific prefix
     */
    public Set<String> getWordsWithPrefix(String prefix) {
        if (!loaded || prefix == null || prefix.trim().isEmpty()) {
            return new HashSet<>();
        }
        
        String upperPrefix = prefix.toUpperCase().trim();
        return validWords.stream()
                         .filter(word -> word.startsWith(upperPrefix))
                         .collect(HashSet::new, HashSet::add, HashSet::addAll);
    }
    
    /**
     * Gets words of a specific length
     */
    public Set<String> getWordsByLength(int length) {
        if (!loaded || length < 2 || length > 15) {
            return new HashSet<>();
        }
        
        return validWords.stream()
                         .filter(word -> word.length() == length)
                         .collect(HashSet::new, HashSet::add, HashSet::addAll);
    }
    
    /**
     * Returns the number of words in the dictionary
     */
    public int size() {
        return validWords.size();
    }
    
    /**
     * Checks if the dictionary was successfully loaded
     */
    public boolean isLoaded() {
        return loaded;
    }
    
    /**
     * Gets all valid words (defensive copy)
     */
    public Set<String> getAllWords() {
        return new HashSet<>(validWords);
    }
    
    /**
     * Searches for words containing specific letters
     */
    public Set<String> searchWords(String pattern) {
        if (!loaded || pattern == null || pattern.trim().isEmpty()) {
            return new HashSet<>();
        }
        
        String upperPattern = pattern.toUpperCase().trim();
        return validWords.stream()
                         .filter(word -> word.contains(upperPattern))
                         .collect(HashSet::new, HashSet::add, HashSet::addAll);
    }
    
    @Override
    public String toString() {
        return String.format("Dictionary(loaded=%s, words=%d)", loaded, validWords.size());
    }
}