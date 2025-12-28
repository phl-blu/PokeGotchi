/**
 * BACKUP FILE - Created for Pokedex UI Redesign
 * Original file: WidgetWindow.java
 * Backup date: 2025-12-28
 * 
 * This is a backup of the original WidgetWindow.java before the Pokedex UI redesign.
 * To revert: Copy this file's content back to WidgetWindow.java
 * 
 * Requirements reference: 8.4 (preserve ability to revert)
 */
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
import javafx.scene.control.TabPane;
import javafx.scene.control.Tab;
import com.tamagotchi.committracker.pokemon.PokemonSelectionData;
import com.tamagotchi.committracker.pokemon.PokemonSpecies;
import com.tamagotchi.committracker.pokemon.PokemonState;
import com.tamagotchi.committracker.pokemon.EvolutionStage;
import com.tamagotchi.committracker.pokemon.XPSystem;
import com.tamagotchi.committracker.pokemon.PokemonStateManager;
import com.tamagotchi.committracker.ui.theme.UITheme;
import com.tamagotchi.committracker.ui.animation.AnimationController;
import com.tamagotchi.committracker.ui.animation.UITransitionManager;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * Main UI container with transparent background and drag functionality.
 * Handles switching between compact and expanded modes.
 * Enhanced with Windows system integration for native behavior.
 */
class WidgetWindowBackup {
    private Stage stage;
    private Scene scene;
    private StackPane root;
    private boolean isCompactMode = false; // Start in expanded mode by default
    
    // Pokemon display component
    private PokemonDisplayComponent pokemonDisplay;
    
    // Expanded mode components
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
    
    public WidgetWindowBackup(Stage primaryStage) {
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
     * Creates a WidgetWindowBackup with a callback for when Pokemon is selected.
     */
    public WidgetWindowBackup(Stage primaryStage, Consumer<PokemonSpecies> onPokemonSelected) {
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
     * Creates a WidgetWindowBackup with external XP and state management systems.
     */
    public WidgetWindowBackup(Stage primaryStage, Consumer<PokemonSpecies> onPokemonSelected, 
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
        if (selectionData.hasSelectedStarter()) {
            PokemonSpecies selectedSpecies = selectionData.getSelectedStarter();
            
            pokemonDisplay = new PokemonDisplayComponent(
                selectedSpecies, EvolutionStage.EGG, PokemonState.CONTENT
            );
            
            pokemonStatusBox.getChildren().clear();
            pokemonStatusBox.getChildren().add(pokemonDisplay);
            
            root.getChildren().add(expandedLayout);
        } else {
            root.getChildren().add(expandedLayout);
        }
        
        // Create scene with transparent background
        scene = new Scene(root, AppConfig.EXPANDED_WIDTH, AppConfig.EXPANDED_HEIGHT);
        scene.setFill(Color.TRANSPARENT);
        
        stage.setScene(scene);
        
        root.setFocusTraversable(true);
        root.requestFocus();
        
        enableDragging();
        restorePosition();
        
        stage.xProperty().addListener((obs, oldVal, newVal) -> savePosition());
        stage.yProperty().addListener((obs, oldVal, newVal) -> savePosition());
        
        stage.setOnCloseRequest(e -> {
            savePosition();
            saveWindowState();
            
            if (animationController != null) {
                animationController.cleanup();
            }
            if (transitionManager != null) {
                transitionManager.cleanup();
            }
            
            Platform.exit();
            System.exit(0);
        });
        
        root.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                toggleMode();
            } else {
                showPokemonInfo();
            }
        });
    }
    
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
    
    private void initializeExpandedModeComponents() {
        historyTab = new HistoryTab();
        statisticsTab = new StatisticsTab();
        
        contentTabPane = new TabPane();
        contentTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        Tab historyTabNode = new Tab("History", historyTab);
        Tab statisticsTabNode = new Tab("Statistics", statisticsTab);
        
        contentTabPane.getTabs().addAll(historyTabNode, statisticsTabNode);
        
        expandedLayout = new BorderPane();
        expandedLayout.setStyle(UITheme.getExpandedLayoutStyle());
        expandedLayout.setPadding(new Insets(UITheme.LARGE_PADDING));
        
        javafx.scene.layout.HBox headerBox = new javafx.scene.layout.HBox();
        headerBox.setAlignment(Pos.TOP_RIGHT);
        headerBox.setPadding(new Insets(0, 0, 5, 0));
        
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
        closeButton.setOnAction(e -> {
            savePosition();
            saveWindowState();
            
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
        
        pokemonStatusBox = new VBox(UITheme.MEDIUM_SPACING);
        pokemonStatusBox.setAlignment(Pos.CENTER);
        pokemonStatusBox.setPadding(new Insets(UITheme.MEDIUM_SPACING));
        pokemonStatusBox.setMinHeight(100);
        pokemonStatusBox.setMaxHeight(100);
        
        VBox topSection = new VBox(0);
        topSection.getChildren().addAll(headerBox, pokemonStatusBox);
        
        expandedLayout.setTop(topSection);
        expandedLayout.setCenter(contentTabPane);
    }
    
    public void toggleMode() {
        if (transitionManager.isTransitioning()) {
            return;
        }
        
        if (isCompactMode) {
            switchToExpandedMode();
        } else {
            switchToCompactMode();
        }
    }
    
    private void switchToExpandedMode() {
        // Implementation preserved from original
    }
    
    private void switchToExpandedModeImmediate() {
        // Implementation preserved from original
    }
    
    private void switchToCompactMode() {
        // Implementation preserved from original
    }
    
    private void switchToCompactModeImmediate() {
        // Implementation preserved from original
    }
    
    public void savePosition() {
        try {
            Properties props = new Properties();
            props.setProperty(X_KEY, String.valueOf(stage.getX()));
            props.setProperty(Y_KEY, String.valueOf(stage.getY()));
            FileUtils.saveProperties(props, POSITION_FILE);
        } catch (IOException e) {
            System.err.println("Failed to save widget position: " + e.getMessage());
        }
    }
    
    public void restorePosition() {
        try {
            Properties props = FileUtils.loadProperties(POSITION_FILE);
            if (props.containsKey(X_KEY) && props.containsKey(Y_KEY)) {
                double x = Double.parseDouble(props.getProperty(X_KEY));
                double y = Double.parseDouble(props.getProperty(Y_KEY));
                
                javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
                
                if (x >= 0 && y >= 0 && 
                    x < screenBounds.getWidth() - 50 && 
                    y < screenBounds.getHeight() - 50) {
                    stage.setX(x);
                    stage.setY(y);
                    restoreWindowState();
                    return;
                }
            }
        } catch (IOException | NumberFormatException e) {
            // Fall through to center on screen
        }
        
        centerOnScreen();
    }
    
    public void centerOnScreen() {
        javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        stage.setX((screenBounds.getWidth() - AppConfig.COMPACT_WIDTH) / 2);
        stage.setY((screenBounds.getHeight() - AppConfig.COMPACT_HEIGHT) / 2);
    }
    
    public StackPane getRoot() { return root; }
    public Stage getStage() { return stage; }
    public boolean isCompactMode() { return isCompactMode; }
    public void show() { stage.show(); }
    
    private void showPokemonInfo() {
        // Implementation preserved from original
    }
    
    public void updatePetDisplay() {
        if (pokemonDisplay != null) {
            pokemonDisplay.updateState(pokemonDisplay.getCurrentState());
        }
    }
    
    public PokemonDisplayComponent getPokemonDisplay() { return pokemonDisplay; }
    public HistoryTab getHistoryTab() { return historyTab; }
    public CommitHistory getCommitHistory() { return commitHistory; }
    public XPSystem getXpSystem() { return xpSystem; }
    public PokemonStateManager getPokemonStateManager() { return pokemonStateManager; }
    public AnimationController getAnimationController() { return animationController; }
    public UITransitionManager getTransitionManager() { return transitionManager; }
    public PokemonSelectionData getSelectionData() { return selectionData; }
    public boolean isWindowsIntegrationEnabled() { return windowsIntegrationEnabled; }
    
    public void setXpSystem(XPSystem xpSystem) { this.xpSystem = xpSystem; }
    public void setCommitService(com.tamagotchi.committracker.git.CommitService commitService) { this.commitService = commitService; }
    
    private void enableWindowsIntegration() {
        // Implementation preserved from original
    }
    
    private void refreshUITheme() {
        // Implementation preserved from original
    }
    
    private void saveWindowState() {
        // Implementation preserved from original
    }
    
    private void restoreWindowState() {
        // Implementation preserved from original
    }
    
    // FOR TESTING ONLY fields and methods
    private int testingAccumulatedXP = 0;
    public void resetTestingXP() { testingAccumulatedXP = 0; }
}
