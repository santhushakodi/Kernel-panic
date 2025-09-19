package com.scrabble.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import com.scrabble.common.service.Dictionary;

/**
 * Main Spring Boot application for the Scrabble server.
 */
@SpringBootApplication
public class ScrabbleServerApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ScrabbleServerApplication.class, args);
    }
    
    /**
     * Creates a Dictionary bean for the application
     */
    @Bean
    public Dictionary dictionary() {
        return new Dictionary();
    }
}