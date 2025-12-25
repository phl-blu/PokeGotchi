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
import com.tamagotchi.committracker.config.AppConfig;

/**
 * Utility class for animation and sprite management.
 * Handles loading and cycling through Pokemon sprite frames for smooth animation.
 * Optimized with sprite caching and lazy loading for better performance.
 */
public class AnimationUtils {
    
    private static final double FRAME_DURATION_MS = 500.0; // 500ms per frame for 2 FPS animation
    private static final double EGG_FRAME_DURATION_MS = 91.0; // ~91ms per frame for 11 FPS egg animation
    private static final int SPRITE_SIZE = 64; // 64x64 pixel sprites
    
    /**
     * Extended frame durations for evolution animation (~3500ms total).
     * Implements Requirements 3.1, 3.2 for longer, smoother evolution animations.
     * 
     * Animation sequence with glow pulse loop (frames 2→3→2→3):
     * - Frame 1: Old Pokemon with sparkles (400ms)
     * - Frame 2: Glow intensifying (350ms) - first pulse
     * - Frame 3: Silhouette (300ms) - first pulse
     * - Frame 2: Glow intensifying (350ms) - second pulse (loop back)
     * - Frame 3: Silhouette (300ms) - second pulse (loop back)
     * - Frame 4: Peak white flash (200ms) - extended from 150ms
     * - Frame 5: New form silhouette (300ms)
     * - Frame 6: New form fade-in (500ms)
     * - Frame 7: Celebratory sparkles (600ms)
     * Total: ~3300ms + transition overhead ≈ 3500ms
     */
    public static final double[] EXTENDED_FRAME_DURATIONS = {
        400,  // Frame 1: Old form with sparkles
        350,  // Frame 2: Glow intensifying (first pulse)
        300,  // Frame 3: Silhouette (first pulse)
        350,  // Frame 2: Glow (second pulse - loop back)
        300,  // Frame 3: Silhouette (second pulse - loop back)
        200,  // Frame 4: Peak white flash (extended from 150ms)
        300,  // Frame 5: New form silhouette
        500,  // Frame 6: New form fade-in
        600   // Frame 7: Celebratory sparkles
    };
    
    /**
     * Glow pulse loop configuration.
     * Defines which frame indices to use for the pulsing effect (frames 2→3→2→3).
     * Index refers to the evolution effect frame (0-based).
     */
    public static final int[] GLOW_PULSE_FRAME_INDICES = {1, 2, 1, 2}; // Effect frames 2 and 3 (0-indexed)
    
    /**
     * Number of glow pulse loops before the flash.
     */
    public static final int GLOW_PULSE_COUNT = 2;
    
    /**
     * Flash duration in milliseconds (extended from 150ms to 200ms).
     * Requirement 3.4
     */
    public static final double FLASH_DURATION_MS = 200.0;
    
    /**
     * Fade-in duration for new Pokemon reveal in milliseconds.
     * Requirement 3.5
     */
    public static final double FADE_IN_DURATION_MS = 500.0;
    
    /**
     * Celebratory sparkles duration in milliseconds.
     * Requirement 3.6
     */
    public static final double SPARKLES_DURATION_MS = 600.0;
    
    /**
     * Gets the total duration of the extended evolution animation.
     * @return Total duration in milliseconds (~3500ms)
     */
    public static double getExtendedEvolutionDuration() {
        double total = 0;
        for (double duration : EXTENDED_FRAME_DURATIONS) {
            total += duration;
        }
        return total;
    }
    
    /**
     * Loads sprite frames for a specific Pokemon species, evolution stage, and animation state.
     * Uses sprite cache for improved performance when enabled.
     * 
     * @param species The Pokemon species
     * @param stage The evolution stage
     * @param state The animation state
     * @return List of Image objects representing animation frames
     */
    public static List<Image> loadSpriteFrames(PokemonSpecies species, EvolutionStage stage, PokemonState state) {
        // Use sprite cache if enabled
        if (AppConfig.isSpriteCachingEnabled()) {
            return SpriteCache.getInstance().getSpriteFrames(species, stage, state);
        }
        
        // Fallback to direct loading if caching is disabled
        return loadSpriteFramesDirect(species, stage, state);
    }
    
    /**
     * Directly loads sprite frames without caching.
     * Used as fallback when caching is disabled or for cache population.
     */
    public static List<Image> loadSpriteFramesDirect(PokemonSpecies species, EvolutionStage stage, PokemonState state) {
        List<Image> frames = new ArrayList<>();
        
        // Special handling for EGG stage - use universal egg sprites
        if (stage == EvolutionStage.EGG) {
            return loadEggSpriteFramesDirect(species, state);
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
     * Uses sprite cache for improved performance when enabled.
     * 
     * @param species The Pokemon species (determines which egg sprites to load)
     * @param state The animation state (determines if animating or static)
     * @return List of Image objects representing egg animation frames
     */
    public static List<Image> loadEggSpriteFrames(PokemonSpecies species, PokemonState state) {
        // Use sprite cache if enabled
        if (AppConfig.isSpriteCachingEnabled()) {
            // Default to stage 1 egg when no XP specified
            return SpriteCache.getInstance().getEggSpriteFrames(species, 0, state);
        }
        
        // Fallback to direct loading
        return loadEggSpriteFramesDirect(species, state);
    }
    
    /**
     * Directly loads Pokemon-specific egg sprite frames without caching.
     */
    public static List<Image> loadEggSpriteFramesDirect(PokemonSpecies species, PokemonState state) {
        List<Image> frames = new ArrayList<>();
        
        // Determine egg stage based on commit streak (this will be passed from the component)
        // For now, default to stage1, but this will be updated by loadEggSpriteFramesForStreak
        int eggStage = 1; // This will be overridden by loadEggSpriteFramesForStreak
        
        return loadPokemonEggSpriteFramesForStageDirect(species, eggStage, state);
    }
    
    /**
     * Loads Pokemon-specific egg sprite frames for a specific stage and animation state.
     * Uses sprite cache for improved performance when enabled.
     * 
     * @param species The Pokemon species (determines which egg folder to use)
     * @param eggStage The egg stage (1-4 based on XP days)
     * @param state The animation state (determines if animating or static)
     * @return List of Image objects representing egg animation frames
     */
    public static List<Image> loadPokemonEggSpriteFramesForStage(PokemonSpecies species, int eggStage, PokemonState state) {
        // Use sprite cache if enabled
        if (AppConfig.isSpriteCachingEnabled()) {
            int xp = (eggStage - 1) * 15; // Convert stage to approximate XP
            return SpriteCache.getInstance().getEggSpriteFrames(species, xp, state);
        }
        
        // Fallback to direct loading
        return loadPokemonEggSpriteFramesForStageDirect(species, eggStage, state);
    }
    
    /**
     * Directly loads Pokemon-specific egg sprite frames for a specific stage without caching.
     */
    public static List<Image> loadPokemonEggSpriteFramesForStageDirect(PokemonSpecies species, int eggStage, PokemonState state) {
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
     * Gets the egg stage based on total accumulated XP.
     * XP thresholds for egg stage progression:
     * Stage 1: 0-10 XP, Stage 2: 11-25 XP, Stage 3: 26-40 XP, Stage 4: 41-60 XP
     * 
     * @param totalXP Total accumulated XP
     * @return Egg stage (1-4)
     */
    public static int getEggStageFromXPDays(int totalXP) {
        if (totalXP <= 10) {
            return 1; // Stage 1: Fresh egg (0-10 XP)
        } else if (totalXP <= 25) {
            return 2; // Stage 2: Barely cracked (11-25 XP)
        } else if (totalXP <= 40) {
            return 3; // Stage 3: More cracked (26-40 XP)
        } else {
            return 4; // Stage 4: Very cracked, ready to hatch (41-60 XP)
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
     * Loads Pokemon-specific egg sprite frames based on XP and animation state.
     * This is the main method to be called by components for XP-based progression.
     * Uses sprite cache for improved performance when enabled.
     * 
     * XP Progression: Stage 1 (0-10 XP) -> Stage 2 (11-25 XP) -> Stage 3 (26-40 XP) -> Stage 4 (41-60 XP) -> Evolution
     * 
     * @param species The Pokemon species (determines which egg sprites to load)
     * @param totalXP Total accumulated XP
     * @param state The animation state (determines if animating or static)
     * @return List of Image objects representing egg animation frames
     */
    public static List<Image> loadEggSpriteFramesForXP(PokemonSpecies species, int totalXP, PokemonState state) {
        // Use sprite cache if enabled
        if (AppConfig.isSpriteCachingEnabled()) {
            return SpriteCache.getInstance().getEggSpriteFrames(species, totalXP, state);
        }
        
        // Fallback to direct loading
        int eggStage = getEggStageFromXPDays(totalXP);
        return loadPokemonEggSpriteFramesForStageDirect(species, eggStage, state);
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
        return loadPokemonEggSpriteFramesForStageDirect(species, eggStage, state);
    }
    
    /**
     * Creates a fallback image when sprite frames cannot be loaded.
     * This ensures the animation system always has at least one frame to display.
     * Implements graceful degradation for missing sprite files (Requirement 8.3).
     */
    private static Image createFallbackImage(PokemonSpecies species, EvolutionStage stage) {
        // Try multiple fallback strategies in order of preference
        
        // Strategy 1: Try to load a generic fallback sprite for the stage
        String fallbackPath = "/pokemon/sprites/fallback/" + stage.name().toLowerCase() + ".png";
        Image fallback = tryLoadImage(fallbackPath);
        if (fallback != null) {
            ErrorHandler.handleSpriteLoadingError(species + "/" + stage, true);
            return fallback;
        }
        
        // Strategy 2: Try to load any available sprite from the species folder
        String speciesFolder = getBaseSpeciesFolder(species);
        String[] fallbackPaths = {
            "/pokemon/sprites/" + speciesFolder + "/basic/content/frame1.png",
            "/pokemon/sprites/" + speciesFolder + "/egg/stage1/frame1.png",
            "/pokemon/sprites/" + speciesFolder + "/stage_1/content/frame1.png"
        };
        
        for (String path : fallbackPaths) {
            fallback = tryLoadImage(path);
            if (fallback != null) {
                System.out.println("🔄 Using alternative fallback sprite: " + path);
                ErrorHandler.handleSpriteLoadingError(species + "/" + stage, true);
                return fallback;
            }
        }
        
        // Strategy 3: Try to load from any available Pokemon as last resort
        String[] genericFallbacks = {
            "/pokemon/sprites/charmander/basic/content/frame1.png",
            "/pokemon/sprites/charmander/egg/stage1/frame1.png",
            "/pokemon/sprites/cyndaquil/basic/content/frame1.png"
        };
        
        for (String path : genericFallbacks) {
            fallback = tryLoadImage(path);
            if (fallback != null) {
                System.out.println("🔄 Using generic fallback sprite: " + path);
                ErrorHandler.handleSpriteLoadingError(species + "/" + stage, true);
                return fallback;
            }
        }
        
        // Strategy 4: Create a programmatic placeholder image
        fallback = createPlaceholderImage(species, stage);
        if (fallback != null) {
            System.out.println("🔄 Using programmatic placeholder for: " + species + "/" + stage);
            ErrorHandler.handleSpriteLoadingError(species + "/" + stage, true);
            return fallback;
        }
        
        // Final fallback: return null and let caller handle it
        ErrorHandler.handleSpriteLoadingError(species + "/" + stage, false);
        System.err.println("❌ No fallback available for: " + species + "/" + stage);
        return null;
    }
    
    /**
     * Attempts to load an image from the given path.
     * Returns null if loading fails.
     */
    private static Image tryLoadImage(String path) {
        try {
            InputStream stream = AnimationUtils.class.getResourceAsStream(path);
            if (stream != null) {
                Image image = new Image(stream, SPRITE_SIZE, SPRITE_SIZE, true, true);
                stream.close();
                if (!image.isError()) {
                    return image;
                }
            }
        } catch (Exception e) {
            // Silently fail and return null
        }
        return null;
    }
    
    /**
     * Creates a simple programmatic placeholder image when no sprites are available.
     * This ensures the UI always has something to display.
     */
    private static Image createPlaceholderImage(PokemonSpecies species, EvolutionStage stage) {
        try {
            // Create a simple colored placeholder using JavaFX
            // The color is based on the Pokemon type for visual consistency
            int width = SPRITE_SIZE;
            int height = SPRITE_SIZE;
            
            javafx.scene.image.WritableImage writableImage = new javafx.scene.image.WritableImage(width, height);
            javafx.scene.image.PixelWriter pixelWriter = writableImage.getPixelWriter();
            
            // Determine color based on Pokemon type
            javafx.scene.paint.Color baseColor = getPokemonTypeColor(species);
            javafx.scene.paint.Color borderColor = baseColor.darker();
            
            // Draw a simple egg or Pokemon shape
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    // Create an oval shape
                    double centerX = width / 2.0;
                    double centerY = height / 2.0;
                    double radiusX = width / 2.5;
                    double radiusY = height / 2.2;
                    
                    double normalizedX = (x - centerX) / radiusX;
                    double normalizedY = (y - centerY) / radiusY;
                    double distance = normalizedX * normalizedX + normalizedY * normalizedY;
                    
                    if (distance <= 1.0) {
                        if (distance > 0.85) {
                            pixelWriter.setColor(x, y, borderColor);
                        } else {
                            pixelWriter.setColor(x, y, baseColor);
                        }
                    } else {
                        pixelWriter.setColor(x, y, javafx.scene.paint.Color.TRANSPARENT);
                    }
                }
            }
            
            return writableImage;
        } catch (Exception e) {
            System.err.println("Failed to create placeholder image: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Gets a color representing the Pokemon's type for placeholder images.
     */
    private static javafx.scene.paint.Color getPokemonTypeColor(PokemonSpecies species) {
        switch (species) {
            // Fire types - orange/red
            case CHARMANDER:
            case CHARMELEON:
            case CHARIZARD:
            case CYNDAQUIL:
            case QUILAVA:
            case TYPHLOSION:
            case FUECOCO:
            case CROCALOR:
            case SKELEDIRGE:
                return javafx.scene.paint.Color.rgb(255, 140, 80);
            
            // Water types - blue
            case MUDKIP:
            case MARSHTOMP:
            case SWAMPERT:
            case PIPLUP:
            case PRINPLUP:
            case EMPOLEON:
            case FROAKIE:
            case FROGADIER:
            case GRENINJA:
                return javafx.scene.paint.Color.rgb(100, 180, 255);
            
            // Grass types - green
            case SNIVY:
            case SERVINE:
            case SERPERIOR:
            case ROWLET:
            case DARTRIX:
            case DECIDUEYE:
            case GROOKEY:
            case THWACKEY:
            case RILLABOOM:
                return javafx.scene.paint.Color.rgb(120, 200, 120);
            
            default:
                return javafx.scene.paint.Color.rgb(200, 200, 200);
        }
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
            System.out.println("❌ Cannot create single-cycle animation - no frames provided");
            return new Timeline(); // Return empty timeline if no frames
        }
        
        Timeline timeline = new Timeline();
        
        // Get Pokemon-specific animation speed based on species and stage
        double frameDuration = getPokemonAnimationSpeed(species, stage);
        
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
     * Smooth frame rate for evolution animation (60 FPS equivalent).
     * Higher frame rate = smoother transitions.
     */
    private static final double SMOOTH_FRAME_INTERVAL_MS = 33.0; // ~30 FPS for smooth animation
    
    /**
     * Applies ease-in-out interpolation for smoother transitions.
     * Uses cubic easing: slow start, fast middle, slow end.
     * 
     * @param t Progress value from 0.0 to 1.0
     * @return Eased value from 0.0 to 1.0
     */
    private static double easeInOutCubic(double t) {
        return t < 0.5 
            ? 4 * t * t * t 
            : 1 - Math.pow(-2 * t + 2, 3) / 2;
    }
    
    /**
     * Applies ease-out interpolation for smooth deceleration.
     * 
     * @param t Progress value from 0.0 to 1.0
     * @return Eased value from 0.0 to 1.0
     */
    private static double easeOutCubic(double t) {
        return 1 - Math.pow(1 - t, 3);
    }
    
    /**
     * Creates a blended image between two images with smooth interpolation.
     * 
     * @param from Starting image
     * @param to Ending image
     * @param progress Blend progress (0.0 = from, 1.0 = to)
     * @return Blended image
     */
    private static Image blendImages(Image from, Image to, double progress) {
        if (from == null) return to;
        if (to == null) return from;
        if (progress <= 0) return from;
        if (progress >= 1) return to;
        
        try {
            int width = SPRITE_SIZE;
            int height = SPRITE_SIZE;
            
            javafx.scene.image.WritableImage blended = new javafx.scene.image.WritableImage(width, height);
            javafx.scene.image.PixelWriter writer = blended.getPixelWriter();
            javafx.scene.image.PixelReader fromReader = from.getPixelReader();
            javafx.scene.image.PixelReader toReader = to.getPixelReader();
            
            double fromWeight = 1.0 - progress;
            double toWeight = progress;
            
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    javafx.scene.paint.Color fromColor = fromReader.getColor(x, y);
                    javafx.scene.paint.Color toColor = toReader.getColor(x, y);
                    
                    double r = fromColor.getRed() * fromWeight + toColor.getRed() * toWeight;
                    double g = fromColor.getGreen() * fromWeight + toColor.getGreen() * toWeight;
                    double b = fromColor.getBlue() * fromWeight + toColor.getBlue() * toWeight;
                    double a = fromColor.getOpacity() * fromWeight + toColor.getOpacity() * toWeight;
                    
                    writer.setColor(x, y, javafx.scene.paint.Color.color(
                        Math.min(1.0, Math.max(0.0, r)),
                        Math.min(1.0, Math.max(0.0, g)),
                        Math.min(1.0, Math.max(0.0, b)),
                        Math.min(1.0, Math.max(0.0, a))
                    ));
                }
            }
            
            return blended;
        } catch (Exception e) {
            return progress < 0.5 ? from : to;
        }
    }
    
    /**
     * Adds a single blink cycle (glow → silhouette → glow) to the timeline.
     * 
     * @param timeline The timeline to add keyframes to
     * @param callback Frame update callback
     * @param frameGlow The glow frame
     * @param frameSilhouette The silhouette frame
     * @param startTime Starting time in milliseconds
     * @param transitionDuration Duration of each transition in milliseconds
     * @return The end time after this blink cycle
     */
    private static double addBlinkCycle(Timeline timeline, Consumer<Image> callback,
                                        Image frameGlow, Image frameSilhouette,
                                        double startTime, double transitionDuration) {
        double currentTime = startTime;
        int frames = Math.max(2, (int) (transitionDuration / SMOOTH_FRAME_INTERVAL_MS));
        
        // Part A: Glow to silhouette
        for (int i = 0; i <= frames; i++) {
            final double progress = easeInOutCubic((double) i / frames);
            final double time = currentTime + (i * SMOOTH_FRAME_INTERVAL_MS);
            final Image blendedFrame = (frameGlow != null && frameSilhouette != null) 
                ? blendImages(frameGlow, frameSilhouette, progress)
                : frameGlow;
            if (blendedFrame != null) {
                timeline.getKeyFrames().add(new KeyFrame(
                    Duration.millis(time),
                    e -> callback.accept(blendedFrame)
                ));
            }
        }
        currentTime += transitionDuration;
        
        // Part B: Silhouette back to glow
        for (int i = 0; i <= frames; i++) {
            final double progress = easeInOutCubic((double) i / frames);
            final double time = currentTime + (i * SMOOTH_FRAME_INTERVAL_MS);
            final Image blendedFrame = (frameSilhouette != null && frameGlow != null) 
                ? blendImages(frameSilhouette, frameGlow, progress)
                : frameSilhouette;
            if (blendedFrame != null) {
                timeline.getKeyFrames().add(new KeyFrame(
                    Duration.millis(time),
                    e -> callback.accept(blendedFrame)
                ));
            }
        }
        currentTime += transitionDuration;
        
        return currentTime;
    }
    
    /**
     * Adds a blink cycle that ends with a flash (glow → silhouette → flash).
     * 
     * @param timeline The timeline to add keyframes to
     * @param callback Frame update callback
     * @param frameGlow The glow frame
     * @param frameSilhouette The silhouette frame
     * @param frameFlash The flash frame
     * @param startTime Starting time in milliseconds
     * @param transitionDuration Duration of each transition in milliseconds
     * @return The end time after this blink cycle
     */
    private static double addBlinkToFlash(Timeline timeline, Consumer<Image> callback,
                                          Image frameGlow, Image frameSilhouette, Image frameFlash,
                                          double startTime, double transitionDuration) {
        double currentTime = startTime;
        int frames = Math.max(2, (int) (transitionDuration / SMOOTH_FRAME_INTERVAL_MS));
        
        // Part A: Glow to silhouette
        for (int i = 0; i <= frames; i++) {
            final double progress = easeInOutCubic((double) i / frames);
            final double time = currentTime + (i * SMOOTH_FRAME_INTERVAL_MS);
            final Image blendedFrame = (frameGlow != null && frameSilhouette != null) 
                ? blendImages(frameGlow, frameSilhouette, progress)
                : frameGlow;
            if (blendedFrame != null) {
                timeline.getKeyFrames().add(new KeyFrame(
                    Duration.millis(time),
                    e -> callback.accept(blendedFrame)
                ));
            }
        }
        currentTime += transitionDuration;
        
        // Part B: Silhouette to flash
        for (int i = 0; i <= frames; i++) {
            final double progress = easeInOutCubic((double) i / frames);
            final double time = currentTime + (i * SMOOTH_FRAME_INTERVAL_MS);
            final Image blendedFrame = (frameSilhouette != null && frameFlash != null) 
                ? blendImages(frameSilhouette, frameFlash, progress)
                : frameSilhouette;
            if (blendedFrame != null) {
                timeline.getKeyFrames().add(new KeyFrame(
                    Duration.millis(time),
                    e -> callback.accept(blendedFrame)
                ));
            }
        }
        currentTime += transitionDuration;
        
        return currentTime;
    }
    
    /**
     * Creates a special evolution animation sequence that transitions between stages.
     * Uses smooth interpolation with 3 accelerating flashes for dramatic effect.
     * 
     * Animation sequence with 3 accelerating flashes (~3500ms total):
     * - Phase 1: Old Pokemon with sparkles (300ms) - intro
     * - Flash 1: SLOW - glow → silhouette → glow (700ms total)
     * - Flash 2: MEDIUM - glow → silhouette → glow (450ms total)
     * - Flash 3: FAST - glow → silhouette → BRIGHT FLASH (300ms total)
     * - Phase 5: Peak white flash - THE SWAP MOMENT (200ms)
     * - Phase 6: New form silhouette emergence (300ms)
     * - Phase 7: New form fade-in with easing (400ms)
     * - Phase 8: Celebratory sparkles fade-out (500ms)
     * 
     * @param fromSpecies Starting Pokemon species
     * @param toSpecies Target Pokemon species after evolution
     * @param fromStage Starting evolution stage
     * @param toStage Target evolution stage
     * @param frameUpdateCallback Callback to update the displayed frame
     * @param evolutionCompleteCallback Callback when evolution animation completes
     * @return Timeline for evolution animation
     */
    public static Timeline createEvolutionAnimation(PokemonSpecies fromSpecies, PokemonSpecies toSpecies,
                                                   EvolutionStage fromStage, EvolutionStage toStage,
                                                   Consumer<Image> frameUpdateCallback,
                                                   Runnable evolutionCompleteCallback) {
        
        Timeline evolutionTimeline = new Timeline();
        
        // Load sprites
        List<Image> oldPokemonFrames = loadSpriteFrames(fromSpecies, fromStage, PokemonState.CONTENT);
        List<Image> newPokemonFrames = loadSpriteFrames(toSpecies, toStage, PokemonState.HAPPY);
        List<Image> evolutionEffects = loadEvolutionEffectFrames();
        
        Image oldPokemon = oldPokemonFrames.isEmpty() ? null : oldPokemonFrames.get(0);
        Image newPokemon = newPokemonFrames.isEmpty() ? null : newPokemonFrames.get(0);
        
        // Pre-compute frames using EvolutionFrameCache for performance (Requirement 1.1, 1.2)
        EvolutionFrameCache.preComputeEvolutionFrames(
            fromSpecies, toSpecies, fromStage, toStage,
            oldPokemon, newPokemon, evolutionEffects);
        
        // Get cached silhouettes
        String oldSilhouetteKey = EvolutionFrameCache.createSilhouetteCacheKey(fromSpecies, fromStage);
        String newSilhouetteKey = EvolutionFrameCache.createSilhouetteCacheKey(toSpecies, toStage);
        Image oldSilhouette = EvolutionFrameCache.getCachedSilhouette(oldPokemon, oldSilhouetteKey);
        Image newSilhouette = EvolutionFrameCache.getCachedSilhouette(newPokemon, newSilhouetteKey);
        
        // Pre-compute key frames for smooth blending
        Image frameSparkles = (oldPokemon != null && evolutionEffects.size() > 0) 
            ? EvolutionFrameCache.createCompositeFrame(oldPokemon, evolutionEffects.get(0), 1.0) : oldPokemon;
        Image frameGlow = (oldPokemon != null && evolutionEffects.size() > 1) 
            ? EvolutionFrameCache.createCompositeFrame(oldPokemon, evolutionEffects.get(1), 1.0) : oldPokemon;
        Image frameSilhouette = (oldSilhouette != null && evolutionEffects.size() > 2) 
            ? EvolutionFrameCache.createCompositeFrame(oldSilhouette, evolutionEffects.get(2), 1.0) : oldSilhouette;
        Image frameFlash = (evolutionEffects.size() > 3) ? evolutionEffects.get(3) : null;
        Image frameNewSilhouette = (newSilhouette != null && evolutionEffects.size() > 4) 
            ? EvolutionFrameCache.createCompositeFrame(newSilhouette, evolutionEffects.get(4), 1.0) : newSilhouette;
        Image frameNewSparkles = (newPokemon != null && evolutionEffects.size() > 5) 
            ? EvolutionFrameCache.createCompositeFrame(newPokemon, evolutionEffects.get(5), 1.0) : newPokemon;
        
        double currentTime = 0;
        
        // === PHASE 1: Old Pokemon with sparkles (300ms) - intro ===
        double introDuration = 300;
        int introFrames = (int) (introDuration / SMOOTH_FRAME_INTERVAL_MS);
        for (int i = 0; i <= introFrames; i++) {
            final double progress = easeOutCubic((double) i / introFrames);
            final double time = currentTime + (i * SMOOTH_FRAME_INTERVAL_MS);
            final Image blendedFrame = (frameSparkles != null && frameGlow != null) 
                ? blendImages(frameSparkles, frameGlow, progress * 0.3)
                : frameSparkles;
            if (blendedFrame != null) {
                evolutionTimeline.getKeyFrames().add(new KeyFrame(
                    Duration.millis(time),
                    e -> frameUpdateCallback.accept(blendedFrame)
                ));
            }
        }
        currentTime += introDuration;
        
        // === RAPID BLINKING SEQUENCE - Accelerating flashes ===
        // Each blink: glow → silhouette → glow, getting faster each time
        
        // Blink 1: SLOW (200ms per transition = 400ms total)
        currentTime = addBlinkCycle(evolutionTimeline, frameUpdateCallback, 
            frameGlow, frameSilhouette, currentTime, 200);
        
        // Blink 2: MEDIUM (150ms per transition = 300ms total)
        currentTime = addBlinkCycle(evolutionTimeline, frameUpdateCallback, 
            frameGlow, frameSilhouette, currentTime, 150);
        
        // Blink 3: FASTER (100ms per transition = 200ms total)
        currentTime = addBlinkCycle(evolutionTimeline, frameUpdateCallback, 
            frameGlow, frameSilhouette, currentTime, 100);
        
        // Blink 4: FAST (70ms per transition = 140ms total)
        currentTime = addBlinkCycle(evolutionTimeline, frameUpdateCallback, 
            frameGlow, frameSilhouette, currentTime, 70);
        
        // Blink 5: VERY FAST (50ms per transition = 100ms total)
        currentTime = addBlinkCycle(evolutionTimeline, frameUpdateCallback, 
            frameGlow, frameSilhouette, currentTime, 50);
        
        // Blink 6: RAPID (40ms per transition = 80ms total) - building to flash
        currentTime = addBlinkToFlash(evolutionTimeline, frameUpdateCallback, 
            frameGlow, frameSilhouette, frameFlash, currentTime, 40);
        
        // === PEAK WHITE FLASH - THE SWAP MOMENT ===
        // Hold pure white flash for 300ms
        double flashHoldDuration = 300;
        int flashHoldFrames = (int) (flashHoldDuration / SMOOTH_FRAME_INTERVAL_MS);
        for (int i = 0; i <= flashHoldFrames; i++) {
            final double time = currentTime + (i * SMOOTH_FRAME_INTERVAL_MS);
            if (frameFlash != null) {
                evolutionTimeline.getKeyFrames().add(new KeyFrame(
                    Duration.millis(time),
                    e -> frameUpdateCallback.accept(frameFlash)
                ));
            }
        }
        currentTime += flashHoldDuration;
        
        // === DECELERATING BLINK PHASE-OUT ===
        // After the bright flash, blink between new silhouette and new sparkles, gradually slowing down
        
        // Blink 1: FAST (60ms per transition = 120ms total)
        currentTime = addBlinkCycle(evolutionTimeline, frameUpdateCallback, 
            frameNewSilhouette, frameNewSparkles, currentTime, 60);
        
        // Blink 2: MEDIUM (90ms per transition = 180ms total)
        currentTime = addBlinkCycle(evolutionTimeline, frameUpdateCallback, 
            frameNewSilhouette, frameNewSparkles, currentTime, 90);
        
        // Blink 3: SLOW (130ms per transition = 260ms total)
        currentTime = addBlinkCycle(evolutionTimeline, frameUpdateCallback, 
            frameNewSilhouette, frameNewSparkles, currentTime, 130);
        
        // Blink 4: VERY SLOW (180ms per transition = 360ms total) - final phase-out
        currentTime = addBlinkCycle(evolutionTimeline, frameUpdateCallback, 
            frameNewSilhouette, frameNewSparkles, currentTime, 180);
        
        // === NEW FORM FADE-IN (500ms) - Requirement 3.5 ===
        // Transition from silhouette to full visibility over 500ms
        double fadeInDuration = FADE_IN_DURATION_MS; // 500ms from constant
        int fadeInFrames = (int) (fadeInDuration / SMOOTH_FRAME_INTERVAL_MS);
        for (int i = 0; i <= fadeInFrames; i++) {
            final double progress = easeOutCubic((double) i / fadeInFrames);
            final double time = currentTime + (i * SMOOTH_FRAME_INTERVAL_MS);
            
            // Create intermediate frames with increasing opacity
            // Start from new silhouette (0% opacity) to full Pokemon (100% opacity)
            final Image blendedFrame = createOpacityTransition(frameNewSilhouette, newPokemon, progress);
            
            if (blendedFrame != null) {
                evolutionTimeline.getKeyFrames().add(new KeyFrame(
                    Duration.millis(time),
                    e -> frameUpdateCallback.accept(blendedFrame)
                ));
            }
        }
        currentTime += fadeInDuration;
        
        // === CELEBRATORY SPARKLES FADE-OUT (600ms) - Requirement 3.6 ===
        double sparklesDuration = SPARKLES_DURATION_MS; // 600ms from constant
        int sparklesFrames = (int) (sparklesDuration / SMOOTH_FRAME_INTERVAL_MS);
        for (int i = 0; i <= sparklesFrames; i++) {
            final double progress = easeOutCubic((double) i / sparklesFrames);
            final double time = currentTime + (i * SMOOTH_FRAME_INTERVAL_MS);
            final Image blendedFrame = (frameNewSparkles != null && newPokemon != null) 
                ? blendImages(frameNewSparkles, newPokemon, 0.5 + progress * 0.5)
                : newPokemon;
            if (blendedFrame != null) {
                evolutionTimeline.getKeyFrames().add(new KeyFrame(
                    Duration.millis(time),
                    e -> frameUpdateCallback.accept(blendedFrame)
                ));
            }
        }
        currentTime += sparklesDuration;
        
        // === FINAL: Show clean new Pokemon and trigger callback ===
        if (newPokemon != null) {
            final Image pokemon = newPokemon;
            final double finalTime = currentTime;
            evolutionTimeline.getKeyFrames().add(new KeyFrame(
                Duration.millis(finalTime),
                e -> {
                    frameUpdateCallback.accept(pokemon);
                    if (evolutionCompleteCallback != null) {
                        evolutionCompleteCallback.run();
                    }
                }
            ));
        }
        
        evolutionTimeline.setCycleCount(1);
        System.out.println("✨ Created evolution animation with 3 accelerating flashes: " + fromSpecies + " → " + toSpecies + 
                          " (duration: " + currentTime + "ms)");
        
        return evolutionTimeline;
    }
    
    /**
     * Creates a composite frame by overlaying an effect on top of a Pokemon sprite.
     * 
     * @param pokemonSprite The base Pokemon sprite
     * @param effectSprite The effect overlay sprite
     * @param pokemonOpacity Opacity of the Pokemon sprite (0.0 to 1.0)
     * @return Combined image with effect overlaid on Pokemon
     */
    private static Image createCompositeFrame(Image pokemonSprite, Image effectSprite, double pokemonOpacity) {
        if (pokemonSprite == null) return effectSprite;
        if (effectSprite == null) return pokemonSprite;
        
        try {
            int width = SPRITE_SIZE;
            int height = SPRITE_SIZE;
            
            javafx.scene.image.WritableImage composite = new javafx.scene.image.WritableImage(width, height);
            javafx.scene.image.PixelWriter writer = composite.getPixelWriter();
            javafx.scene.image.PixelReader pokemonReader = pokemonSprite.getPixelReader();
            javafx.scene.image.PixelReader effectReader = effectSprite.getPixelReader();
            
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    javafx.scene.paint.Color pokemonColor = pokemonReader.getColor(x, y);
                    javafx.scene.paint.Color effectColor = effectReader.getColor(x, y);
                    
                    // Apply opacity to Pokemon
                    double pAlpha = pokemonColor.getOpacity() * pokemonOpacity;
                    double eAlpha = effectColor.getOpacity();
                    
                    // Blend: effect on top of Pokemon (standard alpha compositing)
                    double outAlpha = eAlpha + pAlpha * (1 - eAlpha);
                    
                    if (outAlpha > 0) {
                        double r = (effectColor.getRed() * eAlpha + pokemonColor.getRed() * pAlpha * (1 - eAlpha)) / outAlpha;
                        double g = (effectColor.getGreen() * eAlpha + pokemonColor.getGreen() * pAlpha * (1 - eAlpha)) / outAlpha;
                        double b = (effectColor.getBlue() * eAlpha + pokemonColor.getBlue() * pAlpha * (1 - eAlpha)) / outAlpha;
                        
                        writer.setColor(x, y, javafx.scene.paint.Color.color(
                            Math.min(1.0, r), Math.min(1.0, g), Math.min(1.0, b), outAlpha
                        ));
                    } else {
                        writer.setColor(x, y, javafx.scene.paint.Color.TRANSPARENT);
                    }
                }
            }
            
            return composite;
        } catch (Exception e) {
            System.err.println("Failed to create composite frame: " + e.getMessage());
            return pokemonSprite; // Fallback to Pokemon sprite
        }
    }
    
    /**
     * Creates a white silhouette version of a Pokemon sprite.
     * Used during evolution transition frames (3 and 5).
     * 
     * @param sprite The original Pokemon sprite
     * @return White silhouette version of the sprite
     */
    private static Image createWhiteSilhouette(Image sprite) {
        if (sprite == null) return null;
        
        try {
            int width = SPRITE_SIZE;
            int height = SPRITE_SIZE;
            
            javafx.scene.image.WritableImage silhouette = new javafx.scene.image.WritableImage(width, height);
            javafx.scene.image.PixelWriter writer = silhouette.getPixelWriter();
            javafx.scene.image.PixelReader reader = sprite.getPixelReader();
            
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    javafx.scene.paint.Color original = reader.getColor(x, y);
                    
                    if (original.getOpacity() > 0.1) {
                        // Make it white but keep the alpha (silhouette effect)
                        // Blend 80% white with 20% original color for slight form visibility
                        double whiteness = 0.85;
                        double r = original.getRed() * (1 - whiteness) + whiteness;
                        double g = original.getGreen() * (1 - whiteness) + whiteness;
                        double b = original.getBlue() * (1 - whiteness) + whiteness;
                        
                        writer.setColor(x, y, javafx.scene.paint.Color.color(r, g, b, original.getOpacity()));
                    } else {
                        writer.setColor(x, y, javafx.scene.paint.Color.TRANSPARENT);
                    }
                }
            }
            
            return silhouette;
        } catch (Exception e) {
            System.err.println("Failed to create white silhouette: " + e.getMessage());
            return sprite; // Fallback to original
        }
    }
    
    /**
     * Creates an opacity transition between two images.
     * Generates intermediate frames with increasing opacity from silhouette to full visibility.
     * 
     * @param silhouette The starting silhouette image
     * @param fullImage The target full-visibility image
     * @param progress Transition progress (0.0 = silhouette, 1.0 = full image)
     * @return Image with intermediate opacity
     */
    private static Image createOpacityTransition(Image silhouette, Image fullImage, double progress) {
        if (silhouette == null) return fullImage;
        if (fullImage == null) return silhouette;
        if (progress <= 0) return silhouette;
        if (progress >= 1) return fullImage;
        
        try {
            int width = SPRITE_SIZE;
            int height = SPRITE_SIZE;
            
            javafx.scene.image.WritableImage transition = new javafx.scene.image.WritableImage(width, height);
            javafx.scene.image.PixelWriter writer = transition.getPixelWriter();
            javafx.scene.image.PixelReader silhouetteReader = silhouette.getPixelReader();
            javafx.scene.image.PixelReader fullReader = fullImage.getPixelReader();
            
            // Blend from silhouette to full image based on progress
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    javafx.scene.paint.Color silhouetteColor = silhouetteReader.getColor(x, y);
                    javafx.scene.paint.Color fullColor = fullReader.getColor(x, y);
                    
                    // Interpolate between silhouette and full color
                    double r = silhouetteColor.getRed() * (1 - progress) + fullColor.getRed() * progress;
                    double g = silhouetteColor.getGreen() * (1 - progress) + fullColor.getGreen() * progress;
                    double b = silhouetteColor.getBlue() * (1 - progress) + fullColor.getBlue() * progress;
                    
                    // Maintain the alpha channel from the full image
                    double alpha = fullColor.getOpacity();
                    
                    writer.setColor(x, y, javafx.scene.paint.Color.color(
                        Math.min(1.0, Math.max(0.0, r)),
                        Math.min(1.0, Math.max(0.0, g)),
                        Math.min(1.0, Math.max(0.0, b)),
                        Math.min(1.0, Math.max(0.0, alpha))
                    ));
                }
            }
            
            return transition;
        } catch (Exception e) {
            System.err.println("Failed to create opacity transition: " + e.getMessage());
            return progress < 0.5 ? silhouette : fullImage;
        }
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
     * Loads a preview sprite for the Pokemon selection screen.
     * Shows the egg (stage 1, frame 1) for the given species.
     * 
     * @param species The Pokemon species
     * @return The egg preview image, or null if not found
     */
    public static Image loadEggPreviewSprite(PokemonSpecies species) {
        String speciesFolder = getBaseSpeciesFolder(species);
        String framePath = "/pokemon/sprites/" + speciesFolder + "/egg/stage1/frame1.png";
        
        InputStream frameStream = AnimationUtils.class.getResourceAsStream(framePath);
        
        if (frameStream != null) {
            try {
                Image frame = new Image(frameStream, SPRITE_SIZE, SPRITE_SIZE, true, true);
                frameStream.close();
                return frame;
            } catch (Exception e) {
                System.err.println("Failed to load egg preview for " + species + ": " + e.getMessage());
            }
        }
        
        // Try fallback - generic egg image
        System.out.println("🥚 No egg preview found for " + species + ", using fallback");
        return null;
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