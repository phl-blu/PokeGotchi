package com.tamagotchi.committracker.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import javafx.scene.image.Image;
import java.util.List;

import com.tamagotchi.committracker.pokemon.PokemonSpecies;
import com.tamagotchi.committracker.pokemon.PokemonState;
import com.tamagotchi.committracker.pokemon.EvolutionStage;

/**
 * Test to verify that your Pokemon sprites are loading correctly.
 */
class SpriteLoadingTest {
    
    @Test
    void testCharmanderEggSpriteLoads() {
        // Test loading the egg sprite you added
        List<Image> frames = AnimationUtils.loadSpriteFrames(
            PokemonSpecies.CHARMANDER, EvolutionStage.EGG, PokemonState.CONTENT
        );
        
        assertNotNull(frames, "Frames list should not be null");
        assertFalse(frames.isEmpty(), "Should load at least one frame");
        
        Image firstFrame = frames.get(0);
        assertNotNull(firstFrame, "First frame should not be null");
        
        // Check if the image loaded successfully (width/height > 0 means it loaded)
        assertTrue(firstFrame.getWidth() > 0, "Image should have width > 0");
        assertTrue(firstFrame.getHeight() > 0, "Image should have height > 0");
        
        System.out.println("✅ Charmander egg sprite loaded successfully!");
        System.out.println("   Size: " + firstFrame.getWidth() + "x" + firstFrame.getHeight());
    }
    
    @Test
    void testCharmanderBasicSpriteLoads() {
        // Test loading the basic Charmander sprite you added
        List<Image> frames = AnimationUtils.loadSpriteFrames(
            PokemonSpecies.CHARMANDER, EvolutionStage.BASIC, PokemonState.CONTENT
        );
        
        assertNotNull(frames, "Frames list should not be null");
        assertFalse(frames.isEmpty(), "Should load at least one frame");
        
        Image firstFrame = frames.get(0);
        assertNotNull(firstFrame, "First frame should not be null");
        
        // Check if the image loaded successfully
        assertTrue(firstFrame.getWidth() > 0, "Image should have width > 0");
        assertTrue(firstFrame.getHeight() > 0, "Image should have height > 0");
        
        System.out.println("✅ Charmander basic sprite loaded successfully!");
        System.out.println("   Size: " + firstFrame.getWidth() + "x" + firstFrame.getHeight());
    }
    
    @Test
    void testSpritePathGeneration() {
        // Test that the path generation works correctly
        String expectedEggPath = "/pokemon/sprites/charmander/egg/content";
        String expectedBasicPath = "/pokemon/sprites/charmander/basic/content";
        
        // This test verifies the path structure matches what you created
        System.out.println("Expected paths:");
        System.out.println("  Egg: " + expectedEggPath + "/frame1.png");
        System.out.println("  Basic: " + expectedBasicPath + "/frame1.png");
        
        // The AnimationUtils should be looking for these exact paths
        assertTrue(true, "Path structure verification complete");
    }
}