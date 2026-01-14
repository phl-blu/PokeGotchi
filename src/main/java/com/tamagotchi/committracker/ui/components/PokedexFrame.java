package com.tamagotchi.committracker.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import com.tamagotchi.committracker.domain.Commit;
import com.tamagotchi.committracker.domain.CommitHistory;
import com.tamagotchi.committracker.github.AccessTokenResponse;
import com.tamagotchi.committracker.github.GitHubOAuthService;
import com.tamagotchi.committracker.pokemon.EvolutionStage;
import com.tamagotchi.committracker.pokemon.PokemonSpecies;
import com.tamagotchi.committracker.ui.theme.PokedexTheme;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Main container component for the Pokedex-style UI.
 * Renders a retro handheld gaming device appearance with:
 * - Red/coral frame with dark navy borders
 * - Window controls (4 colored dots) in top-left corner
 * - Screen area with gray/purple background
 * - Screen indicator showing current screen name
 * - D-pad and action buttons at the bottom for navigation
 * 
 * Supports multiple screen modes:
 * - Selection mode: Shows Pokemon selection grid (first-time only)
 * - Pokemon mode: Shows selected Pokemon with stats
 * - History mode: Shows commit history log
 * - Statistics mode: Shows detailed statistics
 * 
 * Navigation:
 * - D-pad left/right: Cycle through screens (Pokemon → Statistics → History)
 * - Button B: Return to Pokemon screen from any other screen
 * 
 * Requirements: 1.1, 1.2, 1.3, 1.4, 2.5, 6.1, 6.2, 6.4, 6.5, 6.6, 6.7
 */
public class PokedexFrame extends StackPane {
    
    // Frame dimensions from PokedexTheme
    private static final int FRAME_WIDTH = PokedexTheme.FRAME_WIDTH;
    private static final int FRAME_HEIGHT = PokedexTheme.FRAME_HEIGHT;
    private static final int SCREEN_WIDTH = PokedexTheme.SCREEN_WIDTH;
    private static final int SCREEN_HEIGHT = PokedexTheme.SCREEN_HEIGHT;
    
    // Window control dot size
    private static final int CONTROL_DOT_SIZE = 8;
    private static final int CONTROL_DOT_SPACING = 4;
    
    // Window control dot hover size (slightly larger)
    private static final int CONTROL_DOT_HOVER_SIZE = 10;
    
    // Components
    private HBox windowControls;
    private StackPane screenArea;
    private PokedexControls controls;
    
    // Window control dots (for state management)
    private Region blueDot;
    private Region pinkDot;
    private Region orangeDot;
    private Region greenDot;
    
    // Window control handlers
    private Runnable onClosePressed;
    private Runnable onAlwaysOnTopPressed;
    private Runnable onSettingsPressed;
    private Runnable onToggleModePressed;
    
    // Always-on-top state
    private boolean isAlwaysOnTop = true;
    
    // Screen content
    private Node currentScreen;
    private GitHubAuthScreen authScreen;
    private PokedexSelectionScreen selectionScreen;
    private PokedexMainDisplay mainDisplay;
    private PokedexHistoryScreen historyScreen;
    private PokedexStatisticsScreen statisticsScreen;
    
    // Current screen mode
    private PokedexScreenMode currentMode = PokedexScreenMode.SELECTION;
    
    // Current Pokemon state
    private PokemonSpecies currentSpecies;
    private EvolutionStage currentStage;
    
    // Cached data for screen updates
    private CommitHistory cachedCommitHistory;
    private List<Commit> cachedCommits = new ArrayList<>();
    private int cachedXP = 0;
    private int cachedNextThreshold = 100;
    private int cachedStreak = 0;
    
    /**
     * Creates a new PokedexFrame with default styling.
     */
    public PokedexFrame() {
        initializeFrame();
    }
    
    /**
     * Initializes the frame layout and components.
     */
    private void initializeFrame() {
        // Set frame dimensions
        this.setPrefSize(FRAME_WIDTH, FRAME_HEIGHT);
        this.setMinSize(FRAME_WIDTH, FRAME_HEIGHT);
        this.setMaxSize(FRAME_WIDTH, FRAME_HEIGHT);
        
        // Apply frame styling (red/coral background with dark borders)
        this.setStyle(PokedexTheme.getFrameStyle());
        
        // Apply rounded corner clipping to prevent corner artifacts
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(FRAME_WIDTH, FRAME_HEIGHT);
        clip.setArcWidth(26); // 2x the corner radius (13px)
        clip.setArcHeight(26);
        this.setClip(clip);
        
        // Create main layout container with transparent background
        BorderPane mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(10));
        mainLayout.setStyle("-fx-background-color: transparent;");
        
        // Create and add window controls (top)
        windowControls = createWindowControls();
        
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setStyle("-fx-background-color: transparent;");
        topRow.getChildren().add(windowControls);
        
        VBox topSection = new VBox(8);
        topSection.setAlignment(Pos.TOP_LEFT);
        topSection.setStyle("-fx-background-color: transparent;");
        topSection.getChildren().add(topRow);
        mainLayout.setTop(topSection);
        
        // Create screen area (center)
        screenArea = createScreenArea();
        mainLayout.setCenter(screenArea);
        BorderPane.setMargin(screenArea, new Insets(8, 0, 8, 0));
        
        // Create and add controls (bottom)
        controls = new PokedexControls();
        wireNavigationHandlers();
        
        HBox bottomSection = new HBox();
        bottomSection.setAlignment(Pos.CENTER);
        bottomSection.setStyle("-fx-background-color: transparent;");
        bottomSection.getChildren().add(controls);
        mainLayout.setBottom(bottomSection);
        
        // Add main layout to frame
        this.getChildren().add(mainLayout);
    }
    
    /**
     * Wires up the D-pad and button handlers for screen navigation.
     * 
     * Requirements: 6.1, 6.2, 6.4
     */
    private void wireNavigationHandlers() {
        // D-pad left: Navigate to previous screen
        controls.setOnLeftPressed(this::navigateLeft);
        
        // D-pad right: Navigate to next screen
        controls.setOnRightPressed(this::navigateRight);
        
        // Button B: Return to Pokemon screen
        controls.setOnButtonBPressed(this::goToHome);
    }
    
    /**
     * Navigates to the previous screen in the cycle.
     * Cycle: POKEMON → HISTORY → STATISTICS → POKEMON
     * 
     * Requirements: 6.1
     */
    public void navigateLeft() {
        if (currentMode.isNavigable()) {
            PokedexScreenMode newMode = currentMode.navigateLeft();
            switchToScreen(newMode);
        }
    }
    
    /**
     * Navigates to the next screen in the cycle.
     * Cycle: POKEMON → STATISTICS → HISTORY → POKEMON
     * 
     * Requirements: 6.2
     */
    public void navigateRight() {
        if (currentMode.isNavigable()) {
            PokedexScreenMode newMode = currentMode.navigateRight();
            switchToScreen(newMode);
        }
    }
    
    /**
     * Returns to the Pokemon main display screen.
     * Called when Button B is pressed.
     * 
     * Requirements: 6.4
     */
    public void goToHome() {
        if (currentMode != PokedexScreenMode.SELECTION && currentMode != PokedexScreenMode.POKEMON) {
            switchToScreen(PokedexScreenMode.POKEMON);
        }
    }
    
    /**
     * Switches to the specified screen mode.
     * Preserves Pokemon state when switching screens.
     * 
     * @param mode The screen mode to switch to
     */
    private void switchToScreen(PokedexScreenMode mode) {
        if (mode == currentMode) {
            return;
        }
        
        currentMode = mode;
        
        switch (mode) {
            case POKEMON:
                if (currentSpecies != null) {
                    showMainDisplay(currentSpecies, currentStage);
                }
                break;
            case HISTORY:
                showHistoryScreen();
                break;
            case STATISTICS:
                showStatisticsScreen();
                break;
            case AUTH:
                // Auth screen is handled separately via showAuthScreen()
                break;
            case SELECTION:
                // Selection screen is handled separately
                break;
        }
    }
    
    /**
     * Shows the GitHub authentication screen.
     * Used for first-time users to authenticate with GitHub before Pokemon selection.
     * 
     * Requirements: 1.1, 1.5
     * 
     * @param oauthService The OAuth service to use for authentication
     * @param onSuccess Callback when authentication succeeds
     * @param onSkipped Callback when user skips authentication
     */
    public void showAuthScreen(GitHubOAuthService oauthService,
                               Consumer<AccessTokenResponse> onSuccess,
                               Runnable onSkipped) {
        // Clear current screen
        screenArea.getChildren().clear();
        
        // Create auth screen with callbacks
        authScreen = new GitHubAuthScreen(oauthService, onSuccess, onSkipped);
        
        currentScreen = authScreen;
        currentMode = PokedexScreenMode.AUTH;
        screenArea.getChildren().add(authScreen);
        
        // Start the authentication flow
        authScreen.startAuthentication();
    }
    
    /**
     * Checks if the authentication screen is currently displayed.
     * 
     * @return true if showing auth screen
     */
    public boolean isShowingAuthScreen() {
        return currentScreen instanceof GitHubAuthScreen;
    }
    
    /**
     * Gets the auth screen if it has been created.
     * 
     * @return The GitHubAuthScreen, or null if not created
     */
    public GitHubAuthScreen getAuthScreen() {
        return authScreen;
    }
    
    /**
     * Shows the history screen with commit log.
     * 
     * Requirements: 6.6
     */
    public void showHistoryScreen() {
        // Clear current screen
        screenArea.getChildren().clear();
        
        // Create history screen if not exists
        if (historyScreen == null) {
            historyScreen = new PokedexHistoryScreen();
        }
        
        // Always update with cached data (even if null/empty)
        historyScreen.updateCommitHistory(cachedCommitHistory);
        historyScreen.updatePokemonStatus(currentSpecies, currentStage, cachedXP, cachedStreak);
        
        currentScreen = historyScreen;
        currentMode = PokedexScreenMode.HISTORY;
        screenArea.getChildren().add(historyScreen);
    }
    
    /**
     * Shows the statistics screen with detailed stats.
     * 
     * Requirements: 6.7
     */
    public void showStatisticsScreen() {
        // Clear current screen
        screenArea.getChildren().clear();
        
        // Create statistics screen if not exists
        if (statisticsScreen == null) {
            statisticsScreen = new PokedexStatisticsScreen();
        }
        
        // Always update with cached data (even if null/empty)
        statisticsScreen.updateStatistics(cachedCommits, currentSpecies, currentStage, cachedStreak);
        
        currentScreen = statisticsScreen;
        currentMode = PokedexScreenMode.STATISTICS;
        screenArea.getChildren().add(statisticsScreen);
    }
    
    /**
     * Creates the window controls (4 colored dots) as functional buttons.
     * Colors and functions:
     * - Blue: Close application (save state first)
     * - Pink: Toggle always-on-top mode
     * - Orange: Open settings/preferences (future)
     * - Green: Toggle compact/expanded mode
     * 
     * Requirements: 1.3, 9.1
     * 
     * @return HBox containing the window control dots
     */
    private HBox createWindowControls() {
        HBox controlsBox = new HBox(CONTROL_DOT_SPACING);
        controlsBox.setAlignment(Pos.CENTER_LEFT);
        
        // Create 4 colored dots as functional buttons
        blueDot = createFunctionalControlDot(PokedexTheme.CONTROL_BLUE, "Close", () -> {
            if (onClosePressed != null) {
                onClosePressed.run();
            }
        });
        
        pinkDot = createFunctionalControlDot(PokedexTheme.CONTROL_PINK, "Always on Top", () -> {
            if (onAlwaysOnTopPressed != null) {
                onAlwaysOnTopPressed.run();
            }
        });
        // Update pink dot visual state based on always-on-top status
        updateAlwaysOnTopIndicator();
        
        orangeDot = createFunctionalControlDot(PokedexTheme.CONTROL_ORANGE, "Settings (Coming Soon)", () -> {
            if (onSettingsPressed != null) {
                onSettingsPressed.run();
            }
        });
        
        greenDot = createFunctionalControlDot(PokedexTheme.CONTROL_GREEN, "Toggle Compact Mode", () -> {
            if (onToggleModePressed != null) {
                onToggleModePressed.run();
            }
        });
        
        controlsBox.getChildren().addAll(blueDot, pinkDot, orangeDot, greenDot);
        
        return controlsBox;
    }
    
    /**
     * Creates a functional control dot with click handler, hover effects, and tooltip.
     * 
     * @param color The dot color (hex string)
     * @param tooltipText The tooltip text to display on hover
     * @param onClick The action to perform when clicked
     * @return Region styled as a functional colored dot
     */
    private Region createFunctionalControlDot(String color, String tooltipText, Runnable onClick) {
        Region dot = new Region();
        dot.setStyle(PokedexTheme.getControlDotStyle(color, CONTROL_DOT_SIZE));
        dot.setCursor(javafx.scene.Cursor.HAND);
        
        // Create tooltip
        Tooltip tooltip = new Tooltip(tooltipText);
        tooltip.setShowDelay(Duration.millis(300));
        tooltip.setStyle("-fx-font-size: 10px;");
        Tooltip.install(dot, tooltip);
        
        // Store original color for state restoration
        final String originalColor = color;
        
        // Hover effect - slight glow and size increase
        dot.setOnMouseEntered(e -> {
            // Create glow effect
            DropShadow glow = new DropShadow();
            glow.setColor(Color.web(originalColor));
            glow.setRadius(6);
            glow.setSpread(0.3);
            dot.setEffect(glow);
            
            // Increase size slightly
            dot.setStyle(PokedexTheme.getControlDotStyle(originalColor, CONTROL_DOT_HOVER_SIZE));
        });
        
        dot.setOnMouseExited(e -> {
            // Remove glow effect
            dot.setEffect(null);
            
            // Restore original size
            dot.setStyle(PokedexTheme.getControlDotStyle(originalColor, CONTROL_DOT_SIZE));
            
            // Re-apply always-on-top indicator if this is the pink dot
            if (dot == pinkDot) {
                updateAlwaysOnTopIndicator();
            }
        });
        
        // Pressed effect - darker color
        dot.setOnMousePressed(e -> {
            // Darken the color for pressed state
            String pressedColor = darkenColor(originalColor);
            dot.setStyle(PokedexTheme.getControlDotStyle(pressedColor, CONTROL_DOT_HOVER_SIZE));
        });
        
        dot.setOnMouseReleased(e -> {
            // Restore hover state (still hovering after release)
            dot.setStyle(PokedexTheme.getControlDotStyle(originalColor, CONTROL_DOT_HOVER_SIZE));
            
            // Execute click action
            if (onClick != null) {
                onClick.run();
            }
        });
        
        return dot;
    }
    
    /**
     * Darkens a hex color for pressed state effect.
     * 
     * @param hexColor The original hex color (e.g., "#4A90D9")
     * @return A darker version of the color
     */
    private String darkenColor(String hexColor) {
        try {
            Color color = Color.web(hexColor);
            Color darker = color.darker();
            return String.format("#%02X%02X%02X",
                (int) (darker.getRed() * 255),
                (int) (darker.getGreen() * 255),
                (int) (darker.getBlue() * 255));
        } catch (Exception e) {
            return hexColor; // Return original if parsing fails
        }
    }
    
    /**
     * Updates the visual indicator for always-on-top state on the pink dot.
     * When enabled: Shows a bright white glow/ring effect that stands out against the red frame.
     * When disabled: Shows a darker, muted appearance to indicate it's off.
     */
    private void updateAlwaysOnTopIndicator() {
        if (pinkDot != null) {
            if (isAlwaysOnTop) {
                // Show bright white glow when always-on-top is active
                // White stands out well against the red frame
                DropShadow ring = new DropShadow();
                ring.setColor(Color.WHITE);
                ring.setRadius(6);
                ring.setSpread(0.7);
                pinkDot.setEffect(ring);
                // Keep the pink color bright
                pinkDot.setStyle(PokedexTheme.getControlDotStyle(PokedexTheme.CONTROL_PINK, CONTROL_DOT_SIZE));
            } else {
                // Show muted/darker appearance when always-on-top is disabled
                pinkDot.setEffect(null);
                // Use a darker, grayed-out pink to indicate disabled state
                pinkDot.setStyle(PokedexTheme.getControlDotStyle("#9A6B7A", CONTROL_DOT_SIZE));
            }
        }
    }
    
    /**
     * Sets the always-on-top state and updates the visual indicator.
     * 
     * @param alwaysOnTop true if window should stay on top
     */
    public void setAlwaysOnTop(boolean alwaysOnTop) {
        this.isAlwaysOnTop = alwaysOnTop;
        updateAlwaysOnTopIndicator();
    }
    
    /**
     * Gets the current always-on-top state.
     * 
     * @return true if always-on-top is enabled
     */
    public boolean isAlwaysOnTop() {
        return isAlwaysOnTop;
    }
    
    /**
     * Sets the handler for the close (blue) button.
     * 
     * @param handler The action to perform when close is pressed
     */
    public void setOnClosePressed(Runnable handler) {
        this.onClosePressed = handler;
    }
    
    /**
     * Sets the handler for the always-on-top toggle (pink) button.
     * 
     * @param handler The action to perform when always-on-top is toggled
     */
    public void setOnAlwaysOnTopPressed(Runnable handler) {
        this.onAlwaysOnTopPressed = handler;
    }
    
    /**
     * Sets the handler for the settings (orange) button.
     * 
     * @param handler The action to perform when settings is pressed
     */
    public void setOnSettingsPressed(Runnable handler) {
        this.onSettingsPressed = handler;
    }
    
    /**
     * Sets the handler for the toggle mode (green) button.
     * 
     * @param handler The action to perform when toggle mode is pressed
     */
    public void setOnToggleModePressed(Runnable handler) {
        this.onToggleModePressed = handler;
    }
    
    /**
     * Creates the screen area with gray/purple background.
     * 
     * Requirements: 1.4
     * 
     * @return StackPane for the screen content
     */
    private StackPane createScreenArea() {
        StackPane screen = new StackPane();
        screen.setPrefSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        screen.setMinSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        screen.setMaxSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        screen.setStyle(PokedexTheme.getScreenStyle());
        screen.setAlignment(Pos.CENTER);
        
        return screen;
    }

    
    /**
     * Shows the Pokemon selection screen.
     * Used for first-time users to choose their starter Pokemon.
     * 
     * Requirements: 2.1, 2.5
     * 
     * @param onSelected Callback when a Pokemon is selected
     */
    public void showSelectionScreen(Consumer<PokemonSpecies> onSelected) {
        // Clear current screen
        screenArea.getChildren().clear();
        
        // Create selection screen with callback that transitions to main display
        selectionScreen = new PokedexSelectionScreen(species -> {
            // Store selected species
            this.currentSpecies = species;
            this.currentStage = EvolutionStage.EGG;
            
            // Notify external callback
            if (onSelected != null) {
                onSelected.accept(species);
            }
            
            // Transition to main display
            showMainDisplay(species);
        });
        
        currentScreen = selectionScreen;
        currentMode = PokedexScreenMode.SELECTION;
        screenArea.getChildren().add(selectionScreen);
    }
    
    /**
     * Shows the main display screen with the selected Pokemon.
     * Used for returning users who have already selected a Pokemon.
     * 
     * Requirements: 2.4
     * 
     * @param species The Pokemon species to display
     */
    public void showMainDisplay(PokemonSpecies species) {
        showMainDisplay(species, EvolutionStage.EGG);
    }
    
    /**
     * Shows the main display screen with the selected Pokemon at a specific stage.
     * 
     * @param species The Pokemon species to display
     * @param stage The evolution stage to display
     */
    public void showMainDisplay(PokemonSpecies species, EvolutionStage stage) {
        // Store current state
        this.currentSpecies = species;
        this.currentStage = stage;
        
        // Clear current screen
        screenArea.getChildren().clear();
        
        // Create main display
        mainDisplay = new PokedexMainDisplay(species, stage);
        
        // Update with cached stats
        mainDisplay.updateStats(cachedXP, cachedNextThreshold, cachedStreak, stage);
        
        currentScreen = mainDisplay;
        currentMode = PokedexScreenMode.POKEMON;
        screenArea.getChildren().add(mainDisplay);
    }
    
    /**
     * Updates the stats display on the main display screen.
     * Also caches the values for use when switching screens.
     * 
     * @param xp Current XP amount
     * @param nextThreshold XP needed for next evolution
     * @param streak Current commit streak in days
     * @param stage Current evolution stage
     */
    public void updateStats(int xp, int nextThreshold, int streak, EvolutionStage stage) {
        // Cache values for screen switching
        this.cachedXP = xp;
        this.cachedNextThreshold = nextThreshold;
        this.cachedStreak = streak;
        this.currentStage = stage;
        
        if (mainDisplay != null) {
            mainDisplay.updateStats(xp, nextThreshold, streak, stage);
        }
    }
    
    /**
     * Updates the commit history data.
     * Caches the history for use when switching to history screen.
     * 
     * @param history The commit history to cache and display
     */
    public void updateCommitHistory(CommitHistory history) {
        this.cachedCommitHistory = history;
        if (historyScreen != null) {
            historyScreen.updateCommitHistory(history);
        }
    }
    
    /**
     * Updates the commits list for statistics.
     * Caches the commits for use when switching to statistics screen.
     * 
     * @param commits The list of commits to cache and display
     */
    public void updateCommits(List<Commit> commits) {
        this.cachedCommits = commits != null ? new ArrayList<>(commits) : new ArrayList<>();
        if (statisticsScreen != null && currentSpecies != null) {
            statisticsScreen.updateStatistics(cachedCommits, currentSpecies, currentStage, cachedStreak);
        }
    }
    
    /**
     * Updates the Pokemon name on the main display screen.
     * 
     * @param name The Pokemon name to display
     */
    public void updatePokemonName(String name) {
        if (mainDisplay != null) {
            mainDisplay.updateName(name);
        }
    }
    
    /**
     * Gets the window controls component for testing.
     * 
     * @return The HBox containing window control dots
     */
    public HBox getWindowControls() {
        return windowControls;
    }
    
    /**
     * Gets the number of window control dots.
     * 
     * @return The count of control dots (should be 4)
     */
    public int getWindowControlCount() {
        return windowControls != null ? windowControls.getChildren().size() : 0;
    }
    
    /**
     * Gets the screen area component.
     * 
     * @return The StackPane containing the current screen
     */
    public StackPane getScreenArea() {
        return screenArea;
    }
    
    /**
     * Gets the current screen content.
     * 
     * @return The current screen Node (selection or main display)
     */
    public Node getCurrentScreen() {
        return currentScreen;
    }
    
    /**
     * Gets the Pokedex controls component.
     * 
     * @return The PokedexControls (D-pad and buttons)
     */
    public PokedexControls getControls() {
        return controls;
    }
    
    /**
     * Gets the selection screen if currently displayed.
     * 
     * @return The PokedexSelectionScreen, or null if not showing
     */
    public PokedexSelectionScreen getSelectionScreen() {
        return selectionScreen;
    }
    
    /**
     * Gets the main display if currently displayed.
     * 
     * @return The PokedexMainDisplay, or null if not showing
     */
    public PokedexMainDisplay getMainDisplay() {
        return mainDisplay;
    }
    
    /**
     * Gets the current Pokemon species.
     * 
     * @return The current PokemonSpecies, or null if none selected
     */
    public PokemonSpecies getCurrentSpecies() {
        return currentSpecies;
    }
    
    /**
     * Gets the current evolution stage.
     * 
     * @return The current EvolutionStage
     */
    public EvolutionStage getCurrentStage() {
        return currentStage;
    }
    
    /**
     * Checks if the selection screen is currently displayed.
     * 
     * @return true if showing selection screen
     */
    public boolean isShowingSelectionScreen() {
        return currentScreen instanceof PokedexSelectionScreen;
    }
    
    /**
     * Checks if the main display is currently displayed.
     * 
     * @return true if showing main display
     */
    public boolean isShowingMainDisplay() {
        return currentScreen instanceof PokedexMainDisplay;
    }
    
    /**
     * Checks if the history screen is currently displayed.
     * 
     * @return true if showing history screen
     */
    public boolean isShowingHistoryScreen() {
        return currentScreen instanceof PokedexHistoryScreen;
    }
    
    /**
     * Checks if the statistics screen is currently displayed.
     * 
     * @return true if showing statistics screen
     */
    public boolean isShowingStatisticsScreen() {
        return currentScreen instanceof PokedexStatisticsScreen;
    }
    
    /**
     * Gets the current screen mode.
     * 
     * @return The current PokedexScreenMode
     */
    public PokedexScreenMode getCurrentMode() {
        return currentMode;
    }
    
    /**
     * Gets the history screen if it has been created.
     * 
     * @return The PokedexHistoryScreen, or null if not created
     */
    public PokedexHistoryScreen getHistoryScreen() {
        return historyScreen;
    }
    
    /**
     * Gets the statistics screen if it has been created.
     * 
     * @return The PokedexStatisticsScreen, or null if not created
     */
    public PokedexStatisticsScreen getStatisticsScreen() {
        return statisticsScreen;
    }
    
    /**
     * Gets the Pokemon display component from the main display.
     * Useful for triggering animations or checking evolution status.
     * 
     * @return The PokemonDisplayComponent, or null if not showing main display
     */
    public PokemonDisplayComponent getPokemonDisplay() {
        return mainDisplay != null ? mainDisplay.getPokemonDisplay() : null;
    }
    
    /**
     * Cleans up resources when the frame is no longer needed.
     */
    public void cleanup() {
        if (mainDisplay != null) {
            mainDisplay.cleanup();
        }
    }
    
    /**
     * Updates the Pokemon state animation (happy, sad, thriving, content).
     * Triggers the appropriate animation based on the new state.
     * 
     * Requirements: 9.3
     * 
     * @param state The new Pokemon state
     */
    public void updatePokemonState(com.tamagotchi.committracker.pokemon.PokemonState state) {
        if (mainDisplay != null && state != null) {
            mainDisplay.updatePokemonState(state);
        }
    }
    
    /**
     * Triggers a commit animation on the Pokemon display.
     * For eggs: plays shake animation
     * For Pokemon: plays happy animation
     * 
     * Requirements: 9.1
     * 
     * @param totalXP Total accumulated XP
     * @param streakDays Current commit streak in days
     */
    public void triggerCommitAnimation(int totalXP, int streakDays) {
        if (mainDisplay != null) {
            mainDisplay.triggerCommitAnimation(totalXP, streakDays);
        }
    }
    
    /**
     * Checks evolution requirements and triggers evolution if conditions are met.
     * Updates the current stage if evolution occurs.
     * 
     * Requirements: 9.2
     * 
     * @param xpLevel Current XP level
     * @param streakDays Current commit streak in days
     * @return true if evolution was triggered
     */
    public boolean checkEvolutionRequirements(int xpLevel, int streakDays) {
        if (mainDisplay != null) {
            boolean evolved = mainDisplay.checkEvolutionRequirements(xpLevel, streakDays);
            if (evolved) {
                this.currentStage = mainDisplay.getCurrentStage();
                this.currentSpecies = mainDisplay.getCurrentSpecies();
            }
            return evolved;
        }
        return false;
    }
    
    /**
     * Sets the evolution listener to be notified when evolution completes.
     * The listener is called after the evolution animation finishes and
     * the Pokemon name label has been updated.
     * 
     * @param listener The evolution listener
     */
    public void setEvolutionListener(PokemonDisplayComponent.EvolutionListener listener) {
        if (mainDisplay != null) {
            mainDisplay.setEvolutionListener((newSpecies, newStage) -> {
                // Update internal state
                this.currentSpecies = newSpecies;
                this.currentStage = newStage;
                
                // Notify external listener
                if (listener != null) {
                    listener.onEvolutionComplete(newSpecies, newStage);
                }
            });
        }
    }
}
