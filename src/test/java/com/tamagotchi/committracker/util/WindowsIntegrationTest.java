package com.tamagotchi.committracker.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Windows integration functionality.
 * Tests Windows-specific features like theme detection and credential storage.
 */
class WindowsIntegrationTest {

    @Test
    void testIsWindows() {
        // Test should work on any platform
        boolean isWindows = WindowsIntegration.isWindows();
        
        // On Windows, should return true
        // On other platforms, should return false
        String osName = System.getProperty("os.name").toLowerCase();
        boolean expectedWindows = osName.contains("windows");
        
        assertEquals(expectedWindows, isWindows);
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testDetectWindowsTheme() {
        // This test only runs on Windows
        WindowsIntegration.WindowsTheme theme = WindowsIntegration.detectWindowsTheme();
        
        // Should return a valid theme (not null)
        assertNotNull(theme);
        
        // Should be one of the expected values
        assertTrue(theme == WindowsIntegration.WindowsTheme.LIGHT || 
                  theme == WindowsIntegration.WindowsTheme.DARK || 
                  theme == WindowsIntegration.WindowsTheme.HIGH_CONTRAST);
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testCredentialStorageFallback() {
        // Test fallback credential storage (uses Java Preferences)
        String testKey = "test_repo_url";
        String testValue = "test_username";
        
        // Store credential
        WindowsIntegration.storeCredentialFallback(testKey, testValue);
        
        // Retrieve credential
        String retrieved = WindowsIntegration.retrieveCredentialFallback(testKey);
        
        assertEquals(testValue, retrieved);
        
        // Clean up - store null to remove
        WindowsIntegration.storeCredentialFallback(testKey, null);
    }

    @Test
    void testGetCurrentTheme() {
        // Should return a valid theme
        WindowsIntegration.WindowsTheme currentTheme = WindowsIntegration.getCurrentTheme();
        assertNotNull(currentTheme);
    }

    @Test
    void testCredentialStorageOnNonWindows() {
        // On non-Windows platforms, credential storage should handle gracefully
        if (!WindowsIntegration.isWindows()) {
            boolean result = WindowsIntegration.storeGitCredentials("test_url", "test_user", "test_token");
            assertFalse(result); // Should return false on non-Windows
            
            String[] credentials = WindowsIntegration.retrieveGitCredentials("test_url");
            assertNull(credentials); // Should return null on non-Windows
        }
    }
}