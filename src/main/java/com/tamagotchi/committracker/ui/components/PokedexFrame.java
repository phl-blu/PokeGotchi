package com.tamagotchi.committracker.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import com.tamagotchi.committracker.pokemon.EvolutionStage;
import com.tamagotchi.committracker.pokemon.PokemonSpecies;
import com.tamagotchi.committracker.ui.theme.PokedexTheme;

import java.util.function.Consumer;

/**
 * Main container component for the Pokedex-style UI.
 * Renders a retro handheld gaming device appearance with:
 * - Red/coral frame with dark navy borders
 * - Window controls (4 colored dots) in top-left corner
 * - Screen area with gray/purple background
 * - D-pad and action buttons at the bottom
 * 
 * Supports two screen modes:
 * - Selection mode: Shows Pokemon selection grid
 * - Display mode: Shows selected Pokemon with stats
 * 
 * Requirements: 1.1, 1.2, 1.3, 1.4, 2.5
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
    
    // Components
    private HBox windowControls;
    private StackPane screenArea;
    private PokedexControls controls;
    
    // Screen content
    private Node currentScreen;
    private PokedexSelectionScreen selectionScreen;
    private PokedexMainDisplay mainDisplay;
    
    // Current Pokemon state
    private PokemonSpecies currentSpecies;
    private EvolutionStage currentStage;
    
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
        
        // Create and add window controls (top-left)
        windowControls = createWindowControls();
        VBox topSection = new VBox(8);
        topSection.setAlignment(Pos.TOP_LEFT);
        topSection.setStyle("-fx-background-color: transparent;");
        topSection.getChildren().add(windowControls);
        mainLayout.setTop(topSection);
        
        // Create screen area (center)
        screenArea = createScreenArea();
        mainLayout.setCenter(screenArea);
        BorderPane.setMargin(screenArea, new Insets(8, 0, 8, 0));
        
        // Create and add controls (bottom)
        controls = new PokedexControls();
        HBox bottomSection = new HBox();
        bottomSection.setAlignment(Pos.CENTER);
        bottomSection.setStyle("-fx-background-color: transparent;");
        bottomSection.getChildren().add(controls);
        mainLayout.setBottom(bottomSection);
        
        // Add main layout to frame
        this.getChildren().add(mainLayout);
    }
    
    /**
     * Creates the window controls (4 colored dots).
     * Colors: blue, pink, orange, green
     * 
     * Requirements: 1.3
     * 
     * @return HBox containing the window control dots
     */
    private HBox createWindowControls() {
        HBox controlsBox = new HBox(CONTROL_DOT_SPACING);
        controlsBox.setAlignment(Pos.CENTER_LEFT);
        
        // Create 4 colored dots
        Region blueDot = createControlDot(PokedexTheme.CONTROL_BLUE);
        Region pinkDot = createControlDot(PokedexTheme.CONTROL_PINK);
        Region orangeDot = createControlDot(PokedexTheme.CONTROL_ORANGE);
        Region greenDot = createControlDot(PokedexTheme.CONTROL_GREEN);
        
        controlsBox.getChildren().addAll(blueDot, pinkDot, orangeDot, greenDot);
        
        return controlsBox;
    }
    
    /**
     * Creates a single control dot with the specified color.
     * 
     * @param color The dot color (hex string)
     * @return Region styled as a colored dot
     */
    private Region createControlDot(String color) {
        Region dot = new Region();
        dot.setStyle(PokedexTheme.getControlDotStyle(color, CONTROL_DOT_SIZE));
        return dot;
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
        
        currentScreen = mainDisplay;
        screenArea.getChildren().add(mainDisplay);
    }
    
    /**
     * Updates the stats display on the main display screen.
     * 
     * @param xp Current XP amount
     * @param nextThreshold XP needed for next evolution
     * @param streak Current commit streak in days
     * @param stage Current evolution stage
     */
    public void updateStats(int xp, int nextThreshold, int streak, EvolutionStage stage) {
        if (mainDisplay != null) {
            mainDisplay.updateStats(xp, nextThreshold, streak, stage);
            this.currentStage = stage;
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
