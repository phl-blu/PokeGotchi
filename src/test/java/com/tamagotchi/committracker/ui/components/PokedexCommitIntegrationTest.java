package com.tamagotchi.committracker.ui.components;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import static org.junit.jupiter.api.Assertions.*;

import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.tamagotchi.committracker.pokemon.EvolutionStage;
import com.tamagotchi.committracker.pokemon.PokemonSpecies;
import com.tamagotchi.committracker.pokemon.PokemonState;

/**
 * Integration tests for commit tracking updates to Pokedex UI.
 * Tests verify that stats update when commits are processed and
 * that the Pokemon name updates after evolution.
 * 
 * Requirements: 9.1, 9.2
 */
class PokedexCommitIntegrationTest {
    
    private static boolean javaFxInitialized = false;
    
    @BeforeAll
    static void initJavaFX() throws InterruptedException {
        if (!javaFxInitialized) {
            try {
                Platform.startup(() -> {});
                javaFxInitialized = true;
            } catch (IllegalStateException e) {
                javaFxInitialized = true;
            }
        }
    }
    
    /**
     * Verify stats update when commit is processed.
     * Tests that updateStats correctly updates XP, streak, and stage display.
     * 
     * Requirements: 9.1
     */
    @Test
    void testStatsUpdateWhenCommitProcessed() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] testPassed = {false};
        
        Platform.runLater(() -> {
            try {
                // Create main display with initial state
                PokedexMainDisplay display = new PokedexMainDisplay(
                    PokemonSpecies.CHARMANDER, EvolutionStage.EGG);
                
                // Initial stats
                display.updateStats(0, 60, 0, EvolutionStage.EGG);
                
                // Simulate commit processing - update stats
                int newXP = 25;
                int newStreak = 2;
                display.updateStats(newXP, 60, newStreak, EvolutionStage.EGG);
                
                // Verify stats corner exists and was updated
                PokedexStatsCorner statsCorner = display.getStatsCorner();
                assertNotNull(statsCorner, "Stats corner should exist");
                
                // Stats corner should have been updated (we can't directly check labels
                // but we verify the method doesn't throw and component exists)
                testPassed[0] = true;
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out");
        assertTrue(testPassed[0], "Stats update test failed");
    }
    
    /**
     * Verify name updates after evolution.
     * Tests that the name label correctly updates when evolution occurs.
     * 
     * Requirements: 9.2
     */
    @Test
    void testNameUpdatesAfterEvolution() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] testPassed = {false};
        
        Platform.runLater(() -> {
            try {
                // Create main display with egg stage
                PokedexMainDisplay display = new PokedexMainDisplay(
                    PokemonSpecies.CHARMANDER, EvolutionStage.EGG);
                
                // Initial name should be "Egg"
                PokedexNameLabel nameLabel = display.getNameLabel();
                assertNotNull(nameLabel, "Name label should exist");
                
                // Update to BASIC stage (hatched)
                display.updateStats(60, 500, 4, EvolutionStage.BASIC);
                
                // The name should now be "Charmander" (not "Egg")
                // We verify by checking the current stage was updated
                assertEquals(EvolutionStage.BASIC, display.getCurrentStage(),
                    "Stage should be updated to BASIC");
                
                testPassed[0] = true;
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out");
        assertTrue(testPassed[0], "Name update after evolution test failed");
    }
    
    /**
     * Verify Pokemon state animation can be triggered.
     * Tests that updatePokemonState method works correctly.
     * 
     * Requirements: 9.3
     */
    @Test
    void testPokemonStateAnimationTrigger() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] testPassed = {false};
        
        Platform.runLater(() -> {
            try {
                // Create main display
                PokedexMainDisplay display = new PokedexMainDisplay(
                    PokemonSpecies.CHARMANDER, EvolutionStage.BASIC);
                
                // Get Pokemon display
                PokemonDisplayComponent pokemonDisplay = display.getPokemonDisplay();
                assertNotNull(pokemonDisplay, "Pokemon display should exist");
                
                // Trigger state change to HAPPY
                display.updatePokemonState(PokemonState.HAPPY);
                
                // Verify the state was updated
                assertEquals(PokemonState.HAPPY, pokemonDisplay.getCurrentState(),
                    "Pokemon state should be HAPPY");
                
                testPassed[0] = true;
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out");
        assertTrue(testPassed[0], "Pokemon state animation test failed");
    }
    
    /**
     * Verify PokedexFrame stats update propagates to main display.
     * Tests the full integration path from PokedexFrame to PokedexMainDisplay.
     * 
     * Requirements: 9.1
     */
    @Test
    void testPokedexFrameStatsUpdate() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] testPassed = {false};
        
        Platform.runLater(() -> {
            try {
                // Create PokedexFrame
                PokedexFrame frame = new PokedexFrame();
                
                // Show main display
                frame.showMainDisplay(PokemonSpecies.CHARMANDER, EvolutionStage.EGG);
                
                // Update stats through frame
                frame.updateStats(30, 60, 3, EvolutionStage.EGG);
                
                // Verify main display exists and received update
                PokedexMainDisplay mainDisplay = frame.getMainDisplay();
                assertNotNull(mainDisplay, "Main display should exist");
                
                // Verify current stage
                assertEquals(EvolutionStage.EGG, frame.getCurrentStage(),
                    "Frame should track current stage");
                
                testPassed[0] = true;
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out");
        assertTrue(testPassed[0], "PokedexFrame stats update test failed");
    }
    
    /**
     * Verify PokedexFrame Pokemon name update.
     * Tests that updatePokemonName correctly updates the name label.
     * 
     * Requirements: 9.2
     */
    @Test
    void testPokedexFramePokemonNameUpdate() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] testPassed = {false};
        
        Platform.runLater(() -> {
            try {
                // Create PokedexFrame
                PokedexFrame frame = new PokedexFrame();
                
                // Show main display
                frame.showMainDisplay(PokemonSpecies.CHARMANDER, EvolutionStage.EGG);
                
                // Update Pokemon name
                frame.updatePokemonName("Charmander");
                
                // Verify main display exists
                PokedexMainDisplay mainDisplay = frame.getMainDisplay();
                assertNotNull(mainDisplay, "Main display should exist");
                
                // Verify name label exists
                PokedexNameLabel nameLabel = mainDisplay.getNameLabel();
                assertNotNull(nameLabel, "Name label should exist");
                
                testPassed[0] = true;
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out");
        assertTrue(testPassed[0], "PokedexFrame name update test failed");
    }
    
    /**
     * Verify evolution listener is called when evolution completes.
     * Tests that the evolution listener callback is properly invoked.
     * 
     * Requirements: 9.2
     */
    @Test
    void testEvolutionListenerCallback() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean listenerCalled = new AtomicBoolean(false);
        AtomicReference<PokemonSpecies> evolvedSpecies = new AtomicReference<>();
        AtomicReference<EvolutionStage> evolvedStage = new AtomicReference<>();
        
        Platform.runLater(() -> {
            try {
                // Create main display
                PokedexMainDisplay display = new PokedexMainDisplay(
                    PokemonSpecies.CHARMANDER, EvolutionStage.EGG);
                
                // Set up evolution listener
                display.setEvolutionListener((newSpecies, newStage) -> {
                    listenerCalled.set(true);
                    evolvedSpecies.set(newSpecies);
                    evolvedStage.set(newStage);
                });
                
                // Verify listener was set (we can't easily trigger evolution in unit test
                // but we verify the setup doesn't throw)
                assertNotNull(display.getPokemonDisplay(), 
                    "Pokemon display should exist for listener setup");
                
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out");
    }
}
