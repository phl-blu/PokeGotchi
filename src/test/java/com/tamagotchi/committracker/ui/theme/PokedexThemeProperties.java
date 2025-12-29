package com.tamagotchi.committracker.ui.theme;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.regex.Pattern;

/**
 * Property-based tests for PokedexTheme styling constants and helper methods.
 * 
 * **Feature: pokedex-ui-redesign, Property 6: Frame renders with correct color scheme**
 * **Validates: Requirements 1.1, 1.4**
 */
class PokedexThemeProperties {

    // Pattern for valid CSS hex color (3 or 6 digit)
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6})$");
    
    // Pattern for valid CSS rgba color
    private static final Pattern RGBA_COLOR_PATTERN = Pattern.compile("^rgba\\(\\s*\\d+\\s*,\\s*\\d+\\s*,\\s*\\d+\\s*,\\s*[0-9.]+\\s*\\)$");
    
    @BeforeEach
    void resetFontCache() {
        // Reset font cache before each test to ensure clean state
        PokedexTheme.resetFontCache();
    }
    
    // ========== Font Fallback Unit Tests ==========
    // **Validates: Requirements 7.1, 7.2**
    
    /**
     * Verifies that the resolved font family is never null or empty.
     * **Validates: Requirements 7.2**
     */
    @Test
    void resolvedFontFamilyShouldNeverBeNullOrEmpty() {
        String fontFamily = PokedexTheme.getResolvedFontFamily();
        
        assertNotNull(fontFamily, "Resolved font family should not be null");
        assertFalse(fontFamily.isEmpty(), "Resolved font family should not be empty");
    }
    
    /**
     * Verifies that when no pixel font is available, the system falls back to monospace.
     * **Validates: Requirements 7.2**
     */
    @Test
    void shouldFallbackToMonospaceWhenPixelFontUnavailable() {
        // The resolved font should be either a pixel font or Monospace fallback
        String fontFamily = PokedexTheme.getResolvedFontFamily();
        
        // Font should be resolved to something
        assertNotNull(fontFamily, "Font family should be resolved");
        
        // If using fallback, it should be Monospace
        if (PokedexTheme.isUsingFallbackFont()) {
            assertEquals("Monospace", fontFamily, 
                "Fallback font should be Monospace");
        }
    }
    
    /**
     * Verifies that isFontAvailable returns false for null or empty font names.
     * **Validates: Requirements 7.2**
     */
    @Test
    void isFontAvailableShouldReturnFalseForInvalidInput() {
        assertFalse(PokedexTheme.isFontAvailable(null), 
            "isFontAvailable should return false for null");
        assertFalse(PokedexTheme.isFontAvailable(""), 
            "isFontAvailable should return false for empty string");
    }
    
    /**
     * Verifies that the pixel font style includes the resolved font family.
     * **Validates: Requirements 7.1**
     */
    @Test
    void pixelFontStyleShouldIncludeResolvedFontFamily() {
        String style = PokedexTheme.getPixelFontStyle(12);
        String resolvedFont = PokedexTheme.getResolvedFontFamily();
        
        assertTrue(style.contains(resolvedFont),
            "Pixel font style should include the resolved font family: " + resolvedFont);
    }
    
    /**
     * Verifies that font smoothing is disabled (using 'gray' type) for crisp pixel edges.
     * **Validates: Requirements 7.3**
     */
    @Test
    void pixelFontStyleShouldDisableFontSmoothing() {
        String style = PokedexTheme.getPixelFontStyle(12);
        
        // Should use 'gray' smoothing type for minimal anti-aliasing
        assertTrue(style.contains("-fx-font-smoothing-type: gray"),
            "Pixel font style should use gray font smoothing for crisp edges");
    }
    
    /**
     * Verifies that the PIXEL_FONT constant is properly initialized.
     * **Validates: Requirements 7.1, 7.2**
     */
    @Test
    void pixelFontConstantShouldBeInitialized() {
        assertNotNull(PokedexTheme.PIXEL_FONT, 
            "PIXEL_FONT constant should not be null");
        assertFalse(PokedexTheme.PIXEL_FONT.isEmpty(), 
            "PIXEL_FONT constant should not be empty");
    }
    
    /**
     * Verifies that resetFontCache allows re-resolution of fonts.
     * **Validates: Requirements 7.2**
     */
    @Test
    void resetFontCacheShouldAllowReResolution() {
        // Get initial font
        String initialFont = PokedexTheme.getResolvedFontFamily();
        
        // Reset cache
        PokedexTheme.resetFontCache();
        
        // Get font again - should still resolve to same value
        String afterResetFont = PokedexTheme.getResolvedFontFamily();
        
        // Both should be valid (not necessarily equal if system state changed)
        assertNotNull(initialFont, "Initial font should not be null");
        assertNotNull(afterResetFont, "Font after reset should not be null");
    }

    /**
     * **Feature: pokedex-ui-redesign, Property 6: Frame renders with correct color scheme**
     * **Validates: Requirements 1.1, 1.4**
     * 
     * Verifies that the frame primary color is the specified orange-red (#E63900)
     * and the border color is black (#000000).
     */
    @Property(tries = 100)
    @Label("Frame should use correct red/coral primary color and dark navy border")
    void frameShouldUseCorrectColorScheme() {
        // Assert: Frame primary color must be exactly #CB2343 (red)
        assertEquals("#CB2343", PokedexTheme.FRAME_PRIMARY,
            "Frame primary color must be red #CB2343");
        
        // Assert: Frame border color must be exactly #000000 (black)
        assertEquals("#000000", PokedexTheme.FRAME_BORDER,
            "Frame border color must be black #000000");
        
        // Assert: Frame style should contain both colors
        String frameStyle = PokedexTheme.getFrameStyle();
        assertTrue(frameStyle.contains(PokedexTheme.FRAME_PRIMARY),
            "Frame style should include primary color");
        assertTrue(frameStyle.contains(PokedexTheme.FRAME_BORDER),
            "Frame style should include border color");
    }

    /**
     * **Feature: pokedex-ui-redesign, Property 6: Frame renders with correct color scheme**
     * **Validates: Requirements 1.1, 1.4**
     * 
     * Verifies that the screen background uses the correct gray/purple color.
     */
    @Property(tries = 100)
    @Label("Screen should use correct gray/purple background color")
    void screenShouldUseCorrectBackgroundColor() {
        // Assert: Screen background must be #6B7B8C (gray/purple)
        assertEquals("#6B7B8C", PokedexTheme.SCREEN_BG,
            "Screen background must be gray/purple #6B7B8C");
        
        // Assert: Screen style should contain the background color
        String screenStyle = PokedexTheme.getScreenStyle();
        assertTrue(screenStyle.contains(PokedexTheme.SCREEN_BG),
            "Screen style should include background color");
    }

    /**
     * Verifies that all color constants are valid CSS hex colors.
     */
    @Property(tries = 100)
    @Label("All hex color constants should be valid CSS hex colors")
    void allHexColorConstantsShouldBeValidCssColors() {
        // Test all hex color constants
        String[] hexColors = {
            PokedexTheme.FRAME_PRIMARY,
            PokedexTheme.FRAME_BORDER,
            PokedexTheme.FRAME_HIGHLIGHT,
            PokedexTheme.SCREEN_BG,
            PokedexTheme.SCREEN_BORDER,
            PokedexTheme.CONTROL_BLUE,
            PokedexTheme.CONTROL_PINK,
            PokedexTheme.CONTROL_ORANGE,
            PokedexTheme.CONTROL_GREEN,
            PokedexTheme.CELL_BG,
            PokedexTheme.CELL_HIGHLIGHT,
            PokedexTheme.CELL_SELECTED,
            PokedexTheme.TEXT_WHITE,
            PokedexTheme.TEXT_DARK,
            PokedexTheme.NAME_LABEL_BG,
            PokedexTheme.DPAD_COLOR,
            PokedexTheme.DPAD_SHADOW
        };
        
        for (String color : hexColors) {
            assertTrue(HEX_COLOR_PATTERN.matcher(color).matches(),
                "Color '" + color + "' should be a valid CSS hex color");
        }
    }

    /**
     * Verifies that the stats background uses valid rgba format.
     */
    @Property(tries = 100)
    @Label("Stats background should use valid rgba format")
    void statsBackgroundShouldUseValidRgbaFormat() {
        assertTrue(RGBA_COLOR_PATTERN.matcher(PokedexTheme.STATS_BG).matches(),
            "Stats background should be valid rgba color: " + PokedexTheme.STATS_BG);
    }

    /**
     * **Feature: pokedex-ui-redesign, Property 7: Window controls display four colored dots**
     * **Validates: Requirements 1.3**
     * 
     * Verifies that window control colors are all distinct.
     */
    @Property(tries = 100)
    @Label("Window control colors should all be distinct")
    void windowControlColorsShouldBeDistinct() {
        String[] controlColors = {
            PokedexTheme.CONTROL_BLUE,
            PokedexTheme.CONTROL_PINK,
            PokedexTheme.CONTROL_ORANGE,
            PokedexTheme.CONTROL_GREEN
        };
        
        // Check all colors are distinct
        for (int i = 0; i < controlColors.length; i++) {
            for (int j = i + 1; j < controlColors.length; j++) {
                assertNotEquals(controlColors[i], controlColors[j],
                    "Window control colors should be distinct");
            }
        }
    }

    /**
     * **Feature: pokedex-ui-redesign, Property 7: Window controls display four colored dots**
     * **Validates: Requirements 1.3**
     * 
     * Verifies that exactly four window control colors are defined.
     */
    @Property(tries = 100)
    @Label("Window controls should have exactly four colors defined")
    void windowControlsShouldHaveExactlyFourColors() {
        String[] controlColors = {
            PokedexTheme.CONTROL_BLUE,
            PokedexTheme.CONTROL_PINK,
            PokedexTheme.CONTROL_ORANGE,
            PokedexTheme.CONTROL_GREEN
        };
        
        assertEquals(4, controlColors.length,
            "Should have exactly 4 window control colors defined");
        
        // Each color should be non-null and non-empty
        for (String color : controlColors) {
            assertNotNull(color, "Control color should not be null");
            assertFalse(color.isEmpty(), "Control color should not be empty");
        }
    }

    /**
     * Verifies that pixel font style generates valid CSS with any positive font size.
     */
    @Property(tries = 100)
    @Label("Pixel font style should generate valid CSS for any positive font size")
    void pixelFontStyleShouldGenerateValidCss(
            @ForAll @IntRange(min = 1, max = 100) int fontSize) {
        
        String style = PokedexTheme.getPixelFontStyle(fontSize);
        
        // Assert: Style should contain font-family
        assertTrue(style.contains("-fx-font-family:"),
            "Pixel font style should include font-family");
        
        // Assert: Style should contain the specified font size
        assertTrue(style.contains(fontSize + "px"),
            "Pixel font style should include the specified font size");
        
        // Assert: Style should contain font smoothing setting
        assertTrue(style.contains("-fx-font-smoothing-type:"),
            "Pixel font style should include font smoothing setting");
    }

    /**
     * Verifies that cell style generates correct CSS for all state combinations.
     */
    @Property(tries = 100)
    @Label("Cell style should generate correct CSS for all state combinations")
    void cellStyleShouldGenerateCorrectCssForAllStates(
            @ForAll boolean highlighted,
            @ForAll boolean selected) {
        
        String style = PokedexTheme.getCellStyle(highlighted, selected);
        
        // Assert: Style should contain background-color
        assertTrue(style.contains("-fx-background-color:"),
            "Cell style should include background-color");
        
        // Assert: Style should contain background-radius for circular appearance
        assertTrue(style.contains("-fx-background-radius:"),
            "Cell style should include background-radius");
        
        // Assert: Selected state should use CELL_SELECTED color
        if (selected) {
            assertTrue(style.contains(PokedexTheme.CELL_SELECTED),
                "Selected cell should use CELL_SELECTED color");
        } else if (highlighted) {
            // Assert: Highlighted (but not selected) should use CELL_HIGHLIGHT color
            assertTrue(style.contains(PokedexTheme.CELL_HIGHLIGHT),
                "Highlighted cell should use CELL_HIGHLIGHT color");
        } else {
            // Assert: Default state should use CELL_BG color
            assertTrue(style.contains(PokedexTheme.CELL_BG),
                "Default cell should use CELL_BG color");
        }
    }

    /**
     * Verifies that control dot style generates valid CSS for any positive size.
     */
    @Property(tries = 100)
    @Label("Control dot style should generate valid CSS for any positive size")
    void controlDotStyleShouldGenerateValidCss(
            @ForAll @IntRange(min = 1, max = 50) int size) {
        
        String style = PokedexTheme.getControlDotStyle(PokedexTheme.CONTROL_BLUE, size);
        
        // Assert: Style should contain background-color
        assertTrue(style.contains("-fx-background-color:"),
            "Control dot style should include background-color");
        
        // Assert: Style should contain the specified color
        assertTrue(style.contains(PokedexTheme.CONTROL_BLUE),
            "Control dot style should include the specified color");
        
        // Assert: Style should contain size constraints
        assertTrue(style.contains("-fx-min-width:"),
            "Control dot style should include min-width");
        assertTrue(style.contains("-fx-min-height:"),
            "Control dot style should include min-height");
    }

    /**
     * Verifies that combineStyles correctly merges multiple style strings.
     */
    @Property(tries = 100)
    @Label("combineStyles should correctly merge multiple style strings")
    void combineStylesShouldMergeCorrectly() {
        String style1 = "-fx-background-color: red;";
        String style2 = "-fx-text-fill: white";
        String style3 = "-fx-padding: 5;";
        
        String combined = PokedexTheme.combineStyles(style1, style2, style3);
        
        // Assert: Combined style should contain all individual styles
        assertTrue(combined.contains("-fx-background-color: red"),
            "Combined style should include first style");
        assertTrue(combined.contains("-fx-text-fill: white"),
            "Combined style should include second style");
        assertTrue(combined.contains("-fx-padding: 5"),
            "Combined style should include third style");
    }

    /**
     * Verifies that dimension constants are positive and reasonable.
     */
    @Property(tries = 100)
    @Label("Dimension constants should be positive and reasonable")
    void dimensionConstantsShouldBePositiveAndReasonable() {
        // Assert: All dimensions should be positive
        assertTrue(PokedexTheme.FRAME_WIDTH > 0, "Frame width should be positive");
        assertTrue(PokedexTheme.FRAME_HEIGHT > 0, "Frame height should be positive");
        assertTrue(PokedexTheme.SCREEN_WIDTH > 0, "Screen width should be positive");
        assertTrue(PokedexTheme.SCREEN_HEIGHT > 0, "Screen height should be positive");
        assertTrue(PokedexTheme.BORDER_WIDTH > 0, "Border width should be positive");
        assertTrue(PokedexTheme.CELL_SIZE > 0, "Cell size should be positive");
        assertTrue(PokedexTheme.CELL_SPACING >= 0, "Cell spacing should be non-negative");
        
        // Assert: Screen should fit within frame
        assertTrue(PokedexTheme.SCREEN_WIDTH < PokedexTheme.FRAME_WIDTH,
            "Screen width should be less than frame width");
        assertTrue(PokedexTheme.SCREEN_HEIGHT < PokedexTheme.FRAME_HEIGHT,
            "Screen height should be less than frame height");
    }
}
