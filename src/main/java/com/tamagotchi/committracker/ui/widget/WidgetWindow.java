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
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.util.Duration;
import com.tamagotchi.committracker.config.AppConfig;
import com.tamagotchi.committracker.domain.Commit;
import com.tamagotchi.committracker.domain.CommitHistory;
import com.tamagotchi.committracker.util.FileUtils;
import com.tamagotchi.committracker.util.WindowsIntegration;
import com.tamagotchi.committracker.ui.components.HistoryTab;
import com.tamagotchi.committracker.ui.components.StatisticsTab;
import com.tamagotchi.committracker.ui.components.PokemonDisplayComponent;
import com.tamagotchi.committracker.ui.components.PokemonSelectionScreen;
import com.tamagotchi.committracker.ui.components.PokedexFrame;
import com.tamagotchi.committracker.ui.components.GitHubLoginWindow;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tab;
import com.tamagotchi.committracker.pokemon.PokemonSelectionData;
import com.tamagotchi.committracker.pokemon.PokemonSpecies;
import com.tamagotchi.committracker.pokemon.PokemonState;
import com.tamagotchi.committracker.pokemon.EvolutionStage;
import com.tamagotchi.committracker.pokemon.XPSystem;
import com.tamagotchi.committracker.pokemon.PokemonStateManager;
import com.tamagotchi.committracker.ui.theme.UITheme;
import com.tamagotchi.committracker.ui.theme.PokedexTheme;
import com.tamagotchi.committracker.ui.animation.AnimationController;
import com.tamagotchi.committracker.ui.animation.UITransitionManager;
import com.tamagotchi.committracker.github.GitHubOAuthService;
import com.tamagotchi.committracker.github.GitHubOAuthServiceImpl;
import com.tamagotchi.committracker.github.SecureTokenStorage;
import com.tamagotchi.committracker.github.SecureTokenStorageImpl;
import com.tamagotchi.committracker.github.AccessTokenResponse;
import com.tamagotchi.committracker.github.GitHubApiClient;
import com.tamagotchi.committracker.github.GitHubApiClientImpl;
import com.tamagotchi.committracker.github.GitHubUser;
import com.tamagotchi.committracker.github.GitHubApiException;
import com.tamagotchi.committracker.github.GitHubErrorHandler;
import com.tamagotchi.committracker.github.GitHubConfig;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * Main UI container with transparent background and drag functionality.
 * Handles switching between compact and expanded modes.
 * Enhanced with Windows system integration for native behavior.
 * Now uses PokedexFrame for the retro Pokedex-style UI.
 */
public class WidgetWindow {
    private Stage stage;
    private Scene scene;
    private StackPane root;
    private boolean isCompactMode = false; // Start in expanded mode by default
    
    // Pokedex frame - the main UI container
    private PokedexFrame pokedexFrame;
    
    // Pokemon display component (accessed through PokedexFrame)
    private PokemonDisplayComponent pokemonDisplay;
    
    // Expanded mode components (legacy - kept for compatibility)
    private BorderPane expandedLayout;
    private HistoryTab historyTab;
    private StatisticsTab statisticsTab;
    private TabPane contentTabPane;
    private VBox pokemonStatusBox;
    
    // Commit history data
    private CommitHistory commitHistory;
    
    // Real Pokemon state management (not testing)
    private XPSystem xpSystem;
    private PokemonStateManager pokemonStateManager;
    
    // GitHub integration
    private GitHubOAuthService oauthService;
    private SecureTokenStorage tokenStorage;
    private GitHubApiClient apiClient;
    
    // Animation and transition management
    private AnimationController animationController;
    private UITransitionManager transitionManager;
    
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
        
        // Initialize animation and transition management
        this.animationController = new AnimationController();
        this.transitionManager = new UITransitionManager(animationController);
        
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
        
        // Initialize animation and transition management
        this.animationController = new AnimationController();
        this.transitionManager = new UITransitionManager(animationController);
        
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
        
        // Initialize animation and transition management
        this.animationController = new AnimationController();
        this.transitionManager = new UITransitionManager(animationController);
        
        initialize();
    }
    
    /**
     * Set up transparent stage and initial pet display with Windows integration.
     * Uses PokedexFrame for the retro Pokedex-style UI.
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
        root.setStyle("-fx-background-color: transparent;");
        
        // Initialize commit history
        commitHistory = new CommitHistory();
        
        // Initialize expanded mode components (legacy - kept for compatibility)
        initializeExpandedModeComponents();
        
        // Create the PokedexFrame as the main UI container
        pokedexFrame = new PokedexFrame();
        
        // Wire up window control handlers
        setupWindowControlHandlers();
        
        // Check if user has already selected a starter
        if (selectionData.hasSelectedStarter()) {
            // Returning user - show main display with their Pokemon
            PokemonSpecies selectedSpecies = selectionData.getSelectedStarter();
            EvolutionStage savedStage = loadSavedEvolutionStage();
            
            System.out.println("🎮 Returning user - showing " + selectedSpecies + " at stage " + savedStage);
            
            // Show main display with saved Pokemon
            pokedexFrame.showMainDisplay(selectedSpecies, savedStage);
            
            // Get reference to Pokemon display for updates
            pokemonDisplay = pokedexFrame.getPokemonDisplay();
            
            // Set up evolution listener on PokedexFrame for UI updates
            setupPokedexEvolutionListener();
            
            // Update stats with saved data
            int savedXP = loadSavedXP();
            int savedStreak = loadSavedStreak();
            
            // Also set the streak on commitHistory so it's available for other components
            if (commitHistory != null) {
                commitHistory.setCurrentStreak(savedStreak);
            }
            
            int nextThreshold = getNextEvolutionThreshold(savedStage);
            pokedexFrame.updateStats(savedXP, nextThreshold, savedStreak, savedStage);
            
            System.out.println("📊 Loaded saved stats - XP: " + savedXP + ", Streak: " + savedStreak + " days");
            
            // GitHub tracking will be activated in setCommitService() once commitService is injected
            initializeGitHubServices();
            
            // Refresh GitHub status indicator for returning users
            Platform.runLater(this::refreshGitHubStatusIndicator);
        } else {
            // First-time user - show auth screen first, then selection screen
            System.out.println("🎮 First-time user - starting authentication flow");
            
            // Initialize GitHub services
            initializeGitHubServices();
            
            // Check if GitHub auth is already done (user may have skipped before)
            if (selectionData.isGitHubAuthenticated() || !isGitHubConfigured()) {
                // Skip auth, go directly to Pokemon selection
                showPokemonSelectionForFirstTime();
            } else {
                // Show auth screen first
                showAuthScreenForFirstTime();
            }
        }
        
        // Add PokedexFrame to root
        root.getChildren().add(pokedexFrame);
        
        // Apply rounded corner clipping to root to prevent corner artifacts
        javafx.scene.shape.Rectangle rootClip = new javafx.scene.shape.Rectangle(
            PokedexTheme.FRAME_WIDTH, PokedexTheme.FRAME_HEIGHT);
        rootClip.setArcWidth(26); // 2x the corner radius (13px)
        rootClip.setArcHeight(26);
        root.setClip(rootClip);
        
        // Create scene with transparent background using Pokedex dimensions
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
        
        // Handle window closing to save position and state - ENABLE CLOSE BUTTON
        stage.setOnCloseRequest(e -> {
            System.out.println("🚪 Close button pressed - saving state and exiting");
            savePosition();
            saveWindowState();
            
            // Clean up animation and transition resources
            if (animationController != null) {
                animationController.cleanup();
            }
            if (transitionManager != null) {
                transitionManager.cleanup();
            }
            if (pokedexFrame != null) {
                pokedexFrame.cleanup();
            }
            
            Platform.exit();
            System.exit(0); // Force exit to ensure clean shutdown
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
            // Always get the latest Pokemon display from PokedexFrame
            if (pokedexFrame != null) {
                pokemonDisplay = pokedexFrame.getPokemonDisplay();
            }
            
            switch (event.getCode()) {
                case E: // Press 'E' to force evolution for testing
                    if (isDevUser() && pokemonDisplay != null) {
                        System.out.println("🧪 TESTING: 'E' pressed - Forcing evolution");
                        pokemonDisplay.forceEvolutionForTesting();
                        // Stats/name update handled by evolution listener after animation completes
                    } else if (!isDevUser()) {
                        System.out.println("⚠️ Dev shortcuts locked to developer account only");
                    } else {
                        System.out.println("⚠️ TESTING: No Pokemon display available - select a Pokemon first");
                    }
                    break;
                case R: // Press 'R' to reset Pokemon to egg stage for testing
                    if (isDevUser() && pokemonDisplay != null) {
                        System.out.println("🧪 TESTING: 'R' pressed - Resetting Pokemon to egg stage");
                        pokemonDisplay.forceDeevolutionToEggForTesting();
                        resetTestingXP(); // Also reset the testing XP accumulator
                        // Update PokedexFrame stats after reset
                        updatePokedexFrameStats();
                    } else if (!isDevUser()) {
                        System.out.println("⚠️ Dev shortcuts locked to developer account only");
                    } else {
                        System.out.println("⚠️ TESTING: No Pokemon display available - select a Pokemon first");
                    }
                    break;
                case C: // Press 'C' to simulate a commit for testing egg animations
                    if (isDevUser()) {
                        simulateCommitForTesting();
                    } else {
                        System.out.println("⚠️ Dev shortcuts locked to developer account only");
                    }
                    break;
                case H: // Press 'H' to make Pokemon happy
                    if (pokemonDisplay != null) {
                        pokemonDisplay.updateState(PokemonState.HAPPY);
                        System.out.println("🧪 TESTING: 'H' pressed - Pokemon state changed to HAPPY");
                    } else {
                        System.out.println("⚠️ TESTING: No Pokemon display available - select a Pokemon first");
                    }
                    break;
                case S: // Press 'S' to make Pokemon sad
                    if (pokemonDisplay != null) {
                        pokemonDisplay.updateState(PokemonState.SAD);
                        System.out.println("🧪 TESTING: 'S' pressed - Pokemon state changed to SAD");
                    } else {
                        System.out.println("⚠️ TESTING: No Pokemon display available - select a Pokemon first");
                    }
                    break;
                case T: // Press 'T' to make Pokemon thriving
                    if (pokemonDisplay != null) {
                        pokemonDisplay.updateState(PokemonState.THRIVING);
                        System.out.println("🧪 TESTING: 'T' pressed - Pokemon state changed to THRIVING");
                    } else {
                        System.out.println("⚠️ TESTING: No Pokemon display available - select a Pokemon first");
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
                case P: // Press 'P' to pick a different Pokemon egg for testing
                    if (isDevUser()) {
                        System.out.println("🧪 TESTING: 'P' pressed - Opening Pokemon picker");
                        showPokemonPickerForTesting();
                    } else {
                        System.out.println("⚠️ Dev shortcuts locked to developer account only");
                    }
                    break;
                case ESCAPE: // Press 'ESC' to close the application
                    System.out.println("🚪 ESC pressed - closing application");
                    savePosition();
                    saveWindowState();
                    
                    // Clean up animation and transition resources
                    if (animationController != null) {
                        animationController.cleanup();
                    }
                    if (transitionManager != null) {
                        transitionManager.cleanup();
                    }
                    if (pokedexFrame != null) {
                        pokedexFrame.cleanup();
                    }
                    
                    Platform.exit();
                    System.exit(0);
                    break;
            }
        });
    }
    
    /**
     * Gets the XP threshold for the next evolution stage.
     * 
     * @param currentStage The current evolution stage
     * @return XP needed for next evolution
     */
    private int getNextEvolutionThreshold(EvolutionStage currentStage) {
        int[] thresholds = AppConfig.EVOLUTION_XP_THRESHOLDS;
        int stageIndex = currentStage.ordinal();
        if (stageIndex + 1 < thresholds.length) {
            return thresholds[stageIndex + 1];
        }
        return thresholds[thresholds.length - 1]; // Max stage
    }
    
    /**
     * Updates the PokedexFrame stats display with current data.
     * Recalculates XP from actual commits to ensure accuracy.
     */
    private void updatePokedexFrameStats() {
        if (pokedexFrame != null && pokemonDisplay != null) {
            // Recalculate XP from actual commits
            List<Commit> commits = commitHistory != null ? commitHistory.getRecentCommits() : List.of();
            int currentXP = 0;
            if (xpSystem != null && !commits.isEmpty()) {
                currentXP = xpSystem.recalculateFromCommits(commits);
            } else if (xpSystem != null) {
                currentXP = xpSystem.getCurrentXP();
            }
            
            int currentStreak = commitHistory != null ? commitHistory.getCurrentStreak() : 0;
            EvolutionStage currentStage = pokemonDisplay.getCurrentStage();
            int nextThreshold = getNextEvolutionThreshold(currentStage);
            
            pokedexFrame.updateStats(currentXP, nextThreshold, currentStreak, currentStage);
            
            // Note: Pokemon name is updated by the evolution listener, not here,
            // to avoid overwriting with a stale stage during async evolution animations.
        }
    }
    
    /**
     * Gets the Pokemon name for the current evolution stage.
     * 
     * @return The Pokemon name
     */
    private String getPokemonNameForCurrentStage() {
        if (pokemonDisplay != null) {
            PokemonSpecies species = pokemonDisplay.getCurrentSpecies();
            EvolutionStage stage = pokemonDisplay.getCurrentStage();
            return com.tamagotchi.committracker.ui.components.PokedexNameLabel.getPokemonNameForStage(species, stage);
        }
        return "???";
    }
    
    /**
     * Sets up the evolution listener on the PokedexFrame to update UI when evolution completes.
     * This ensures the Pokemon name label and stats are updated after evolution.
     * 
     * Requirements: 9.2
     */
    private void setupPokedexEvolutionListener() {
        if (pokedexFrame != null) {
            pokedexFrame.setEvolutionListener((newSpecies, newStage) -> {
                System.out.println("🌟 PokedexFrame evolution complete: " + newSpecies + " -> " + newStage);
                
                // Update the Pokemon name label with the evolved Pokemon name
                String newName = com.tamagotchi.committracker.ui.components.PokedexNameLabel.getPokemonNameForStage(newSpecies, newStage);
                pokedexFrame.updatePokemonName(newName);
                
                // Update stats display with new stage
                Platform.runLater(() -> {
                    updatePokedexFrameStats();
                    System.out.println("📊 PokedexFrame stats updated after evolution");
                });
            });
            System.out.println("🔔 PokedexFrame evolution listener set up");
        }
    }
    
    /**
     * Sets up the window control handlers for the PokedexFrame.
     * Connects the colored dots to actual window operations:
     * - Blue: Close application (save state first)
     * - Pink: Toggle always-on-top mode
     * - Orange: Open settings/preferences (future)
     * - Green: Toggle compact/expanded mode
     * 
     * Requirements: 9.1 (close saves state)
     */
    private void setupWindowControlHandlers() {
        if (pokedexFrame == null) {
            return;
        }
        
        // Blue dot: Close application (save state first)
        pokedexFrame.setOnClosePressed(() -> {
            System.out.println("🔵 Close button pressed - saving state and exiting");
            savePosition();
            saveWindowState();
            
            // Clean up animation and transition resources
            if (animationController != null) {
                animationController.cleanup();
            }
            if (transitionManager != null) {
                transitionManager.cleanup();
            }
            if (pokedexFrame != null) {
                pokedexFrame.cleanup();
            }
            
            Platform.exit();
            System.exit(0);
        });
        
        // Pink dot: Toggle always-on-top
        pokedexFrame.setOnAlwaysOnTopPressed(() -> {
            boolean newState = !stage.isAlwaysOnTop();
            stage.setAlwaysOnTop(newState);
            pokedexFrame.setAlwaysOnTop(newState);
            System.out.println("🩷 Always-on-top toggled: " + (newState ? "ON" : "OFF"));
        });
        
        // Orange dot: Settings (placeholder for future)
        pokedexFrame.setOnSettingsPressed(() -> {
            System.out.println("🟠 Settings button pressed - feature coming soon");
            // TODO: Implement settings dialog in future
        });
        
        // Green dot: Toggle compact/expanded mode
        pokedexFrame.setOnToggleModePressed(() -> {
            System.out.println("🟢 Toggle mode button pressed - switching mode");
            toggleMode();
        });
        
        // Initialize always-on-top state from stage
        pokedexFrame.setAlwaysOnTop(stage.isAlwaysOnTop());
        
        System.out.println("🎮 Window control handlers set up");
    }
    
    /**
     * Triggers a commit animation on the PokedexFrame.
     * For eggs: plays shake animation
     * For Pokemon: plays happy animation
     * 
     * Requirements: 9.1
     * 
     * @param totalXP Total accumulated XP
     * @param streakDays Current commit streak in days
     */
    public void triggerPokedexCommitAnimation(int totalXP, int streakDays) {
        if (pokedexFrame != null) {
            pokedexFrame.triggerCommitAnimation(totalXP, streakDays);
        }
    }
    
    /**
     * Updates the Pokemon state on the PokedexFrame.
     * Triggers the appropriate animation based on the new state.
     * 
     * Requirements: 9.3
     * 
     * @param state The new Pokemon state (HAPPY, SAD, THRIVING, CONTENT)
     */
    public void updatePokedexPokemonState(PokemonState state) {
        if (pokedexFrame != null && state != null) {
            pokedexFrame.updatePokemonState(state);
        }
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
        
        // Create statistics tab
        statisticsTab = new StatisticsTab();
        
        // Create tab pane for content
        contentTabPane = new TabPane();
        contentTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // Create tabs
        Tab historyTabNode = new Tab("History", historyTab);
        Tab statisticsTabNode = new Tab("Statistics", statisticsTab);
        
        contentTabPane.getTabs().addAll(historyTabNode, statisticsTabNode);
        
        // Create expanded layout (BorderPane for better organization)
        expandedLayout = new BorderPane();
        expandedLayout.setStyle(UITheme.getExpandedLayoutStyle());
        expandedLayout.setPadding(new Insets(UITheme.LARGE_PADDING));
        
        // Create header with close button
        javafx.scene.layout.HBox headerBox = new javafx.scene.layout.HBox();
        headerBox.setAlignment(Pos.TOP_RIGHT);
        headerBox.setPadding(new Insets(0, 0, 5, 0));
        
        // Create close button (X)
        javafx.scene.control.Button closeButton = new javafx.scene.control.Button("✕");
        closeButton.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-text-fill: #888888; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: bold; " +
            "-fx-cursor: hand; " +
            "-fx-padding: 2 6 2 6; " +
            "-fx-background-radius: 3;"
        );
        closeButton.setOnMouseEntered(e -> closeButton.setStyle(
            "-fx-background-color: #ff4444; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: bold; " +
            "-fx-cursor: hand; " +
            "-fx-padding: 2 6 2 6; " +
            "-fx-background-radius: 3;"
        ));
        closeButton.setOnMouseExited(e -> closeButton.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-text-fill: #888888; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: bold; " +
            "-fx-cursor: hand; " +
            "-fx-padding: 2 6 2 6; " +
            "-fx-background-radius: 3;"
        ));
        closeButton.setOnAction(e -> {
            System.out.println("🚪 Close button clicked - saving state and exiting");
            savePosition();
            saveWindowState();
            
            // Clean up animation and transition resources
            if (animationController != null) {
                animationController.cleanup();
            }
            if (transitionManager != null) {
                transitionManager.cleanup();
            }
            
            Platform.exit();
            System.exit(0);
        });
        
        headerBox.getChildren().add(closeButton);
        
        // Pokemon display area at top (below close button)
        pokemonStatusBox = new VBox(UITheme.MEDIUM_SPACING);
        pokemonStatusBox.setAlignment(Pos.CENTER);
        pokemonStatusBox.setPadding(new Insets(UITheme.MEDIUM_SPACING));
        pokemonStatusBox.setMinHeight(100);
        pokemonStatusBox.setMaxHeight(100);
        
        // Create a VBox to hold header and Pokemon status
        VBox topSection = new VBox(0);
        topSection.getChildren().addAll(headerBox, pokemonStatusBox);
        
        // Tab pane in center
        expandedLayout.setTop(topSection);
        expandedLayout.setCenter(contentTabPane);
    }
    
    /**
     * Switch between compact and expanded modes.
     * Compact mode shows only the Pokemon animation with transparent background.
     * Expanded mode shows the PokedexFrame with Pokemon and stats.
     */
    public void toggleMode() {
        if (isCompactMode) {
            // Switch to expanded mode - show PokedexFrame
            switchToExpandedMode();
        } else {
            // Switch to compact mode - show only Pokemon
            switchToCompactMode();
        }
    }
    
    /**
     * Switches to expanded mode showing the PokedexFrame.
     */
    private void switchToExpandedMode() {
        if (isCompactMode) {
            System.out.println("📖 Switching to expanded mode (PokedexFrame)");
            
            // If pokemonDisplay was detached for compact mode, put it back in PokedexFrame
            if (pokemonDisplay != null && pokemonDisplay.getParent() == root) {
                root.getChildren().remove(pokemonDisplay);
                if (pokedexFrame != null) {
                    pokedexFrame.getScreenArea().getChildren().clear();
                    pokedexFrame.getScreenArea().getChildren().add(pokemonDisplay);
                }
            }
            
            // Resize window to expanded size
            stage.setWidth(AppConfig.EXPANDED_WIDTH);
            stage.setHeight(AppConfig.EXPANDED_HEIGHT);
            root.setPrefSize(AppConfig.EXPANDED_WIDTH, AppConfig.EXPANDED_HEIGHT);
            
            // Clear root and add PokedexFrame
            root.getChildren().clear();
            root.getChildren().add(pokedexFrame);
            
            // Apply rounded corner clipping to root
            javafx.scene.shape.Rectangle rootClip = new javafx.scene.shape.Rectangle(
                PokedexTheme.FRAME_WIDTH, PokedexTheme.FRAME_HEIGHT);
            rootClip.setArcWidth(26);
            rootClip.setArcHeight(26);
            root.setClip(rootClip);
            
            // Update Pokemon display reference from PokedexFrame
            pokemonDisplay = pokedexFrame.getPokemonDisplay();
            
            // Update stats
            updatePokedexFrameStats();
            
            isCompactMode = false;
            System.out.println("📖 Expanded mode active - showing PokedexFrame");
        }
    }
    
    /**
     * Switches to expanded mode immediately without animation.
     * This prevents the Pokemon from glitching/jumping during transition.
     */
    private void switchToExpandedModeImmediate() {
        // First resize window to expanded size
        stage.setWidth(AppConfig.EXPANDED_WIDTH);
        stage.setHeight(AppConfig.EXPANDED_HEIGHT);
        root.setPrefSize(AppConfig.EXPANDED_WIDTH, AppConfig.EXPANDED_HEIGHT);
        
        // Clear root and add PokedexFrame
        root.getChildren().clear();
        root.getChildren().add(pokedexFrame);
        
        // Apply rounded corner clipping to root
        javafx.scene.shape.Rectangle rootClip = new javafx.scene.shape.Rectangle(
            PokedexTheme.FRAME_WIDTH, PokedexTheme.FRAME_HEIGHT);
        rootClip.setArcWidth(26);
        rootClip.setArcHeight(26);
        root.setClip(rootClip);
        
        // Update Pokemon display reference from PokedexFrame
        pokemonDisplay = pokedexFrame.getPokemonDisplay();
        
        // Update stats
        updatePokedexFrameStats();
        
        isCompactMode = false;
        System.out.println("📖 Switched to expanded mode (PokedexFrame)");
    }
    
    /**
     * Switches to compact mode (80x80px) showing only Pokemon with transparent background.
     */
    private void switchToCompactMode() {
        if (!isCompactMode) {
            System.out.println("📦 Switching to compact mode (Pokemon only)");
            
            // First resize the window to compact size
            stage.setWidth(AppConfig.COMPACT_WIDTH);
            stage.setHeight(AppConfig.COMPACT_HEIGHT);
            root.setPrefSize(AppConfig.COMPACT_WIDTH, AppConfig.COMPACT_HEIGHT);
            
            // Get Pokemon display from PokedexFrame if needed
            if (pokemonDisplay == null && pokedexFrame != null) {
                pokemonDisplay = pokedexFrame.getPokemonDisplay();
            }
            
            // Clear root and add only the Pokemon display
            root.getChildren().clear();
            if (pokemonDisplay != null) {
                // Create a new display component for compact mode to avoid parent issues
                PokemonSpecies species = pokemonDisplay.getCurrentSpecies();
                EvolutionStage stage = pokemonDisplay.getCurrentStage();
                PokemonState state = pokemonDisplay.getCurrentState();
                
                PokemonDisplayComponent compactDisplay = new PokemonDisplayComponent(species, stage, state);
                
                // For eggs: load the correct cracked egg visual based on current XP
                if (stage == EvolutionStage.EGG && xpSystem != null) {
                    compactDisplay.loadAndStartAnimationWithXP(xpSystem.getCurrentXP());
                }
                
                root.getChildren().add(compactDisplay);
                
                // Center the Pokemon in the compact view
                StackPane.setAlignment(compactDisplay, Pos.CENTER);
            }
            
            // Remove clipping in compact mode
            root.setClip(null);
            
            // Ensure completely transparent background in compact mode
            root.setStyle("-fx-background-color: transparent;");
            
            // Also ensure the scene background is transparent
            if (scene != null) {
                scene.setFill(Color.TRANSPARENT);
            }
            
            isCompactMode = true;
            System.out.println("📦 Compact mode active - transparent background");
        }
    }
    
    /**
     * Switches to compact mode immediately without animation.
     * This prevents the Pokemon from glitching/jumping during transition.
     */
    private void switchToCompactModeImmediate() {
        // First resize the window to compact size
        stage.setWidth(AppConfig.COMPACT_WIDTH);
        stage.setHeight(AppConfig.COMPACT_HEIGHT);
        root.setPrefSize(AppConfig.COMPACT_WIDTH, AppConfig.COMPACT_HEIGHT);
        
        // Get Pokemon display from PokedexFrame if needed
        if (pokemonDisplay == null && pokedexFrame != null) {
            pokemonDisplay = pokedexFrame.getPokemonDisplay();
        }
        
        // Clear root and add only the Pokemon display
        root.getChildren().clear();
        if (pokemonDisplay != null) {
            // Create a new display component for compact mode to avoid parent issues
            PokemonSpecies species = pokemonDisplay.getCurrentSpecies();
            EvolutionStage stage = pokemonDisplay.getCurrentStage();
            PokemonState state = pokemonDisplay.getCurrentState();
            
            PokemonDisplayComponent compactDisplay = new PokemonDisplayComponent(species, stage, state);
            
            // For eggs: load the correct cracked egg visual based on current XP
            if (stage == EvolutionStage.EGG && xpSystem != null) {
                compactDisplay.loadAndStartAnimationWithXP(xpSystem.getCurrentXP());
            }
            
            root.getChildren().add(compactDisplay);
            
            // Center the Pokemon in the compact view
            StackPane.setAlignment(compactDisplay, Pos.CENTER);
        }
        
        // Remove clipping in compact mode
        root.setClip(null);
        
        // Ensure completely transparent background in compact mode
        root.setStyle("-fx-background-color: transparent;");
        
        // Also ensure the scene background is transparent
        if (scene != null) {
            scene.setFill(Color.TRANSPARENT);
        }
        
        isCompactMode = true;
        System.out.println("📦 Switched to compact mode");
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
        // If using PokedexFrame, get display from there
        if (pokedexFrame != null && pokedexFrame.getPokemonDisplay() != null) {
            return pokedexFrame.getPokemonDisplay();
        }
        return pokemonDisplay;
    }
    
    /**
     * Gets the PokedexFrame component for external access.
     * 
     * @return The PokedexFrame instance
     */
    public PokedexFrame getPokedexFrame() {
        return pokedexFrame;
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
     * Gets the statistics service for external access.
     * Returns the service from the statistics tab.
     * 
     * @return The StatisticsService instance, or null if not available
     */
    public com.tamagotchi.committracker.service.StatisticsService getStatisticsService() {
        return statisticsTab != null ? statisticsTab.getStatisticsService() : null;
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
            
            // REAL-TIME UPDATES: Update both history and statistics tabs immediately
            updateAllTabsWithLatestData();
            
            System.out.println("🔄 Real-time update: Added " + newCommits.size() + " new commits, updated all tabs");
        }
    }
    
    /**
     * Updates all tabs (history and statistics) with the latest commit data.
     * Also updates the PokedexFrame stats display.
     * This ensures real-time updates across the entire UI REGARDLESS of mode.
     * Updates happen even in compact mode so data is ready when user expands.
     * Runs on JavaFX Application Thread to ensure UI updates work properly.
     */
    private void updateAllTabsWithLatestData() {
        // Ensure we run on JavaFX Application Thread for UI updates
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::updateAllTabsWithLatestData);
            return;
        }
        
        // ALWAYS update tabs regardless of mode - data should be ready when user expands
        // Get CURRENT Pokemon state directly from the display component
        PokemonSpecies currentSpecies = pokemonDisplay != null ? pokemonDisplay.getCurrentSpecies() : PokemonSpecies.CHARMANDER;
        EvolutionStage currentStage = pokemonDisplay != null ? pokemonDisplay.getCurrentStage() : EvolutionStage.EGG;
        
        // Recalculate XP from actual commits to ensure accuracy
        List<Commit> commits = commitHistory != null ? commitHistory.getRecentCommits() : List.of();
        int realXP = 0;
        if (xpSystem != null && !commits.isEmpty()) {
            realXP = xpSystem.recalculateFromCommits(commits);
        } else if (xpSystem != null) {
            realXP = xpSystem.getCurrentXP();
        }
        
        int realStreak = commitHistory != null ? commitHistory.getCurrentStreak() : 0;
        
        System.out.println("🔄 Updating all tabs - Species: " + currentSpecies + ", Stage: " + currentStage + ", XP: " + realXP + ", Streak: " + realStreak);
        
        // Update PokedexFrame stats display and commit data
        if (pokedexFrame != null) {
            int nextThreshold = getNextEvolutionThreshold(currentStage);
            pokedexFrame.updateStats(realXP, nextThreshold, realStreak, currentStage);
            
            // Update Pokemon name if needed
            String pokemonName = getPokemonNameForCurrentStage();
            pokedexFrame.updatePokemonName(pokemonName);
            
            // Update commit history for the History screen
            if (commitHistory != null) {
                pokedexFrame.updateCommitHistory(commitHistory);
            }
            
            // Update commits list for the Statistics screen
            pokedexFrame.updateCommits(commits);
            
            System.out.println("📊 PokedexFrame updated: XP=" + realXP + "/" + nextThreshold + ", Streak=" + realStreak + ", Commits=" + commits.size());
        }
        
        // Update history tab with CURRENT Pokemon state (always update, even if no commit history)
        if (historyTab != null) {
            historyTab.updatePokemonStatus(currentSpecies, currentStage, realXP, realStreak);
            System.out.println("📋 History tab Pokemon status updated: " + currentSpecies + " (" + currentStage + ")");
            
            if (commitHistory != null) {
                historyTab.updateCommitHistory(commitHistory);
            }
        }
        
        // Update statistics tab with latest commit data (including evolution history)
        if (statisticsTab != null) {
            statisticsTab.updateStatistics(commits, currentSpecies, currentStage, realStreak);
            
            System.out.println("📊 Statistics tab updated with " + commits.size() + " commits (mode: " + (isCompactMode ? "compact" : "expanded") + ")");
        }
    }
    
    /**
     * Sets the commit history directly.
     * Calculates the streak from the commit data to ensure it's up to date.
     * 
     * @param history The CommitHistory to set
     */
    public void setCommitHistory(CommitHistory history) {
        if (history != null) {
            // Preserve the current streak if the new history has no commit data yet
            // This handles the case where async commit loading hasn't completed
            int preservedStreak = this.commitHistory != null ? this.commitHistory.getCurrentStreak() : 0;
            
            this.commitHistory = history;
            
            // Only recalculate streak if there's actual commit data
            // Otherwise, preserve the saved streak from initialization
            if (!history.getDailyCommitCounts().isEmpty()) {
                commitHistory.calculateCurrentStreak();
                System.out.println("📊 Commit history set - Streak calculated from data: " + commitHistory.getCurrentStreak() + " days");
            } else if (preservedStreak > 0) {
                // No commit data yet, preserve the saved streak
                commitHistory.setCurrentStreak(preservedStreak);
                System.out.println("📊 Commit history set - Preserved saved streak: " + preservedStreak + " days");
            } else {
                System.out.println("📊 Commit history set - No commit data and no saved streak");
            }
            
            commitHistory.calculateAverageCommitsPerDay();
            
            // REAL-TIME UPDATES: Update all tabs immediately when commit history is set
            updateAllTabsWithLatestData();
            
            System.out.println("🔄 Commit history updated, all tabs refreshed with latest data");
        }
    }
    
    /**
     * Updates the Pokemon status display with current real data.
     * Call this method when XP or streak changes to refresh the UI.
     */
    public void updatePokemonStatusDisplay() {
        // REAL-TIME UPDATES: Update all tabs, not just history
        updateAllTabsWithLatestData();
        
        System.out.println("🔄 Pokemon status display updated across all tabs");
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
     * Gets the animation controller for external access.
     * 
     * @return The AnimationController instance
     */
    public AnimationController getAnimationController() {
        return animationController;
    }
    
    /**
     * Gets the UI transition manager for external access.
     * 
     * @return The UITransitionManager instance
     */
    public UITransitionManager getTransitionManager() {
        return transitionManager;
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
        
        // Now that commitService is available, activate GitHub tracking if we have a stored token
        if (tokenStorage != null) {
            tokenStorage.getAccessToken().ifPresent(token -> {
                if (!token.isBlank()) {
                    activateGitHubTracking(token);
                }
            });
        }
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
     * @return true if no Pokemon has been selected yet
     */
    public boolean isFirstTimeUser() {
        return !selectionData.hasSelectedStarter();
    }
    
    /**
     * Shows the Pokemon selection screen.
     * The screen blocks until a selection is made.
     * After selection, the widget starts in expanded mode.
     */
    public void showPokemonSelectionScreen() {
        // Only show if user hasn't selected a Pokemon yet
        if (!isFirstTimeUser()) {
            System.out.println("🎮 Pokemon already selected, skipping selection screen");
            return;
        }
        
        System.out.println("🎮 Showing Pokemon selection screen for first-time user");
        
        PokemonSelectionScreen selectionScreen = new PokemonSelectionScreen(stage, selectedSpecies -> {
            // Save the selection
            selectionData.saveSelection(selectedSpecies);
            
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
        
        // If using PokedexFrame, update through it
        if (pokedexFrame != null) {
            // Show main display with new species (starts as egg)
            pokedexFrame.showMainDisplay(newSpecies, EvolutionStage.EGG);
            
            // Get reference to new Pokemon display
            pokemonDisplay = pokedexFrame.getPokemonDisplay();
            
            // Set up evolution listener for the new Pokemon
            setupPokedexEvolutionListener();
            
            // Update stats
            int initialXP = 0;
            int initialStreak = commitHistory != null ? commitHistory.getCurrentStreak() : 0;
            int nextThreshold = getNextEvolutionThreshold(EvolutionStage.EGG);
            pokedexFrame.updateStats(initialXP, nextThreshold, initialStreak, EvolutionStage.EGG);
        } else {
            // Legacy path - remove old Pokemon display if it exists
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
                
                // Update statistics tab theme
                if (statisticsTab != null) {
                    statisticsTab.refreshTheme();
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
                String speciesName = pokemonDisplay.getCurrentSpecies().name();
                String stageName = pokemonDisplay.getCurrentStage().name();
                String stateName = pokemonDisplay.getCurrentState().name();
                
                props.setProperty("pokemon.species", speciesName);
                props.setProperty("pokemon.stage", stageName);
                props.setProperty("pokemon.state", stateName);
                
                System.out.println("💾 Saving Pokemon state: species=" + speciesName + ", stage=" + stageName + ", state=" + stateName);
            }
            
            // Save XP and streak data
            if (xpSystem != null) {
                props.setProperty("xp.current", String.valueOf(xpSystem.getCurrentXP()));
                System.out.println("💾 Saving XP: " + xpSystem.getCurrentXP());
            }
            
            if (commitHistory != null) {
                props.setProperty("streak.current", String.valueOf(commitHistory.getCurrentStreak()));
            }
            
            FileUtils.saveProperties(props, POSITION_FILE);
            System.out.println("💾 Window state saved successfully to " + POSITION_FILE);
            
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
     * Loads the saved evolution stage from persistent storage.
     * Returns EGG if no saved stage is found.
     * 
     * @return The saved evolution stage, or EGG if not found
     */
    private EvolutionStage loadSavedEvolutionStage() {
        try {
            Properties props = FileUtils.loadProperties(POSITION_FILE);
            String savedStage = props.getProperty("pokemon.stage");
            
            System.out.println("🔍 Loading saved stage from properties: '" + savedStage + "'");
            
            if (savedStage != null && !savedStage.isEmpty()) {
                try {
                    EvolutionStage stage = EvolutionStage.valueOf(savedStage);
                    System.out.println("✅ Loaded saved evolution stage: " + stage);
                    return stage;
                } catch (IllegalArgumentException e) {
                    System.out.println("⚠️ Invalid saved stage '" + savedStage + "', defaulting to EGG");
                }
            } else {
                System.out.println("📝 No pokemon.stage property found in saved state");
            }
        } catch (IOException e) {
            System.out.println("📝 No saved evolution stage found - starting as EGG (file not found)");
        }
        return EvolutionStage.EGG;
    }
    
    /**
     * Loads the saved XP from persistent storage.
     * Returns 0 if no saved XP is found.
     * 
     * @return The saved XP value, or 0 if not found
     */
    private int loadSavedXP() {
        try {
            Properties props = FileUtils.loadProperties(POSITION_FILE);
            String savedXP = props.getProperty("xp.current");
            
            if (savedXP != null && !savedXP.isEmpty()) {
                try {
                    int xp = Integer.parseInt(savedXP);
                    return Math.max(0, xp); // Ensure non-negative
                } catch (NumberFormatException e) {
                    System.out.println("⚠️ Invalid saved XP '" + savedXP + "', defaulting to 0");
                }
            }
        } catch (IOException e) {
            System.out.println("📝 No saved XP found - starting at 0");
        }
        return 0;
    }
    
    /**
     * Loads the saved streak from persistent storage.
     * Returns 0 if no saved streak is found.
     * 
     * @return The saved streak value, or 0 if not found
     */
    private int loadSavedStreak() {
        try {
            Properties props = FileUtils.loadProperties(POSITION_FILE);
            String savedStreak = props.getProperty("streak.current");
            
            if (savedStreak != null && !savedStreak.isEmpty()) {
                try {
                    int streak = Integer.parseInt(savedStreak);
                    return Math.max(0, streak); // Ensure non-negative
                } catch (NumberFormatException e) {
                    System.out.println("⚠️ Invalid saved streak '" + savedStreak + "', defaulting to 0");
                }
            }
        } catch (IOException e) {
            System.out.println("📝 No saved streak found - starting at 0");
        }
        return 0;
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
        // Always get the latest Pokemon display from PokedexFrame
        if (pokedexFrame != null) {
            pokemonDisplay = pokedexFrame.getPokemonDisplay();
        }
        
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
                    
                    // Update PokedexFrame stats
                    updatePokedexFrameStats();
                    
                    // Auto-evolve when XP reaches 60 (XP evolution condition)
                    // Evolution can happen via: 4+ day streak OR 60+ XP
                    if (testingAccumulatedXP >= 60) {
                        System.out.println("🎉 TESTING: Egg has reached 60 XP! Hatching now!");
                        // Use checkEvolutionRequirements which handles both XP and streak conditions
                        pokemonDisplay.checkEvolutionRequirements(testingAccumulatedXP, 0); // 0 streak, but 60+ XP triggers evolution
                        // Update stats after evolution
                        updatePokedexFrameStats();
                    }
                    break;
                    
                case BASIC:
                case STAGE_1:
                case STAGE_2:
                    // Accumulate XP for post-egg stages too
                    int xpBeforeEvolved = testingAccumulatedXP;
                    testingAccumulatedXP += commitXP;
                    System.out.println("XP before commit: " + xpBeforeEvolved);
                    System.out.println("XP earned from commit: " + commitXP);
                    System.out.println("XP after commit: " + testingAccumulatedXP);
                    System.out.println("-----------------------------");
                    
                    // For evolved Pokemon, trigger commit animation
                    System.out.println("🐣 TESTING: Commit gives +" + commitXP + " XP - Pokemon should animate");
                    pokemonDisplay.triggerCommitAnimation(testingAccumulatedXP, 1);
                    
                    // Check if XP threshold reached for next evolution
                    pokemonDisplay.checkEvolutionRequirements(testingAccumulatedXP, 0);
                    
                    // Update PokedexFrame stats
                    updatePokedexFrameStats();
                    break;
            }
        } else {
            System.out.println("⚠️ TESTING: No Pokemon display available - select a Pokemon first");
        }
    }
    
    /**
     * FOR TESTING ONLY: Shows the Pokemon selection screen to pick a different Pokemon egg.
     * This allows testing different Pokemon evolution lines without restarting the app.
     * Uses the PokedexFrame selection screen for consistency.
     * TODO: REMOVE THIS METHOD BEFORE PRODUCTION - See TODO.md
     */
    private void showPokemonPickerForTesting() {
        System.out.println("🧪 TESTING: Opening Pokemon selection screen");
        
        // Use PokedexFrame's selection screen if available
        if (pokedexFrame != null) {
            pokedexFrame.showSelectionScreen(selectedSpecies -> {
                System.out.println("🧪 TESTING: Switching to " + PokemonSelectionData.getDisplayName(selectedSpecies) + " egg");
                
                // Reset XP
                testingAccumulatedXP = 0;
                if (xpSystem != null) {
                    xpSystem.resetForTesting();
                }
                
                // Save the selection
                selectionData.saveSelection(selectedSpecies);
                
                // Get reference to new Pokemon display
                pokemonDisplay = pokedexFrame.getPokemonDisplay();
                
                // Update stats
                int initialXP = 0;
                int initialStreak = commitHistory != null ? commitHistory.getCurrentStreak() : 0;
                int nextThreshold = getNextEvolutionThreshold(EvolutionStage.EGG);
                pokedexFrame.updateStats(initialXP, nextThreshold, initialStreak, EvolutionStage.EGG);
                
                // Update the UI
                updatePokemonStatusDisplay();
                
                System.out.println("🥚 TESTING: Now testing " + PokemonSelectionData.getDisplayName(selectedSpecies) + " egg");
            });
        } else {
            // Legacy path - use the actual Pokemon selection screen (same as first launch)
            PokemonSelectionScreen selectionScreen = new PokemonSelectionScreen(stage, selectedSpecies -> {
                System.out.println("🧪 TESTING: Switching to " + PokemonSelectionData.getDisplayName(selectedSpecies) + " egg");
                
                // Reset XP and change Pokemon
                testingAccumulatedXP = 0;
                if (xpSystem != null) {
                    xpSystem.resetForTesting();
                }
                
                // Change the Pokemon species (this resets to egg stage)
                changePokemonSpecies(selectedSpecies);
                
                // Update the UI
                updatePokemonStatusDisplay();
                
                System.out.println("🥚 TESTING: Now testing " + PokemonSelectionData.getDisplayName(selectedSpecies) + " egg");
            });
            
            selectionScreen.show();
        }
    }
    
    // ==================== GitHub Authentication Integration ====================
    
    /**
     * Initializes GitHub OAuth and API services.
     * Creates instances of OAuth service, token storage, and API client.
     * 
     * Requirements: 1.1, 1.2
     */
    private void initializeGitHubServices() {
        try {
            // Initialize token storage first
            tokenStorage = new SecureTokenStorageImpl();
            
            // Initialize OAuth service
            oauthService = new GitHubOAuthServiceImpl();
            
            // Initialize API client
            apiClient = new GitHubApiClientImpl();
            
            System.out.println("🔐 GitHub services initialized");
        } catch (Exception e) {
            System.err.println("⚠️ Failed to initialize GitHub services: " + e.getMessage());
            // Services will be null, auth will be skipped
        }
    }

    /**
     * Activates GitHub commit tracking using the provided access token.
     * Wires GitHubCommitIntegration into CommitService so all repos are tracked via API.
     */
    private void activateGitHubTracking(String accessToken) {
        if (accessToken == null || accessToken.isBlank() || commitService == null) {
            return;
        }
        try {
            com.tamagotchi.committracker.github.GitHubCommitIntegration integration =
                new com.tamagotchi.committracker.github.GitHubCommitIntegration();
            integration.initialize(accessToken).thenAccept(success -> {
                if (success) {
                    commitService.setGitHubIntegration(integration);
                    integration.startTracking().thenAccept(result ->
                        System.out.println("✅ GitHub tracking active: " + result.repositoryCount() + " repos, " + result.commitCount() + " commits")
                    );
                    System.out.println("🔗 GitHub commit tracking activated");
                }
            });
        } catch (Exception e) {
            System.err.println("⚠️ Failed to activate GitHub tracking: " + e.getMessage());
        }
    }
    
    /**
     * Checks if the currently authenticated GitHub user is the developer.
     * Used to gate dev-only shortcuts (C, E, R, P) to the developer's account.
     * The allowed username is read from config/github.properties (gitignored).
     */
    private boolean isDevUser() {
        String devUsername = com.tamagotchi.committracker.github.GitHubConfig.getDevUsername();
        if (devUsername == null) {
            return false; // No dev username configured
        }
        String authenticatedUsername = selectionData.getGitHubUsername();
        return devUsername.equalsIgnoreCase(authenticatedUsername);
    }

    /**
     * Checks if GitHub OAuth is properly configured.
     * Returns false if client ID is not set, allowing the app to skip auth.
     * 
     * @return true if GitHub OAuth is configured and ready to use
     */
    private boolean isGitHubConfigured() {
        return GitHubConfig.isConfigured();
    }
    
    /**
     * Shows the GitHub authentication screen for first-time users.
     * Opens as a separate window so it has enough space to display properly.
     * After successful authentication, transitions to Pokemon selection.
     */
    private void showAuthScreenForFirstTime() {
        if (oauthService == null) {
            showPokemonSelectionForFirstTime();
            return;
        }

        System.out.println("🔐 Showing GitHub authentication window");

        GitHubLoginWindow loginWindow = new GitHubLoginWindow(
            stage,
            oauthService,
            // On success
            accessTokenResponse -> {
                System.out.println("✅ GitHub authentication successful");

                if (tokenStorage != null && accessTokenResponse != null) {
                    tokenStorage.storeAccessToken(accessTokenResponse.accessToken());
                    if (accessTokenResponse.refreshToken() != null) {
                        tokenStorage.storeRefreshToken(accessTokenResponse.refreshToken());
                    }
                }

                if (apiClient != null && accessTokenResponse != null) {
                    apiClient.setAccessToken(accessTokenResponse.accessToken());
                    apiClient.fetchAuthenticatedUser()
                        .thenAccept(user -> Platform.runLater(() -> {
                            selectionData.saveGitHubAuth(user.id(), user.login());
                            System.out.println("👤 GitHub user linked: " + user.login());
                            refreshGitHubStatusIndicator();
                            // Activate GitHub tracking for all repos
                            activateGitHubTracking(accessTokenResponse.accessToken());
                            showPokemonSelectionForFirstTime();
                        }))
                        .exceptionally(ex -> {
                            Platform.runLater(this::showPokemonSelectionForFirstTime);
                            return null;
                        });
                } else {
                    showPokemonSelectionForFirstTime();
                }
            },
            // On skip
            () -> {
                System.out.println("⏭️ GitHub authentication skipped - using local Git only");
                showPokemonSelectionForFirstTime();
            }
        );

        loginWindow.show();
    }
    
    /**
     * Shows the Pokemon selection screen for first-time users.
     * Called after GitHub authentication (or skip) completes.
     * 
     * Requirements: 1.1, 1.6
     */
    private void showPokemonSelectionForFirstTime() {
        if (pokedexFrame == null) {
            return;
        }
        
        System.out.println("🎮 Showing Pokemon selection screen");
        
        // Center on screen for first-time users (same position as login window)
        javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        stage.setX((screenBounds.getWidth() - com.tamagotchi.committracker.ui.theme.PokedexTheme.FRAME_WIDTH) / 2);
        stage.setY((screenBounds.getHeight() - com.tamagotchi.committracker.ui.theme.PokedexTheme.FRAME_HEIGHT) / 2);
        
        // Show the widget now (auth is done, time to pick a Pokemon)
        stage.show();
        
        pokedexFrame.showSelectionScreen(selectedSpecies -> {
            // Save the selection (this also preserves GitHub auth data)
            selectionData.saveSelection(selectedSpecies);
            
            // Get reference to new Pokemon display
            pokemonDisplay = pokedexFrame.getPokemonDisplay();
            
            // Set up evolution listener
            setupPokedexEvolutionListener();
            
            // Update stats with initial values
            int initialXP = 0;
            int initialStreak = commitHistory != null ? commitHistory.getCurrentStreak() : 0;
            int nextThreshold = getNextEvolutionThreshold(EvolutionStage.EGG);
            pokedexFrame.updateStats(initialXP, nextThreshold, initialStreak, EvolutionStage.EGG);
            
            // Notify callback if set
            if (onPokemonSelected != null) {
                onPokemonSelected.accept(selectedSpecies);
            }
            
            System.out.println("🎉 Pokemon selection complete! Starting with " + 
                PokemonSelectionData.getDisplayName(selectedSpecies) + " egg");
            
            // Update the UI
            updatePokemonStatusDisplay();
        });
    }
    
    /**
     * Gets the OAuth service for external access.
     * 
     * @return The GitHubOAuthService instance, or null if not initialized
     */
    public GitHubOAuthService getOAuthService() {
        return oauthService;
    }
    
    /**
     * Gets the token storage for external access.
     * 
     * @return The SecureTokenStorage instance, or null if not initialized
     */
    public SecureTokenStorage getTokenStorage() {
        return tokenStorage;
    }
    
    /**
     * Gets the GitHub API client for external access.
     * 
     * @return The GitHubApiClient instance, or null if not initialized
     */
    public GitHubApiClient getApiClient() {
        return apiClient;
    }
    
    /**
     * Returns a human-readable GitHub connection status string suitable for display
     * in the widget title bar or a tooltip.
     * 
     * Possible values:
     * <ul>
     *   <li>"GitHub: Connected (@username)" – authenticated and token present</li>
     *   <li>"GitHub: Not connected" – no stored token or auth data</li>
     * </ul>
     * 
     * Requirements: 1.1, 10.6
     * 
     * @return status string
     */
    public String getGitHubStatusText() {
        if (!selectionData.isGitHubAuthenticated()) {
            return "GitHub: Not connected";
        }
        if (tokenStorage == null || tokenStorage.getAccessToken().isEmpty()) {
            return "GitHub: Not connected";
        }
        String username = selectionData.getGitHubUsername();
        if (username != null && !username.isBlank()) {
            return "GitHub: Connected (@" + username + ")";
        }
        return "GitHub: Connected";
    }
    
    /**
     * Updates the stage title to reflect the current GitHub connection status.
     * Safe to call from any thread – dispatches to the JavaFX Application Thread if needed.
     * 
     * Requirements: 1.1, 10.6
     */
    public void refreshGitHubStatusIndicator() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::refreshGitHubStatusIndicator);
            return;
        }
        String status = getGitHubStatusText();
        System.out.println("🔗 GitHub status: " + status);
        stage.setTitle("Pokemon Commit Tracker – " + status);
    }
    
    /**
     * Listener interface for authentication events.
     * Used to notify the application when re-authentication is needed.
     * 
     * Requirements: 1.5, 1.6
     */
    public interface AuthenticationListener {
        /**
         * Called when re-authentication is required (e.g., token revoked).
         */
        void onReAuthenticationRequired();
        
        /**
         * Called when authentication is successful.
         * @param userId The GitHub user ID
         * @param username The GitHub username
         */
        void onAuthenticationSuccess(long userId, String username);
        
        /**
         * Called when authentication fails or is cancelled.
         */
        void onAuthenticationFailed();
    }
    
    private AuthenticationListener authenticationListener;
    
    /**
     * Sets the authentication listener for receiving auth events.
     * 
     * @param listener The listener to set
     */
    public void setAuthenticationListener(AuthenticationListener listener) {
        this.authenticationListener = listener;
    }
    
    /**
     * Triggers re-authentication flow when token is revoked or invalid.
     * Preserves Pokemon state during re-authentication.
     * 
     * Requirements: 1.5, 1.6
     */
    public void triggerReAuthentication() {
        Platform.runLater(() -> {
            System.out.println("🔐 Re-authentication required - token may be revoked");
            
            // Clear stored tokens since they're invalid
            if (tokenStorage != null) {
                tokenStorage.clearAllTokens();
            }
            
            // Clear GitHub auth data but preserve Pokemon selection
            selectionData.clearGitHubAuth();
            
            // Show re-authentication screen
            showReAuthScreen();
        });
    }
    
    /**
     * Shows the re-authentication screen.
     * Preserves Pokemon state and allows user to re-authenticate or skip.
     * 
     * Requirements: 1.5, 1.6
     */
    private void showReAuthScreen() {
        if (pokedexFrame == null || oauthService == null) {
            System.err.println("⚠️ Cannot show re-auth screen - services not available");
            return;
        }
        
        // Store current Pokemon state before showing auth screen
        PokemonSpecies preservedSpecies = currentSpecies();
        EvolutionStage preservedStage = currentStage();
        int preservedXP = xpSystem != null ? xpSystem.getCurrentXP() : 0;
        int preservedStreak = commitHistory != null ? commitHistory.getCurrentStreak() : 0;
        
        System.out.println("🔐 Showing re-authentication screen (preserving Pokemon state)");
        System.out.println("   Preserved: " + preservedSpecies + " at " + preservedStage + 
                          ", XP: " + preservedXP + ", Streak: " + preservedStreak);
        
        pokedexFrame.showAuthScreen(
            oauthService,
            // On success: Store token, fetch user info, restore Pokemon display
            accessTokenResponse -> {
                System.out.println("✅ Re-authentication successful");
                
                // Store the access token securely
                if (tokenStorage != null && accessTokenResponse != null) {
                    tokenStorage.storeAccessToken(accessTokenResponse.accessToken());
                    if (accessTokenResponse.refreshToken() != null) {
                        tokenStorage.storeRefreshToken(accessTokenResponse.refreshToken());
                    }
                }
                
                // Set token on API client
                if (apiClient != null && accessTokenResponse != null) {
                    apiClient.setAccessToken(accessTokenResponse.accessToken());
                    
                    // Fetch user info and save GitHub auth data
                    apiClient.fetchAuthenticatedUser()
                        .thenAccept(user -> {
                            Platform.runLater(() -> {
                                // Save GitHub user ID with Pokemon selection data
                                selectionData.saveGitHubAuth(user.id(), user.login());
                                System.out.println("👤 GitHub user re-linked: " + user.login() + " (ID: " + user.id() + ")");
                                
                                // Update GitHub status indicator
                                refreshGitHubStatusIndicator();
                                
                                // Restore Pokemon display with preserved state
                                restorePokemonDisplay(preservedSpecies, preservedStage, preservedXP, preservedStreak);
                                
                                // Notify listener
                                if (authenticationListener != null) {
                                    authenticationListener.onAuthenticationSuccess(user.id(), user.login());
                                }
                            });
                        })
                        .exceptionally(ex -> {
                            System.err.println("⚠️ Failed to fetch GitHub user: " + ex.getMessage());
                            Platform.runLater(() -> {
                                // Still restore Pokemon display even if user fetch fails
                                restorePokemonDisplay(preservedSpecies, preservedStage, preservedXP, preservedStreak);
                            });
                            return null;
                        });
                } else {
                    // No API client, just restore Pokemon display
                    restorePokemonDisplay(preservedSpecies, preservedStage, preservedXP, preservedStreak);
                }
            },
            // On skip: Just restore Pokemon display without GitHub auth
            () -> {
                System.out.println("⏭️ Re-authentication skipped");
                restorePokemonDisplay(preservedSpecies, preservedStage, preservedXP, preservedStreak);
                
                // Notify listener
                if (authenticationListener != null) {
                    authenticationListener.onAuthenticationFailed();
                }
            }
        );
    }
    
    /**
     * Restores the Pokemon display after re-authentication.
     * Preserves all Pokemon state including species, stage, XP, and streak.
     * 
     * Requirements: 1.6
     * 
     * @param species The Pokemon species to restore
     * @param stage The evolution stage to restore
     * @param xp The XP to restore
     * @param streak The streak to restore
     */
    private void restorePokemonDisplay(PokemonSpecies species, EvolutionStage stage, int xp, int streak) {
        if (pokedexFrame == null) {
            return;
        }
        
        System.out.println("🔄 Restoring Pokemon display: " + species + " at " + stage);
        
        // Show main display with preserved Pokemon state
        if (species != null) {
            pokedexFrame.showMainDisplay(species, stage != null ? stage : EvolutionStage.EGG);
            
            // Get reference to Pokemon display
            pokemonDisplay = pokedexFrame.getPokemonDisplay();
            
            // Set up evolution listener
            setupPokedexEvolutionListener();
            
            // Update stats with preserved values
            int nextThreshold = getNextEvolutionThreshold(stage != null ? stage : EvolutionStage.EGG);
            pokedexFrame.updateStats(xp, nextThreshold, streak, stage != null ? stage : EvolutionStage.EGG);
            
            // Update Pokemon name
            String pokemonName = getPokemonNameForCurrentStage();
            pokedexFrame.updatePokemonName(pokemonName);
            
            System.out.println("✅ Pokemon display restored successfully");
        } else {
            // No Pokemon selected, show selection screen
            showPokemonSelectionForFirstTime();
        }
    }
    
    /**
     * Gets the current Pokemon species from the display.
     * 
     * @return The current species, or null if not available
     */
    private PokemonSpecies currentSpecies() {
        if (pokemonDisplay != null) {
            return pokemonDisplay.getCurrentSpecies();
        }
        if (pokedexFrame != null) {
            return pokedexFrame.getCurrentSpecies();
        }
        return selectionData.getSelectedStarter();
    }
    
    /**
     * Gets the current evolution stage from the display.
     * 
     * @return The current stage, or EGG if not available
     */
    private EvolutionStage currentStage() {
        if (pokemonDisplay != null) {
            return pokemonDisplay.getCurrentStage();
        }
        if (pokedexFrame != null) {
            return pokedexFrame.getCurrentStage();
        }
        return loadSavedEvolutionStage();
    }
    
    /**
     * Checks if the user is currently authenticated with GitHub.
     * 
     * @return true if authenticated
     */
    public boolean isGitHubAuthenticated() {
        return selectionData.isGitHubAuthenticated() && 
               tokenStorage != null && 
               tokenStorage.getAccessToken().isPresent();
    }
    
    /**
     * Creates and returns a GitHubErrorHandler.ErrorListener that triggers
     * re-authentication when needed.
     * 
     * Requirements: 1.5, 8.2
     * 
     * @return An ErrorListener that handles authentication errors
     */
    public GitHubErrorHandler.ErrorListener createErrorListener() {
        return new GitHubErrorHandler.ErrorListener() {
            @Override
            public void onAuthenticationRequired() {
                System.out.println("🔐 Authentication required - triggering re-auth flow");
                triggerReAuthentication();
            }
            
            @Override
            public void onRateLimitExhausted(java.time.Instant resetTime) {
                System.out.println("⏳ Rate limit exhausted - reset at: " + resetTime);
                // Could show a notification to the user here
            }
            
            @Override
            public void onOfflineModeEntered() {
                System.out.println("📴 Entered offline mode");
                // Could update UI to show offline indicator
            }
            
            @Override
            public void onOfflineModeExited() {
                System.out.println("📶 Exited offline mode");
                // Could update UI to remove offline indicator
            }
            
            @Override
            public void onPersistentError(GitHubApiException error) {
                System.err.println("❌ Persistent error: " + error.getMessage());
                // Could show error notification to user
            }
        };
    }
}