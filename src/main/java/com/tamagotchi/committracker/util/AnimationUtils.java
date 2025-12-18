package com.tamagotchi.committracker.util;

import javafx.scene.image.Image;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import java.util.List;
import java.util.ArrayList;
import java.io.InputStream;
import java.util.function.Consumer;

import com.tamagotchi.committracker.pokemon.PokemonSpecies;
import com.tamagotchi.committracker.pokemon.PokemonState;
import com.tamagotchi.committracker.pokemon.EvolutionStage;

/**
 * Utility class for animation and sprite management.
 * Handles loading and cycling through Pokemon sprite frames for smooth animation.
 */
public class AnimationUtils {
    
    private static final double FRAME_DURATION_MS = 500.0; // 500ms per frame for 2 FPS animation
    private static final double EGG_FRAME_DURATION_MS = 91.0; // ~91ms per frame for 11 FPS egg animation
    private static final int SPRITE_SIZE = 64; // 64x64 pixel sprites
    
    /**
     * Loads sprite frames for a specific Pokemon species, evolution stage, and animation state.
     * 
     * @param species The Pokemon species
     * @param stage The evolution stage
     * @param state The animation state
     * @return List of Image objects representing animation frames
     */
    public static List<Image> loadSpriteFrames(PokemonSpecies species, EvolutionStage stage, PokemonState state) {
        List<Image> frames = new ArrayList<>();
        
        // Special handling for EGG stage - use universal egg sprites
        if (stage == EvolutionStage.EGG) {
            return loadEggSpriteFrames(species, state);
        }
        
        // Get the base species folder name (e.g., charmander for all Charmander evolutions)
        String speciesFolder = getBaseSpeciesFolder(species);
        
        // Build the base path for sprites
        String basePath = "/pokemon/sprites/" + speciesFolder + "/" + 
                         stage.name().toLowerCase() + "/" + state.name().toLowerCase();
        
        // Try to load up to 8 frames for the animation (flexible frame count)
        for (int i = 1; i <= 8; i++) {
            String framePath = basePath + "/frame" + i + ".png";
            InputStream frameStream = AnimationUtils.class.getResourceAsStream(framePath);
            
            if (frameStream != null) {
                try {
                    Image frame = new Image(frameStream, SPRITE_SIZE, SPRITE_SIZE, true, true);
                    frames.add(frame);
                    frameStream.close();
                } catch (Exception e) {
                    // If frame loading fails, continue to next frame
                    System.err.println("Failed to load frame: " + framePath + " - " + e.getMessage());
                }
            } else {
                // If no more frames exist, break the loop
                break;
            }
        }
        
        // If no frames were loaded, try to load from 'content' state as fallback
        if (frames.isEmpty() && !state.equals(PokemonState.CONTENT)) {
            String speciesFolderFallback = getBaseSpeciesFolder(species);
            String contentPath = "/pokemon/sprites/" + speciesFolderFallback + "/" + 
                               stage.name().toLowerCase() + "/content";
            
            for (int i = 1; i <= 8; i++) {
                String framePath = contentPath + "/frame" + i + ".png";
                InputStream frameStream = AnimationUtils.class.getResourceAsStream(framePath);
                
                if (frameStream != null) {
                    try {
                        Image frame = new Image(frameStream, SPRITE_SIZE, SPRITE_SIZE, true, true);
                        frames.add(frame);
                        frameStream.close();
                    } catch (Exception e) {
                        System.err.println("Failed to load fallback frame: " + framePath);
                    }
                } else {
                    break;
                }
            }
        }
        
        // If still no frames, use the generic fallback
        if (frames.isEmpty()) {
            frames.add(createFallbackImage(species, stage));
            System.out.println("🖼️ Using fallback image for " + species + "/" + stage + "/" + state);
        } else {
            System.out.println("🖼️ Loaded " + frames.size() + " frames for " + species + "/" + stage + "/" + state);
        }
        return frames;
    }
    
    /**
     * Loads Pokemon-specific egg sprite frames based on commit streak days and animation state.
     * 
     * @param species The Pokemon species (determines which egg sprites to load)
     * @param state The animation state (determines if animating or static)
     * @return List of Image objects representing egg animation frames
     */
    public static List<Image> loadEggSpriteFrames(PokemonSpecies species, PokemonState state) {
        List<Image> frames = new ArrayList<>();
        
        // Determine egg stage based on commit streak (this will be passed from the component)
        // For now, default to stage1, but this will be updated by loadEggSpriteFramesForStreak
        int eggStage = 1; // This will be overridden by loadEggSpriteFramesForStreak
        
        return loadPokemonEggSpriteFramesForStage(species, eggStage, state);
    }
    
    /**
     * Loads Pokemon-specific egg sprite frames for a specific stage and animation state.
     * 
     * @param species The Pokemon species (determines which egg folder to use)
     * @param eggStage The egg stage (1-4 based on XP days)
     * @param state The animation state (determines if animating or static)
     * @return List of Image objects representing egg animation frames
     */
    public static List<Image> loadPokemonEggSpriteFramesForStage(PokemonSpecies species, int eggStage, PokemonState state) {
        List<Image> frames = new ArrayList<>();
        
        // Clamp egg stage to valid range
        eggStage = Math.max(1, Math.min(4, eggStage));
        
        // Get the base species folder name for the egg
        String speciesFolder = getBaseSpeciesFolder(species);
        String basePath = "/pokemon/sprites/" + speciesFolder + "/egg/stage" + eggStage;
        
        // Load frames based on animation state
        if (isAnimationState(state)) {
            // Load all 4 frames for animation when commit is detected
            for (int i = 1; i <= 4; i++) {
                String framePath = basePath + "/frame" + i + ".png";
                InputStream frameStream = AnimationUtils.class.getResourceAsStream(framePath);
                
                if (frameStream != null) {
                    try {
                        Image frame = new Image(frameStream, SPRITE_SIZE, SPRITE_SIZE, true, true);
                        frames.add(frame);
                        frameStream.close();
                    } catch (Exception e) {
                        System.err.println("Failed to load " + species + " egg frame: " + framePath + " - " + e.getMessage());
                    }
                }
            }
        } else {
            // Load only frame1 for static display when no recent commits
            String framePath = basePath + "/frame1.png";
            InputStream frameStream = AnimationUtils.class.getResourceAsStream(framePath);
            
            if (frameStream != null) {
                try {
                    Image frame = new Image(frameStream, SPRITE_SIZE, SPRITE_SIZE, true, true);
                    frames.add(frame);
                    frameStream.close();
                } catch (Exception e) {
                    System.err.println("Failed to load " + species + " egg static frame: " + framePath + " - " + e.getMessage());
                }
            }
        }
        
        // Fallback if no frames loaded
        if (frames.isEmpty()) {
            frames.add(createFallbackImage(species, EvolutionStage.EGG));
            System.out.println("🥚 Using fallback egg image for " + species + " stage " + eggStage);
        } else {
            String animationType = isAnimationState(state) ? "animated (" + frames.size() + " frames)" : "static (frame1 only)";
            System.out.println("🥚 Loaded " + species + " egg for stage " + eggStage + " (" + animationType + ")");
        }
        
        return frames;
    }
    
    /**
     * Determines if the given state should trigger animation.
     * 
     * @param state The Pokemon state
     * @return true if the state should animate, false for static display
     */
    private static boolean isAnimationState(PokemonState state) {
        // Animation triggers for these states (when commits are detected)
        return state == PokemonState.HAPPY || 
               state == PokemonState.THRIVING || 
               state == PokemonState.EVOLVING;
    }
    
    /**
     * Gets the egg stage based on XP days instead of streak days.
     * XP days are calculated from total accumulated XP.
     * 
     * @param totalXP Total accumulated XP
     * @return Egg stage (1-4)
     */
    public static int getEggStageFromXPDays(int totalXP) {
        // Calculate XP days: assuming average 25 XP per day (base 10 + bonuses)
        // This maps XP to equivalent "days of activity"
        int xpDays = Math.max(1, totalXP / 25);
        
        if (xpDays <= 1) {
            return 1; // Stage 1: Fresh egg (1 day XP)
        } else if (xpDays == 2) {
            return 2; // Stage 2: Barely cracked (2 days XP)
        } else if (xpDays == 3) {
            return 3; // Stage 3: More cracked (3 days XP)
        } else {
            return 4; // Stage 4: Very cracked, ready to hatch (4+ days XP)
        }
    }
    
    /**
     * Gets the egg stage based on commit streak days.
     * 
     * @deprecated Use getEggStageFromXPDays() instead for XP-based progression
     * @param streakDays Number of consecutive commit days
     * @return Egg stage (1-4)
     */
    @Deprecated
    public static int getEggStageFromStreak(int streakDays) {
        if (streakDays <= 1) {
            return 1; // Basic egg
        } else if (streakDays == 2) {
            return 2; // Barely cracked
        } else if (streakDays == 3) {
            return 3; // More cracked
        } else {
            return 4; // Very cracked (ready to hatch)
        }
    }
    
    /**
     * Loads Pokemon-specific egg sprite frames based on XP days and animation state.
     * This is the main method to be called by components for XP-based progression.
     * 
     * @param species The Pokemon species (determines which egg sprites to load)
     * @param totalXP Total accumulated XP
     * @param state The animation state (determines if animating or static)
     * @return List of Image objects representing egg animation frames
     */
    public static List<Image> loadEggSpriteFramesForXP(PokemonSpecies species, int totalXP, PokemonState state) {
        int eggStage = getEggStageFromXPDays(totalXP);
        return loadPokemonEggSpriteFramesForStage(species, eggStage, state);
    }
    
    /**
     * Loads Pokemon-specific egg sprite frames for a specific streak and animation state.
     * 
     * @deprecated Use loadEggSpriteFramesForXP() instead for XP-based progression
     * @param species The Pokemon species (determines which egg sprites to load)
     * @param streakDays Number of consecutive commit days
     * @param state The animation state (determines if animating or static)
     * @return List of Image objects representing egg animation frames
     */
    @Deprecated
    public static List<Image> loadEggSpriteFramesForStreak(PokemonSpecies species, int streakDays, PokemonState state) {
        int eggStage = getEggStageFromStreak(streakDays);
        return loadPokemonEggSpriteFramesForStage(species, eggStage, state);
    }
    
    /**
     * Creates a fallback image when sprite frames cannot be loaded.
     * This ensures the animation system always has at least one frame to display.
     */
    private static Image createFallbackImage(PokemonSpecies species, EvolutionStage stage) {
        // Try to load a generic fallback sprite
        String fallbackPath = "/pokemon/sprites/fallback/" + stage.name().toLowerCase() + ".png";
        InputStream fallbackStream = AnimationUtils.class.getResourceAsStream(fallbackPath);
        
        if (fallbackStream != null) {
            try {
                Image fallback = new Image(fallbackStream, SPRITE_SIZE, SPRITE_SIZE, true, true);
                fallbackStream.close();
                return fallback;
            } catch (Exception e) {
                System.err.println("Failed to load fallback image: " + e.getMessage());
            }
        }
        
        // If even fallback fails, create a simple colored rectangle as last resort
        // This would be replaced with actual sprite loading in a real implementation
        return new Image(AnimationUtils.class.getResourceAsStream("/pokemon/sprites/.gitkeep"));
    }
    
    /**
     * Creates a Timeline animation that plays once through the provided frames.
     * Used for commit-triggered egg animations that play once then return to static.
     * 
     * @param frames List of Image frames to animate
     * @param frameUpdateCallback Callback function to update the displayed frame
     * @param species Pokemon species (for individual animation speed)
     * @param stage Evolution stage (affects animation speed for eggs)
     * @param onComplete Callback when animation completes
     * @return Timeline animation object that plays once
     */
    public static Timeline createSingleCycleAnimation(List<Image> frames, Consumer<Image> frameUpdateCallback, 
                                                    PokemonSpecies species, EvolutionStage stage, Runnable onComplete) {
        if (frames.isEmpty()) {
            return new Timeline(); // Return empty timeline if no frames
        }
        
        Timeline timeline = new Timeline();
        
        // Get Pokemon-specific animation speed based on species and stage
        double frameDuration = getPokemonAnimationSpeed(species, stage);
        double fps = 1000.0 / frameDuration;
        
        // Log the single-cycle animation creation
        String stageInfo = stage == EvolutionStage.EGG ? "EGG" : stage.name();
        System.out.println("🎬 Creating single-cycle animation for " + species + "/" + stageInfo + " with " + frames.size() + 
                          " frames at " + frameDuration + "ms per frame (~" + String.format("%.1f", fps) + " FPS)");
        
        // Create keyframes for each sprite frame
        for (int i = 0; i < frames.size(); i++) {
            final int frameIndex = i;
            KeyFrame keyFrame = new KeyFrame(
                Duration.millis(i * frameDuration),
                e -> frameUpdateCallback.accept(frames.get(frameIndex))
            );
            timeline.getKeyFrames().add(keyFrame);
        }
        
        // Add completion callback at the end
        KeyFrame endFrame = new KeyFrame(
            Duration.millis(frames.size() * frameDuration),
            e -> {
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        );
        timeline.getKeyFrames().add(endFrame);
        
        // Set to play only once
        timeline.setCycleCount(1);
        
        return timeline;
    }
    
    /**
     * Creates a Timeline animation that cycles through the provided frames.
     * Uses Pokemon-specific animation speed for unique personalities.
     * 
     * @param frames List of Image frames to animate
     * @param frameUpdateCallback Callback function to update the displayed frame
     * @param species Pokemon species (for individual animation speed)
     * @param stage Evolution stage (affects animation speed for eggs)
     * @return Timeline animation object
     */
    public static Timeline createFrameAnimation(List<Image> frames, Consumer<Image> frameUpdateCallback, 
                                              PokemonSpecies species, EvolutionStage stage) {
        if (frames.isEmpty()) {
            return new Timeline(); // Return empty timeline if no frames
        }
        
        Timeline timeline = new Timeline();
        
        // Get Pokemon-specific animation speed based on species and stage
        double frameDuration = getPokemonAnimationSpeed(species, stage);
        double fps = 1000.0 / frameDuration;
        
        // Log how many frames we're animating with Pokemon-specific speed
        String stageInfo = stage == EvolutionStage.EGG ? "EGG" : stage.name();
        System.out.println("🎬 Creating animation for " + species + "/" + stageInfo + " with " + frames.size() + 
                          " frames at " + frameDuration + "ms per frame (~" + String.format("%.1f", fps) + " FPS)");
        
        // Create keyframes for each sprite frame
        // Each frame starts at its designated time slot
        for (int i = 0; i < frames.size(); i++) {
            final int frameIndex = i;
            KeyFrame keyFrame = new KeyFrame(
                Duration.millis(i * frameDuration),
                e -> frameUpdateCallback.accept(frames.get(frameIndex))
            );
            timeline.getKeyFrames().add(keyFrame);
        }
        
        // Add a final empty keyframe to ensure the last frame displays for the full duration
        // This makes the total cycle = frames.size() * frameDuration
        // Without this, the timeline restarts immediately after the last keyframe triggers
        KeyFrame endFrame = new KeyFrame(
            Duration.millis(frames.size() * frameDuration)
        );
        timeline.getKeyFrames().add(endFrame);
        
        // Set the timeline to cycle indefinitely for continuous animation
        timeline.setCycleCount(Timeline.INDEFINITE);
        
        return timeline;
    }
    
    /**
     * Creates a Timeline animation that cycles through the provided frames.
     * Uses Pokemon-specific animation speed for unique personalities.
     * 
     * @param frames List of Image frames to animate
     * @param frameUpdateCallback Callback function to update the displayed frame
     * @param species Pokemon species (for individual animation speed)
     * @return Timeline animation object
     */
    public static Timeline createFrameAnimation(List<Image> frames, Consumer<Image> frameUpdateCallback, PokemonSpecies species) {
        return createFrameAnimation(frames, frameUpdateCallback, species, EvolutionStage.BASIC);
    }
    
    /**
     * Creates a Timeline animation that cycles through the provided frames.
     * Uses default 2 FPS speed (for backward compatibility).
     * 
     * @param frames List of Image frames to animate
     * @param frameUpdateCallback Callback function to update the displayed frame
     * @return Timeline animation object
     */
    public static Timeline createFrameAnimation(List<Image> frames, Consumer<Image> frameUpdateCallback) {
        return createFrameAnimation(frames, frameUpdateCallback, PokemonSpecies.CHARMANDER); // Default to Charmander speed
    }
    
    /**
     * Creates a special evolution animation sequence that transitions between stages.
     * 
     * @param fromSpecies Starting Pokemon species
     * @param toSpecies Target Pokemon species after evolution
     * @param fromStage Starting evolution stage
     * @param toStage Target evolution stage
     * @param evolutionCompleteCallback Callback when evolution animation completes
     * @return Timeline for evolution animation
     */
    public static Timeline createEvolutionAnimation(PokemonSpecies fromSpecies, PokemonSpecies toSpecies,
                                                   EvolutionStage fromStage, EvolutionStage toStage,
                                                   Consumer<Image> frameUpdateCallback,
                                                   Runnable evolutionCompleteCallback) {
        
        Timeline evolutionTimeline = new Timeline();
        
        // Load evolution effect frames (generic evolution animation)
        List<Image> evolutionFrames = loadEvolutionEffectFrames();
        
        // Create evolution sequence: flash effect -> new Pokemon
        for (int i = 0; i < evolutionFrames.size(); i++) {
            final int frameIndex = i;
            KeyFrame keyFrame = new KeyFrame(
                Duration.millis(i * 200), // Faster frames for evolution effect
                e -> frameUpdateCallback.accept(evolutionFrames.get(frameIndex))
            );
            evolutionTimeline.getKeyFrames().add(keyFrame);
        }
        
        // Final keyframe shows the new evolved Pokemon
        List<Image> newPokemonFrames = loadSpriteFrames(toSpecies, toStage, PokemonState.HAPPY);
        if (!newPokemonFrames.isEmpty()) {
            KeyFrame finalFrame = new KeyFrame(
                Duration.millis(evolutionFrames.size() * 200),
                e -> {
                    frameUpdateCallback.accept(newPokemonFrames.get(0));
                    evolutionCompleteCallback.run();
                }
            );
            evolutionTimeline.getKeyFrames().add(finalFrame);
        }
        
        return evolutionTimeline;
    }
    
    /**
     * Loads generic evolution effect frames (sparkles, light effects, etc.)
     */
    private static List<Image> loadEvolutionEffectFrames() {
        List<Image> evolutionFrames = new ArrayList<>();
        
        // Try to load evolution effect frames
        for (int i = 1; i <= 6; i++) {
            String effectPath = "/pokemon/sprites/effects/evolution/frame" + i + ".png";
            InputStream effectStream = AnimationUtils.class.getResourceAsStream(effectPath);
            
            if (effectStream != null) {
                try {
                    Image frame = new Image(effectStream, SPRITE_SIZE, SPRITE_SIZE, true, true);
                    evolutionFrames.add(frame);
                    effectStream.close();
                } catch (Exception e) {
                    System.err.println("Failed to load evolution effect frame: " + effectPath);
                }
            }
        }
        
        // If no evolution frames found, create a simple flash effect
        if (evolutionFrames.isEmpty()) {
            // Add a simple white flash frame as fallback
            evolutionFrames.add(createFallbackImage(PokemonSpecies.CHARMANDER, EvolutionStage.BASIC));
        }
        
        return evolutionFrames;
    }
    
    /**
     * Gets the appropriate animation state based on Pokemon emotional state.
     * Maps PokemonState to animation-friendly states.
     */
    public static PokemonState getAnimationState(PokemonState pokemonState) {
        // For now, return the same state - this could be expanded to map
        // complex states to simpler animation states if needed
        return pokemonState;
    }
    
    /**
     * Returns the frame duration for all Pokemon states.
     * All animations run at a consistent 2 FPS (500ms per frame).
     */
    public static double getFrameDuration(PokemonState state) {
        return FRAME_DURATION_MS; // 2 FPS - Consistent speed for all states (500ms)
    }
    
    /**
     * Gets the animation speed (frame duration) for a specific Pokemon species and stage.
     * 
     * @param species The Pokemon species
     * @param stage The evolution stage
     * @return Frame duration in milliseconds
     */
    public static double getPokemonAnimationSpeed(PokemonSpecies species, EvolutionStage stage) {
        if (stage == EvolutionStage.EGG) {
            return EGG_FRAME_DURATION_MS; // ~91ms = 11 FPS for egg animations
        }
        
        // All Pokemon use consistent 2 FPS for basic idle animations
        // This ensures smooth, consistent animation across all species
        // Each Pokemon uses exactly 2 frames (frame1.png, frame2.png)
        return FRAME_DURATION_MS; // 500ms = 2 FPS for all Pokemon
    }
    
    /**
     * Gets the animation speed (frame duration) for a specific Pokemon species.
     * All basic idle animations run at 2 FPS with 2 frames for consistency.
     * 
     * @param species The Pokemon species
     * @return Frame duration in milliseconds (500ms = 2 FPS)
     */
    public static double getPokemonAnimationSpeed(PokemonSpecies species) {
        // All Pokemon now use consistent 2 FPS for basic idle animations
        // This ensures smooth, consistent animation across all species
        // Each Pokemon uses exactly 2 frames (frame1.png, frame2.png)
        return FRAME_DURATION_MS; // 500ms = 2 FPS for all Pokemon
    }
    
    /**
     * Gets the base species folder name for sprite loading.
     * All evolutions of a Pokemon line share the same base folder.
     * e.g., Charmander, Charmeleon, Charizard all use "charmander" folder
     */
    private static String getBaseSpeciesFolder(PokemonSpecies species) {
        switch (species) {
            // 1. Charmander line (Kanto Fire)
            case CHARMANDER:
            case CHARMELEON:
            case CHARIZARD:
                return "charmander";
            
            // 2. Cyndaquil line (Johto Fire)
            case CYNDAQUIL:
            case QUILAVA:
            case TYPHLOSION:
                return "cyndaquil";
            
            // 3. Mudkip line (Hoenn Water)
            case MUDKIP:
            case MARSHTOMP:
            case SWAMPERT:
                return "mudkip";
            
            // 4. Piplup line (Sinnoh Water)
            case PIPLUP:
            case PRINPLUP:
            case EMPOLEON:
                return "piplup";
            
            // 5. Snivy line (Unova Grass)
            case SNIVY:
            case SERVINE:
            case SERPERIOR:
                return "snivy";
            
            // 6. Froakie line (Kalos Water)
            case FROAKIE:
            case FROGADIER:
            case GRENINJA:
                return "froakie";
            
            // 7. Rowlet line (Alola Grass)
            case ROWLET:
            case DARTRIX:
            case DECIDUEYE:
                return "rowlet";
            
            // 8. Grookey line (Galar Grass)
            case GROOKEY:
            case THWACKEY:
            case RILLABOOM:
                return "grookey";
            
            // 9. Fuecoco line (Paldea Fire)
            case FUECOCO:
            case CROCALOR:
            case SKELEDIRGE:
                return "fuecoco";
            
            default:
                return species.name().toLowerCase();
        }
    }
}