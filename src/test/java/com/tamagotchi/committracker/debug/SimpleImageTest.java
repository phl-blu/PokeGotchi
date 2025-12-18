package com.tamagotchi.committracker.debug;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import javafx.scene.image.Image;
import java.io.InputStream;

/**
 * Simple test to check if images can be loaded
 */
class SimpleImageTest {
    
    @Test
    void testImagePaths() {
        // Test if we can find the image files
        String eggPath = "/pokemon/sprites/charmander/egg/stage1/frame1.png";
        String charmanderPath = "/pokemon/sprites/charmander/basic/content/frame1.png";
        
        System.out.println("Testing image paths...");
        
        // Test egg image
        InputStream eggStream = SimpleImageTest.class.getResourceAsStream(eggPath);
        if (eggStream != null) {
            System.out.println("✅ Egg image found at: " + eggPath);
            try {
                eggStream.close();
            } catch (Exception e) {
                // ignore
            }
        } else {
            System.out.println("❌ Egg image NOT found at: " + eggPath);
        }
        
        // Test charmander image
        InputStream charmanderStream = SimpleImageTest.class.getResourceAsStream(charmanderPath);
        if (charmanderStream != null) {
            System.out.println("✅ Charmander image found at: " + charmanderPath);
            try {
                charmanderStream.close();
            } catch (Exception e) {
                // ignore
            }
        } else {
            System.out.println("❌ Charmander image NOT found at: " + charmanderPath);
        }
        
        // Test JavaFX Image loading (this might fail in headless mode, but let's try)
        try {
            if (eggStream != null) {
                InputStream eggStream2 = SimpleImageTest.class.getResourceAsStream(eggPath);
                Image eggImage = new Image(eggStream2);
                System.out.println("✅ JavaFX Image created for egg: " + eggImage.getWidth() + "x" + eggImage.getHeight());
                eggStream2.close();
            }
        } catch (Exception e) {
            System.out.println("⚠️ JavaFX Image creation failed (expected in headless mode): " + e.getMessage());
        }
        
        assertNotNull(eggStream, "Egg image should be found");
        assertNotNull(charmanderStream, "Charmander image should be found");
    }
}