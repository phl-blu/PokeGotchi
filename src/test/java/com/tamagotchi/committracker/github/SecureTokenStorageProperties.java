package com.tamagotchi.committracker.github;

import net.jqwik.api.*;
import net.jqwik.api.constraints.StringLength;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for SecureTokenStorage encryption.
 * 
 * These tests validate the correctness properties defined in the design document
 * for secure token storage with AES-256-GCM encryption.
 */
class SecureTokenStorageProperties {

    /**
     * **Feature: github-api-integration, Property 1: Token Encryption Round Trip**
     * **Validates: Requirements 2.1, 2.3**
     * 
     * For any valid access token, encrypting and then decrypting the token 
     * SHALL produce the original token value.
     * 
     * This property tests that the encryption/decryption cycle is lossless
     * for any arbitrary token string.
     */
    @Property(tries = 100)
    void tokenEncryptionRoundTrip(
            @ForAll("validTokens") String originalToken) throws IOException {
        
        // Create a fresh storage instance with a temp directory for each test
        Path tempDir = Files.createTempDirectory("token-roundtrip-test");
        
        try {
            SecureTokenStorageImpl storage = new SecureTokenStorageImpl(tempDir);
            
            // Store the token (encrypts it)
            storage.storeAccessToken(originalToken);
            
            // Retrieve the token (decrypts it)
            Optional<String> retrievedToken = storage.getAccessToken();
            
            // Verify round-trip: decrypted token must equal original
            assertTrue(retrievedToken.isPresent(),
                    "Token should be retrievable after storage");
            assertEquals(originalToken, retrievedToken.get(),
                    String.format("Round-trip failed: stored '%s' but retrieved '%s'",
                            truncateForDisplay(originalToken), 
                            truncateForDisplay(retrievedToken.get())));
            
        } finally {
            // Clean up temp directory
            cleanupTempDir(tempDir);
        }
    }

    /**
     * **Feature: github-api-integration, Property 1: Token Encryption Round Trip (Refresh Token)**
     * **Validates: Requirements 2.1, 2.3**
     * 
     * For any valid refresh token, encrypting and then decrypting the token 
     * SHALL produce the original token value.
     */
    @Property(tries = 100)
    void refreshTokenEncryptionRoundTrip(
            @ForAll("validTokens") String originalToken) throws IOException {
        
        Path tempDir = Files.createTempDirectory("refresh-token-roundtrip-test");
        
        try {
            SecureTokenStorageImpl storage = new SecureTokenStorageImpl(tempDir);
            
            // Store the refresh token (encrypts it)
            storage.storeRefreshToken(originalToken);
            
            // Retrieve the refresh token (decrypts it)
            Optional<String> retrievedToken = storage.getRefreshToken();
            
            // Verify round-trip
            assertTrue(retrievedToken.isPresent(),
                    "Refresh token should be retrievable after storage");
            assertEquals(originalToken, retrievedToken.get(),
                    "Refresh token round-trip should preserve original value");
            
        } finally {
            cleanupTempDir(tempDir);
        }
    }

    /**
     * **Feature: github-api-integration, Property 1: Token Encryption Round Trip (Both Tokens)**
     * **Validates: Requirements 2.1, 2.3**
     * 
     * For any pair of valid tokens, storing both and retrieving them
     * SHALL produce the original values for both tokens independently.
     */
    @Property(tries = 100)
    void bothTokensEncryptionRoundTrip(
            @ForAll("validTokens") String accessToken,
            @ForAll("validTokens") String refreshToken) throws IOException {
        
        Path tempDir = Files.createTempDirectory("both-tokens-roundtrip-test");
        
        try {
            SecureTokenStorageImpl storage = new SecureTokenStorageImpl(tempDir);
            
            // Store both tokens
            storage.storeAccessToken(accessToken);
            storage.storeRefreshToken(refreshToken);
            
            // Retrieve both tokens
            Optional<String> retrievedAccess = storage.getAccessToken();
            Optional<String> retrievedRefresh = storage.getRefreshToken();
            
            // Verify both round-trips
            assertTrue(retrievedAccess.isPresent(), "Access token should be retrievable");
            assertTrue(retrievedRefresh.isPresent(), "Refresh token should be retrievable");
            assertEquals(accessToken, retrievedAccess.get(),
                    "Access token round-trip should preserve original value");
            assertEquals(refreshToken, retrievedRefresh.get(),
                    "Refresh token round-trip should preserve original value");
            
        } finally {
            cleanupTempDir(tempDir);
        }
    }

    /**
     * **Feature: github-api-integration, Property 1: Token Encryption Round Trip (Persistence)**
     * **Validates: Requirements 2.1, 2.3**
     * 
     * For any valid token, storing it and then creating a new storage instance
     * with the same directory SHALL retrieve the original token value.
     * This tests that encryption is deterministic based on machine identity.
     */
    @Property(tries = 50)
    void tokenPersistenceAcrossInstances(
            @ForAll("validTokens") String originalToken) throws IOException {
        
        Path tempDir = Files.createTempDirectory("token-persistence-test");
        
        try {
            // First instance: store the token
            SecureTokenStorageImpl storage1 = new SecureTokenStorageImpl(tempDir);
            storage1.storeAccessToken(originalToken);
            
            // Second instance: retrieve the token (simulates app restart)
            SecureTokenStorageImpl storage2 = new SecureTokenStorageImpl(tempDir);
            Optional<String> retrievedToken = storage2.getAccessToken();
            
            // Verify persistence
            assertTrue(retrievedToken.isPresent(),
                    "Token should be retrievable from new storage instance");
            assertEquals(originalToken, retrievedToken.get(),
                    "Token should be preserved across storage instances");
            
        } finally {
            cleanupTempDir(tempDir);
        }
    }

    /**
     * **Feature: github-api-integration, Property 1: Token Encryption Round Trip (Key Rotation)**
     * **Validates: Requirements 2.1, 2.3, 2.4**
     * 
     * For any valid token, rotating the encryption key SHALL preserve
     * the ability to retrieve the original token value.
     */
    @Property(tries = 50)
    void tokenPreservedAfterKeyRotation(
            @ForAll("validTokens") String originalToken) throws IOException {
        
        Path tempDir = Files.createTempDirectory("key-rotation-test");
        
        try {
            SecureTokenStorageImpl storage = new SecureTokenStorageImpl(tempDir);
            
            // Store the token
            storage.storeAccessToken(originalToken);
            
            // Rotate the encryption key
            storage.rotateEncryptionKey();
            
            // Retrieve the token after rotation
            Optional<String> retrievedToken = storage.getAccessToken();
            
            // Verify round-trip after key rotation
            assertTrue(retrievedToken.isPresent(),
                    "Token should be retrievable after key rotation");
            assertEquals(originalToken, retrievedToken.get(),
                    "Token value should be preserved after key rotation");
            
        } finally {
            cleanupTempDir(tempDir);
        }
    }

    /**
     * Provides valid token strings for property testing.
     * Generates a variety of token formats including:
     * - GitHub-style tokens (ghp_, ghr_ prefixes)
     * - Tokens with special characters
     * - Tokens of various lengths
     * - Unicode tokens
     */
    @Provide
    Arbitrary<String> validTokens() {
        return Arbitraries.oneOf(
                // GitHub-style access tokens
                Arbitraries.strings()
                        .alpha().numeric()
                        .ofMinLength(10).ofMaxLength(50)
                        .map(s -> "ghp_" + s),
                
                // GitHub-style refresh tokens
                Arbitraries.strings()
                        .alpha().numeric()
                        .ofMinLength(10).ofMaxLength(50)
                        .map(s -> "ghr_" + s),
                
                // Tokens with special characters (common in OAuth tokens)
                Arbitraries.strings()
                        .withChars("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.")
                        .ofMinLength(20).ofMaxLength(100),
                
                // Short tokens
                Arbitraries.strings()
                        .alpha().numeric()
                        .ofMinLength(1).ofMaxLength(10),
                
                // Long tokens
                Arbitraries.strings()
                        .alpha().numeric()
                        .ofMinLength(100).ofMaxLength(500),
                
                // Tokens with various ASCII printable characters
                Arbitraries.strings()
                        .withChars("!@#$%^&*()_+-=[]{}|;':\",./<>?ABCabc123")
                        .ofMinLength(10).ofMaxLength(50)
        );
    }

    /**
     * Truncates a string for display in error messages.
     */
    private String truncateForDisplay(String s) {
        if (s == null) return "null";
        if (s.length() <= 20) return s;
        return s.substring(0, 17) + "...";
    }

    /**
     * Cleans up a temporary directory and all its contents.
     */
    private void cleanupTempDir(Path tempDir) {
        try {
            if (Files.exists(tempDir)) {
                Files.walk(tempDir)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                // Ignore cleanup errors
                            }
                        });
            }
        } catch (IOException e) {
            // Ignore cleanup errors
        }
    }
}
