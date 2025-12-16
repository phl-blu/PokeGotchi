package com.tamagotchi.committracker.ui.components;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.animation.Timeline;
import javafx.application.Platform;
import java.util.List;

import com.tamagotchi.committracker.pokemon.PokemonSpecies;
import com.tamagotchi.committracker.pokemon.PokemonState;
import com.tamagotchi.committracker.pokemon.EvolutionStage;
import com.tamagotchi.committracker.util.AnimationUtils;

/**
 * Renders animated Pokemon with evolution stages using frame-based animation.
 * This component handles displaying Pokemon sprites, cycling through animation frames,
 * and managing evolution sequences.
 */
public class PokemonDisplayComponent extends StackPane {
    
    private ImageView pokemonImageView;
    private Timeline currentAnimation;
    
    // Current Pokemon state
    private PokemonSpecies currentSpecies;
    private EvolutionStage currentStage;
    private PokemonState currentState;
    
    // Animation state
    private boolean isEvolutionInProgress;
    private List<Image> currentFrames;
    
    /**
     * Creates a new Pokemon Display Component with default settings.
     */
    public PokemonDisplayComponent() {
        initializeComponent();
        
        // Set default Pokemon (egg state)
        this.currentSpecies = PokemonSpecies.CHARMANDER; // Default starter
        this.currentStage = EvolutionStage.EGG;
        this.currentState = PokemonState.CONTENT;
        this.isEvolutionInProgress = false;
        
        // Load initial animation
        loadAndStartAnimation();
    }
    
    /**
     * Creates a Pokemon Display Component with specific Pokemon.
     */
    public PokemonDisplayComponent(PokemonSpecies species, EvolutionStage stage, PokemonState state) {
        initializeComponent();
        
        this.currentSpecies = species;
        this.currentStage = stage;
        this.currentState = state;
        this.isEvolutionInProgress = false;
        
        loadAndStartAnimation();
    }
    
    /**
     * Initializes the JavaFX components.
     */
    private void initializeComponent() {
        // Create the ImageView for displaying Pokemon sprites
        pokemonImageView = new ImageView();
        pokemonImageView.setFitWidth(64);
        pokemonImageView.setFitHeight(64);
        pokemonImageView.setPreserveRatio(true);
        pokemonImageView.setSmooth(true);
        
        // Add to the StackPane
        this.getChildren().add(pokemonImageView);
        
        // Set preferred size
        this.setPrefSize(80, 80);
        this.setMaxSize(80, 80);
        this.setMinSize(80, 80);
    }
    
    /**
     * Updates the Pokemon's emotional/health state and refreshes animation.
     * 
     * @param newState The new Pokemon state
     */
    public void updateState(PokemonState newState) {
        if (this.currentState != newState && !isEvolutionInProgress) {
            this.currentState = newState;
            loadAndStartAnimation();
        }
    }
    
    /**
     * Plays a specific animation type for the current Pokemon.
     * 
     * @param animationType The type of animation to play
     */
    public void playAnimation(PokemonState animationType) {
        if (!isEvolutionInProgress) {
            PokemonState previousState = this.currentState;
            this.currentState = animationType;
            
            loadAndStartAnimation();
            
            // If this is a temporary animation (like celebrating), revert after a delay
            if (animationType == PokemonState.THRIVING || animationType == PokemonState.HAPPY) {
                Platform.runLater(() -> {
                    try {
                        Thread.sleep(3000); // Show celebration for 3 seconds
                        if (this.currentState == animationType) {
                            this.currentState = previousState;
                            loadAndStartAnimation();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
        }
    }
    
    /**
     * Loads sprite frames for the current Pokemon configuration and starts animation.
     */
    private void loadAndStartAnimation() {
        // Stop current animation if running
        if (currentAnimation != null) {
            currentAnimation.stop();
        }
        
        // Load sprite frames for current configuration
        currentFrames = AnimationUtils.loadSpriteFrames(currentSpecies, currentStage, currentState);
        
        if (!currentFrames.isEmpty()) {
            // Create and start new animation
            currentAnimation = AnimationUtils.createFrameAnimation(
                currentFrames,
                this::updateDisplayedFrame
            );
            
            if (currentAnimation != null) {
                currentAnimation.play();
                
                // Also set the first frame immediately
                updateDisplayedFrame(currentFrames.get(0));
            }
        }
    }
    
    /**
     * Updates the displayed frame in the ImageView.
     * 
     * @param frame The new frame to display
     */
    private void updateDisplayedFrame(Image frame) {
        Platform.runLater(() -> {
            if (pokemonImageView != null && frame != null) {
                pokemonImageView.setImage(frame);
            }
        });
    }
    
    /**
     * Checks if the Pokemon meets evolution requirements and triggers evolution if so.
     * 
     * @param xpLevel Current XP level
     * @param streakDays Current commit streak in days
     * @return true if evolution was triggered, false otherwise
     */
    public boolean checkEvolutionRequirements(int xpLevel, int streakDays) {
        if (isEvolutionInProgress) {
            return false;
        }
        
        EvolutionStage nextStage = getNextEvolutionStage();
        if (nextStage == null) {
            return false; // Already at max evolution
        }
        
        // Check evolution criteria based on design document
        boolean canEvolve = false;
        switch (nextStage) {
            case BASIC:
                // Hatch from egg: 4+ day streak + XP threshold
                canEvolve = (streakDays >= 4 && xpLevel >= 200);
                break;
            case STAGE_1:
                // First evolution: 11+ day streak + XP threshold
                canEvolve = (streakDays >= 11 && xpLevel >= 800);
                break;
            case STAGE_2:
                // Final evolution: 22+ day streak + XP threshold
                canEvolve = (streakDays >= 22 && xpLevel >= 2000);
                break;
        }
        
        if (canEvolve) {
            triggerEvolution(nextStage);
            return true;
        }
        
        return false;
    }
    
    /**
     * Gets the next evolution stage for the current Pokemon.
     */
    private EvolutionStage getNextEvolutionStage() {
        switch (currentStage) {
            case EGG:
                return EvolutionStage.BASIC;
            case BASIC:
                return EvolutionStage.STAGE_1;
            case STAGE_1:
                return EvolutionStage.STAGE_2;
            case STAGE_2:
                return null; // Already at max evolution
            default:
                return null;
        }
    }
    
    /**
     * Triggers evolution animation sequence to the specified stage.
     * 
     * @param newStage The evolution stage to evolve to
     */
    public void triggerEvolution(EvolutionStage newStage) {
        if (isEvolutionInProgress || newStage == currentStage) {
            return;
        }
        
        isEvolutionInProgress = true;
        
        // Stop current animation
        if (currentAnimation != null) {
            currentAnimation.stop();
        }
        
        // Get the evolved Pokemon species
        PokemonSpecies evolvedSpecies = getEvolvedSpecies(currentSpecies, newStage);
        
        // Create evolution animation
        Timeline evolutionAnimation = AnimationUtils.createEvolutionAnimation(
            currentSpecies, evolvedSpecies,
            currentStage, newStage,
            this::updateDisplayedFrame,
            () -> completeEvolution(evolvedSpecies, newStage)
        );
        
        // Set current state to evolving
        this.currentState = PokemonState.EVOLVING;
        
        // Start evolution animation
        evolutionAnimation.play();
    }
    
    /**
     * Completes the evolution process and resumes normal animation.
     */
    private void completeEvolution(PokemonSpecies newSpecies, EvolutionStage newStage) {
        this.currentSpecies = newSpecies;
        this.currentStage = newStage;
        this.currentState = PokemonState.HAPPY; // Happy after evolution
        this.isEvolutionInProgress = false;
        
        // Resume normal animation with new Pokemon
        loadAndStartAnimation();
    }
    
    /**
     * Gets the evolved species based on the current species and target stage.
     * This maps the evolution lines correctly.
     */
    private PokemonSpecies getEvolvedSpecies(PokemonSpecies baseSpecies, EvolutionStage targetStage) {
        // Map each starter to its evolution line
        switch (baseSpecies) {
            // Kanto starters
            case BULBASAUR:
                return targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.IVYSAUR : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.VENUSAUR : baseSpecies;
            case CHARMANDER:
                return targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.CHARMELEON : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.CHARIZARD : baseSpecies;
            case SQUIRTLE:
                return targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.WARTORTLE : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.BLASTOISE : baseSpecies;
            
            // Johto starters
            case CHIKORITA:
                return targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.BAYLEEF : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.MEGANIUM : baseSpecies;
            case CYNDAQUIL:
                return targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.QUILAVA : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.TYPHLOSION : baseSpecies;
            case TOTODILE:
                return targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.CROCONAW : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.FERALIGATR : baseSpecies;
            
            // Hoenn starters
            case TREECKO:
                return targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.GROVYLE : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.SCEPTILE : baseSpecies;
            case TORCHIC:
                return targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.COMBUSKEN : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.BLAZIKEN : baseSpecies;
            case MUDKIP:
                return targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.MARSHTOMP : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.SWAMPERT : baseSpecies;
            
            default:
                return baseSpecies; // Return same species if no evolution found
        }
    }
    
    // Getters for current state
    public PokemonSpecies getCurrentSpecies() {
        return currentSpecies;
    }
    
    public EvolutionStage getCurrentStage() {
        return currentStage;
    }
    
    public PokemonState getCurrentState() {
        return currentState;
    }
    
    public boolean isEvolutionInProgress() {
        return isEvolutionInProgress;
    }
    
    /**
     * Stops all animations and cleans up resources.
     */
    public void cleanup() {
        if (currentAnimation != null) {
            currentAnimation.stop();
            currentAnimation = null;
        }
    }
}