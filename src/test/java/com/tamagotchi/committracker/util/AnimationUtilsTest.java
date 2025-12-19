package com.tamagotchi.committracker.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.tamagotchi.committracker.pokemon.PokemonSpecies;
import com.tamagotchi.committracker.pokemon.PokemonState;
import com.tamagotchi.committracker.pokemon.EvolutionStage;

/**
 * Tests for AnimationUtils sprite loading and animation functionality.
 * Note: These tests focus on the utility methods rather than JavaFX animation functionality
 * to avoid requiring a full JavaFX application context.
 */
class AnimationUtilsTest {
    
    @Test
    void testGetAnimationState() {
        // Test that animation state mapping works
        assertEquals(PokemonState.HAPPY, AnimationUtils.getAnimationState(PokemonState.HAPPY));
        assertEquals(PokemonState.SAD, AnimationUtils.getAnimationState(PokemonState.SAD));
        assertEquals(PokemonState.EVOLVING, AnimationUtils.getAnimationState(PokemonState.EVOLVING));
        assertEquals(PokemonState.THRIVING, AnimationUtils.getAnimationState(PokemonState.THRIVING));
        assertEquals(PokemonState.CONTENT, AnimationUtils.getAnimationState(PokemonState.CONTENT));
        assertEquals(PokemonState.CONCERNED, AnimationUtils.getAnimationState(PokemonState.CONCERNED));
        assertEquals(PokemonState.NEGLECTED, AnimationUtils.getAnimationState(PokemonState.NEGLECTED));
    }
    
    @Test
    void testGetFrameDuration() {
        // Test that all states have consistent frame durations (2 FPS = 500ms)
        double happyDuration = AnimationUtils.getFrameDuration(PokemonState.HAPPY);
        double sadDuration = AnimationUtils.getFrameDuration(PokemonState.SAD);
        double evolvingDuration = AnimationUtils.getFrameDuration(PokemonState.EVOLVING);
        double thrivingDuration = AnimationUtils.getFrameDuration(PokemonState.THRIVING);
        double contentDuration = AnimationUtils.getFrameDuration(PokemonState.CONTENT);
        double concernedDuration = AnimationUtils.getFrameDuration(PokemonState.CONCERNED);
        double neglectedDuration = AnimationUtils.getFrameDuration(PokemonState.NEGLECTED);
        
        // All durations should be positive
        assertTrue(happyDuration > 0);
        assertTrue(sadDuration > 0);
        assertTrue(evolvingDuration > 0);
        assertTrue(thrivingDuration > 0);
        assertTrue(contentDuration > 0);
        assertTrue(concernedDuration > 0);
        assertTrue(neglectedDuration > 0);
        
        // All Pokemon states should have consistent 2 FPS animation (500ms per frame)
        assertEquals(500.0, happyDuration, 0.001);
        assertEquals(500.0, sadDuration, 0.001);
        assertEquals(500.0, evolvingDuration, 0.001);
        assertEquals(500.0, thrivingDuration, 0.001);
        assertEquals(500.0, contentDuration, 0.001);
        assertEquals(500.0, concernedDuration, 0.001);
        assertEquals(500.0, neglectedDuration, 0.001);
    }
    
    @Test
    void testFrameDurationConsistency() {
        // Test that all frame durations are consistent (2 FPS = 500ms)
        double happyDuration = AnimationUtils.getFrameDuration(PokemonState.HAPPY);
        double thrivingDuration = AnimationUtils.getFrameDuration(PokemonState.THRIVING);
        double sadDuration = AnimationUtils.getFrameDuration(PokemonState.SAD);
        double neglectedDuration = AnimationUtils.getFrameDuration(PokemonState.NEGLECTED);
        
        // All states should have the same duration for consistent animation
        assertEquals(happyDuration, thrivingDuration, 0.001);
        assertEquals(sadDuration, neglectedDuration, 0.001);
        assertEquals(happyDuration, sadDuration, 0.001);
        assertEquals(thrivingDuration, neglectedDuration, 0.001);
    }
    
    @Test
    void testAnimationStateMapping() {
        // Test that all Pokemon states can be mapped to animation states
        for (PokemonState state : PokemonState.values()) {
            PokemonState animationState = AnimationUtils.getAnimationState(state);
            assertNotNull(animationState);
            // For now, the mapping is identity, but this test ensures the method works
            assertEquals(state, animationState);
        }
    }
    
    @Test
    void testFrameDurationRanges() {
        // Test that frame durations are within reasonable ranges
        for (PokemonState state : PokemonState.values()) {
            double duration = AnimationUtils.getFrameDuration(state);
            
            // Duration should be between 100ms and 1000ms (reasonable for animation)
            assertTrue(duration >= 100.0, "Duration too short for state: " + state);
            assertTrue(duration <= 1000.0, "Duration too long for state: " + state);
        }
    }
    
    @Test
    void testEggStageFromXPDays() {
        // Test the new XP-based egg stage calculation
        
        // Stage 1: 0-10 XP
        assertEquals(1, AnimationUtils.getEggStageFromXPDays(0));
        assertEquals(1, AnimationUtils.getEggStageFromXPDays(5));
        assertEquals(1, AnimationUtils.getEggStageFromXPDays(10));
        
        // Stage 2: 11-20 XP
        assertEquals(2, AnimationUtils.getEggStageFromXPDays(11));
        assertEquals(2, AnimationUtils.getEggStageFromXPDays(15));
        assertEquals(2, AnimationUtils.getEggStageFromXPDays(20));
        
        // Stage 3: 21-35 XP
        assertEquals(3, AnimationUtils.getEggStageFromXPDays(21));
        assertEquals(3, AnimationUtils.getEggStageFromXPDays(28));
        assertEquals(3, AnimationUtils.getEggStageFromXPDays(35));
        
        // Stage 4: 36-50+ XP
        assertEquals(4, AnimationUtils.getEggStageFromXPDays(36));
        assertEquals(4, AnimationUtils.getEggStageFromXPDays(45));
        assertEquals(4, AnimationUtils.getEggStageFromXPDays(50));
        assertEquals(4, AnimationUtils.getEggStageFromXPDays(100)); // Still stage 4 even with high XP
    }
    
    @Test
    void testEggStageThresholds() {
        // Test boundary conditions for egg stages
        
        // Test exact boundaries
        assertEquals(1, AnimationUtils.getEggStageFromXPDays(10)); // Upper bound of stage 1
        assertEquals(2, AnimationUtils.getEggStageFromXPDays(11)); // Lower bound of stage 2
        assertEquals(2, AnimationUtils.getEggStageFromXPDays(20)); // Upper bound of stage 2
        assertEquals(3, AnimationUtils.getEggStageFromXPDays(21)); // Lower bound of stage 3
        assertEquals(3, AnimationUtils.getEggStageFromXPDays(35)); // Upper bound of stage 3
        assertEquals(4, AnimationUtils.getEggStageFromXPDays(36)); // Lower bound of stage 4
        
        // Test negative XP (should still return stage 1)
        assertEquals(1, AnimationUtils.getEggStageFromXPDays(-5));
    }
    
    @Test
    void testEggStageProgression() {
        // Test that egg stages progress correctly as XP increases
        int previousStage = 0;
        
        for (int xp = 0; xp <= 60; xp += 5) {
            int currentStage = AnimationUtils.getEggStageFromXPDays(xp);
            
            // Stage should never decrease
            assertTrue(currentStage >= previousStage, 
                "Egg stage decreased from " + previousStage + " to " + currentStage + " at XP " + xp);
            
            // Stage should be between 1 and 4
            assertTrue(currentStage >= 1 && currentStage <= 4, 
                "Invalid egg stage " + currentStage + " at XP " + xp);
            
            previousStage = currentStage;
        }
    }
}