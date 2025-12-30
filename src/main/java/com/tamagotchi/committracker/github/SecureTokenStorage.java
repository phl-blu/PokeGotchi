package com.tamagotchi.committracker.github;

import java.util.Optional;

/**
 * Interface for secure storage of authentication tokens.
 * Implementations must use AES-256-GCM encryption for token storage.
 * 
 * Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6
 */
public interface SecureTokenStorage {
    
    /**
     * Stores an access token securely with AES-256-GCM encryption.
     * @param accessToken the access token to store
     * @throws SecurityException if encryption fails
     */
    void storeAccessToken(String accessToken);
    
    /**
     * Stores a refresh token securely.
     * @param refreshToken the refresh token to store
     * @throws SecurityException if encryption fails
     */
    void storeRefreshToken(String refreshToken);
    
    /**
     * Retrieves and decrypts the stored access token.
     * @return Optional containing the access token, or empty if no token stored
     */
    Optional<String> getAccessToken();
    
    /**
     * Retrieves and decrypts the stored refresh token.
     * @return Optional containing the refresh token, or empty if no token stored
     */
    Optional<String> getRefreshToken();
    
    /**
     * Securely deletes all stored tokens.
     * Overwrites token data before deletion for security.
     */
    void clearAllTokens();
    
    /**
     * Rotates encryption keys without losing user data.
     * Re-encrypts all stored tokens with a new key.
     * @throws SecurityException if key rotation fails
     */
    void rotateEncryptionKey();
    
    /**
     * Checks if any tokens are currently stored.
     * @return true if at least one token is stored
     */
    boolean hasStoredTokens();
}
