package com.tamagotchi.committracker.util;

import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * Windows-specific integration utilities for native system behavior.
 * Handles theme detection, credential storage, and window management.
 */
public class WindowsIntegration {
    private static final Logger logger = Logger.getLogger(WindowsIntegration.class.getName());
    
    // Windows Registry keys for theme detection
    private static final String THEME_REGISTRY_KEY = "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize";
    private static final String APPS_USE_LIGHT_THEME = "AppsUseLightTheme";
    
    // Preferences node for credential storage
    private static final String PREFS_NODE = "com/tamagotchi/committracker/credentials";
    
    private static WindowsTheme currentTheme = WindowsTheme.DARK;
    private static boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");
    
    /**
     * Enum representing Windows theme modes.
     */
    public enum WindowsTheme {
        LIGHT, DARK, HIGH_CONTRAST
    }
    
    /**
     * Detects the current Windows theme by reading registry values.
     * 
     * @return The current Windows theme
     */
    public static WindowsTheme detectWindowsTheme() {
        if (!isWindows) {
            logger.fine("Not running on Windows, returning default dark theme");
            return WindowsTheme.DARK;
        }
        
        try {
            // Query Windows registry for theme setting
            ProcessBuilder pb = new ProcessBuilder(
                "reg", "query", THEME_REGISTRY_KEY, "/v", APPS_USE_LIGHT_THEME
            );
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(APPS_USE_LIGHT_THEME)) {
                        // Parse the registry value (0x1 = light theme, 0x0 = dark theme)
                        if (line.contains("0x1")) {
                            currentTheme = WindowsTheme.LIGHT;
                            logger.info("Detected Windows light theme");
                            return WindowsTheme.LIGHT;
                        } else if (line.contains("0x0")) {
                            currentTheme = WindowsTheme.DARK;
                            logger.info("Detected Windows dark theme");
                            return WindowsTheme.DARK;
                        }
                    }
                }
            }
            
            process.waitFor();
            
        } catch (IOException | InterruptedException e) {
            logger.log(Level.WARNING, "Failed to detect Windows theme, using default dark theme", e);
        }
        
        return WindowsTheme.DARK;
    }
    
    /**
     * Gets the current cached Windows theme.
     * 
     * @return The current Windows theme
     */
    public static WindowsTheme getCurrentTheme() {
        return currentTheme;
    }
    
    /**
     * Applies Windows theme-appropriate styling to a JavaFX stage.
     * 
     * @param stage The JavaFX stage to style
     * @param theme The Windows theme to apply
     */
    public static void applyWindowsTheme(Stage stage, WindowsTheme theme) {
        if (!isWindows || stage == null) {
            return;
        }
        
        Platform.runLater(() -> {
            try {
                switch (theme) {
                    case LIGHT:
                        applyLightTheme(stage);
                        break;
                    case DARK:
                        applyDarkTheme(stage);
                        break;
                    case HIGH_CONTRAST:
                        applyHighContrastTheme(stage);
                        break;
                }
                logger.fine("Applied Windows " + theme + " theme to stage");
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to apply Windows theme", e);
            }
        });
    }
    
    /**
     * Applies light theme styling.
     */
    private static void applyLightTheme(Stage stage) {
        // Update window chrome to match light theme
        if (stage.getScene() != null && stage.getScene().getRoot() != null) {
            String lightStyle = """
                -fx-background-color: rgba(248, 248, 248, 0.95);
                -fx-text-fill: #333333;
                """;
            stage.getScene().getRoot().setStyle(lightStyle);
        }
    }
    
    /**
     * Applies dark theme styling.
     */
    private static void applyDarkTheme(Stage stage) {
        // Update window chrome to match dark theme
        if (stage.getScene() != null && stage.getScene().getRoot() != null) {
            String darkStyle = """
                -fx-background-color: rgba(32, 32, 32, 0.95);
                -fx-text-fill: #ffffff;
                """;
            stage.getScene().getRoot().setStyle(darkStyle);
        }
    }
    
    /**
     * Applies high contrast theme styling.
     */
    private static void applyHighContrastTheme(Stage stage) {
        // Update window chrome for high contrast
        if (stage.getScene() != null && stage.getScene().getRoot() != null) {
            String highContrastStyle = """
                -fx-background-color: rgba(0, 0, 0, 1.0);
                -fx-text-fill: #ffffff;
                -fx-border-color: #ffffff;
                -fx-border-width: 2px;
                """;
            stage.getScene().getRoot().setStyle(highContrastStyle);
        }
    }
    
    /**
     * Configures proper Windows z-order and focus behavior for a stage.
     * 
     * @param stage The JavaFX stage to configure
     */
    public static void configureWindowsBehavior(Stage stage) {
        if (!isWindows || stage == null) {
            return;
        }
        
        Platform.runLater(() -> {
            try {
                // Configure proper focus behavior
                stage.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                    if (isNowFocused) {
                        // Bring to front when focused
                        stage.toFront();
                    }
                });
                
                // Configure proper z-order behavior
                stage.setAlwaysOnTop(true);
                
                // Ensure proper taskbar integration
                stage.setIconified(false);
                
                logger.fine("Configured Windows-specific behavior for stage");
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to configure Windows behavior", e);
            }
        });
    }
    
    /**
     * Ensures proper taskbar minimization behavior.
     * 
     * @param stage The JavaFX stage to configure
     */
    public static void configureTaskbarBehavior(Stage stage) {
        if (!isWindows || stage == null) {
            return;
        }
        
        Platform.runLater(() -> {
            try {
                // Configure minimize behavior to taskbar (not system tray)
                stage.iconifiedProperty().addListener((obs, wasIconified, isNowIconified) -> {
                    if (isNowIconified) {
                        logger.fine("Widget minimized to taskbar");
                    } else {
                        logger.fine("Widget restored from taskbar");
                        // Ensure proper restoration
                        stage.toFront();
                        stage.requestFocus();
                    }
                });
                
                // Prevent closing to system tray - always minimize to taskbar
                stage.setOnCloseRequest(event -> {
                    event.consume(); // Prevent default close
                    stage.setIconified(true); // Minimize to taskbar instead
                });
                
                logger.fine("Configured taskbar behavior for stage");
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to configure taskbar behavior", e);
            }
        });
    }
    
    /**
     * Stores Git credentials securely using Windows Credential Manager.
     * 
     * @param repositoryUrl The repository URL
     * @param username The username
     * @param token The access token or password
     * @return true if credentials were stored successfully
     */
    public static boolean storeGitCredentials(String repositoryUrl, String username, String token) {
        if (!isWindows || repositoryUrl == null || username == null || token == null) {
            return false;
        }
        
        try {
            // Use Windows Credential Manager via cmdkey command
            ProcessBuilder pb = new ProcessBuilder(
                "cmdkey", "/generic:" + repositoryUrl, "/user:" + username, "/pass:" + token
            );
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                logger.info("Successfully stored Git credentials for: " + repositoryUrl);
                return true;
            } else {
                logger.warning("Failed to store Git credentials, exit code: " + exitCode);
                return false;
            }
            
        } catch (IOException | InterruptedException e) {
            logger.log(Level.WARNING, "Failed to store Git credentials", e);
            return false;
        }
    }
    
    /**
     * Retrieves Git credentials from Windows Credential Manager.
     * 
     * @param repositoryUrl The repository URL
     * @return The stored credentials as [username, token], or null if not found
     */
    public static String[] retrieveGitCredentials(String repositoryUrl) {
        if (!isWindows || repositoryUrl == null) {
            return null;
        }
        
        try {
            // Query Windows Credential Manager
            ProcessBuilder pb = new ProcessBuilder(
                "cmdkey", "/list:" + repositoryUrl
            );
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                String username = null;
                
                while ((line = reader.readLine()) != null) {
                    if (line.contains("User:")) {
                        username = line.substring(line.indexOf("User:") + 5).trim();
                        break;
                    }
                }
                
                if (username != null) {
                    logger.fine("Found stored credentials for: " + repositoryUrl);
                    return new String[]{username, ""}; // Password retrieval requires additional security
                }
            }
            
            process.waitFor();
            
        } catch (IOException | InterruptedException e) {
            logger.log(Level.FINE, "No stored credentials found for: " + repositoryUrl, e);
        }
        
        return null;
    }
    
    /**
     * Uses Java Preferences API as fallback for credential storage.
     * 
     * @param key The credential key
     * @param value The credential value (null to remove)
     */
    public static void storeCredentialFallback(String key, String value) {
        try {
            Preferences prefs = Preferences.userRoot().node(PREFS_NODE);
            if (value != null) {
                prefs.put(key, value);
            } else {
                prefs.remove(key);
            }
            prefs.flush();
            logger.fine("Stored credential using fallback method: " + key);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to store credential using fallback", e);
        }
    }
    
    /**
     * Retrieves credentials using Java Preferences API fallback.
     * 
     * @param key The credential key
     * @return The stored credential value, or null if not found
     */
    public static String retrieveCredentialFallback(String key) {
        try {
            Preferences prefs = Preferences.userRoot().node(PREFS_NODE);
            String value = prefs.get(key, null);
            if (value != null) {
                logger.fine("Retrieved credential using fallback method: " + key);
            }
            return value;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to retrieve credential using fallback", e);
            return null;
        }
    }
    
    /**
     * Monitors Windows theme changes asynchronously.
     * 
     * @param onThemeChange Callback to execute when theme changes
     */
    public static void monitorThemeChanges(Runnable onThemeChange) {
        if (!isWindows) {
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            WindowsTheme lastTheme = currentTheme;
            
            while (true) {
                try {
                    Thread.sleep(5000); // Check every 5 seconds
                    
                    WindowsTheme newTheme = detectWindowsTheme();
                    if (newTheme != lastTheme) {
                        logger.info("Windows theme changed from " + lastTheme + " to " + newTheme);
                        lastTheme = newTheme;
                        
                        if (onThemeChange != null) {
                            Platform.runLater(onThemeChange);
                        }
                    }
                    
                } catch (InterruptedException e) {
                    logger.info("Theme monitoring interrupted");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error monitoring theme changes", e);
                }
            }
        });
    }
    
    /**
     * Checks if the application is running on Windows.
     * 
     * @return true if running on Windows
     */
    public static boolean isWindows() {
        return isWindows;
    }
}
