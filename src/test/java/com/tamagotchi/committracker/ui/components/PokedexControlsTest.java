package com.tamagotchi.committracker.ui.components;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import static org.junit.jupiter.api.Assertions.*;

import javafx.application.Platform;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Circle;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Unit tests for PokedexControls component.
 * Tests verify the D-pad shape and action button count.
 * 
 * Requirements: 6.1, 6.2
 */
class PokedexControlsTest {
    
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
     * Verify D-pad is created with correct cross shape.
     * The D-pad should contain horizontal and vertical rectangles forming a cross.
     * 
     * Requirements: 6.1, 6.3
     */
    @Test
    void testDPadIsCreatedWithCorrectShape() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] testPassed = {false};
        
        Platform.runLater(() -> {
            try {
                PokedexControls controls = new PokedexControls();
                Pane dPad = controls.getDPad();
                
                // D-pad should exist
                assertNotNull(dPad, "D-pad should not be null");
                
                // D-pad should be a StackPane containing the cross shape
                assertTrue(dPad instanceof StackPane, "D-pad should be a StackPane");
                
                StackPane dPadStack = (StackPane) dPad;
                
                // Should have at least 2 rectangles (horizontal and vertical arms)
                long rectangleCount = dPadStack.getChildren().stream()
                    .filter(node -> node instanceof Rectangle)
                    .count();
                
                assertTrue(rectangleCount >= 2, 
                    "D-pad should have at least 2 rectangles for cross shape, found: " + rectangleCount);
                
                // Verify the rectangles form a cross (one wider than tall, one taller than wide)
                boolean hasHorizontalArm = false;
                boolean hasVerticalArm = false;
                
                for (var node : dPadStack.getChildren()) {
                    if (node instanceof Rectangle) {
                        Rectangle rect = (Rectangle) node;
                        if (rect.getWidth() > rect.getHeight()) {
                            hasHorizontalArm = true;
                        }
                        if (rect.getHeight() > rect.getWidth()) {
                            hasVerticalArm = true;
                        }
                    }
                }
                
                assertTrue(hasHorizontalArm, "D-pad should have a horizontal arm");
                assertTrue(hasVerticalArm, "D-pad should have a vertical arm");
                
                testPassed[0] = true;
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out");
        assertTrue(testPassed[0], "D-pad shape test failed");
    }
    
    /**
     * Verify exactly 2 action buttons are created.
     * 
     * Requirements: 6.2, 6.4
     */
    @Test
    void testExactlyTwoActionButtonsAreCreated() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] testPassed = {false};
        
        Platform.runLater(() -> {
            try {
                PokedexControls controls = new PokedexControls();
                
                // Verify button count via getter method
                assertEquals(2, controls.getActionButtonCount(), 
                    "Should have exactly 2 action buttons");
                
                // Also verify via direct access to buttons container
                HBox actionButtons = controls.getActionButtons();
                assertNotNull(actionButtons, "Action buttons container should not be null");
                assertEquals(2, actionButtons.getChildren().size(), 
                    "Action buttons container should have exactly 2 children");
                
                // Verify each button is a StackPane with circles
                for (var button : actionButtons.getChildren()) {
                    assertTrue(button instanceof StackPane, 
                        "Each action button should be a StackPane");
                    
                    StackPane buttonPane = (StackPane) button;
                    
                    // Each button should have circles (outer and inner)
                    long circleCount = buttonPane.getChildren().stream()
                        .filter(node -> node instanceof Circle)
                        .count();
                    
                    assertTrue(circleCount >= 2, 
                        "Each button should have at least 2 circles (outer and inner)");
                }
                
                testPassed[0] = true;
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out");
        assertTrue(testPassed[0], "Action buttons test failed");
    }
    
    /**
     * Verify the controls are laid out in an HBox with D-pad left and buttons right.
     * 
     * Requirements: 6.1, 6.2
     */
    @Test
    void testControlsLayoutOrder() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] testPassed = {false};
        
        Platform.runLater(() -> {
            try {
                PokedexControls controls = new PokedexControls();
                
                // Controls should be an HBox
                assertTrue(controls instanceof HBox, "PokedexControls should extend HBox");
                
                // Should have exactly 2 children (D-pad and action buttons)
                assertEquals(2, controls.getChildren().size(), 
                    "Controls should have exactly 2 children");
                
                // First child should be the D-pad
                assertTrue(controls.getChildren().get(0) instanceof StackPane, 
                    "First child should be D-pad (StackPane)");
                
                // Second child should be the action buttons container
                assertTrue(controls.getChildren().get(1) instanceof HBox, 
                    "Second child should be action buttons (HBox)");
                
                testPassed[0] = true;
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out");
        assertTrue(testPassed[0], "Controls layout test failed");
    }
}
