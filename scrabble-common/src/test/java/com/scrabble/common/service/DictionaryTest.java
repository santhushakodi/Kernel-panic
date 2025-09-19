package com.scrabble.common.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

class DictionaryTest {
    
    private Dictionary dictionary;
    private Dictionary testDictionary;
    
    @BeforeEach
    void setUp() {
        // Create a test dictionary with known words
        Set<String> testWords = Set.of(
            "CAT", "DOG", "BIRD", "FISH", "HORSE",
            "RUN", "WALK", "JUMP", "FLY", "SWIM",
            "RED", "BLUE", "GREEN", "YELLOW", "BLACK",
            "ONE", "TWO", "THREE", "FOUR", "FIVE"
        );
        testDictionary = new Dictionary(testWords);
        
        // Also create a dictionary that loads from file
        dictionary = new Dictionary();
    }
    
    @Test
    @DisplayName("Test dictionary loads words correctly")
    void testDictionaryLoading() {
        assertTrue(testDictionary.isLoaded());
        assertEquals(20, testDictionary.size());
    }
    
    @Test
    @DisplayName("Valid word detection works")
    void testValidWordDetection() {
        assertTrue(testDictionary.isValidWord("CAT"));
        assertTrue(testDictionary.isValidWord("cat")); // Case insensitive
        assertTrue(testDictionary.isValidWord("Dog"));
        assertFalse(testDictionary.isValidWord("ELEPHANT"));
        assertFalse(testDictionary.isValidWord(""));
        assertFalse(testDictionary.isValidWord(null));
    }
    
    @Test
    @DisplayName("Multiple word validation works")
    void testMultipleWordValidation() {
        List<String> validWords = Arrays.asList("CAT", "DOG", "RUN");
        List<String> mixedWords = Arrays.asList("CAT", "ELEPHANT", "DOG");
        List<String> invalidWords = Arrays.asList("ELEPHANT", "GIRAFFE");
        
        assertTrue(testDictionary.areValidWords(validWords));
        assertFalse(testDictionary.areValidWords(mixedWords));
        assertFalse(testDictionary.areValidWords(invalidWords));
        assertFalse(testDictionary.areValidWords(Collections.emptyList()));
    }
    
    @Test
    @DisplayName("Find words from letters works")
    void testFindWordsFromLetters() {
        List<Character> letters = Arrays.asList('C', 'A', 'T', 'S');
        Set<String> foundWords = testDictionary.findWordsFromLetters(letters);
        
        assertTrue(foundWords.contains("CAT"));
        assertFalse(foundWords.contains("DOG")); // Requires letters not in the list
    }
    
    @Test
    @DisplayName("Get words with prefix works")
    void testGetWordsWithPrefix() {
        Set<String> redWords = testDictionary.getWordsWithPrefix("R");
        assertTrue(redWords.contains("RUN"));
        assertTrue(redWords.contains("RED"));
        assertFalse(redWords.contains("CAT"));
        
        Set<String> threeWords = testDictionary.getWordsWithPrefix("THR");
        assertTrue(threeWords.contains("THREE"));
        assertFalse(threeWords.contains("TWO"));
    }
    
    @Test
    @DisplayName("Get words by length works")
    void testGetWordsByLength() {
        Set<String> threeLetterWords = testDictionary.getWordsByLength(3);
        assertTrue(threeLetterWords.contains("CAT"));
        assertTrue(threeLetterWords.contains("DOG"));
        assertTrue(threeLetterWords.contains("RUN"));
        assertFalse(threeLetterWords.contains("HORSE"));
        
        Set<String> fiveLetterWords = testDictionary.getWordsByLength(5);
        assertTrue(fiveLetterWords.contains("HORSE"));
        assertTrue(fiveLetterWords.contains("THREE"));
        assertFalse(fiveLetterWords.contains("CAT"));
    }
    
    @Test
    @DisplayName("Search words containing pattern works")
    void testSearchWords() {
        Set<String> wordsWithA = testDictionary.searchWords("A");
        assertTrue(wordsWithA.contains("CAT"));
        assertTrue(wordsWithA.contains("WALK"));
        assertFalse(wordsWithA.contains("DOG"));
        
        Set<String> wordsWithEE = testDictionary.searchWords("EE");
        assertTrue(wordsWithEE.contains("GREEN"));
        assertTrue(wordsWithEE.contains("THREE"));
        assertFalse(wordsWithEE.contains("RED"));
    }
    
    @Test
    @DisplayName("File-based dictionary loads if available")
    void testFileDictionaryLoading() {
        // This will succeed if CSW24.txt is available, otherwise fail gracefully
        if (dictionary.isLoaded()) {
            assertTrue(dictionary.size() > 0);
            assertTrue(dictionary.isValidWord("HELLO")); // Common word that should exist
        } else {
            assertFalse(dictionary.isLoaded());
            assertEquals(0, dictionary.size());
        }
    }
    
    @Test
    @DisplayName("Empty collections handled correctly")
    void testEmptyCollections() {
        Set<String> emptyWords = testDictionary.findWordsFromLetters(Collections.emptyList());
        assertTrue(emptyWords.isEmpty());
        
        Set<String> emptyPrefix = testDictionary.getWordsWithPrefix("");
        assertTrue(emptyPrefix.isEmpty());
        
        Set<String> emptySearch = testDictionary.searchWords("");
        assertTrue(emptySearch.isEmpty());
    }
    
    @Test
    @DisplayName("Invalid inputs handled correctly")
    void testInvalidInputs() {
        assertFalse(testDictionary.isValidWord(null));
        assertFalse(testDictionary.areValidWords(null));
        
        Set<String> nullLetters = testDictionary.findWordsFromLetters(null);
        assertTrue(nullLetters.isEmpty());
        
        Set<String> nullPrefix = testDictionary.getWordsWithPrefix(null);
        assertTrue(nullPrefix.isEmpty());
        
        Set<String> invalidLength = testDictionary.getWordsByLength(0);
        assertTrue(invalidLength.isEmpty());
        
        Set<String> nullSearch = testDictionary.searchWords(null);
        assertTrue(nullSearch.isEmpty());
    }
    
    @Test
    @DisplayName("Get all words returns defensive copy")
    void testGetAllWords() {
        Set<String> allWords = testDictionary.getAllWords();
        assertEquals(20, allWords.size());
        
        // Modifying the returned set shouldn't affect the original
        allWords.add("NEWWORD");
        assertEquals(20, testDictionary.size());
    }
    
    @Test
    @DisplayName("ToString provides useful information")
    void testToString() {
        String description = testDictionary.toString();
        assertTrue(description.contains("loaded=true"));
        assertTrue(description.contains("words=20"));
    }
}