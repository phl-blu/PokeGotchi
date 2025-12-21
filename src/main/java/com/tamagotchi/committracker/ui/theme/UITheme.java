package com.tamagotchi.committracker.ui.theme;

import com.tamagotchi.committracker.util.WindowsIntegration;

/**
 * UI Theme configuration for customizing the appearance of the widget.
 * This allows easy modification of colors, styles, and layout without
 * changing the core UI logic. Now supports Windows theme adaptation.
 */
public class UITheme {
    
    // Current theme mode
    private static WindowsIntegration.WindowsTheme currentWindowsTheme = WindowsIntegration.WindowsTheme.DARK;
    
    // Dark theme colors (default)
    private static final String DARK_COMPACT_BACKGROUND = "transparent";
    private static final String DARK_EXPANDED_BACKGROUND = "rgba(20, 20, 30, 0.95)";
    private static final String DARK_STATUS_SECTION_BACKGROUND = "rgba(50, 50, 70, 0.8)";
    private static final String DARK_COMMIT_ENTRY_BACKGROUND = "rgba(60, 60, 80, 0.7)";
    private static final String DARK_PRIMARY_TEXT_COLOR = "white";
    private static final String DARK_SECONDARY_TEXT_COLOR = "lightgray";
    private static final String DARK_TERTIARY_TEXT_COLOR = "gray";
    
    // Light theme colors
    private static final String LIGHT_COMPACT_BACKGROUND = "transparent";
    private static final String LIGHT_EXPANDED_BACKGROUND = "rgba(248, 248, 248, 0.95)";
    private static final String LIGHT_STATUS_SECTION_BACKGROUND = "rgba(230, 230, 230, 0.8)";
    private static final String LIGHT_COMMIT_ENTRY_BACKGROUND = "rgba(220, 220, 220, 0.7)";
    private static final String LIGHT_PRIMARY_TEXT_COLOR = "#333333";
    private static final String LIGHT_SECONDARY_TEXT_COLOR = "#666666";
    private static final String LIGHT_TERTIARY_TEXT_COLOR = "#999999";
    
    // High contrast theme colors
    private static final String HC_COMPACT_BACKGROUND = "transparent";
    private static final String HC_EXPANDED_BACKGROUND = "rgba(0, 0, 0, 1.0)";
    private static final String HC_STATUS_SECTION_BACKGROUND = "rgba(0, 0, 0, 1.0)";
    private static final String HC_COMMIT_ENTRY_BACKGROUND = "rgba(0, 0, 0, 1.0)";
    private static final String HC_PRIMARY_TEXT_COLOR = "white";
    private static final String HC_SECONDARY_TEXT_COLOR = "white";
    private static final String HC_TERTIARY_TEXT_COLOR = "white";
    
    // Dynamic background colors (adapt to Windows theme)
    public static String COMPACT_BACKGROUND = DARK_COMPACT_BACKGROUND;
    public static String EXPANDED_BACKGROUND = DARK_EXPANDED_BACKGROUND;
    public static String STATUS_SECTION_BACKGROUND = DARK_STATUS_SECTION_BACKGROUND;
    public static String COMMIT_ENTRY_BACKGROUND = DARK_COMMIT_ENTRY_BACKGROUND;
    
    // Border radius
    public static final String BORDER_RADIUS = "10";
    public static final String SMALL_BORDER_RADIUS = "8";
    public static final String TINY_BORDER_RADIUS = "5";
    
    // Dynamic text colors (adapt to Windows theme)
    public static String PRIMARY_TEXT_COLOR = DARK_PRIMARY_TEXT_COLOR;
    public static String SECONDARY_TEXT_COLOR = DARK_SECONDARY_TEXT_COLOR;
    public static String TERTIARY_TEXT_COLOR = DARK_TERTIARY_TEXT_COLOR;
    public static final String XP_COLOR = "lightgreen";
    
    // Fonts
    public static final String PRIMARY_FONT = "System";
    public static final int HEADER_FONT_SIZE = 14;
    public static final int NORMAL_FONT_SIZE = 12;
    public static final int SMALL_FONT_SIZE = 11;
    public static final int TINY_FONT_SIZE = 10;
    public static final int MICRO_FONT_SIZE = 9;
    
    // Spacing
    public static final int LARGE_SPACING = 10;
    public static final int MEDIUM_SPACING = 5;
    public static final int SMALL_SPACING = 2;
    
    // Padding
    public static final int LARGE_PADDING = 10;
    public static final int MEDIUM_PADDING = 8;
    public static final int SMALL_PADDING = 5;
    
    /**
     * Updates the theme colors based on the current Windows theme.
     * 
     * @param windowsTheme The Windows theme to apply
     */
    public static void updateWindowsTheme(WindowsIntegration.WindowsTheme windowsTheme) {
        currentWindowsTheme = windowsTheme;
        
        switch (windowsTheme) {
            case LIGHT:
                COMPACT_BACKGROUND = LIGHT_COMPACT_BACKGROUND;
                EXPANDED_BACKGROUND = LIGHT_EXPANDED_BACKGROUND;
                STATUS_SECTION_BACKGROUND = LIGHT_STATUS_SECTION_BACKGROUND;
                COMMIT_ENTRY_BACKGROUND = LIGHT_COMMIT_ENTRY_BACKGROUND;
                PRIMARY_TEXT_COLOR = LIGHT_PRIMARY_TEXT_COLOR;
                SECONDARY_TEXT_COLOR = LIGHT_SECONDARY_TEXT_COLOR;
                TERTIARY_TEXT_COLOR = LIGHT_TERTIARY_TEXT_COLOR;
                break;
                
            case HIGH_CONTRAST:
                COMPACT_BACKGROUND = HC_COMPACT_BACKGROUND;
                EXPANDED_BACKGROUND = HC_EXPANDED_BACKGROUND;
                STATUS_SECTION_BACKGROUND = HC_STATUS_SECTION_BACKGROUND;
                COMMIT_ENTRY_BACKGROUND = HC_COMMIT_ENTRY_BACKGROUND;
                PRIMARY_TEXT_COLOR = HC_PRIMARY_TEXT_COLOR;
                SECONDARY_TEXT_COLOR = HC_SECONDARY_TEXT_COLOR;
                TERTIARY_TEXT_COLOR = HC_TERTIARY_TEXT_COLOR;
                break;
                
            case DARK:
            default:
                COMPACT_BACKGROUND = DARK_COMPACT_BACKGROUND;
                EXPANDED_BACKGROUND = DARK_EXPANDED_BACKGROUND;
                STATUS_SECTION_BACKGROUND = DARK_STATUS_SECTION_BACKGROUND;
                COMMIT_ENTRY_BACKGROUND = DARK_COMMIT_ENTRY_BACKGROUND;
                PRIMARY_TEXT_COLOR = DARK_PRIMARY_TEXT_COLOR;
                SECONDARY_TEXT_COLOR = DARK_SECONDARY_TEXT_COLOR;
                TERTIARY_TEXT_COLOR = DARK_TERTIARY_TEXT_COLOR;
                break;
        }
    }
    
    /**
     * Gets the current Windows theme.
     * 
     * @return The current Windows theme
     */
    public static WindowsIntegration.WindowsTheme getCurrentWindowsTheme() {
        return currentWindowsTheme;
    }
    
    /**
     * Gets the CSS style for the expanded layout background with current theme.
     * 
     * @return CSS style string
     */
    public static String getExpandedLayoutStyle() {
        return String.format("-fx-background-color: %s; -fx-background-radius: %s; -fx-text-fill: %s;", 
            EXPANDED_BACKGROUND, BORDER_RADIUS, PRIMARY_TEXT_COLOR);
    }
    
    /**
     * Gets the CSS style for the status section background with current theme.
     * 
     * @return CSS style string
     */
    public static String getStatusSectionStyle() {
        return String.format("-fx-background-color: %s; -fx-background-radius: %s; -fx-text-fill: %s;", 
            STATUS_SECTION_BACKGROUND, SMALL_BORDER_RADIUS, PRIMARY_TEXT_COLOR);
    }
    
    /**
     * Gets the CSS style for commit entry backgrounds with current theme.
     * 
     * @return CSS style string
     */
    public static String getCommitEntryStyle() {
        return String.format("-fx-background-color: %s; -fx-background-radius: %s; -fx-text-fill: %s;", 
            COMMIT_ENTRY_BACKGROUND, TINY_BORDER_RADIUS, SECONDARY_TEXT_COLOR);
    }
    
    /**
     * Gets the CSS style for transparent backgrounds.
     * 
     * @return CSS style string
     */
    public static String getTransparentStyle() {
        return "-fx-background-color: transparent;";
    }
    
    /**
     * Load custom theme from configuration file and apply Windows theme.
     * This method detects the Windows theme and applies appropriate styling.
     * 
     * @param themeName The name of the theme to load (optional, can be null for auto-detection)
     * @return true if theme was loaded successfully
     */
    public static boolean loadCustomTheme(String themeName) {
        try {
            // Detect current Windows theme
            WindowsIntegration.WindowsTheme windowsTheme = WindowsIntegration.detectWindowsTheme();
            
            // Apply the detected theme
            updateWindowsTheme(windowsTheme);
            
            return true;
        } catch (Exception e) {
            // Fallback to dark theme if detection fails
            updateWindowsTheme(WindowsIntegration.WindowsTheme.DARK);
            return false;
        }
    }
    
    /**
     * Apply a custom color scheme with Windows theme awareness.
     * This method respects the Windows theme while allowing customization.
     * 
     * @param primaryColor The primary color for the theme (optional override)
     * @param secondaryColor The secondary color for the theme (optional override)
     * @param backgroundColor The background color for the theme (optional override)
     */
    public static void applyCustomColors(String primaryColor, String secondaryColor, String backgroundColor) {
        // First apply Windows theme as base
        WindowsIntegration.WindowsTheme windowsTheme = WindowsIntegration.detectWindowsTheme();
        updateWindowsTheme(windowsTheme);
        
        // Then apply custom overrides if provided
        if (primaryColor != null && !primaryColor.isEmpty()) {
            PRIMARY_TEXT_COLOR = primaryColor;
        }
        if (secondaryColor != null && !secondaryColor.isEmpty()) {
            SECONDARY_TEXT_COLOR = secondaryColor;
        }
        if (backgroundColor != null && !backgroundColor.isEmpty()) {
            EXPANDED_BACKGROUND = backgroundColor;
        }
    }
}