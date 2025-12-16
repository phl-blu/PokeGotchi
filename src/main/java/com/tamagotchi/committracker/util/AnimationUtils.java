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
    
    private static final double FRAME_DURATION_MS = 500.0; // 500ms per frame for smooth animation
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
        
        // Build the base path for sprites
        String basePath = "/pokemon/sprites/" + species.name().toLowerCase() + "/" + 
                         stage.name().toLowerCase() + "/" + state.name().toLowerCase();
        
        // Try to load 3-4 frames for the animation
        for (int i = 1; i <= 4; i++) {
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
        
        // If no frames were loaded, try to load a fallback default frame
        if (frames.isEmpty()) {
            frames.add(createFallbackImage(species, stage));
        }
        
        return frames;
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
     * Creates a Timeline animation that cycles through the provided frames.
     * 
     * @param frames List of Image frames to animate
     * @param frameUpdateCallback Callback function to update the displayed frame
     * @return Timeline animation object
     */
    public static Timeline createFrameAnimation(List<Image> frames, Consumer<Image> frameUpdateCallback) {
        if (frames.isEmpty()) {
            return new Timeline(); // Return empty timeline if no frames
        }
        
        Timeline timeline = new Timeline();
        
        // Create keyframes for each sprite frame
        for (int i = 0; i < frames.size(); i++) {
            final int frameIndex = i;
            KeyFrame keyFrame = new KeyFrame(
                Duration.millis(i * FRAME_DURATION_MS),
                e -> frameUpdateCallback.accept(frames.get(frameIndex))
            );
            timeline.getKeyFrames().add(keyFrame);
        }
        
        // Set the timeline to cycle indefinitely
        timeline.setCycleCount(Timeline.INDEFINITE);
        
        return timeline;
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
     * Calculates the appropriate frame duration based on Pokemon state.
     * More energetic states have faster animations.
     */
    public static double getFrameDuration(PokemonState state) {
        switch (state) {
            case THRIVING:
            case HAPPY:
                return 400.0; // Faster animation for happy Pokemon
            case EVOLVING:
                return 200.0; // Very fast for evolution
            case SAD:
            case NEGLECTED:
                return 800.0; // Slower animation for sad Pokemon
            default:
                return FRAME_DURATION_MS; // Default speed
        }
    }
}