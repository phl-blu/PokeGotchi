package com.tamagotchi.committracker.ui.widget;

import org.junit.jupiter.api.Test;
import com.tamagotchi.committracker.util.FileUtils;

import java.io.IOException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for WidgetWindow functionality.
 * Tests the core functionality without requiring JavaFX environment.
 */
public class WidgetWindowTest {

    @Test
    void testFileUtilsPropertiesSaveAndLoad() throws IOException {
        // Test the properties save/load functionality used by WidgetWindow
        Properties testProps = new Properties();
        testProps.setProperty("test.x", "100.0");
        testProps.setProperty("test.y", "200.0");
        
        String testFile = "test-widget-position.properties";
        
        // Test saving properties
        assertDoesNotThrow(() -> FileUtils.saveProperties(testProps, testFile), 
            "Save properties should not throw exception");
        
        // Test loading properties
        Properties loadedProps = FileUtils.loadProperties(testFile);
        assertNotNull(loadedProps, "Loaded properties should not be null");
        assertEquals("100.0", loadedProps.getProperty("test.x"), "X position should be preserved");
        assertEquals("200.0", loadedProps.getProperty("test.y"), "Y position should be preserved");
    }

    @Test
    void testFileUtilsLoadNonExistentFile() throws IOException {
        // Test loading a file that doesn't exist
        Properties props = FileUtils.loadProperties("non-existent-file.properties");
        assertNotNull(props, "Properties should not be null even for non-existent file");
        assertTrue(props.isEmpty(), "Properties should be empty for non-existent file");
    }

    @Test
    void testAppDataDirectoryAccess() {
        // Test that we can access the app data directory
        String appDataDir = FileUtils.getAppDataDirectory();
        assertNotNull(appDataDir, "App data directory should not be null");
        assertTrue(appDataDir.contains(".tamagotchi-commit-tracker"), 
            "App data directory should contain application name");
    }
}