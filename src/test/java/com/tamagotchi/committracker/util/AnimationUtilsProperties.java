package com.tamagotchi.committracker.util;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import com.tamagotchi.committracker.pokemon.PokemonSpecies;
import com.tamagotchi.committracker.pokemon.EvolutionStage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for AnimationUtils.
 * 
 * These tests validate the correctness properties defined in the design document
 * for the extended evolution animation timing system.
 */
class AnimationUtilsProperties {

    /**
     * Minimum expected total duration for extended evolution animation.
     * Based on design: 3500ms ± 300ms tolerance
     */
    private static final double MIN_ANIMATION_DURATION_MS = 3200.0;
    
    /**
     * Maximum expected total duration for extended evolution animation.
     * Based on design: 3500ms ± 300ms tolerance
     */
    private static final double MAX_ANIMATION_DURATION_MS = 3800.0;
    
    /**
     * Expected flash duration in milliseconds.
     * Requirement 3.4: 200ms ± 10ms tolerance
     */
    private static final double EXPECTED_FLASH_DURATION_MS = 200.0;
    private static final double FLASH_DURATION_TOLERANCE_MS = 10.0;

    /**
     * **Feature: evolution-animation-improvements, Property 5: Animation Duration Bounds**
     * **Validates: Requirements 3.1**
     * 
     * For any evolution animation, the total duration should be within 
     * 3200ms to 3800ms (3500ms ± 300ms tolerance).
     */
    @Property(tries = 100)
    void animationDurationBounds(
            @ForAll("starterSpecies") PokemonSpecies fromSpecies,
            @ForAll("evolvedSpecies") PokemonSpecies toSpecies,
            @ForAll("fromStages") EvolutionStage fromStage,
            @ForAll("toStages") EvolutionStage toStage) {
        
        // Skip invalid evolution combinations (must evolve to higher stage)
        if (toStage.getLevel() <= fromStage.getLevel()) {
            return;
        }
        
        // Get the total duration from the extended frame durations
        double totalDuration = AnimationUtils.getExtendedEvolutionDuration();
        
        // Verify duration is within bounds (3500ms ± 300ms)
        assertTrue(totalDuration >= MIN_ANIMATION_DURATION_MS,
                String.format("Animation duration %.0fms is below minimum %.0fms for %s→%s (%s→%s)",
                    totalDuration, MIN_ANIMATION_DURATION_MS, 
                    fromSpecies, toSpecies, fromStage, toStage));
        
        assertTrue(totalDuration <= MAX_ANIMATION_DURATION_MS,
                String.format("Animation duration %.0fms exceeds maximum %.0fms for %s→%s (%s→%s)",
                    totalDuration, MAX_ANIMATION_DURATION_MS,
                    fromSpecies, toSpecies, fromStage, toStage));
    }
    
    /**
     * **Feature: evolution-animation-improvements, Property 6: Flash Duration Consistency**
     * **Validates: Requirements 3.4**
     * 
     * For any evolution animation, the peak white flash frame (frame 4) 
     * should be displayed for exactly 200ms (±10ms tolerance).
     */
    @Property(tries = 100)
    void flashDurationConsistency(
            @ForAll("starterSpecies") PokemonSpecies fromSpecies,
            @ForAll("evolvedSpecies") PokemonSpecies toSpecies,
            @ForAll("fromStages") EvolutionStage fromStage,
            @ForAll("toStages") EvolutionStage toStage) {
        
        // Skip invalid evolution combinations (must evolve to higher stage)
        if (toStage.getLevel() <= fromStage.getLevel()) {
            return;
        }
        
        // Get the flash duration constant
        double flashDuration = AnimationUtils.FLASH_DURATION_MS;
        
        // Verify flash duration is within tolerance (200ms ± 10ms)
        double minFlash = EXPECTED_FLASH_DURATION_MS - FLASH_DURATION_TOLERANCE_MS;
        double maxFlash = EXPECTED_FLASH_DURATION_MS + FLASH_DURATION_TOLERANCE_MS;
        
        assertTrue(flashDuration >= minFlash,
                String.format("Flash duration %.0fms is below minimum %.0fms for %s→%s (%s→%s)",
                    flashDuration, minFlash,
                    fromSpecies, toSpecies, fromStage, toStage));
        
        assertTrue(flashDuration <= maxFlash,
                String.format("Flash duration %.0fms exceeds maximum %.0fms for %s→%s (%s→%s)",
                    flashDuration, maxFlash,
                    fromSpecies, toSpecies, fromStage, toStage));
        
        // Also verify the flash duration in the EXTENDED_FRAME_DURATIONS array
        // Frame index 5 (0-based) is the flash frame after the glow pulse loop
        double[] durations = AnimationUtils.EXTENDED_FRAME_DURATIONS;
        assertTrue(durations.length > 5, "EXTENDED_FRAME_DURATIONS should have at least 6 frames");
        
        double arrayFlashDuration = durations[5]; // Index 5 is the flash frame
        assertTrue(arrayFlashDuration >= minFlash && arrayFlashDuration <= maxFlash,
                String.format("Flash duration in array %.0fms should be within %.0fms-%.0fms",
                    arrayFlashDuration, minFlash, maxFlash));
    }
    
    /**
     * Property: Extended frame durations array should have correct structure.
     * Validates that the glow pulse loop is properly configured.
     */
    @Property(tries = 100)
    void extendedFrameDurationsStructure(
            @ForAll @IntRange(min = 0, max = 8) int frameIndex) {
        
        double[] durations = AnimationUtils.EXTENDED_FRAME_DURATIONS;
        
        // Array should have exactly 9 frames for the extended animation
        assertEquals(9, durations.length, 
                "EXTENDED_FRAME_DURATIONS should have exactly 9 frames");
        
        // All durations should be positive
        if (frameIndex < durations.length) {
            assertTrue(durations[frameIndex] > 0,
                    String.format("Frame %d duration should be positive, got %.0fms", 
                        frameIndex, durations[frameIndex]));
        }
    }
    
    /**
     * Property: Glow pulse configuration should be valid.
     * Validates that the glow pulse loop indices are within bounds.
     */
    @Property(tries = 100)
    void glowPulseConfigurationValid(
            @ForAll @IntRange(min = 0, max = 3) int pulseIndex) {
        
        int[] pulseIndices = AnimationUtils.GLOW_PULSE_FRAME_INDICES;
        
        // Should have exactly 4 indices for 2 complete pulses (2→3→2→3)
        assertEquals(4, pulseIndices.length,
                "GLOW_PULSE_FRAME_INDICES should have exactly 4 indices");
        
        // Each index should be valid (0-5 for 6 evolution effect frames)
        if (pulseIndex < pulseIndices.length) {
            int effectIndex = pulseIndices[pulseIndex];
            assertTrue(effectIndex >= 0 && effectIndex <= 5,
                    String.format("Pulse index %d has invalid effect frame index %d (should be 0-5)",
                        pulseIndex, effectIndex));
        }
        
        // Verify the pulse count constant
        assertEquals(2, AnimationUtils.GLOW_PULSE_COUNT,
                "GLOW_PULSE_COUNT should be 2 for two complete pulses");
    }
    
    /**
     * Expected fade-in duration in milliseconds.
     * Requirement 3.5: 500ms ± 50ms tolerance
     */
    private static final double EXPECTED_FADE_IN_DURATION_MS = 500.0;
    private static final double FADE_IN_DURATION_TOLERANCE_MS = 50.0;
    
    /**
     * Expected sparkles duration in milliseconds.
     * Requirement 3.6: 600ms ± 50ms tolerance
     */
    private static final double EXPECTED_SPARKLES_DURATION_MS = 600.0;
    private static final double SPARKLES_DURATION_TOLERANCE_MS = 50.0;
    
    /**
     * **Feature: evolution-animation-improvements, Property 7: Fade-In Duration Consistency**
     * **Validates: Requirements 3.5**
     * 
     * For any evolution animation, the new Pokemon form should transition 
     * from silhouette to full visibility over 500ms (±50ms tolerance).
     */
    @Property(tries = 100)
    void fadeInDurationConsistency(
            @ForAll("starterSpecies") PokemonSpecies fromSpecies,
            @ForAll("evolvedSpecies") PokemonSpecies toSpecies,
            @ForAll("fromStages") EvolutionStage fromStage,
            @ForAll("toStages") EvolutionStage toStage) {
        
        // Skip invalid evolution combinations (must evolve to higher stage)
        if (toStage.getLevel() <= fromStage.getLevel()) {
            return;
        }
        
        // Get the fade-in duration constant
        double fadeInDuration = AnimationUtils.FADE_IN_DURATION_MS;
        
        // Verify fade-in duration is within tolerance (500ms ± 50ms)
        double minFadeIn = EXPECTED_FADE_IN_DURATION_MS - FADE_IN_DURATION_TOLERANCE_MS;
        double maxFadeIn = EXPECTED_FADE_IN_DURATION_MS + FADE_IN_DURATION_TOLERANCE_MS;
        
        assertTrue(fadeInDuration >= minFadeIn,
                String.format("Fade-in duration %.0fms is below minimum %.0fms for %s→%s (%s→%s)",
                    fadeInDuration, minFadeIn,
                    fromSpecies, toSpecies, fromStage, toStage));
        
        assertTrue(fadeInDuration <= maxFadeIn,
                String.format("Fade-in duration %.0fms exceeds maximum %.0fms for %s→%s (%s→%s)",
                    fadeInDuration, maxFadeIn,
                    fromSpecies, toSpecies, fromStage, toStage));
    }
    
    /**
     * Property: Celebratory sparkles duration should be consistent.
     * Validates that sparkles phase is 600ms (±50ms tolerance).
     */
    @Property(tries = 100)
    void sparklesDurationConsistency(
            @ForAll("starterSpecies") PokemonSpecies fromSpecies,
            @ForAll("evolvedSpecies") PokemonSpecies toSpecies,
            @ForAll("fromStages") EvolutionStage fromStage,
            @ForAll("toStages") EvolutionStage toStage) {
        
        // Skip invalid evolution combinations (must evolve to higher stage)
        if (toStage.getLevel() <= fromStage.getLevel()) {
            return;
        }
        
        // Get the sparkles duration constant
        double sparklesDuration = AnimationUtils.SPARKLES_DURATION_MS;
        
        // Verify sparkles duration is within tolerance (600ms ± 50ms)
        double minSparkles = EXPECTED_SPARKLES_DURATION_MS - SPARKLES_DURATION_TOLERANCE_MS;
        double maxSparkles = EXPECTED_SPARKLES_DURATION_MS + SPARKLES_DURATION_TOLERANCE_MS;
        
        assertTrue(sparklesDuration >= minSparkles,
                String.format("Sparkles duration %.0fms is below minimum %.0fms for %s→%s (%s→%s)",
                    sparklesDuration, minSparkles,
                    fromSpecies, toSpecies, fromStage, toStage));
        
        assertTrue(sparklesDuration <= maxSparkles,
                String.format("Sparkles duration %.0fms exceeds maximum %.0fms for %s→%s (%s→%s)",
                    sparklesDuration, maxSparkles,
                    fromSpecies, toSpecies, fromStage, toStage));
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
}
