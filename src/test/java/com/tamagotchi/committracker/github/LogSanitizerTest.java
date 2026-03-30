package com.tamagotchi.committracker.github;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LogSanitizer.
 * 
 * Tests Requirements: 9.4, 9.6
 */
class LogSanitizerTest {
    
    private boolean originalSanitizationState;
    
    @BeforeEach
    void setUp() {
        originalSanitizationState = LogSanitizer.isSanitizationEnabled();
        LogSanitizer.setSanitizationEnabled(true);
    }
    
    @AfterEach
    void tearDown() {
        LogSanitizer.setSanitizationEnabled(originalSanitizationState);
    }
    
    // ==================== Token Sanitization Tests ====================
    
    @Test
    void testSanitize_GitHubPersonalAccessToken() {
        // Requirement 9.6: Ensure no user data is included in logs
        String input = "Using token ghp_1234567890abcdefghijklmnopqrstuvwxyz for auth";
        String sanitized = LogSanitizer.sanitize(input);
        
        assertFalse(sanitized.contains("ghp_"));
        assertTrue(sanitized.contains("[TOKEN_REDACTED]"));
    }
    
    @Test
    void testSanitize_GitHubOAuthToken() {
        String input = "OAuth token: gho_abcdefghijklmnopqrstuvwxyz123456";
        String sanitized = LogSanitizer.sanitize(input);
        
        assertFalse(sanitized.contains("gho_"));
        assertTrue(sanitized.contains("[TOKEN_REDACTED]"));
    }
    
    @Test
    void testSanitize_GitHubFineGrainedToken() {
        String input = "Token github_pat_11ABCDEFG0123456789_abcdefghijklmnopqrstuvwxyz";
        String sanitized = LogSanitizer.sanitize(input);
        
        assertFalse(sanitized.contains("github_pat_"));
        assertTrue(sanitized.contains("[TOKEN_REDACTED]"));
    }
    
    @Test
    void testSanitize_BearerToken() {
        String input = "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";
        String sanitized = LogSanitizer.sanitize(input);
        
        assertFalse(sanitized.contains("Bearer eyJ"));
        assertTrue(sanitized.contains("[TOKEN_REDACTED]"));
    }
    
    @Test
    void testSanitize_GenericToken() {
        String input = "token=abc123def456ghi789jkl012mno345pqr678";
        String sanitized = LogSanitizer.sanitize(input);
        
        assertFalse(sanitized.contains("abc123def456"));
        assertTrue(sanitized.contains("[CREDENTIAL_REDACTED]"));
    }
    
    // ==================== API Key and Credential Tests ====================
    
    @Test
    void testSanitize_ApiKey() {
        String input = "api_key=sk_live_1234567890abcdef";
        String sanitized = LogSanitizer.sanitize(input);
        
        assertFalse(sanitized.contains("sk_live_"));
        assertTrue(sanitized.contains("[CREDENTIAL_REDACTED]"));
    }
    
    @Test
    void testSanitize_Secret() {
        String input = "secret: my_secret_value_12345";
        String sanitized = LogSanitizer.sanitize(input);
        
        assertFalse(sanitized.contains("my_secret_value"));
        assertTrue(sanitized.contains("[CREDENTIAL_REDACTED]"));
    }
    
    @Test
    void testSanitize_Password() {
        String input = "password=\"SuperSecret123\"";
        String sanitized = LogSanitizer.sanitize(input);
        
        assertFalse(sanitized.contains("SuperSecret123"));
        assertTrue(sanitized.contains("[CREDENTIAL_REDACTED]"));
    }
    
    @Test
    void testSanitize_UrlWithCredentials() {
        String input = "Cloning from https://username:password@github.com/repo.git";
        String sanitized = LogSanitizer.sanitize(input);
        
        assertFalse(sanitized.contains("username:password"));
        assertTrue(sanitized.contains("[URL_WITH_CREDENTIALS_REDACTED]"));
    }
    
    // ==================== External Use Sanitization Tests ====================
    
    @Test
    void testSanitizeForExternalUse_Email() {
        // Requirement 9.4: Anonymize data before any transmission
        String input = "User john.doe@example.com committed changes";
        String sanitized = LogSanitizer.sanitizeForExternalUse(input);
        
        assertFalse(sanitized.contains("john.doe@example.com"));
        assertTrue(sanitized.contains("[EMAIL_REDACTED]"));
    }
    
    @Test
    void testSanitizeForExternalUse_GitHubUsername() {
        String input = "Repository: github.com/johndoe/myrepo";
        String sanitized = LogSanitizer.sanitizeForExternalUse(input);
        
        assertFalse(sanitized.contains("johndoe"));
        assertTrue(sanitized.contains("[USER]"));
    }
    
    @Test
    void testSanitizeForExternalUse_TokenAndEmail() {
        String input = "Token ghp_abc123 for user@example.com";
        String sanitized = LogSanitizer.sanitizeForExternalUse(input);
        
        assertFalse(sanitized.contains("ghp_"));
        assertFalse(sanitized.contains("user@example.com"));
        assertTrue(sanitized.contains("[TOKEN_REDACTED]"));
        assertTrue(sanitized.contains("[EMAIL_REDACTED]"));
    }
    
    // ==================== Exception Sanitization Tests ====================
    
    @Test
    void testSanitizeException_WithToken() {
        Exception ex = new RuntimeException("Auth failed with token ghp_secret123");
        String sanitized = LogSanitizer.sanitizeException(ex);
        
        assertTrue(sanitized.contains("RuntimeException"));
        assertFalse(sanitized.contains("ghp_secret123"));
        assertTrue(sanitized.contains("[TOKEN_REDACTED]"));
    }
    
    @Test
    void testSanitizeException_WithCause() {
        Exception cause = new IllegalStateException("Invalid token ghp_abc123");
        Exception ex = new RuntimeException("Operation failed", cause);
        String sanitized = LogSanitizer.sanitizeException(ex);
        
        assertTrue(sanitized.contains("RuntimeException"));
        assertTrue(sanitized.contains("IllegalStateException"));
        assertTrue(sanitized.contains("Caused by:"));
        assertFalse(sanitized.contains("ghp_abc123"));
    }
    
    @Test
    void testSanitizeException_Null() {
        String sanitized = LogSanitizer.sanitizeException(null);
        assertEquals("", sanitized);
    }
    
    @Test
    void testSanitizeStackTrace() {
        Exception ex = new RuntimeException("Error with token ghp_test123");
        String sanitized = LogSanitizer.sanitizeStackTrace(ex);
        
        assertTrue(sanitized.contains("RuntimeException"));
        assertTrue(sanitized.contains("\tat "));
        assertFalse(sanitized.contains("ghp_test123"));
    }
    
    // ==================== Sensitive Data Detection Tests ====================
    
    @Test
    void testContainsSensitiveData_Token() {
        assertTrue(LogSanitizer.containsSensitiveData("token ghp_abc123"));
        assertTrue(LogSanitizer.containsSensitiveData("Bearer xyz123456"));
        assertTrue(LogSanitizer.containsSensitiveData("api_key=secret123"));
    }
    
    @Test
    void testContainsSensitiveData_NoSensitiveData() {
        assertFalse(LogSanitizer.containsSensitiveData("Normal log message"));
        assertFalse(LogSanitizer.containsSensitiveData("Commit abc123 pushed"));
    }
    
    @Test
    void testContainsSensitiveData_NullOrEmpty() {
        assertFalse(LogSanitizer.containsSensitiveData(null));
        assertFalse(LogSanitizer.containsSensitiveData(""));
    }
    
    @Test
    void testContainsPII_Email() {
        assertTrue(LogSanitizer.containsPII("user@example.com"));
        assertTrue(LogSanitizer.containsPII("Contact: john.doe@company.org"));
    }
    
    @Test
    void testContainsPII_NoPII() {
        assertFalse(LogSanitizer.containsPII("Normal message"));
        assertFalse(LogSanitizer.containsPII("No personal info here"));
    }
    
    // ==================== Sanitization Toggle Tests ====================
    
    @Test
    void testSanitizationEnabled() {
        LogSanitizer.setSanitizationEnabled(true);
        assertTrue(LogSanitizer.isSanitizationEnabled());
        
        String input = "token ghp_abc123";
        String sanitized = LogSanitizer.sanitize(input);
        assertFalse(sanitized.contains("ghp_"));
    }
    
    @Test
    void testSanitizationDisabled() {
        LogSanitizer.setSanitizationEnabled(false);
        assertFalse(LogSanitizer.isSanitizationEnabled());
        
        String input = "token ghp_abc123";
        String sanitized = LogSanitizer.sanitize(input);
        assertTrue(sanitized.contains("ghp_abc123")); // Not sanitized when disabled
    }
    
    // ==================== Formatter Tests ====================
    
    @Test
    void testCreateSanitizingFormatter() {
        SimpleFormatter delegate = new SimpleFormatter();
        var sanitizingFormatter = LogSanitizer.createSanitizingFormatter(delegate);
        
        assertNotNull(sanitizingFormatter);
    }
    
    @Test
    void testInstallSanitizingHandlers() {
        Logger logger = Logger.getLogger("test.logger");
        
        // Should not throw exception
        assertDoesNotThrow(() -> LogSanitizer.installSanitizingHandlers(logger));
    }
    
    @Test
    void testInstallSanitizingHandlers_NullLogger() {
        assertThrows(NullPointerException.class, 
            () -> LogSanitizer.installSanitizingHandlers(null));
    }
    
    @Test
    void testInstallGlobalSanitization() {
        // Should not throw exception
        assertDoesNotThrow(() -> LogSanitizer.installGlobalSanitization());
    }
    
    // ==================== Edge Cases ====================
    
    @Test
    void testSanitize_MultipleTokensInSameString() {
        String input = "Token1: ghp_abc123 and Token2: gho_def456";
        String sanitized = LogSanitizer.sanitize(input);
        
        assertFalse(sanitized.contains("ghp_abc123"));
        assertFalse(sanitized.contains("gho_def456"));
        assertTrue(sanitized.contains("[TOKEN_REDACTED]"));
    }
    
    @Test
    void testSanitize_MixedSensitiveData() {
        String input = "Auth with token ghp_abc123 for user@example.com using api_key=secret123";
        String sanitized = LogSanitizer.sanitizeForExternalUse(input);
        
        assertFalse(sanitized.contains("ghp_abc123"));
        assertFalse(sanitized.contains("user@example.com"));
        assertFalse(sanitized.contains("secret123"));
    }
    
    @Test
    void testSanitize_EmptyString() {
        String sanitized = LogSanitizer.sanitize("");
        assertEquals("", sanitized);
    }
    
    @Test
    void testSanitize_NullString() {
        String sanitized = LogSanitizer.sanitize(null);
        assertNull(sanitized);
    }
}
