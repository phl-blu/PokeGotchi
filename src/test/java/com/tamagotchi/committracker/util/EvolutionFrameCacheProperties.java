package com.tamagotchi.committracker.util;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;

import com.tamagotchi.committracker.pokemon.PokemonSpecies;
import com.tamagotchi.committracker.pokemon.EvolutionStage;

import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for EvolutionFrameCache.
 * 
 * These tests validate the correctness properties defined in the design document
 * for the evolution animation caching system.
 */
class EvolutionFrameCacheProperties {

    private static final int SPRITE_SIZE = 64;

    /**
     * **Feature: evolution-animation-improvements, Property 3: Silhouette Cache Hit Rate**
     * **Validates: Requirements 1.5**
     * 
     * For any sprite that has been converted to a silhouette once, subsequent 
     * silhouette requests for the same sprite should return the cached version 
     * without pixel manipulation.
     */
    @Property(tries = 100)
    void silhouetteCacheHitRate(
            @ForAll("pokemonSpecies") PokemonSpecies species,
            @ForAll("evolutionStages") EvolutionStage stage,
            @ForAll @IntRange(min = 1, max = 10) int accessCount) {
        
        // Clear cache before test
        EvolutionFrameCache.clearSilhouetteCache();
        
        // Create a test sprite
        Image testSprite = createTestSprite(species, stage);
        String cacheKey = EvolutionFrameCache.createSilhouetteCacheKey(species, stage);
        
        // First access - should create and cache the silhouette
        assertFalse(EvolutionFrameCache.hasCachedSilhouette(cacheKey),
                "Cache should be empty before first access");
        
        Image firstSilhouette = EvolutionFrameCache.getCachedSilhouette(testSprite, cacheKey);
        assertNotNull(firstSilhouette, "First silhouette creation should succeed");
        
        assertTrue(EvolutionFrameCache.hasCachedSilhouette(cacheKey),
                "Silhouette should be cached after first access");
        
        // Subsequent accesses should return the same cached instance
        for (int i = 0; i < accessCount; i++) {
            Image cachedSilhouette = EvolutionFrameCache.getCachedSilhouette(testSprite, cacheKey);
            
            // Should return the exact same object (reference equality)
            assertSame(firstSilhouette, cachedSilhouette,
                    String.format("Access %d should return cached silhouette instance", i + 1));
        }
        
        // Verify cache size is still 1 (no duplicates)
        assertEquals(1, EvolutionFrameCache.getSilhouetteCacheSize(),
                "Cache should contain exactly one silhouette entry");
    }


    /**
     * **Feature: evolution-animation-improvements, Property 1: Composite Frame Caching Consistency**
     * **Validates: Requirements 1.1, 1.2**
     * 
     * For any evolution animation request with the same fromSpecies, toSpecies, 
     * fromStage, and toStage, the pre-computed frames returned should be identical 
     * (same pixel data) regardless of when the request is made.
     */
    @Property(tries = 100)
    void compositeFrameCachingConsistency(
            @ForAll("starterSpecies") PokemonSpecies fromSpecies,
            @ForAll("evolvedSpecies") PokemonSpecies toSpecies,
            @ForAll("fromStages") EvolutionStage fromStage,
            @ForAll("toStages") EvolutionStage toStage) {
        
        // Skip invalid evolution combinations (must evolve to higher stage)
        if (toStage.getLevel() <= fromStage.getLevel()) {
            return;
        }
        
        // Clear cache before test
        EvolutionFrameCache.clearCache();
        
        // Create test sprites
        Image oldPokemon = createTestSprite(fromSpecies, fromStage);
        Image newPokemon = createTestSprite(toSpecies, toStage);
        List<Image> evolutionEffects = createTestEvolutionEffects();
        
        // First request - should compute and cache
        assertFalse(EvolutionFrameCache.hasCachedEvolutionFrames(fromSpecies, toSpecies, fromStage, toStage),
                "Cache should be empty before first request");
        
        EvolutionFrameCache.CachedEvolutionSequence firstResult = 
            EvolutionFrameCache.preComputeEvolutionFrames(
                fromSpecies, toSpecies, fromStage, toStage,
                oldPokemon, newPokemon, evolutionEffects);
        
        assertNotNull(firstResult, "First computation should succeed");
        assertTrue(firstResult.frames.size() > 0, "Should have at least one frame");
        
        assertTrue(EvolutionFrameCache.hasCachedEvolutionFrames(fromSpecies, toSpecies, fromStage, toStage),
                "Frames should be cached after first request");
        
        // Second request - should return cached result
        EvolutionFrameCache.CachedEvolutionSequence secondResult = 
            EvolutionFrameCache.preComputeEvolutionFrames(
                fromSpecies, toSpecies, fromStage, toStage,
                oldPokemon, newPokemon, evolutionEffects);
        
        assertNotNull(secondResult, "Second request should succeed");
        
        // Verify same object is returned (cache hit)
        assertSame(firstResult, secondResult,
                "Second request should return the same cached sequence object");
        
        // Verify frame count is identical
        assertEquals(firstResult.frames.size(), secondResult.frames.size(),
                "Frame count should be identical");
        
        // Verify each frame is the same object (not recomputed)
        for (int i = 0; i < firstResult.frames.size(); i++) {
            assertSame(firstResult.frames.get(i), secondResult.frames.get(i),
                    String.format("Frame %d should be the same cached object", i));
        }
        
        // Verify cache size is still 1 (no duplicates)
        assertEquals(1, EvolutionFrameCache.getFrameCacheSize(),
                "Cache should contain exactly one evolution sequence");
    }
    
    /**
     * Creates test evolution effect frames.
     */
    private List<Image> createTestEvolutionEffects() {
        List<Image> effects = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            WritableImage effect = new WritableImage(SPRITE_SIZE, SPRITE_SIZE);
            PixelWriter writer = effect.getPixelWriter();
            
            // Create a simple glow effect with varying intensity
            double intensity = 0.2 + (i * 0.15);
            Color glowColor = Color.color(1.0, 1.0, 1.0, intensity);
            
            for (int y = 0; y < SPRITE_SIZE; y++) {
                for (int x = 0; x < SPRITE_SIZE; x++) {
                    double centerX = SPRITE_SIZE / 2.0;
                    double centerY = SPRITE_SIZE / 2.0;
                    double distance = Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2));
                    double maxDistance = SPRITE_SIZE / 2.0;
                    
                    if (distance < maxDistance) {
                        double alpha = intensity * (1 - distance / maxDistance);
                        writer.setColor(x, y, Color.color(1.0, 1.0, 1.0, alpha));
                    } else {
                        writer.setColor(x, y, Color.TRANSPARENT);
                    }
                }
            }
            effects.add(effect);
        }
        return effects;
    }
    
    /**
     * Provides starter Pokemon species (base forms).
     */
    @Provide
    Arbitrary<PokemonSpecies> starterSpecies() {
        return Arbitraries.of(
            PokemonSpecies.CHARMANDER, PokemonSpecies.CYNDAQUIL, PokemonSpecies.MUDKIP,
            PokemonSpecies.PIPLUP, PokemonSpecies.SNIVY, PokemonSpecies.FROAKIE,
            PokemonSpecies.ROWLET, PokemonSpecies.GROOKEY, PokemonSpecies.FUECOCO
        );
    }
    
    /**
     * Provides evolved Pokemon species.
     */
    @Provide
    Arbitrary<PokemonSpecies> evolvedSpecies() {
        return Arbitraries.of(
            PokemonSpecies.CHARMANDER, PokemonSpecies.CHARMELEON, PokemonSpecies.CHARIZARD,
            PokemonSpecies.CYNDAQUIL, PokemonSpecies.QUILAVA, PokemonSpecies.TYPHLOSION,
            PokemonSpecies.MUDKIP, PokemonSpecies.MARSHTOMP, PokemonSpecies.SWAMPERT
        );
    }
    
    /**
     * Provides "from" evolution stages (can evolve from).
     */
    @Provide
    Arbitrary<EvolutionStage> fromStages() {
        return Arbitraries.of(EvolutionStage.EGG, EvolutionStage.BASIC, EvolutionStage.STAGE_1);
    }
    
    /**
     * Provides "to" evolution stages (can evolve to).
     */
    @Provide
    Arbitrary<EvolutionStage> toStages() {
        return Arbitraries.of(EvolutionStage.BASIC, EvolutionStage.STAGE_1, EvolutionStage.STAGE_2);
    }

    /**
     * Provides arbitrary Pokemon species for testing.
     */
    @Provide
    Arbitrary<PokemonSpecies> pokemonSpecies() {
        return Arbitraries.of(
            PokemonSpecies.CHARMANDER, PokemonSpecies.CYNDAQUIL, PokemonSpecies.MUDKIP,
            PokemonSpecies.PIPLUP, PokemonSpecies.SNIVY, PokemonSpecies.FROAKIE,
            PokemonSpecies.ROWLET, PokemonSpecies.GROOKEY, PokemonSpecies.FUECOCO
        );
    }

    /**
     * Provides arbitrary evolution stages for testing.
     */
    @Provide
    Arbitrary<EvolutionStage> evolutionStages() {
        return Arbitraries.of(EvolutionStage.EGG, EvolutionStage.BASIC, 
                              EvolutionStage.STAGE_1, EvolutionStage.STAGE_2);
    }

    /**
     * Creates a test sprite image with deterministic colors based on species and stage.
     * This ensures consistent test behavior.
     */
    private Image createTestSprite(PokemonSpecies species, EvolutionStage stage) {
        WritableImage image = new WritableImage(SPRITE_SIZE, SPRITE_SIZE);
        PixelWriter writer = image.getPixelWriter();
        
        // Use species and stage ordinals to create deterministic colors
        double hue = (species.ordinal() * 30.0) % 360.0;
        double saturation = 0.7 + (stage.ordinal() * 0.1);
        double brightness = 0.8;
        
        Color baseColor = Color.hsb(hue, saturation, brightness);
        
        // Create a simple oval shape (like a Pokemon sprite)
        for (int y = 0; y < SPRITE_SIZE; y++) {
            for (int x = 0; x < SPRITE_SIZE; x++) {
                double centerX = SPRITE_SIZE / 2.0;
                double centerY = SPRITE_SIZE / 2.0;
                double radiusX = SPRITE_SIZE / 2.5;
                double radiusY = SPRITE_SIZE / 2.2;
                
                double normalizedX = (x - centerX) / radiusX;
                double normalizedY = (y - centerY) / radiusY;
                double distance = normalizedX * normalizedX + normalizedY * normalizedY;
                
                if (distance <= 1.0) {
                    writer.setColor(x, y, baseColor);
                } else {
                    writer.setColor(x, y, Color.TRANSPARENT);
                }
            }
        }
        
        return image;
    }
}
