# UI Customization Guide

This guide explains how to customize the appearance and design of the Tamagotchi Commit Tracker widget.

## Theme System

The application uses a centralized theme system located in `src/main/java/com/tamagotchi/committracker/ui/theme/UITheme.java`. This makes it easy to modify colors, fonts, spacing, and other visual elements without changing the core UI logic.

## Current Customizable Elements

### Colors
- **Background Colors**: Compact mode (transparent), expanded mode, status sections, commit entries
- **Text Colors**: Primary (white), secondary (light gray), tertiary (gray), XP color (light green)
- **Border Radius**: Large (10px), small (8px), tiny (5px)

### Typography
- **Font Family**: System font (can be changed to custom fonts)
- **Font Sizes**: Header (14px), normal (12px), small (11px), tiny (10px), micro (9px)
- **Font Weights**: Bold for headers, medium for commit messages, normal for details

### Spacing & Layout
- **Spacing**: Large (10px), medium (5px), small (2px)
- **Padding**: Large (10px), medium (8px), small (5px)

## How to Customize

### Method 1: Direct Theme Modification
1. Open `src/main/java/com/tamagotchi/committracker/ui/theme/UITheme.java`
2. Modify the static constants for colors, fonts, or spacing
3. Recompile the application

Example - Change to a dark blue theme:
```java
public static final String EXPANDED_BACKGROUND = "rgba(15, 25, 45, 0.95)";
public static final String STATUS_SECTION_BACKGROUND = "rgba(25, 35, 55, 0.8)";
public static final String COMMIT_ENTRY_BACKGROUND = "rgba(35, 45, 65, 0.7)";
```

### Method 2: Runtime Theme Loading (Future)
The `UITheme` class includes placeholder methods for future enhancements:
- `loadCustomTheme(String themeName)` - Load themes from external files
- `applyCustomColors(...)` - Apply color schemes at runtime

## Customization Examples

### Example 1: Light Theme
```java
// Light theme colors
public static final String EXPANDED_BACKGROUND = "rgba(240, 240, 245, 0.95)";
public static final String PRIMARY_TEXT_COLOR = "black";
public static final String SECONDARY_TEXT_COLOR = "darkgray";
```

### Example 2: Gaming Theme
```java
// Gaming-inspired colors
public static final String EXPANDED_BACKGROUND = "rgba(0, 20, 0, 0.95)";
public static final String XP_COLOR = "lime";
public static final String PRIMARY_TEXT_COLOR = "lightgreen";
```

### Example 3: Custom Fonts
```java
// Custom font family
public static final String PRIMARY_FONT = "Consolas"; // Monospace font
public static final int HEADER_FONT_SIZE = 16; // Larger headers
```

## UI Components That Use Themes

### HistoryTab
- Status section background and text colors
- Commit entry styling and colors
- Font sizes and spacing throughout

### WidgetWindow
- Expanded layout background
- Pokemon status box styling
- Overall spacing and padding

## Future Customization Features

The theme system is designed to support future enhancements:

1. **External Theme Files**: Load themes from JSON or CSS files
2. **Runtime Theme Switching**: Change themes without restarting
3. **User Preferences**: Save and load user-selected themes
4. **Custom Animations**: Configurable animation speeds and effects
5. **Layout Variants**: Different layout options (vertical, horizontal, etc.)

## Adding New Customizable Elements

To add new customizable elements:

1. Add constants to `UITheme.java`
2. Create helper methods for complex styles
3. Update UI components to use the new theme elements
4. Test with different values to ensure flexibility

Example:
```java
// In UITheme.java
public static final String POKEMON_BORDER_COLOR = "gold";
public static final int POKEMON_BORDER_WIDTH = 2;

public static String getPokemonBorderStyle() {
    return String.format("-fx-border-color: %s; -fx-border-width: %dpx;", 
        POKEMON_BORDER_COLOR, POKEMON_BORDER_WIDTH);
}

// In UI component
pokemonDisplay.setStyle(UITheme.getPokemonBorderStyle());
```

## Tips for Custom Designs

1. **Maintain Readability**: Ensure text remains readable with your color choices
2. **Test Different Sizes**: Verify the design works in both compact and expanded modes
3. **Consider Accessibility**: Use sufficient color contrast for users with visual impairments
4. **Keep Performance in Mind**: Complex styles may impact animation performance
5. **Backup Original**: Keep a copy of the original theme values before making changes

## Uploading Custom Designs

When you're ready to upload your custom design:

1. Take screenshots of your customized widget in both modes
2. Export your modified `UITheme.java` file
3. Document any additional changes made to other UI components
4. Include notes about the design inspiration and intended use case

The modular theme system makes it easy to share and apply different visual styles while maintaining the core functionality of the application.