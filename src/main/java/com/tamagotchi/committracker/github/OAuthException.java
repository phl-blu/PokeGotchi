package com.tamagotchi.committracker.github;

/**
 * Exception thrown when OAuth operations fail.
 * 
 * Requirements: 1.5, 8.1
 */
public class OAuthException extends RuntimeException {
    
    public OAuthException(String message) {
        super(message);
    }
    
    public OAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
