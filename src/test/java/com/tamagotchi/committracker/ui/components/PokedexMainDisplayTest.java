package com.tamagotchi.committracker.ui.components;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import static org.junit.jupiter.api.Assertions.*;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.tamagotchi.committracker.pokemon.EvolutionStage;
import com.tamagotchi.committracker.pokemon.PokemonSpecies;

/**
 * Unit tests for PokedexMainDisplay component.
 * Tests verify the layout positioning of stats corner, Pokemon display, and name label.
 * 
 * Requirements: 3.1, 4.1, 5.1
 */
class PokedexMainDisplayTest {
    
    private static boolean javaFxInitialized = false;
    
    @BeforeAll
    static void initJavaFX() throws InterruptedException {
        if (!javaFxInitialized) {
            try {
                // Try to initialize JavaFX toolkit
                Platform.startup(() -> {});
                javaFxInitialized = true;
            } catch (IllegalStateException e) {
                // Already initialized
                javaFxInitialized = true;
            }
        }
    }
    
    /**
     * Verify stats corner is positioned in the top-left area (overlaid on center StackPane).
     * 
     * Requirements: 4.1
     */
    @Test
    void testStatsCornerIsPositionedTopLeft() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] testPassed = {false};
        
        Platform.runLater(() -> {
            try {
                PokedexMainDisplay display = new PokedexMainDisplay(
                    PokemonSpecies.CHARMANDER, EvolutionStage.EGG);
                
                // Stats corner should exist
                PokedexStatsCorner statsCorner = display.getStatsCorner();
                assertNotNull(statsCorner, "Stats corner should not be null");
                
                // Stats corner should be in the center StackPane (overlaid)
                Node centerNode = display.getCenter();
                assertNotNull(centerNode, "Center region should not be null");
                
                assertTrue(centerNode instanceof StackPane, 
                    "Center region should be a StackPane container");
                
                StackPane centerContainer = (StackPane) centerNode;
                
                // Stats corner should be a child of the center container
                assertTrue(centerContainer.getChildren().contains(statsCorner),
                    "Stats corner should be in the center container (overlaid)");
                
                // Stats corner should have TOP_LEFT alignment
                assertEquals(Pos.TOP_LEFT, StackPane.getAlignment(statsCorner),
                    "Stats corner should have TOP_LEFT alignment");
                
                testPassed[0] = true;
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out");
        assertTrue(testPassed[0], "Stats corner position test failed");
    }
    
    /**
     * Verify Pokemon display is centered in the main area.
     * 
     * Requirements: 3.1
     */
    @Test
    void testPokemonDisplayIsCentered() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] testPassed = {false};
        
        Platform.runLater(() -> {
            try {
                PokedexMainDisplay display = new PokedexMainDisplay(
                    PokemonSpecies.CHARMANDER, EvolutionStage.EGG);
                
                // Pokemon display should exist
                PokemonDisplayComponent pokemonDisplay = display.getPokemonDisplay();
                assertNotNull(pokemonDisplay, "Pokemon display should not be null");
                
                // Pokemon display should be in the center region of BorderPane
                Node centerNode = display.getCenter();
                assertNotNull(centerNode, "Center region should not be null");
                
                // Center node should be a container with the Pokemon display
                assertTrue(centerNode instanceof StackPane, 
                    "Center region should be a StackPane container");
                
                StackPane centerContainer = (StackPane) centerNode;
                
                // Container should have CENTER alignment
                assertEquals(Pos.CENTER, centerContainer.getAlignment(),
                    "Pokemon display container should have CENTER alignment");
                
                // Pokemon display should be a child of the center container
                assertTrue(centerContainer.getChildren().contains(pokemonDisplay),
                    "Pokemon display should be in the center container");
                
                testPassed[0] = true;
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out");
        assertTrue(testPassed[0], "Pokemon display center test failed");
    }
    
    /**
     * Verify name label is positioned at the bottom-right (overlaid on center StackPane).
     * 
     * Requirements: 5.1
     */
    @Test
    void testNameLabelIsAtBottom() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] testPassed = {false};
        
        Platform.runLater(() -> {
            try {
                PokedexMainDisplay display = new PokedexMainDisplay(
                    PokemonSpecies.CHARMANDER, EvolutionStage.EGG);
                
                // Name label should exist
                PokedexNameLabel nameLabel = display.getNameLabel();
                assertNotNull(nameLabel, "Name label should not be null");
                
                // Name label should be in the center StackPane (overlaid)
                Node centerNode = display.getCenter();
                assertNotNull(centerNode, "Center region should not be null");
                
                assertTrue(centerNode instanceof StackPane, 
                    "Center region should be a StackPane container");
                
                StackPane centerContainer = (StackPane) centerNode;
                
                // Name label should be a child of the center container
                assertTrue(centerContainer.getChildren().contains(nameLabel),
                    "Name label should be in the center container (overlaid)");
                
                // Name label should have BOTTOM_RIGHT alignment
                assertEquals(Pos.BOTTOM_RIGHT, StackPane.getAlignment(nameLabel),
                    "Name label should have BOTTOM_RIGHT alignment");
                
                testPassed[0] = true;
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out");
        assertTrue(testPassed[0], "Name label position test failed");
    }
    
    /**
     * Verify the main display uses BorderPane layout with center StackPane for overlays.
     * 
     * Requirements: 3.1, 4.1, 5.1
     */
    @Test
    void testMainDisplayUsesBorderPaneLayout() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] testPassed = {false};
        
        Platform.runLater(() -> {
            try {
                PokedexMainDisplay display = new PokedexMainDisplay(
                    PokemonSpecies.CHARMANDER, EvolutionStage.EGG);
                
                // Should be a BorderPane
                assertTrue(display instanceof BorderPane, 
                    "PokedexMainDisplay should extend BorderPane");
                
                // Should have center region populated (contains all overlaid components)
                assertNotNull(display.getCenter(), "Center region should be populated");
                
                // Center should be a StackPane containing Pokemon, stats, and name
                Node centerNode = display.getCenter();
                assertTrue(centerNode instanceof StackPane, 
                    "Center region should be a StackPane");
                
                StackPane centerStack = (StackPane) centerNode;
                
                // Should contain Pokemon display, stats corner, and name label
                assertTrue(centerStack.getChildren().contains(display.getPokemonDisplay()),
                    "Center should contain Pokemon display");
                assertTrue(centerStack.getChildren().contains(display.getStatsCorner()),
                    "Center should contain stats corner");
                assertTrue(centerStack.getChildren().contains(display.getNameLabel()),
                    "Center should contain name label");
                
                testPassed[0] = true;
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out");
        assertTrue(testPassed[0], "BorderPane layout test failed");
    }
    
    /**
     * Verify all three components are accessible via getters.
     * 
     * Requirements: 3.1, 4.1, 5.1
     */
    @Test
    void testAllComponentsAreAccessible() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] testPassed = {false};
        
        Platform.runLater(() -> {
            try {
                PokedexMainDisplay display = new PokedexMainDisplay(
                    PokemonSpecies.CHARMANDER, EvolutionStage.EGG);
                
                // All components should be accessible
                assertNotNull(display.getStatsCorner(), 
                    "Stats corner should be accessible");
                assertNotNull(display.getPokemonDisplay(), 
                    "Pokemon display should be accessible");
                assertNotNull(display.getNameLabel(), 
                    "Name label should be accessible");
                
                // Current species and stage should be accessible
                assertEquals(PokemonSpecies.CHARMANDER, display.getCurrentSpecies(),
                    "Current species should be CHARMANDER");
                assertEquals(EvolutionStage.EGG, display.getCurrentStage(),
                    "Current stage should be EGG");
                
                testPassed[0] = true;
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out");
        assertTrue(testPassed[0], "Component accessibility test failed");
    }
}
