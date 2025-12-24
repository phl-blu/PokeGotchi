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
    
    /**
     * Listener interface for reset events.
     * TODO: REMOVE THIS INTERFACE BEFORE PRODUCTION - See TODO.md
     */
    @FunctionalInterface
    public interface ResetListener {
        void onReset();
    }
    
    /**
     * Listener interface for evolution completion events.
     * Called when a Pokemon evolution animation completes and the stage changes.
     */
    @FunctionalInterface
    public interface EvolutionListener {
        void onEvolutionComplete(PokemonSpecies newSpecies, EvolutionStage newStage);
    }
    
    private ImageView pokemonImageView;
    private Timeline currentAnimation;
    
    // Current Pokemon state
    private PokemonSpecies currentSpecies;
    private EvolutionStage currentStage;
    private PokemonState currentState;
    
    // Animation state
    private boolean isEvolutionInProgress;
    private List<Image> currentFrames;
    
    // Reset listener for notifying when Pokemon is reset
    // TODO: REMOVE THIS FIELD BEFORE PRODUCTION - See TODO.md
    private ResetListener resetListener;
    
    // Evolution listener for notifying when evolution completes
    private EvolutionListener evolutionListener;
    
    /**
     * Creates a new Pokemon Display Component with default settings.
     * Note: This constructor should only be used when a species will be set later.
     * For proper initialization, use the constructor that takes a PokemonSpecies.
     * 
     * @deprecated Use PokemonDisplayComponent(PokemonSpecies, EvolutionStage, PokemonState) instead
     */
    @Deprecated
    public PokemonDisplayComponent() {
        initializeComponent();
        
        // Initialize with null species - must be set via changeSpecies() before use
        this.currentSpecies = null;
        this.currentStage = EvolutionStage.EGG;
        this.currentState = PokemonState.CONTENT;
        this.isEvolutionInProgress = false;
        
        // Don't load animation - no species selected yet
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
            updateStateWithXP(newState, 0); // Use 0 XP for eggs (stage 1)
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
                // For eggs, convert to XP (approximately 10 XP per day)
                int approximateXP = streakDays * 10;
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
            System.out.println("🥚 Cannot play egg animation - not an egg or evolution in progress");
            return;
        }
        
        System.out.println("🥚 Triggering egg animation! XP: " + totalXP);
        
        // Stop current animation if running
        if (currentAnimation != null) {
            currentAnimation.stop();
        }
        
        // Load all 4 frames for the animation
        List<Image> animationFrames = AnimationUtils.loadEggSpriteFramesForXP(currentSpecies, totalXP, PokemonState.HAPPY);
        
        if (!animationFrames.isEmpty()) {
            System.out.println("🎬 Starting egg animation with " + animationFrames.size() + " frames");
            
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
                System.out.println("🎬 Egg animation started successfully!");
            } else {
                System.out.println("❌ Failed to create egg animation");
            }
        } else {
            System.out.println("❌ No animation frames loaded for egg");
        }
    }
    
    /**
     * Returns the egg to static display (frame1 only) after animation completes.
     * 
     * @param totalXP Total accumulated XP (for egg stage calculation)
     */
    private void returnToStaticEgg(int totalXP) {
        if (currentStage == EvolutionStage.EGG) {
            System.out.println("🥚 Returning egg to static display");
            
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
                
                System.out.println("🥚 Egg returned to static state");
            } else {
                System.out.println("❌ Failed to load static egg frame");
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
        // Don't restart animation during evolution
        if (isEvolutionInProgress) {
            System.out.println("⚠️ Skipping loadAndStartAnimation - evolution in progress");
            return;
        }
        
        if (currentStage == EvolutionStage.EGG) {
            // For eggs, always show static - use XP-based loading
            loadAndStartAnimationWithXP(0); // Default to 0 XP (stage 1 egg)
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
        // Don't restart animation during evolution
        if (isEvolutionInProgress) {
            System.out.println("⚠️ Skipping loadAndStartAnimationWithXP - evolution in progress");
            return;
        }
        
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
        int approximateXP = streakDays * 10; // Assume 10 XP per day
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
                // Hatch from egg: EITHER 4+ day streak OR 60+ XP (whichever comes first)
                canEvolve = (streakDays >= 4 || xpLevel >= 60);
                break;
            case STAGE_1:
                // First evolution: ONLY 11+ day streak (XP doesn't matter after hatching)
                canEvolve = (streakDays >= 11);
                break;
            case STAGE_2:
                // Final evolution: ONLY 22+ day streak (XP doesn't matter after hatching)
                canEvolve = (streakDays >= 22);
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
        if (newStage == currentStage) {
            System.out.println("⚠️ Evolution skipped: already at stage " + currentStage);
            return;
        }
        
        if (isEvolutionInProgress) {
            System.out.println("⚠️ Evolution skipped: evolution already in progress");
            return;
        }
        
        isEvolutionInProgress = true;
        System.out.println("🌟 Starting evolution: " + currentStage + " -> " + newStage);
        
        // Stop current animation completely
        if (currentAnimation != null) {
            currentAnimation.stop();
            currentAnimation = null;
        }
        
        // Get the evolved Pokemon species
        PokemonSpecies evolvedSpecies = getEvolvedSpecies(currentSpecies, newStage);
        System.out.println("🌟 Evolving species: " + currentSpecies + " -> " + evolvedSpecies);
        
        // Set current state to evolving BEFORE creating animation
        this.currentState = PokemonState.EVOLVING;
        
        // Create evolution animation and store it as currentAnimation
        // This prevents any other animation from interfering
        currentAnimation = AnimationUtils.createEvolutionAnimation(
            currentSpecies, evolvedSpecies,
            currentStage, newStage,
            this::updateDisplayedFrame,
            () -> completeEvolution(evolvedSpecies, newStage)
        );
        
        // Start evolution animation
        currentAnimation.play();
    }
    
    /**
     * Completes the evolution process and resumes normal animation.
     */
    private void completeEvolution(PokemonSpecies newSpecies, EvolutionStage newStage) {
        System.out.println("✅ Evolution complete: " + currentSpecies + " -> " + newSpecies + ", Stage: " + currentStage + " -> " + newStage);
        
        this.currentSpecies = newSpecies;
        this.currentStage = newStage;
        this.currentState = PokemonState.HAPPY; // Happy after evolution
        this.isEvolutionInProgress = false;
        
        System.out.println("✅ Updated state - Species: " + currentSpecies + ", Stage: " + currentStage);
        
        // Resume normal animation with new Pokemon
        loadAndStartAnimation();
        
        // Notify evolution listener that evolution is complete
        if (evolutionListener != null) {
            Platform.runLater(() -> {
                evolutionListener.onEvolutionComplete(newSpecies, newStage);
                System.out.println("🔔 Evolution listener notified: " + newSpecies + " -> " + newStage);
            });
        }
    }
    
    /**
     * Gets the evolved species based on the current species and target stage.
     * This maps the evolution lines correctly for the updated Pokemon list.
     * Handles both base species and already-evolved species.
     */
    private PokemonSpecies getEvolvedSpecies(PokemonSpecies species, EvolutionStage targetStage) {
        // First, get the base species (starter) for this evolution line
        PokemonSpecies baseSpecies = getBaseSpecies(species);
        
        // Map each starter to its evolution line
        switch (baseSpecies) {
            // 1. Charmander line (Kanto Fire)
            case CHARMANDER:
                if (targetStage == EvolutionStage.BASIC) return PokemonSpecies.CHARMANDER;
                if (targetStage == EvolutionStage.STAGE_1) return PokemonSpecies.CHARMELEON;
                if (targetStage == EvolutionStage.STAGE_2) return PokemonSpecies.CHARIZARD;
                return baseSpecies;
            
            // 2. Cyndaquil line (Johto Fire)
            case CYNDAQUIL:
                if (targetStage == EvolutionStage.BASIC) return PokemonSpecies.CYNDAQUIL;
                if (targetStage == EvolutionStage.STAGE_1) return PokemonSpecies.QUILAVA;
                if (targetStage == EvolutionStage.STAGE_2) return PokemonSpecies.TYPHLOSION;
                return baseSpecies;
            
            // 3. Mudkip line (Hoenn Water)
            case MUDKIP:
                if (targetStage == EvolutionStage.BASIC) return PokemonSpecies.MUDKIP;
                if (targetStage == EvolutionStage.STAGE_1) return PokemonSpecies.MARSHTOMP;
                if (targetStage == EvolutionStage.STAGE_2) return PokemonSpecies.SWAMPERT;
                return baseSpecies;
            
            // 4. Piplup line (Sinnoh Water)
            case PIPLUP:
                if (targetStage == EvolutionStage.BASIC) return PokemonSpecies.PIPLUP;
                if (targetStage == EvolutionStage.STAGE_1) return PokemonSpecies.PRINPLUP;
                if (targetStage == EvolutionStage.STAGE_2) return PokemonSpecies.EMPOLEON;
                return baseSpecies;
            
            // 5. Snivy line (Unova Grass)
            case SNIVY:
                if (targetStage == EvolutionStage.BASIC) return PokemonSpecies.SNIVY;
                if (targetStage == EvolutionStage.STAGE_1) return PokemonSpecies.SERVINE;
                if (targetStage == EvolutionStage.STAGE_2) return PokemonSpecies.SERPERIOR;
                return baseSpecies;
            
            // 6. Froakie line (Kalos Water)
            case FROAKIE:
                if (targetStage == EvolutionStage.BASIC) return PokemonSpecies.FROAKIE;
                if (targetStage == EvolutionStage.STAGE_1) return PokemonSpecies.FROGADIER;
                if (targetStage == EvolutionStage.STAGE_2) return PokemonSpecies.GRENINJA;
                return baseSpecies;
            
            // 7. Rowlet line (Alola Grass)
            case ROWLET:
                if (targetStage == EvolutionStage.BASIC) return PokemonSpecies.ROWLET;
                if (targetStage == EvolutionStage.STAGE_1) return PokemonSpecies.DARTRIX;
                if (targetStage == EvolutionStage.STAGE_2) return PokemonSpecies.DECIDUEYE;
                return baseSpecies;
            
            // 8. Grookey line (Galar Grass)
            case GROOKEY:
                if (targetStage == EvolutionStage.BASIC) return PokemonSpecies.GROOKEY;
                if (targetStage == EvolutionStage.STAGE_1) return PokemonSpecies.THWACKEY;
                if (targetStage == EvolutionStage.STAGE_2) return PokemonSpecies.RILLABOOM;
                return baseSpecies;
            
            // 9. Fuecoco line (Paldea Fire)
            case FUECOCO:
                if (targetStage == EvolutionStage.BASIC) return PokemonSpecies.FUECOCO;
                if (targetStage == EvolutionStage.STAGE_1) return PokemonSpecies.CROCALOR;
                if (targetStage == EvolutionStage.STAGE_2) return PokemonSpecies.SKELEDIRGE;
                return baseSpecies;
            
            default:
                return species; // Return same species if no evolution found
        }
    }
    
    /**
     * Gets the base (starter) species for any Pokemon in an evolution line.
     * For example, CHARMELEON and CHARIZARD both return CHARMANDER.
     * 
     * @param species Any Pokemon species
     * @return The base starter species for that evolution line
     */
    private PokemonSpecies getBaseSpecies(PokemonSpecies species) {
        switch (species) {
            // Charmander line
            case CHARMANDER:
            case CHARMELEON:
            case CHARIZARD:
                return PokemonSpecies.CHARMANDER;
            
            // Cyndaquil line
            case CYNDAQUIL:
            case QUILAVA:
            case TYPHLOSION:
                return PokemonSpecies.CYNDAQUIL;
            
            // Mudkip line
            case MUDKIP:
            case MARSHTOMP:
            case SWAMPERT:
                return PokemonSpecies.MUDKIP;
            
            // Piplup line
            case PIPLUP:
            case PRINPLUP:
            case EMPOLEON:
                return PokemonSpecies.PIPLUP;
            
            // Snivy line
            case SNIVY:
            case SERVINE:
            case SERPERIOR:
                return PokemonSpecies.SNIVY;
            
            // Froakie line
            case FROAKIE:
            case FROGADIER:
            case GRENINJA:
                return PokemonSpecies.FROAKIE;
            
            // Rowlet line
            case ROWLET:
            case DARTRIX:
            case DECIDUEYE:
                return PokemonSpecies.ROWLET;
            
            // Grookey line
            case GROOKEY:
            case THWACKEY:
            case RILLABOOM:
                return PokemonSpecies.GROOKEY;
            
            // Fuecoco line
            case FUECOCO:
            case CROCALOR:
            case SKELEDIRGE:
                return PokemonSpecies.FUECOCO;
            
            default:
                return species; // Return same species if unknown
        }
    }
    
    /**
     * Changes the Pokemon species and resets to egg stage.
     * Used when user selects a different Pokemon.
     * 
     * @param newSpecies The new Pokemon species
     */
    public void changeSpecies(PokemonSpecies newSpecies) {
        if (newSpecies == null || newSpecies == currentSpecies) {
            return;
        }
        
        System.out.println("🔄 Changing Pokemon species from " + currentSpecies + " to " + newSpecies);
        
        // Stop current animation
        if (currentAnimation != null) {
            currentAnimation.stop();
            currentAnimation = null;
        }
        
        // Update species and reset to egg
        this.currentSpecies = newSpecies;
        this.currentStage = EvolutionStage.EGG;
        this.currentState = PokemonState.CONTENT;
        this.isEvolutionInProgress = false;
        
        // Load new egg animation
        loadAndStartAnimationWithXP(0);
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
        // Reset evolution in progress flag to allow forced evolution
        // This is needed because the animation might not have completed properly
        if (isEvolutionInProgress) {
            System.out.println("🧪 TESTING: Resetting evolution in progress flag");
            isEvolutionInProgress = false;
        }
        
        EvolutionStage nextStage = getNextEvolutionStage();
        if (nextStage != null) {
            System.out.println("🧪 TESTING: Forcing evolution from " + currentStage + " to " + nextStage);
            System.out.println("🧪 TESTING: Current species: " + currentSpecies);
            triggerEvolution(nextStage);
        } else {
            System.out.println("🧪 TESTING: Already at max evolution stage: " + currentStage);
        }
    }
    
    /**
     * FOR TESTING ONLY: Sets a listener to be notified when Pokemon is reset.
     * TODO: REMOVE THIS METHOD BEFORE PRODUCTION - See TODO.md
     */
    public void setResetListener(ResetListener listener) {
        this.resetListener = listener;
    }
    
    /**
     * Sets a listener to be notified when Pokemon evolution completes.
     * This is called after the evolution animation finishes and the stage changes.
     * 
     * @param listener The listener to notify on evolution completion
     */
    public void setEvolutionListener(EvolutionListener listener) {
        this.evolutionListener = listener;
    }
    
    /**
     * FOR TESTING ONLY: Forces de-evolution back to egg stage 1 (no cracks) with 0 XP.
     * This allows testing of egg behavior without waiting for natural progression.
     * TODO: REMOVE THIS METHOD BEFORE PRODUCTION - See TODO.md
     */
    public void forceDeevolutionToEggForTesting() {
        System.out.println("🧪 TESTING: Forcing de-evolution to EGG stage 1 (0 XP)");
        
        // Stop current animation
        if (currentAnimation != null) {
            currentAnimation.stop();
            currentAnimation = null;
        }
        
        // Reset to egg stage
        this.currentStage = EvolutionStage.EGG;
        this.currentState = PokemonState.CONTENT;
        this.isEvolutionInProgress = false;
        
        // Load egg animation with 0 XP (stage 1 egg - no cracks)
        loadAndStartAnimationWithXP(0);
        
        // Notify listener to reset XP system
        if (resetListener != null) {
            resetListener.onReset();
        }
        
        System.out.println("🥚 TESTING: Pokemon reset to egg stage 1 (0 XP, no cracks)");
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