package com.tamagotchi.committracker.github;

import org.junit.jupiter.api.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SecureTokenStorageImpl.
 * Tests token storage, retrieval, encryption round-trip, and lifecycle management.
 * 
 * Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6
 */
class SecureTokenStorageTest {
    
    private Path tempDir;
    private SecureTokenStorageImpl storage;
    
    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("secure-token-test");
        storage = new SecureTokenStorageImpl(tempDir);
    }
    
    @AfterEach
    void tearDown() throws IOException {
        // Clean up temp directory
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
    }
    
    @Test
    @DisplayName("Store and retrieve access token")
    void testStoreAndRetrieveAccessToken() {
        String token = "ghp_test_access_token_12345";
        
        storage.storeAccessToken(token);
        Optional<String> retrieved = storage.getAccessToken();
        
        assertTrue(retrieved.isPresent(), "Access token should be present");
        assertEquals(token, retrieved.get(), "Retrieved token should match stored token");
    }
    
    @Test
    @DisplayName("Store and retrieve refresh token")
    void testStoreAndRetrieveRefreshToken() {
        String token = "ghr_test_refresh_token_67890";
        
        storage.storeRefreshToken(token);
        Optional<String> retrieved = storage.getRefreshToken();
        
        assertTrue(retrieved.isPresent(), "Refresh token should be present");
        assertEquals(token, retrieved.get(), "Retrieved token should match stored token");
    }
    
    @Test
    @DisplayName("Store both tokens and retrieve independently")
    void testStoreBothTokens() {
        String accessToken = "ghp_access_token";
        String refreshToken = "ghr_refresh_token";
        
        storage.storeAccessToken(accessToken);
        storage.storeRefreshToken(refreshToken);
        
        assertEquals(accessToken, storage.getAccessToken().orElse(null));
        assertEquals(refreshToken, storage.getRefreshToken().orElse(null));
    }
    
    @Test
    @DisplayName("Clear all tokens removes both tokens")
    void testClearAllTokens() {
        storage.storeAccessToken("access_token");
        storage.storeRefreshToken("refresh_token");
        
        assertTrue(storage.hasStoredTokens(), "Should have tokens before clear");
        
        storage.clearAllTokens();
        
        assertFalse(storage.hasStoredTokens(), "Should not have tokens after clear");
        assertTrue(storage.getAccessToken().isEmpty(), "Access token should be empty");
        assertTrue(storage.getRefreshToken().isEmpty(), "Refresh token should be empty");
    }
    
    @Test
    @DisplayName("Rotate encryption key preserves tokens")
    void testRotateEncryptionKey() {
        String accessToken = "ghp_original_access";
        String refreshToken = "ghr_original_refresh";
        
        storage.storeAccessToken(accessToken);
        storage.storeRefreshToken(refreshToken);
        
        // Rotate the key
        storage.rotateEncryptionKey();
        
        // Tokens should still be retrievable
        assertEquals(accessToken, storage.getAccessToken().orElse(null));
        assertEquals(refreshToken, storage.getRefreshToken().orElse(null));
    }
    
    @Test
    @DisplayName("Empty storage returns empty optionals")
    void testEmptyStorage() {
        assertTrue(storage.getAccessToken().isEmpty());
        assertTrue(storage.getRefreshToken().isEmpty());
        assertFalse(storage.hasStoredTokens());
    }
    
    @Test
    @DisplayName("Null access token throws exception")
    void testNullAccessTokenThrows() {
        assertThrows(NullPointerException.class, () -> storage.storeAccessToken(null));
    }
    
    @Test
    @DisplayName("Null refresh token throws exception")
    void testNullRefreshTokenThrows() {
        assertThrows(NullPointerException.class, () -> storage.storeRefreshToken(null));
    }
    
    @Test
    @DisplayName("Token with special characters is preserved")
    void testSpecialCharactersInToken() {
        String token = "ghp_token!@#$%^&*()_+-=[]{}|;':\",./<>?";
        
        storage.storeAccessToken(token);
        
        assertEquals(token, storage.getAccessToken().orElse(null));
    }
    
    @Test
    @DisplayName("Long token is preserved")
    void testLongToken() {
        String token = "ghp_" + "a".repeat(1000);
        
        storage.storeAccessToken(token);
        
        assertEquals(token, storage.getAccessToken().orElse(null));
    }
    
    @Test
    @DisplayName("Overwriting token replaces previous value")
    void testOverwriteToken() {
        storage.storeAccessToken("first_token");
        storage.storeAccessToken("second_token");
        
        assertEquals("second_token", storage.getAccessToken().orElse(null));
    }
    
    @Test
    @DisplayName("Storage creates encrypted file")
    void testEncryptedFileCreated() throws IOException {
        storage.storeAccessToken("test_token");
        
        Path tokensFile = tempDir.resolve("tokens.enc");
        assertTrue(Files.exists(tokensFile), "Encrypted tokens file should exist");
        
        // Verify file content is not plaintext (read as bytes)
        byte[] fileContent = Files.readAllBytes(tokensFile);
        String contentAsString = new String(fileContent, java.nio.charset.StandardCharsets.ISO_8859_1);
        assertFalse(contentAsString.contains("test_token"), 
            "Token should not appear in plaintext in file");
    }
    
    @Test
    @DisplayName("New storage instance can read existing tokens")
    void testPersistenceAcrossInstances() {
        String token = "ghp_persistent_token";
        storage.storeAccessToken(token);
        
        // Create new instance with same directory
        SecureTokenStorageImpl newStorage = new SecureTokenStorageImpl(tempDir);
        
        assertEquals(token, newStorage.getAccessToken().orElse(null),
            "New instance should read token stored by previous instance");
    }
}
