package com.tamagotchi.committracker.ui.widget;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import com.tamagotchi.committracker.config.AppConfig;
import com.tamagotchi.committracker.domain.Commit;
import com.tamagotchi.committracker.domain.CommitHistory;
import com.tamagotchi.committracker.util.FileUtils;
import com.tamagotchi.committracker.util.WindowsIntegration;
import com.tamagotchi.committracker.ui.components.HistoryTab;
import com.tamagotchi.committracker.ui.components.PokemonDisplayComponent;
import com.tamagotchi.committracker.ui.components.PokemonSelectionScreen;
import com.tamagotchi.committracker.pokemon.PokemonSelectionData;
import com.tamagotchi.committracker.pokemon.PokemonSpecies;
import com.tamagotchi.committracker.pokemon.PokemonState;
import com.tamagotchi.committracker.pokemon.EvolutionStage;
import com.tamagotchi.committracker.pokemon.XPSystem;
import com.tamagotchi.committracker.pokemon.PokemonStateManager;
import com.tamagotchi.committracker.ui.theme.UITheme;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * Main UI container with transparent background and drag functionality.
 * Handles switching between compact and expanded modes.
 * Enhanced with Windows system integration for native behavior.
 */
public class WidgetWindow {
    private Stage stage;
    private Scene scene;
    private StackPane root;
    private boolean isCompactMode = false; // Start in expanded mode by default
    
    // Pokemon display component
    private PokemonDisplayComponent pokemonDisplay;
    
    // Expanded mode components
    private BorderPane expandedLayout;
    private HistoryTab historyTab;
    private VBox pokemonStatusBox;
    
    // Commit history data
    private CommitHistory commitHistory;
    
    // Real Pokemon state management (not testing)
    private XPSystem xpSystem;
    private PokemonStateManager pokemonStateManager;
    
    // Reference to commit service for manual scanning
    private com.tamagotchi.committracker.git.CommitService commitService;
    
    // For dragging functionality
    private double xOffset = 0;
    private double yOffset = 0;
    
    // Position persistence
    private static final String POSITION_FILE = "widget-position.properties";
    private static final String X_KEY = "widget.x";
    private static final String Y_KEY = "widget.y";
    
    // Pokemon selection
    private PokemonSelectionData selectionData;
    private Consumer<PokemonSpecies> onPokemonSelected;
    
    // Windows integration
    private boolean windowsIntegrationEnabled = false;
    
    public WidgetWindow(Stage primaryStage) {
        this.stage = primaryStage;
        this.selectionData = new PokemonSelectionData();
        this.xpSystem = new XPSystem();
        this.pokemonStateManager = new PokemonStateManager();
        initialize();
    }
    
    /**
     * Creates a WidgetWindow with a callback for when Pokemon is selected.
     * 
     * @param primaryStage The primary stage
     * @param onPokemonSelected Callback when Pokemon is selected (for first-time users)
     */
    public WidgetWindow(Stage primaryStage, Consumer<PokemonSpecies> onPokemonSelected) {
        this.stage = primaryStage;
        this.selectionData = new PokemonSelectionData();
        this.onPokemonSelected = onPokemonSelected;
        this.xpSystem = new XPSystem();
        this.pokemonStateManager = new PokemonStateManager();
        initialize();
    }
    
    /**
     * Creates a WidgetWindow with external XP and state management systems.
     * This constructor allows the main application to provide shared instances.
     * 
     * @param primaryStage The primary stage
     * @param onPokemonSelected Callback when Pokemon is selected
     * @param xpSystem The XP system instance
     * @param pokemonStateManager The Pokemon state manager instance
     */
    public WidgetWindow(Stage primaryStage, Consumer<PokemonSpecies> onPokemonSelected, 
                       XPSystem xpSystem, PokemonStateManager pokemonStateManager) {
        this.stage = primaryStage;
        this.selectionData = new PokemonSelectionData();
        this.onPokemonSelected = onPokemonSelected;
        this.xpSystem = xpSystem != null ? xpSystem : new XPSystem();
        this.pokemonStateManager = pokemonStateManager != null ? pokemonStateManager : new PokemonStateManager();
        initialize();
    }
    
    /**
     * Set up transparent stage and initial pet display with Windows integration
     */
    public void initialize() {
        // Configure stage properties
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);
        stage.setResizable(false);
        
        // Enable Windows system integration
        enableWindowsIntegration();
        
        // Create root container
        root = new StackPane();
        root.setStyle(UITheme.getTransparentStyle());
        
        // Initialize commit history
        commitHistory = new CommitHistory();
        
        // Initialize expanded mode components
        initializeExpandedModeComponents();
        
        // Only create Pokemon display if user has already selected a starter
        // For first-time users, the display will be created after selection
        if (selectionData.hasSelectedStarter()) {
            PokemonSpecies selectedSpecies = selectionData.getSelectedStarter();
            
            // Create Pokemon display component (starts as egg, will evolve based on initial scan)
            pokemonDisplay = new PokemonDisplayComponent(
                selectedSpecies, EvolutionStage.EGG, PokemonState.CONTENT
            );
            
            // Start in expanded mode by default - add Pokemon to status box
            pokemonStatusBox.getChildren().clear();
            pokemonStatusBox.getChildren().add(pokemonDisplay);
            
            // Add expanded layout to root (start in expanded mode)
            root.getChildren().add(expandedLayout);
        } else {
            // For first-time users, still start in expanded mode but without Pokemon
            root.getChildren().add(expandedLayout);
        }
        
        // Create scene with transparent background - start in expanded mode
        scene = new Scene(root, AppConfig.EXPANDED_WIDTH, AppConfig.EXPANDED_HEIGHT);
        scene.setFill(Color.TRANSPARENT);
        
        stage.setScene(scene);
        
        // Make sure the scene can receive keyboard focus
        root.setFocusTraversable(true);
        root.requestFocus();
        
        // Enable dragging
        enableDragging();
        
        // Restore saved position
        restorePosition();
        
        // Save position when window is moved
        stage.xProperty().addListener((obs, oldVal, newVal) -> savePosition());
        stage.yProperty().addListener((obs, oldVal, newVal) -> savePosition());
        
        // Handle window closing to save position and state
        stage.setOnCloseRequest(e -> {
            savePosition();
            saveWindowState();
            Platform.exit();
        });
        
        // Add click handler to toggle mode
        root.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) { // Double-click
                toggleMode();
            } else { // Single click - show Pokemon info
                showPokemonInfo();
            }
        });
        
        // Add keyboard shortcuts for testing
        // TODO: REMOVE THESE TESTING SHORTCUTS BEFORE PRODUCTION - See TODO.md
        scene.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case E: // Press 'E' to force evolution for testing
                    if (pokemonDisplay != null) {
                        System.out.println("🧪 TESTING: 'E' pressed - Forcing evolution");
                        pokemonDisplay.forceEvolutionForTesting();
                    }
                    break;
                case R: // Press 'R' to reset Pokemon to egg stage for testing
                    if (pokemonDisplay != null) {
                        System.out.println("🧪 TESTING: 'R' pressed - Resetting Pokemon to egg stage");
                        pokemonDisplay.forceDeevolutionToEggForTesting();
                        resetTestingXP(); // Also reset the testing XP accumulator
                    }
                    break;
                case C: // Press 'C' to simulate a commit for testing egg animations
                    simulateCommitForTesting();
                    break;
                case H: // Press 'H' to make Pokemon happy
                    if (pokemonDisplay != null) {
                        pokemonDisplay.updateState(PokemonState.HAPPY);
                        System.out.println("🧪 TESTING: 'H' pressed - Pokemon state changed to HAPPY");
                    }
                    break;
                case S: // Press 'S' to make Pokemon sad
                    if (pokemonDisplay != null) {
                        pokemonDisplay.updateState(PokemonState.SAD);
                        System.out.println("🧪 TESTING: 'S' pressed - Pokemon state changed to SAD");
                    }
                    break;
                case T: // Press 'T' to make Pokemon thriving
                    if (pokemonDisplay != null) {
                        pokemonDisplay.updateState(PokemonState.THRIVING);
                        System.out.println("🧪 TESTING: 'T' pressed - Pokemon state changed to THRIVING");
                    }
                    break;
                case I: // Press 'I' to show Pokemon info
                    System.out.println("🧪 TESTING: 'I' pressed - Showing Pokemon info");
                    showPokemonInfo();
                    break;
                case F: // Press 'F' to force repository scan for testing
                    System.out.println("🧪 TESTING: 'F' pressed - Forcing repository scan");
                    forceRepositoryScan();
                    break;
            }
        });
    }
    
    /**
     * Allow user to drag widget to new desktop position
     */
    public void enableDragging() {
        root.setOnMousePressed((MouseEvent event) -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        
        root.setOnMouseDragged((MouseEvent event) -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
    }
    
    /**
     * Initializes the expanded mode UI components.
     * Creates the history tab and Pokemon status display for expanded view.
     */
    private void initializeExpandedModeComponents() {
        // Create history tab
        historyTab = new HistoryTab();
        
        // Create expanded layout (BorderPane for better organization)
        expandedLayout = new BorderPane();
        expandedLayout.setStyle(UITheme.getExpandedLayoutStyle());
        expandedLayout.setPadding(new Insets(UITheme.LARGE_PADDING));
        
        // Pokemon display area at top
        pokemonStatusBox = new VBox(UITheme.MEDIUM_SPACING);
        pokemonStatusBox.setAlignment(Pos.CENTER);
        pokemonStatusBox.setPadding(new Insets(UITheme.MEDIUM_SPACING));
        pokemonStatusBox.setMinHeight(100);
        pokemonStatusBox.setMaxHeight(100);
        
        // History tab in center
        expandedLayout.setTop(pokemonStatusBox);
        expandedLayout.setCenter(historyTab);
    }
    
    /**
     * Switch between compact and expanded modes.
     * Compact mode shows only the Pokemon animation.
     * Expanded mode shows Pokemon status and commit history.
     */
    public void toggleMode() {
        if (isCompactMode) {
            // Switch to expanded mode
            switchToExpandedMode();
        } else {
            // Switch to compact mode
            switchToCompactMode();
        }
    }
    
    /**
     * Switches to expanded mode (320x450px) showing commit history.
     */
    private void switchToExpandedMode() {
        // Update Pokemon status in history tab with real data
        if (pokemonDisplay != null && historyTab != null) {
            int realXP = xpSystem != null ? xpSystem.getCurrentXP() : 0;
            int realStreak = commitHistory != null ? commitHistory.getCurrentStreak() : 0;
            
            historyTab.updatePokemonStatus(
                pokemonDisplay.getCurrentSpecies(),
                pokemonDisplay.getCurrentStage(),
                realXP,
                realStreak
            );
        }
        
        // Update commit history display
        if (historyTab != null) {
            historyTab.updateCommitHistory(commitHistory);
        }
        
        // Move Pokemon display to expanded layout
        if (pokemonDisplay != null) {
            root.getChildren().remove(pokemonDisplay);
            pokemonStatusBox.getChildren().clear();
            pokemonStatusBox.getChildren().add(pokemonDisplay);
        }
        
        // Add expanded layout to root
        root.getChildren().clear();
        root.getChildren().add(expandedLayout);
        
        // Resize window
        root.setPrefSize(AppConfig.EXPANDED_WIDTH, AppConfig.EXPANDED_HEIGHT);
        stage.setWidth(AppConfig.EXPANDED_WIDTH);
        stage.setHeight(AppConfig.EXPANDED_HEIGHT);
        
        isCompactMode = false;
        System.out.println("📖 Switched to expanded mode - XP: " + 
            (xpSystem != null ? xpSystem.getCurrentXP() : 0) + 
            ", Streak: " + (commitHistory != null ? commitHistory.getCurrentStreak() : 0) + " days");
    }
    
    /**
     * Switches to compact mode (80x80px) showing only Pokemon.
     */
    private void switchToCompactMode() {
        // Move Pokemon display back to root
        if (pokemonDisplay != null) {
            pokemonStatusBox.getChildren().remove(pokemonDisplay);
            root.getChildren().clear();
            root.getChildren().add(pokemonDisplay);
        } else {
            root.getChildren().clear();
        }
        
        // Ensure completely transparent background in compact mode
        root.setStyle("-fx-background-color: transparent;");
        
        // Also ensure the scene background is transparent
        if (scene != null) {
            scene.setFill(Color.TRANSPARENT);
        }
        
        // Resize window
        root.setPrefSize(AppConfig.COMPACT_WIDTH, AppConfig.COMPACT_HEIGHT);
        stage.setWidth(AppConfig.COMPACT_WIDTH);
        stage.setHeight(AppConfig.COMPACT_HEIGHT);
        
        isCompactMode = true;
        System.out.println("📦 Switched to compact mode with fully transparent background");
    }
    
    /**
     * Persist widget position for next startup
     */
    public void savePosition() {
        try {
            Properties props = new Properties();
            props.setProperty(X_KEY, String.valueOf(stage.getX()));
            props.setProperty(Y_KEY, String.valueOf(stage.getY()));
            FileUtils.saveProperties(props, POSITION_FILE);
        } catch (IOException e) {
            // Log error but don't fail - position saving is not critical
            System.err.println("Failed to save widget position: " + e.getMessage());
        }
    }
    
    /**
     * Load saved widget position on startup.
     * If no saved position or position is off-screen, centers on screen.
     */
    public void restorePosition() {
        try {
            Properties props = FileUtils.loadProperties(POSITION_FILE);
            if (props.containsKey(X_KEY) && props.containsKey(Y_KEY)) {
                double x = Double.parseDouble(props.getProperty(X_KEY));
                double y = Double.parseDouble(props.getProperty(Y_KEY));
                
                // Get screen bounds to validate position
                javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
                
                // Ensure position is within screen bounds (with some margin)
                if (x >= 0 && y >= 0 && 
                    x < screenBounds.getWidth() - 50 && 
                    y < screenBounds.getHeight() - 50) {
                    stage.setX(x);
                    stage.setY(y);
                    
                    // Also restore other window state
                    restoreWindowState();
                    return;
                }
            }
        } catch (IOException | NumberFormatException e) {
            // Fall through to center on screen
        }
        
        // Default: center on screen
        centerOnScreen();
    }
    
    /**
     * Centers the widget on the screen.
     */
    public void centerOnScreen() {
        javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        stage.setX((screenBounds.getWidth() - AppConfig.COMPACT_WIDTH) / 2);
        stage.setY((screenBounds.getHeight() - AppConfig.COMPACT_HEIGHT) / 2);
    }
    
    /**
     * Get the root container for adding UI components
     */
    public StackPane getRoot() {
        return root;
    }
    
    /**
     * Get the stage for external configuration
     */
    public Stage getStage() {
        return stage;
    }
    
    /**
     * Check if widget is in compact mode
     */
    public boolean isCompactMode() {
        return isCompactMode;
    }
    
    /**
     * Show the widget window
     */
    public void show() {
        stage.show();
    }
    
    /**
     * Shows current Pokemon information when clicked.
     */
    private void showPokemonInfo() {
        if (pokemonDisplay != null) {
            System.out.println("🎮 Pokemon Info:");
            System.out.println("   Species: " + pokemonDisplay.getCurrentSpecies());
            System.out.println("   Stage: " + pokemonDisplay.getCurrentStage());
            System.out.println("   State: " + pokemonDisplay.getCurrentState());
            System.out.println("   Evolution in progress: " + pokemonDisplay.isEvolutionInProgress());
            
            // Debug: Show current XP and streak data
            int realXP = xpSystem != null ? xpSystem.getCurrentXP() : 0;
            int realStreak = commitHistory != null ? commitHistory.getCurrentStreak() : 0;
            System.out.println("   Real XP: " + realXP);
            System.out.println("   Real Streak: " + realStreak + " days");
            
            if (commitHistory != null) {
                System.out.println("   Total commits in history: " + commitHistory.getRecentCommits().size());
                System.out.println("   Last commit time: " + commitHistory.getLastCommitTime());
                System.out.println("   Daily commit counts: " + commitHistory.getDailyCommitCounts().size() + " days");
            }
        }
    }
    
    /**
     * Update Pokemon display based on current state
     */
    public void updatePetDisplay() {
        // This method can be called by other components to update the Pokemon
        if (pokemonDisplay != null) {
            // For now, just refresh the current state
            pokemonDisplay.updateState(pokemonDisplay.getCurrentState());
        }
    }
    
    /**
     * Get the Pokemon display component for external access
     */
    public PokemonDisplayComponent getPokemonDisplay() {
        return pokemonDisplay;
    }
    
    /**
     * Gets the history tab component for external access.
     * 
     * @return The HistoryTab component
     */
    public HistoryTab getHistoryTab() {
        return historyTab;
    }
    
    /**
     * Gets the current commit history.
     * 
     * @return The CommitHistory object
     */
    public CommitHistory getCommitHistory() {
        return commitHistory;
    }
    
    /**
     * Updates the commit history with new commits.
     * 
     * @param newCommits List of new commits to add
     */
    public void addCommits(List<Commit> newCommits) {
        if (newCommits != null) {
            for (Commit commit : newCommits) {
                commitHistory.addCommit(commit);
            }
            commitHistory.calculateCurrentStreak();
            commitHistory.calculateAverageCommitsPerDay();
            
            // Update history tab if in expanded mode
            if (!isCompactMode && historyTab != null) {
                historyTab.updateCommitHistory(commitHistory);
                if (pokemonDisplay != null) {
                    int realXP = xpSystem != null ? xpSystem.getCurrentXP() : 0;
                    int realStreak = commitHistory.getCurrentStreak();
                    
                    historyTab.updatePokemonStatus(
                        pokemonDisplay.getCurrentSpecies(),
                        pokemonDisplay.getCurrentStage(),
                        realXP,
                        realStreak
                    );
                }
            }
        }
    }
    
    /**
     * Sets the commit history directly.
     * 
     * @param history The CommitHistory to set
     */
    public void setCommitHistory(CommitHistory history) {
        if (history != null) {
            this.commitHistory = history;
            
            // Update history tab if in expanded mode
            if (!isCompactMode && historyTab != null) {
                historyTab.updateCommitHistory(commitHistory);
                
                // Also update Pokemon status with current data
                if (pokemonDisplay != null) {
                    int realXP = xpSystem != null ? xpSystem.getCurrentXP() : 0;
                    int realStreak = commitHistory.getCurrentStreak();
                    
                    historyTab.updatePokemonStatus(
                        pokemonDisplay.getCurrentSpecies(),
                        pokemonDisplay.getCurrentStage(),
                        realXP,
                        realStreak
                    );
                }
            }
        }
    }
    
    /**
     * Updates the Pokemon status display with current real data.
     * Call this method when XP or streak changes to refresh the UI.
     */
    public void updatePokemonStatusDisplay() {
        if (!isCompactMode && historyTab != null && pokemonDisplay != null) {
            int realXP = xpSystem != null ? xpSystem.getCurrentXP() : 0;
            int realStreak = commitHistory != null ? commitHistory.getCurrentStreak() : 0;
            
            historyTab.updatePokemonStatus(
                pokemonDisplay.getCurrentSpecies(),
                pokemonDisplay.getCurrentStage(),
                realXP,
                realStreak
            );
            
            // Also refresh the commit history display
            if (commitHistory != null) {
                historyTab.updateCommitHistory(commitHistory);
            }
        }
    }
    
    /**
     * Gets the XP system for external access.
     * 
     * @return The XPSystem instance
     */
    public XPSystem getXpSystem() {
        return xpSystem;
    }
    
    /**
     * Gets the Pokemon state manager for external access.
     * 
     * @return The PokemonStateManager instance
     */
    public PokemonStateManager getPokemonStateManager() {
        return pokemonStateManager;
    }
    
    /**
     * Sets the XP system (used by main application to provide shared instance).
     * 
     * @param xpSystem The XPSystem to use
     */
    public void setXpSystem(XPSystem xpSystem) {
        this.xpSystem = xpSystem;
    }
    
    /**
     * Sets the commit service for manual repository scanning.
     * 
     * @param commitService The CommitService to use
     */
    public void setCommitService(com.tamagotchi.committracker.git.CommitService commitService) {
        this.commitService = commitService;
    }
    
    /**
     * FOR TESTING ONLY: Forces a manual repository scan to refresh commit data.
     * TODO: REMOVE THIS METHOD BEFORE PRODUCTION - See TODO.md
     */
    private void forceRepositoryScan() {
        if (commitService != null) {
            System.out.println("🔍 Forcing manual repository scan...");
            commitService.scanAllRepositories();
            
            // Update the widget with the latest commit history
            setCommitHistory(commitService.getCommitHistory());
            updatePokemonStatusDisplay();
            
            System.out.println("✅ Manual scan complete. Updated commit history.");
        } else {
            System.out.println("❌ No commit service available for manual scan.");
        }
    }
    
    // FOR TESTING ONLY: Accumulated XP for testing commit simulation
    // TODO: REMOVE THIS FIELD BEFORE PRODUCTION - See TODO.md
    private int testingAccumulatedXP = 0;
    
    /**
     * FOR TESTING ONLY: Resets the testing XP accumulator to 0.
     * Called when Pokemon is reset via R key.
     * TODO: REMOVE THIS METHOD BEFORE PRODUCTION - See TODO.md
     */
    public void resetTestingXP() {
        testingAccumulatedXP = 0;
        System.out.println("🧪 TESTING: Testing XP reset to 0");
    }
    
    /**
     * Checks if this is a first-time user who needs to select a Pokemon.
     * 
     * TODO: FOR PRODUCTION - This should check if user has selected a Pokemon via GitHub auth.
     * Currently always returns true for testing purposes.
     * See TODO.md for details on GitHub authentication integration.
     * 
     * @return true if no Pokemon has been selected yet (always true for testing)
     */
    public boolean isFirstTimeUser() {
        // TODO: TESTING MODE - Always show selection screen for testing
        // For production: return selectionData.isFirstTimeUser();
        return true;
    }
    
    /**
     * Shows the Pokemon selection screen.
     * The screen blocks until a selection is made.
     * After selection, the widget starts in expanded mode.
     * 
     * TODO: FOR PRODUCTION - This should only show on first GitHub sign-up.
     * Currently shows every time for testing purposes.
     * See TODO.md for details on GitHub authentication integration.
     */
    public void showPokemonSelectionScreen() {
        // TODO: TESTING MODE - Always show selection screen
        // For production: check if (!selectionData.isFirstTimeUser()) and return early
        
        System.out.println("🎮 Showing Pokemon selection screen (TESTING MODE - always shows)");
        
        PokemonSelectionScreen selectionScreen = new PokemonSelectionScreen(stage, selectedSpecies -> {
            // Update the Pokemon display with the selected species
            changePokemonSpecies(selectedSpecies);
            
            // Notify callback if set
            if (onPokemonSelected != null) {
                onPokemonSelected.accept(selectedSpecies);
            }
            
            System.out.println("🎉 Pokemon selection complete! Starting with " + 
                PokemonSelectionData.getDisplayName(selectedSpecies) + " egg");
            
            // IMPORTANT: Start in expanded mode after Pokemon selection
            // This shows the user the commit history and Pokemon status immediately
            if (isCompactMode) {
                switchToExpandedMode();
                System.out.println("📖 Starting in expanded mode to show Pokemon status and commit history");
            }
        });
        
        selectionScreen.show();
    }
    
    /**
     * Changes the current Pokemon species.
     * This creates a new Pokemon display with the selected species.
     * Used both for initial selection and for switching Pokemon later.
     * 
     * @param newSpecies The new Pokemon species to display
     */
    public void changePokemonSpecies(PokemonSpecies newSpecies) {
        if (newSpecies == null) {
            return;
        }
        
        System.out.println("🔄 Setting Pokemon to: " + PokemonSelectionData.getDisplayName(newSpecies));
        
        // Remove old Pokemon display if it exists
        if (pokemonDisplay != null) {
            pokemonDisplay.cleanup();
            pokemonStatusBox.getChildren().remove(pokemonDisplay);
            root.getChildren().remove(pokemonDisplay);
        }
        
        // Create new Pokemon display with selected species (starts as egg)
        pokemonDisplay = new PokemonDisplayComponent(
            newSpecies, EvolutionStage.EGG, PokemonState.CONTENT
        );
        
        // Since we start in expanded mode, add to status box
        if (!isCompactMode) {
            pokemonStatusBox.getChildren().clear();
            pokemonStatusBox.getChildren().add(pokemonDisplay);
        } else {
            // If in compact mode, add directly to root
            root.getChildren().add(0, pokemonDisplay);
        }
        
        // Reset testing XP when changing Pokemon
        testingAccumulatedXP = 0;
        
        System.out.println("🥚 Now displaying " + PokemonSelectionData.getDisplayName(newSpecies) + " egg");
    }
    
    /**
     * Gets the currently selected Pokemon species.
     * 
     * @return The selected Pokemon species, or null if none selected
     */
    public PokemonSpecies getSelectedPokemonSpecies() {
        return selectionData.getSelectedStarter();
    }
    
    /**
     * Gets the Pokemon selection data for external access.
     * 
     * @return The PokemonSelectionData instance
     */
    public PokemonSelectionData getSelectionData() {
        return selectionData;
    }
    
    /**
     * Enables Windows system integration features.
     * Configures theme adaptation, z-order behavior, taskbar integration, and credential storage.
     */
    private void enableWindowsIntegration() {
        if (!WindowsIntegration.isWindows()) {
            System.out.println("🖥️ Not running on Windows - skipping Windows integration");
            return;
        }
        
        try {
            // Detect and apply current Windows theme
            WindowsIntegration.WindowsTheme currentTheme = WindowsIntegration.detectWindowsTheme();
            UITheme.updateWindowsTheme(currentTheme);
            System.out.println("🎨 Applied Windows " + currentTheme + " theme");
            
            // Configure Windows-specific behavior after stage is shown
            Platform.runLater(() -> {
                // Apply Windows theme to stage
                WindowsIntegration.applyWindowsTheme(stage, currentTheme);
                
                // Configure proper z-order and focus behavior
                WindowsIntegration.configureWindowsBehavior(stage);
                
                // Configure taskbar minimization behavior
                WindowsIntegration.configureTaskbarBehavior(stage);
                
                System.out.println("🪟 Windows system integration enabled");
            });
            
            // Start monitoring theme changes
            WindowsIntegration.monitorThemeChanges(() -> {
                WindowsIntegration.WindowsTheme newTheme = WindowsIntegration.getCurrentTheme();
                UITheme.updateWindowsTheme(newTheme);
                WindowsIntegration.applyWindowsTheme(stage, newTheme);
                
                // Refresh UI components with new theme
                refreshUITheme();
                
                System.out.println("🎨 Windows theme changed to " + newTheme + " - UI updated");
            });
            
            windowsIntegrationEnabled = true;
            
        } catch (Exception e) {
            System.err.println("⚠️ Failed to enable Windows integration: " + e.getMessage());
            windowsIntegrationEnabled = false;
        }
    }
    
    /**
     * Refreshes the UI theme for all components when Windows theme changes.
     */
    private void refreshUITheme() {
        Platform.runLater(() -> {
            try {
                // Update root container style
                if (root != null) {
                    root.setStyle(UITheme.getTransparentStyle());
                }
                
                // Update expanded layout style
                if (expandedLayout != null) {
                    expandedLayout.setStyle(UITheme.getExpandedLayoutStyle());
                }
                
                // Update Pokemon status box style
                if (pokemonStatusBox != null) {
                    pokemonStatusBox.setStyle(UITheme.getStatusSectionStyle());
                }
                
                // Update history tab theme
                if (historyTab != null) {
                    historyTab.refreshTheme();
                }
                
                System.out.println("🔄 UI theme refreshed for all components");
                
            } catch (Exception e) {
                System.err.println("⚠️ Failed to refresh UI theme: " + e.getMessage());
            }
        });
    }
    
    /**
     * Saves the current window state for restoration on next startup.
     * Includes position, mode, and Pokemon selection data.
     */
    private void saveWindowState() {
        try {
            Properties props = new Properties();
            
            // Save window position
            props.setProperty(X_KEY, String.valueOf(stage.getX()));
            props.setProperty(Y_KEY, String.valueOf(stage.getY()));
            
            // Save window mode
            props.setProperty("widget.mode", isCompactMode ? "compact" : "expanded");
            
            // Save Pokemon selection if available
            if (pokemonDisplay != null) {
                props.setProperty("pokemon.species", pokemonDisplay.getCurrentSpecies().name());
                props.setProperty("pokemon.stage", pokemonDisplay.getCurrentStage().name());
                props.setProperty("pokemon.state", pokemonDisplay.getCurrentState().name());
            }
            
            // Save XP and streak data
            if (xpSystem != null) {
                props.setProperty("xp.current", String.valueOf(xpSystem.getCurrentXP()));
            }
            
            if (commitHistory != null) {
                props.setProperty("streak.current", String.valueOf(commitHistory.getCurrentStreak()));
            }
            
            FileUtils.saveProperties(props, POSITION_FILE);
            System.out.println("💾 Window state saved successfully");
            
        } catch (IOException e) {
            System.err.println("⚠️ Failed to save window state: " + e.getMessage());
        }
    }
    
    /**
     * Restores the window state from saved properties.
     * Called during initialization to restore previous session.
     */
    private void restoreWindowState() {
        try {
            Properties props = FileUtils.loadProperties(POSITION_FILE);
            
            // Restore window mode if saved
            String savedMode = props.getProperty("widget.mode");
            if ("expanded".equals(savedMode) && isCompactMode) {
                Platform.runLater(() -> switchToExpandedMode());
            }
            
            System.out.println("🔄 Window state restored successfully");
            
        } catch (IOException e) {
            // No saved state found - use defaults
            System.out.println("📝 No saved window state found - using defaults");
        }
    }
    
    /**
     * Integrates with Windows credential storage for Git authentication.
     * Stores and retrieves Git credentials securely using Windows Credential Manager.
     * 
     * @param repositoryUrl The Git repository URL
     * @param username The username for authentication
     * @param token The access token or password
     * @return true if credentials were stored successfully
     */
    public boolean storeGitCredentials(String repositoryUrl, String username, String token) {
        if (!windowsIntegrationEnabled) {
            return false;
        }
        
        boolean success = WindowsIntegration.storeGitCredentials(repositoryUrl, username, token);
        if (success) {
            System.out.println("🔐 Git credentials stored securely for: " + repositoryUrl);
        } else {
            System.err.println("⚠️ Failed to store Git credentials for: " + repositoryUrl);
            // Try fallback storage
            WindowsIntegration.storeCredentialFallback(repositoryUrl + "_user", username);
            WindowsIntegration.storeCredentialFallback(repositoryUrl + "_token", token);
        }
        
        return success;
    }
    
    /**
     * Retrieves Git credentials from Windows credential storage.
     * 
     * @param repositoryUrl The Git repository URL
     * @return Array containing [username, token], or null if not found
     */
    public String[] retrieveGitCredentials(String repositoryUrl) {
        if (!windowsIntegrationEnabled) {
            return null;
        }
        
        String[] credentials = WindowsIntegration.retrieveGitCredentials(repositoryUrl);
        if (credentials == null) {
            // Try fallback storage
            String username = WindowsIntegration.retrieveCredentialFallback(repositoryUrl + "_user");
            String token = WindowsIntegration.retrieveCredentialFallback(repositoryUrl + "_token");
            
            if (username != null && token != null) {
                credentials = new String[]{username, token};
            }
        }
        
        if (credentials != null) {
            System.out.println("🔐 Retrieved Git credentials for: " + repositoryUrl);
        }
        
        return credentials;
    }
    
    /**
     * Checks if Windows integration is enabled and working.
     * 
     * @return true if Windows integration is active
     */
    public boolean isWindowsIntegrationEnabled() {
        return windowsIntegrationEnabled;
    }
    
    /**
     * FOR TESTING ONLY: Simulates a commit being made to trigger egg animations.
     * This simulates individual commits with random XP (6-10) to test egg shaking on every commit.
     * XP is accumulated properly across multiple C key presses.
     * TODO: REMOVE THIS METHOD BEFORE PRODUCTION - See TODO.md
     */
    private void simulateCommitForTesting() {
        if (pokemonDisplay != null) {
            // Generate random XP between 6-10 to simulate different commit quality/size
            int commitXP = 6 + (int) (Math.random() * 5); // Random between 6-10
            
            System.out.println("🧪 TESTING: 'C' pressed - Simulating single commit");
            
            switch (pokemonDisplay.getCurrentStage()) {
                case EGG:
                    // Log XP before
                    int xpBefore = testingAccumulatedXP;
                    
                    // Add XP to accumulated total
                    testingAccumulatedXP += commitXP;
                    
                    // Log in the requested format
                    System.out.println("XP before commit: " + xpBefore);
                    System.out.println("XP earned from commit: " + commitXP);
                    System.out.println("XP after commit: " + testingAccumulatedXP);
                    System.out.println("-----------------------------");
                    
                    // Determine egg stage based on XP thresholds
                    // Stage 1: 0-10 XP, Stage 2: 11-25 XP, Stage 3: 26-40 XP, Stage 4: 41-60 XP
                    int eggStage;
                    if (testingAccumulatedXP <= 10) {
                        eggStage = 1;
                    } else if (testingAccumulatedXP <= 25) {
                        eggStage = 2;
                    } else if (testingAccumulatedXP <= 40) {
                        eggStage = 3;
                    } else {
                        eggStage = 4;
                    }
                    System.out.println("🥚 TESTING: Egg is at stage " + eggStage + " (XP: " + testingAccumulatedXP + ")");
                    
                    // Trigger animation with accumulated XP (this determines egg stage visually)
                    pokemonDisplay.triggerCommitAnimation(testingAccumulatedXP);
                    
                    // Auto-evolve when XP reaches 60 (XP evolution condition)
                    // Evolution can happen via: 4+ day streak OR 60+ XP
                    if (testingAccumulatedXP >= 60) {
                        System.out.println("🎉 TESTING: Egg has reached 60 XP! Hatching now!");
                        // Use checkEvolutionRequirements which handles both XP and streak conditions
                        pokemonDisplay.checkEvolutionRequirements(testingAccumulatedXP, 0); // 0 streak, but 60+ XP triggers evolution
                    }
                    break;
                    
                case BASIC:
                case STAGE_1:
                case STAGE_2:
                    // For evolved Pokemon, trigger commit animation
                    System.out.println("🐣 TESTING: Commit gives +" + commitXP + " XP - Pokemon should animate");
                    pokemonDisplay.triggerCommitAnimation(commitXP, 1); // This commit's XP, 1 day streak
                    break;
            }
        }
    }
}