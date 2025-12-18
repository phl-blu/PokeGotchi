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
        if (currentStage == EvolutionStage.EGG) {
            updateStateWithXP(newState, 25); // Use XP for eggs
        } else {
            updateStateWithStreak(newState, 1); // Use streak for Pokemon
        }
    }
    
    /**
     * Updates the Pokemon's emotional/health state with XP information.
     * Used for eggs only.
     * 
     * @param newState The new Pokemon state
     * @param totalXP Total accumulated XP
     */
    public void updateStateWithXP(PokemonState newState, int totalXP) {
        if (this.currentState != newState && !isEvolutionInProgress) {
            this.currentState = newState;
            if (currentStage == EvolutionStage.EGG) {
                loadAndStartAnimationWithXP(totalXP);
            } else {
                // For non-eggs, just load normal animation
                loadAndStartAnimation();
            }
        }
    }
    
    /**
     * Updates the Pokemon's emotional/health state with streak information.
     * Used for Pokemon after they hatch.
     * 
     * @param newState The new Pokemon state
     * @param streakDays Number of consecutive commit days
     */
    public void updateStateWithStreak(PokemonState newState, int streakDays) {
        if (this.currentState != newState && !isEvolutionInProgress) {
            this.currentState = newState;
            if (currentStage == EvolutionStage.EGG) {
                // For eggs, convert to XP
                int approximateXP = streakDays * 25;
                loadAndStartAnimationWithXP(approximateXP);
            } else {
                // For Pokemon, just load normal animation
                loadAndStartAnimation();
            }
        }
    }
    
    /**
     * Triggers animation when a new commit is detected.
     * For eggs: plays frames 1-4 once at 11 FPS then returns to static frame1
     * For Pokemon: cycles through 2 frames at 2 FPS then returns to static
     * 
     * @param totalXP Total accumulated XP (for egg stage calculation)
     */
    public void triggerCommitAnimation(int totalXP) {
        if (!isEvolutionInProgress) {
            if (currentStage == EvolutionStage.EGG) {
                // For eggs: play single-cycle animation then return to static
                playOnceAnimation(totalXP);
            } else {
                // For Pokemon: set to HAPPY state temporarily
                PokemonState animationState = PokemonState.HAPPY;
                this.currentState = animationState;
                loadAndStartAnimation(); // Use normal animation for Pokemon
                
                // Revert to static state after 3 seconds for Pokemon
                Platform.runLater(() -> {
                    try {
                        Thread.sleep(3000);
                        if (this.currentState == animationState) {
                            this.currentState = PokemonState.CONTENT;
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
     * Triggers animation when a new commit is detected with both XP and streak data.
     * For eggs: uses XP for stage calculation
     * For Pokemon: uses streak for evolution requirements
     * 
     * @param totalXP Total accumulated XP (for egg stage calculation)
     * @param streakDays Current commit streak (for Pokemon evolution)
     */
    public void triggerCommitAnimation(int totalXP, int streakDays) {
        if (!isEvolutionInProgress) {
            if (currentStage == EvolutionStage.EGG) {
                // For eggs: play single-cycle animation then return to static
                playOnceAnimation(totalXP);
            } else {
                // For Pokemon: set to HAPPY state temporarily
                PokemonState animationState = PokemonState.HAPPY;
                this.currentState = animationState;
                loadAndStartAnimation(); // Use normal animation for Pokemon
                
                // Revert to static state after 3 seconds for Pokemon
                Platform.runLater(() -> {
                    try {
                        Thread.sleep(3000);
                        if (this.currentState == animationState) {
                            this.currentState = PokemonState.CONTENT;
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
     * Plays a single-cycle animation for eggs when a commit is detected.
     * Animation plays frames 1-4 once at 11 FPS then returns to static frame1.
     * 
     * @param totalXP Total accumulated XP (for egg stage calculation)
     */
    public void playOnceAnimation(int totalXP) {
        if (currentStage != EvolutionStage.EGG || isEvolutionInProgress) {
            return;
        }
        
        // Stop current animation if running
        if (currentAnimation != null) {
            currentAnimation.stop();
        }
        
        // Load all 4 frames for the animation
        List<Image> animationFrames = AnimationUtils.loadEggSpriteFramesForXP(currentSpecies, totalXP, PokemonState.HAPPY);
        
        if (!animationFrames.isEmpty()) {
            // Create single-cycle animation that returns to static after completion
            currentAnimation = AnimationUtils.createSingleCycleAnimation(
                animationFrames,
                this::updateDisplayedFrame,
                currentSpecies,
                currentStage,
                () -> returnToStaticEgg(totalXP) // Callback when animation completes
            );
            
            if (currentAnimation != null) {
                currentAnimation.play();
            }
        }
    }
    
    /**
     * Returns the egg to static display (frame1 only) after animation completes.
     * 
     * @param totalXP Total accumulated XP (for egg stage calculation)
     */
    private void returnToStaticEgg(int totalXP) {
        if (currentStage == EvolutionStage.EGG) {
            // Load only frame1 for static display
            List<Image> staticFrames = AnimationUtils.loadEggSpriteFramesForXP(currentSpecies, totalXP, PokemonState.CONTENT);
            
            if (!staticFrames.isEmpty()) {
                // Display the first frame statically
                updateDisplayedFrame(staticFrames.get(0));
                
                // Stop any running animation
                if (currentAnimation != null) {
                    currentAnimation.stop();
                    currentAnimation = null;
                }
            }
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
        if (currentStage == EvolutionStage.EGG) {
            // For eggs, always show static - use XP-based loading
            loadAndStartAnimationWithXP(25); // Default to 25 XP (1 day equivalent)
        } else {
            // For Pokemon, load normal sprite animation
            // Stop current animation if running
            if (currentAnimation != null) {
                currentAnimation.stop();
                currentAnimation = null;
            }
            
            // Load sprite frames for current Pokemon configuration
            currentFrames = AnimationUtils.loadSpriteFrames(currentSpecies, currentStage, currentState);
            
            if (!currentFrames.isEmpty()) {
                // Create continuous animation for Pokemon
                currentAnimation = AnimationUtils.createFrameAnimation(
                    currentFrames,
                    this::updateDisplayedFrame,
                    currentSpecies,
                    currentStage
                );
                
                if (currentAnimation != null) {
                    currentAnimation.play();
                    
                    // Also set the first frame immediately
                    updateDisplayedFrame(currentFrames.get(0));
                }
            }
        }
    }
    
    /**
     * Loads sprite frames for the current Pokemon configuration with specific XP data.
     * 
     * @param totalXP Total accumulated XP (for egg stage determination)
     */
    public void loadAndStartAnimationWithXP(int totalXP) {
        // Stop current animation if running
        if (currentAnimation != null) {
            currentAnimation.stop();
            currentAnimation = null;
        }
        
        // Load sprite frames for current configuration
        if (currentStage == EvolutionStage.EGG) {
            // For eggs, ALWAYS show static display unless explicitly animating
            // Only animate during commit-triggered single-cycle animations
            currentFrames = AnimationUtils.loadEggSpriteFramesForXP(currentSpecies, totalXP, PokemonState.CONTENT);
            
            // Display first frame statically without any continuous animation
            if (!currentFrames.isEmpty()) {
                updateDisplayedFrame(currentFrames.get(0));
            }
            return; // Never start continuous animation for eggs - they should be static
        } else {
            // Use normal Pokemon sprite loading for non-egg stages
            currentFrames = AnimationUtils.loadSpriteFrames(currentSpecies, currentStage, currentState);
            
            if (!currentFrames.isEmpty()) {
                // For Pokemon, create continuous animation
                currentAnimation = AnimationUtils.createFrameAnimation(
                    currentFrames,
                    this::updateDisplayedFrame,
                    currentSpecies,
                    currentStage
                );
                
                if (currentAnimation != null) {
                    currentAnimation.play();
                    
                    // Also set the first frame immediately
                    updateDisplayedFrame(currentFrames.get(0));
                }
            }
        }
    }
    
    /**
     * Loads sprite frames for the current Pokemon configuration with specific streak data.
     * 
     * @deprecated Use loadAndStartAnimationWithXP() instead for XP-based progression
     * @param streakDays Number of consecutive commit days (for egg stage determination)
     */
    @Deprecated
    public void loadAndStartAnimationWithStreak(int streakDays) {
        // Convert streak days to approximate XP for backward compatibility
        int approximateXP = streakDays * 25; // Assume 25 XP per day
        loadAndStartAnimationWithXP(approximateXP);
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
     * This maps the evolution lines correctly for the updated Pokemon list.
     */
    private PokemonSpecies getEvolvedSpecies(PokemonSpecies baseSpecies, EvolutionStage targetStage) {
        // Map each starter to its evolution line
        switch (baseSpecies) {
            // 1. Charmander line (Kanto Fire)
            case CHARMANDER:
                return targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.CHARMELEON : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.CHARIZARD : baseSpecies;
            
            // 2. Cyndaquil line (Johto Fire)
            case CYNDAQUIL:
                return targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.QUILAVA : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.TYPHLOSION : baseSpecies;
            
            // 3. Mudkip line (Hoenn Water)
            case MUDKIP:
                return targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.MARSHTOMP : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.SWAMPERT : baseSpecies;
            
            // 4. Piplup line (Sinnoh Water)
            case PIPLUP:
                return targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.PRINPLUP : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.EMPOLEON : baseSpecies;
            
            // 5. Snivy line (Unova Grass)
            case SNIVY:
                return targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.SERVINE : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.SERPERIOR : baseSpecies;
            
            // 6. Froakie line (Kalos Water)
            case FROAKIE:
                return targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.FROGADIER : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.GRENINJA : baseSpecies;
            
            // 7. Rowlet line (Alola Grass)
            case ROWLET:
                return targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.DARTRIX : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.DECIDUEYE : baseSpecies;
            
            // 8. Grookey line (Galar Grass)
            case GROOKEY:
                return targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.THWACKEY : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.RILLABOOM : baseSpecies;
            
            // 9. Fuecoco line (Paldea Fire)
            case FUECOCO:
                return targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.CROCALOR : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.SKELEDIRGE : baseSpecies;
            
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
     * FOR TESTING ONLY: Forces evolution to the next stage regardless of requirements.
     * This bypasses the normal XP and streak requirements for testing purposes.
     * TODO: REMOVE THIS METHOD BEFORE PRODUCTION - See TODO.md
     */
    public void forceEvolutionForTesting() {
        EvolutionStage nextStage = getNextEvolutionStage();
        if (nextStage != null) {
            System.out.println("🧪 TESTING: Forcing evolution from " + currentStage + " to " + nextStage);
            triggerEvolution(nextStage);
        } else {
            System.out.println("🧪 TESTING: Already at max evolution stage: " + currentStage);
        }
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