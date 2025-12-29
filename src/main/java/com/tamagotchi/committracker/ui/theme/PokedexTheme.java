package com.tamagotchi.committracker.ui.theme;

import javafx.scene.text.Font;
import java.io.InputStream;
import java.util.List;

/**
 * Centralized styling constants for the Pokedex-style UI aesthetic.
 * Provides color definitions, font styling, and CSS helper methods
 * for creating a retro handheld gaming device appearance.
 */
public class PokedexTheme {
    
    // ========== Font Configuration ==========
    /** Primary pixel font name to attempt loading */
    private static final String PIXEL_FONT_PRIMARY = "Press Start 2P";
    
    /** Secondary pixel font options to try */
    private static final String[] PIXEL_FONT_ALTERNATIVES = {
        "VT323",
        "Silkscreen", 
        "Pixelify Sans",
        "Courier New",
        "Consolas"
    };
    
    /** Fallback monospace font */
    private static final String MONOSPACE_FALLBACK = "Monospace";
    
    /** The resolved font family to use (cached after first resolution) */
    private static String resolvedFontFamily = null;
    
    /** Flag indicating if font resolution has been attempted */
    private static boolean fontResolutionAttempted = false;
    
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
    /** Pixel font family (resolved at runtime with fallback) */
    public static final String PIXEL_FONT = getResolvedFontFamily();
    
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
     * Resolves the best available pixel font family.
     * Attempts to find a pixel-style font, falling back to monospace if none available.
     * The result is cached for subsequent calls.
     * 
     * @return The resolved font family name
     */
    public static String getResolvedFontFamily() {
        if (!fontResolutionAttempted) {
            fontResolutionAttempted = true;
            resolvedFontFamily = resolveFontFamily();
        }
        return resolvedFontFamily != null ? resolvedFontFamily : MONOSPACE_FALLBACK;
    }
    
    /**
     * Attempts to resolve a pixel font from available system fonts.
     * 
     * @return The name of an available pixel font, or null if none found
     */
    private static String resolveFontFamily() {
        // First try the primary pixel font
        if (isFontAvailable(PIXEL_FONT_PRIMARY)) {
            return PIXEL_FONT_PRIMARY;
        }
        
        // Try alternative pixel fonts
        for (String fontName : PIXEL_FONT_ALTERNATIVES) {
            if (isFontAvailable(fontName)) {
                return fontName;
            }
        }
        
        // Fall back to monospace
        return MONOSPACE_FALLBACK;
    }
    
    /**
     * Checks if a font is available on the system.
     * 
     * @param fontName The name of the font to check
     * @return true if the font is available, false otherwise
     */
    public static boolean isFontAvailable(String fontName) {
        if (fontName == null || fontName.isEmpty()) {
            return false;
        }
        
        try {
            List<String> fontFamilies = Font.getFamilies();
            for (String family : fontFamilies) {
                if (family.equalsIgnoreCase(fontName)) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Font system not available (e.g., headless environment)
            return false;
        }
        
        return false;
    }
    
    /**
     * Attempts to load a custom font from resources.
     * 
     * @param resourcePath The path to the font resource (e.g., "/fonts/pixel.ttf")
     * @param size The font size
     * @return The loaded Font, or null if loading failed
     */
    public static Font loadCustomFont(String resourcePath, double size) {
        try {
            InputStream fontStream = PokedexTheme.class.getResourceAsStream(resourcePath);
            if (fontStream != null) {
                Font font = Font.loadFont(fontStream, size);
                fontStream.close();
                return font;
            }
        } catch (Exception e) {
            // Font loading failed, will use fallback
        }
        return null;
    }
    
    /**
     * Checks if the current font is the monospace fallback.
     * 
     * @return true if using monospace fallback, false if using a pixel font
     */
    public static boolean isUsingFallbackFont() {
        return MONOSPACE_FALLBACK.equals(getResolvedFontFamily());
    }
    
    /**
     * Gets the CSS style for pixel font with specified size.
     * Disables font smoothing for crisp pixel edges using gray font smoothing type.
     * 
     * @param size The font size in pixels
     * @return CSS style string for pixel font
     */
    public static String getPixelFontStyle(int size) {
        // Use 'gray' smoothing type for crisper pixel edges (less anti-aliasing than 'lcd')
        // Also disable sub-pixel rendering for authentic pixel look
        return String.format(
            "-fx-font-family: '%s'; -fx-font-size: %dpx; -fx-font-smoothing-type: gray;",
            getResolvedFontFamily(), size
        );
    }
    
    /**
     * Gets the CSS style for pixel font with disabled smoothing for maximum crispness.
     * This variant completely disables font smoothing for the most authentic pixel look.
     * 
     * @param size The font size in pixels
     * @return CSS style string for pixel font with no smoothing
     */
    public static String getPixelFontStyleNoSmoothing(int size) {
        // Use gray smoothing (minimal anti-aliasing) for crisp pixel edges
        return String.format(
            "-fx-font-family: '%s'; -fx-font-size: %dpx; -fx-font-smoothing-type: gray;",
            getResolvedFontFamily(), size
        );
    }
    
    /**
     * Resets the font resolution cache. Useful for testing.
     */
    public static void resetFontCache() {
        fontResolutionAttempted = false;
        resolvedFontFamily = null;
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
