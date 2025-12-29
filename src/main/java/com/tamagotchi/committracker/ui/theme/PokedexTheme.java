package com.tamagotchi.committracker.ui.theme;

/**
 * Centralized styling constants for the Pokedex-style UI aesthetic.
 * Provides color definitions, font styling, and CSS helper methods
 * for creating a retro handheld gaming device appearance.
 */
public class PokedexTheme {
    
    // ========== Frame Colors ==========
    /** Primary red color for the Pokedex frame */
    public static final String FRAME_PRIMARY = "#CB2343";
    
    /** Black border color for the frame */
    public static final String FRAME_BORDER = "#000000";
    
    /** Highlight color for frame accents */
    public static final String FRAME_HIGHLIGHT = "#FF6B7A";
    
    // ========== Screen Colors ==========
    /** Gray/purple background for the main screen area */
    public static final String SCREEN_BG = "#6B7B8C";
    
    /** Black border color for the screen area */
    public static final String SCREEN_BORDER = "#000000";
    
    // ========== Window Control Colors ==========
    /** Blue dot color for window controls */
    public static final String CONTROL_BLUE = "#4A90D9";
    
    /** Pink dot color for window controls */
    public static final String CONTROL_PINK = "#E891B0";
    
    /** Orange dot color for window controls */
    public static final String CONTROL_ORANGE = "#E8A84A";
    
    /** Green dot color for window controls */
    public static final String CONTROL_GREEN = "#5CB85C";
    
    // ========== Cell Styling Colors ==========
    /** Dark background for grid cells */
    public static final String CELL_BG = "#3D4852";
    
    /** Lighter background for highlighted cells */
    public static final String CELL_HIGHLIGHT = "#5A6B7A";
    
    /** Blue color for selected cells */
    public static final String CELL_SELECTED = "#4A90D9";
    
    // ========== Text Styling ==========
    /** Pixel font family (monospace fallback) */
    public static final String PIXEL_FONT = "Monospace";
    
    /** Default pixel font size */
    public static final int PIXEL_FONT_SIZE = 10;
    
    /** White text color for labels */
    public static final String TEXT_WHITE = "#FFFFFF";
    
    /** Dark text color for contrast */
    public static final String TEXT_DARK = "#2D3436";
    
    // ========== Stats Corner Styling ==========
    /** Semi-transparent dark background for stats overlay */
    public static final String STATS_BG = "rgba(0, 0, 0, 0.6)";
    
    /** Padding for stats corner */
    public static final int STATS_PADDING = 6;
    
    // ========== Name Label Styling ==========
    /** Background color for name label */
    public static final String NAME_LABEL_BG = "#4A5568";
    
    // ========== D-Pad Colors ==========
    /** Light color for D-pad segments */
    public static final String DPAD_COLOR = "#F5F5F5";
    
    /** Shadow color for D-pad depth effect */
    public static final String DPAD_SHADOW = "#CCCCCC";
    
    // ========== Dimensions ==========
    /** Frame width in pixels - compact to fit eggs/Pokemon */
    public static final int FRAME_WIDTH = 200;
    
    /** Frame height in pixels - compact to fit eggs/Pokemon */
    public static final int FRAME_HEIGHT = 260;
    
    /** Screen width in pixels */
    public static final int SCREEN_WIDTH = 170;
    
    /** Screen height in pixels */
    public static final int SCREEN_HEIGHT = 180;
    
    /** Border width in pixels */
    public static final int BORDER_WIDTH = 4;
    
    /** Cell size for selection grid - bigger for better visibility */
    public static final int CELL_SIZE = 50;
    
    /** Spacing between grid cells */
    public static final int CELL_SPACING = 6;
    
    // ========== Helper Methods ==========
    
    /**
     * Gets the CSS style for pixel font with specified size.
     * Disables font smoothing for crisp pixel edges.
     * 
     * @param size The font size in pixels
     * @return CSS style string for pixel font
     */
    public static String getPixelFontStyle(int size) {
        return String.format(
            "-fx-font-family: '%s'; -fx-font-size: %dpx; -fx-font-smoothing-type: lcd;",
            PIXEL_FONT, size
        );
    }
    
    /**
     * Gets the CSS style for the main Pokedex frame.
     * Uses layered backgrounds: transparent outer layer, black middle layer with rounded corners,
     * orange inner layer with rounded corners, properly inset.
     * 
     * @return CSS style string for frame background and border
     */
    public static String getFrameStyle() {
        // Three-layer approach: transparent base, black border layer, orange fill
        // The transparent base ensures corners outside the rounded area are see-through
        return String.format(
            "-fx-background-color: transparent, %s, %s; -fx-background-radius: 0, 13, 13; -fx-background-insets: 0, 0, %d;",
            FRAME_BORDER, FRAME_PRIMARY, BORDER_WIDTH
        );
    }
    
    /**
     * Gets the CSS style for the screen area.
     * 
     * @return CSS style string for screen background and border
     */
    public static String getScreenStyle() {
        return String.format(
            "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: 2; -fx-background-radius: 10; -fx-border-radius: 10;",
            SCREEN_BG, SCREEN_BORDER
        );
    }
    
    /**
     * Gets the CSS style for a grid cell based on its state.
     * 
     * @param highlighted Whether the cell is currently hovered
     * @param selected Whether the cell is currently selected
     * @return CSS style string for the cell
     */
    public static String getCellStyle(boolean highlighted, boolean selected) {
        String bgColor;
        if (selected) {
            bgColor = CELL_SELECTED;
        } else if (highlighted) {
            bgColor = CELL_HIGHLIGHT;
        } else {
            bgColor = CELL_BG;
        }
        return String.format(
            "-fx-background-color: %s; -fx-background-radius: 50%%;",
            bgColor
        );
    }
    
    /**
     * Gets the CSS style for the stats corner overlay.
     * 
     * @return CSS style string for stats corner
     */
    public static String getStatsCornerStyle() {
        return String.format(
            "-fx-background-color: %s; -fx-padding: %d; -fx-background-radius: 5;",
            STATS_BG, STATS_PADDING
        );
    }
    
    /**
     * Gets the CSS style for the name label at the bottom.
     * Uses the same semi-transparent background as the stats corner.
     * 
     * @return CSS style string for name label
     */
    public static String getNameLabelStyle() {
        return String.format(
            "-fx-background-color: %s; -fx-padding: 4 8 4 8; -fx-background-radius: 5;",
            STATS_BG
        );
    }
    
    /**
     * Gets the CSS style for text with specified color.
     * 
     * @param color The text color (hex or named)
     * @return CSS style string for text fill
     */
    public static String getTextStyle(String color) {
        return String.format("-fx-text-fill: %s;", color);
    }
    
    /**
     * Gets the CSS style for a window control dot.
     * 
     * @param color The dot color
     * @param size The dot diameter in pixels
     * @return CSS style string for the control dot
     */
    public static String getControlDotStyle(String color, int size) {
        return String.format(
            "-fx-background-color: %s; -fx-background-radius: %d; -fx-min-width: %d; -fx-min-height: %d; -fx-max-width: %d; -fx-max-height: %d;",
            color, size / 2, size, size, size, size
        );
    }
    
    /**
     * Gets the CSS style for the D-pad control.
     * 
     * @return CSS style string for D-pad
     */
    public static String getDPadStyle() {
        return String.format(
            "-fx-background-color: %s;",
            DPAD_COLOR
        );
    }
    
    /**
     * Gets the CSS style for action buttons.
     * 
     * @return CSS style string for action buttons
     */
    public static String getActionButtonStyle() {
        return String.format(
            "-fx-background-color: %s; -fx-background-radius: 50%%;",
            DPAD_COLOR
        );
    }
    
    /**
     * Combines multiple CSS style strings.
     * 
     * @param styles Variable number of style strings to combine
     * @return Combined CSS style string
     */
    public static String combineStyles(String... styles) {
        StringBuilder combined = new StringBuilder();
        for (String style : styles) {
            if (style != null && !style.isEmpty()) {
                combined.append(style);
                if (!style.endsWith(";")) {
                    combined.append(";");
                }
                combined.append(" ");
            }
        }
        return combined.toString().trim();
    }
}
