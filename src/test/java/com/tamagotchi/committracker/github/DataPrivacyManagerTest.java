package com.tamagotchi.committracker.github;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DataPrivacyManager.
 * 
 * Tests Requirements: 9.1, 9.2, 9.3
 */
class DataPrivacyManagerTest {
    
    @TempDir
    Path tempDir;
    
    private DataPrivacyManager privacyManager;
    private MockSecureTokenStorage tokenStorage;
    private OfflineCacheManager cacheManager;
    
    @BeforeEach
    void setUp() {
        tokenStorage = new MockSecureTokenStorage();
        cacheManager = new OfflineCacheManager(tempDir);
        privacyManager = new DataPrivacyManager(tokenStorage, cacheManager, tempDir);
    }
    
    // ==================== Data Minimization Tests ====================
    
    @Test
    void testValidateCommitMetadata_ValidCommit() {
        // Requirement 9.2: Store only metadata (hash, message, timestamp) not code content
        GitHubCommit commit = new GitHubCommit(
            "abc123",
            "Test commit",
            "John Doe",
            "john@example.com",
            Instant.now(),
            "owner/repo",
            "https://github.com/owner/repo/commit/abc123"
        );
        
        assertTrue(privacyManager.validateCommitMetadata(commit));
    }
    
    @Test
    void testValidateCommitMetadata_NullCommit() {
        assertFalse(privacyManager.validateCommitMetadata(null));
    }
    
    @Test
    void testSanitizeCommitMessage_RemovesTokens() {
        String message = "Fix auth with token=ghp_1234567890abcdefghijklmnopqrstuvwxyz";
        String sanitized = privacyManager.sanitizeCommitMessage(message, 200);
        
        assertFalse(sanitized.contains("ghp_"));
        assertTrue(sanitized.contains("[REDACTED]"));
    }
    
    @Test
    void testSanitizeCommitMessage_RemovesApiKeys() {
        String message = "Update config with api_key=secret123456";
        String sanitized = privacyManager.sanitizeCommitMessage(message, 200);
        
        assertFalse(sanitized.contains("secret123456"));
        assertTrue(sanitized.contains("[REDACTED]"));
    }
    
    @Test
    void testSanitizeCommitMessage_RemovesUrlCredentials() {
        String message = "Clone from https://user:pass@github.com/repo";
        String sanitized = privacyManager.sanitizeCommitMessage(message, 200);
        
        assertFalse(sanitized.contains("user:pass"));
        assertTrue(sanitized.contains("[REDACTED]"));
    }
    
    @Test
    void testSanitizeCommitMessage_TruncatesLongMessages() {
        String longMessage = "A".repeat(300);
        String sanitized = privacyManager.sanitizeCommitMessage(longMessage, 100);
        
        assertEquals(100, sanitized.length());
        assertTrue(sanitized.endsWith("..."));
    }
    
    @Test
    void testSanitizeCommitMessage_HandlesNull() {
        String sanitized = privacyManager.sanitizeCommitMessage(null, 100);
        assertEquals("", sanitized);
    }
    
    // ==================== OAuth Scope Tests ====================
    
    @Test
    void testGetMinimumOAuthScopes() {
        // Requirement 9.1: Only request minimum necessary OAuth scopes
        String[] scopes = privacyManager.getMinimumOAuthScopes();
        
        assertNotNull(scopes);
        assertEquals(2, scopes.length);
        assertTrue(containsScope(scopes, "repo"));
        assertTrue(containsScope(scopes, "read:user"));
    }
    
    @Test
    void testValidateMinimalScopes_ValidScopes() {
        assertTrue(privacyManager.validateMinimalScopes("repo read:user"));
        assertTrue(privacyManager.validateMinimalScopes("read:user repo"));
        assertTrue(privacyManager.validateMinimalScopes("repo"));
        assertTrue(privacyManager.validateMinimalScopes("read:user"));
    }
    
    @Test
    void testValidateMinimalScopes_ExtraScopes() {
        assertFalse(privacyManager.validateMinimalScopes("repo read:user admin:org"));
        assertFalse(privacyManager.validateMinimalScopes("write:packages"));
    }
    
    @Test
    void testValidateMinimalScopes_NullOrEmpty() {
        assertFalse(privacyManager.validateMinimalScopes(null));
        assertFalse(privacyManager.validateMinimalScopes(""));
        assertFalse(privacyManager.validateMinimalScopes("   "));
    }
    
    // ==================== Secure Data Deletion Tests ====================
    
    @Test
    void testSecurelyDeleteAllUserData() throws IOException {
        // Requirement 9.3: Securely delete all stored user data on request
        
        // Set up some data
        tokenStorage.storeAccessToken("test_token");
        
        // Create a test file in data directory
        Path testFile = tempDir.resolve("test.dat");
        Files.writeString(testFile, "test data");
        
        assertTrue(tokenStorage.hasStoredTokens());
        assertTrue(Files.exists(testFile));
        
        // Delete all data
        boolean success = privacyManager.securelyDeleteAllUserData();
        
        assertTrue(success);
        assertFalse(tokenStorage.hasStoredTokens());
    }
    
    @Test
    void testSecurelyDeleteFile() throws IOException {
        Path testFile = tempDir.resolve("secret.txt");
        Files.writeString(testFile, "sensitive data");
        
        assertTrue(Files.exists(testFile));
        
        privacyManager.securelyDeleteFile(testFile);
        
        assertFalse(Files.exists(testFile));
    }
    
    @Test
    void testSecurelyDeleteFile_NonExistent() throws IOException {
        Path nonExistent = tempDir.resolve("nonexistent.txt");
        
        // Should not throw exception
        assertDoesNotThrow(() -> privacyManager.securelyDeleteFile(nonExistent));
    }
    
    @Test
    void testSecurelyDeleteFile_ThrowsOnDirectory() throws IOException {
        Path dir = tempDir.resolve("testdir");
        Files.createDirectory(dir);
        
        assertThrows(IllegalArgumentException.class, 
            () -> privacyManager.securelyDeleteFile(dir));
    }
    
    @Test
    void testSecurelyDeleteDirectory() throws IOException {
        Path dir = tempDir.resolve("testdir");
        Files.createDirectory(dir);
        
        Path file1 = dir.resolve("file1.txt");
        Path file2 = dir.resolve("file2.txt");
        Files.writeString(file1, "data1");
        Files.writeString(file2, "data2");
        
        assertTrue(Files.exists(dir));
        assertTrue(Files.exists(file1));
        assertTrue(Files.exists(file2));
        
        privacyManager.securelyDeleteDirectory(dir);
        
        assertFalse(Files.exists(dir));
        assertFalse(Files.exists(file1));
        assertFalse(Files.exists(file2));
    }
    
    @Test
    void testSecurelyDeleteDirectory_NonExistent() throws IOException {
        Path nonExistent = tempDir.resolve("nonexistent");
        
        // Should not throw exception
        assertDoesNotThrow(() -> privacyManager.securelyDeleteDirectory(nonExistent));
    }
    
    // ==================== Data Summary Tests ====================
    
    @Test
    void testGetStoredDataSummary_NoData() {
        var summary = privacyManager.getStoredDataSummary();
        
        assertNotNull(summary);
        assertFalse(summary.hasAuthTokens());
        assertEquals(0, summary.cachedCommitCount());
        assertEquals(0, summary.cachedRepositoryCount());
        assertFalse(summary.hasAnyData());
    }
    
    @Test
    void testGetStoredDataSummary_WithData() {
        tokenStorage.storeAccessToken("test_token");
        
        var summary = privacyManager.getStoredDataSummary();
        
        assertNotNull(summary);
        assertTrue(summary.hasAuthTokens());
        assertTrue(summary.hasAnyData());
    }
    
    // ==================== Helper Methods ====================
    
    private boolean containsScope(String[] scopes, String scope) {
        for (String s : scopes) {
            if (s.equals(scope)) {
                return true;
            }
        }
        return false;
    }
    
    // ==================== Mock Classes ====================
    
    /**
     * Mock implementation of SecureTokenStorage for testing.
     */
    private static class MockSecureTokenStorage implements SecureTokenStorage {
        
        private String accessToken;
        private String refreshToken;
        
        @Override
        public void storeAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }
        
        @Override
        public void storeRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
        
        @Override
        public Optional<String> getAccessToken() {
            return Optional.ofNullable(accessToken);
        }
        
        @Override
        public Optional<String> getRefreshToken() {
            return Optional.ofNullable(refreshToken);
        }
        
        @Override
        public void clearAllTokens() {
            accessToken = null;
            refreshToken = null;
        }
        
        @Override
        public void rotateEncryptionKey() {
            // No-op for mock
        }
        
        @Override
        public boolean hasStoredTokens() {
            return accessToken != null || refreshToken != null;
        }
    }
}
