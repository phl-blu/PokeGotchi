package com.tamagotchi.committracker.ui.theme;

/**
 * UI Theme configuration for customizing the appearance of the widget.
 * This allows easy modification of colors, styles, and layout without
 * changing the core UI logic.
 */
public class UITheme {
    
    // Background colors
    public static final String COMPACT_BACKGROUND = "transparent";
    public static final String EXPANDED_BACKGROUND = "rgba(20, 20, 30, 0.95)";
    public static final String STATUS_SECTION_BACKGROUND = "rgba(50, 50, 70, 0.8)";
    public static final String COMMIT_ENTRY_BACKGROUND = "rgba(60, 60, 80, 0.7)";
    
    // Border radius
    public static final String BORDER_RADIUS = "10";
    public static final String SMALL_BORDER_RADIUS = "8";
    public static final String TINY_BORDER_RADIUS = "5";
    
    // Text colors
    public static final String PRIMARY_TEXT_COLOR = "white";
    public static final String SECONDARY_TEXT_COLOR = "lightgray";
    public static final String TERTIARY_TEXT_COLOR = "gray";
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
     * Gets the CSS style for the expanded layout background.
     * 
     * @return CSS style string
     */
    public static String getExpandedLayoutStyle() {
        return String.format("-fx-background-color: %s; -fx-background-radius: %s;", 
            EXPANDED_BACKGROUND, BORDER_RADIUS);
    }
    
    /**
     * Gets the CSS style for the status section background.
     * 
     * @return CSS style string
     */
    public static String getStatusSectionStyle() {
        return String.format("-fx-background-color: %s; -fx-background-radius: %s;", 
            STATUS_SECTION_BACKGROUND, SMALL_BORDER_RADIUS);
    }
    
    /**
     * Gets the CSS style for commit entry backgrounds.
     * 
     * @return CSS style string
     */
    public static String getCommitEntryStyle() {
        return String.format("-fx-background-color: %s; -fx-background-radius: %s;", 
            COMMIT_ENTRY_BACKGROUND, TINY_BORDER_RADIUS);
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
     * Future: Load custom theme from configuration file.
     * This method is a placeholder for when you want to implement
     * custom themes loaded from external files.
     * 
     * @param themeName The name of the theme to load
     * @return true if theme was loaded successfully
     */
    public static boolean loadCustomTheme(String themeName) {
        // TODO: Implement custom theme loading
        // This could load from JSON, CSS files, or other configuration formats
        return false;
    }
    
    /**
     * Future: Apply a custom color scheme.
     * This method is a placeholder for when you want to implement
     * runtime color scheme changes.
     * 
     * @param primaryColor The primary color for the theme
     * @param secondaryColor The secondary color for the theme
     * @param backgroundColor The background color for the theme
     */
    public static void applyCustomColors(String primaryColor, String secondaryColor, String backgroundColor) {
        // TODO: Implement runtime color scheme changes
        // This could update static fields or use a configuration object
    }
}