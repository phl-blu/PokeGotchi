package com.tamagotchi.committracker.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Utility class for file operations and path management.
 */
public class FileUtils {
    
    private static final String APP_DATA_DIR = System.getProperty("user.home") + "/.tamagotchi-commit-tracker";
    
    /**
     * Save properties to a file in the application data directory
     */
    public static void saveProperties(Properties properties, String filename) throws IOException {
        Path appDataPath = Paths.get(APP_DATA_DIR);
        if (!Files.exists(appDataPath)) {
            Files.createDirectories(appDataPath);
        }
        
        Path filePath = appDataPath.resolve(filename);
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            properties.store(fos, "Tamagotchi Commit Tracker Settings");
        }
    }
    
    /**
     * Load properties from a file in the application data directory
     */
    public static Properties loadProperties(String filename) throws IOException {
        Path filePath = Paths.get(APP_DATA_DIR, filename);
        Properties properties = new Properties();
        
        if (Files.exists(filePath)) {
            try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
                properties.load(fis);
            }
        }
        
        return properties;
    }
    
    /**
     * Get the application data directory path
     */
    public static String getAppDataDirectory() {
        return APP_DATA_DIR;
    }
}