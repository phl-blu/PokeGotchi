package com.tamagotchi.committracker.ui.components;

import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import com.tamagotchi.committracker.ui.theme.PokedexTheme;

/**
 * Decorative D-pad and action buttons component for the Pokedex UI.
 * Creates a retro handheld gaming device control layout with:
 * - A cross-shaped D-pad on the left
 * - Two circular action buttons on the right
 * 
 * These controls are purely decorative and do not have interactive functionality.
 * 
 * Requirements: 6.1, 6.2, 6.3, 6.4
 */
public class PokedexControls extends HBox {
    
    // D-pad dimensions - smaller for compact frame
    private static final int DPAD_SIZE = 35;
    private static final int DPAD_ARM_WIDTH = 12;
    private static final int DPAD_ARM_LENGTH = 14;
    
    // Action button dimensions - smaller for compact frame
    private static final int BUTTON_SIZE = 14;
    private static final int BUTTON_INNER_SIZE = 8;
    private static final int BUTTON_SPACING = 6;
    
    // Component spacing
    private static final int CONTROLS_SPACING = 30;
    
    private Pane dPad;
    private HBox actionButtons;
    
    /**
     * Creates a new PokedexControls component with D-pad and action buttons.
     */
    public PokedexControls() {
        initializeControls();
    }
    
    /**
     * Initializes the control layout with D-pad on left and buttons on right.
     */
    private void initializeControls() {
        this.setSpacing(CONTROLS_SPACING);
        this.setAlignment(Pos.CENTER);
        
        // Create D-pad on the left
        dPad = createDPad();
        
        // Create action buttons on the right
        actionButtons = createActionButtons();
        
        // Add components to HBox
        this.getChildren().addAll(dPad, actionButtons);
    }
    
    /**
     * Creates a cross-shaped D-pad control element.
     * The D-pad consists of a horizontal and vertical rectangle
     * forming a plus/cross shape with white/light colored segments.
     * 
     * Requirements: 6.1, 6.3
     * 
     * @return A Pane containing the D-pad shape
     */
    Pane createDPad() {
        StackPane dPadContainer = new StackPane();
        dPadContainer.setPrefSize(DPAD_SIZE, DPAD_SIZE);
        dPadContainer.setMaxSize(DPAD_SIZE, DPAD_SIZE);
        dPadContainer.setMinSize(DPAD_SIZE, DPAD_SIZE);
        
        // Create horizontal arm of the cross
        Rectangle horizontalArm = new Rectangle(DPAD_SIZE, DPAD_ARM_WIDTH);
        horizontalArm.setStyle(getDPadArmStyle());
        horizontalArm.setArcWidth(4);
        horizontalArm.setArcHeight(4);
        
        // Create vertical arm of the cross
        Rectangle verticalArm = new Rectangle(DPAD_ARM_WIDTH, DPAD_SIZE);
        verticalArm.setStyle(getDPadArmStyle());
        verticalArm.setArcWidth(4);
        verticalArm.setArcHeight(4);
        
        // Create center circle for depth effect
        Circle centerCircle = new Circle(DPAD_ARM_WIDTH / 2.5);
        centerCircle.setStyle(String.format(
            "-fx-fill: %s;",
            PokedexTheme.DPAD_SHADOW
        ));
        
        // Stack the arms to form a cross
        dPadContainer.getChildren().addAll(horizontalArm, verticalArm, centerCircle);
        
        return dPadContainer;
    }
    
    /**
     * Creates two circular action buttons.
     * Each button has an outer circle and an inner detail circle
     * for visual authenticity.
     * 
     * Requirements: 6.2, 6.4
     * 
     * @return An HBox containing the two action buttons
     */
    HBox createActionButtons() {
        HBox buttonsContainer = new HBox(BUTTON_SPACING);
        buttonsContainer.setAlignment(Pos.CENTER);
        
        // Create two action buttons
        StackPane button1 = createActionButton();
        StackPane button2 = createActionButton();
        
        buttonsContainer.getChildren().addAll(button1, button2);
        
        return buttonsContainer;
    }
    
    /**
     * Creates a single circular action button with inner detail.
     * 
     * @return A StackPane containing the button circles
     */
    private StackPane createActionButton() {
        StackPane buttonContainer = new StackPane();
        buttonContainer.setPrefSize(BUTTON_SIZE, BUTTON_SIZE);
        buttonContainer.setMaxSize(BUTTON_SIZE, BUTTON_SIZE);
        buttonContainer.setMinSize(BUTTON_SIZE, BUTTON_SIZE);
        
        // Outer circle
        Circle outerCircle = new Circle(BUTTON_SIZE / 2.0);
        outerCircle.setStyle(String.format(
            "-fx-fill: %s;",
            PokedexTheme.DPAD_COLOR
        ));
        
        // Inner detail circle for depth
        Circle innerCircle = new Circle(BUTTON_INNER_SIZE / 2.0);
        innerCircle.setStyle(String.format(
            "-fx-fill: %s;",
            PokedexTheme.DPAD_SHADOW
        ));
        
        buttonContainer.getChildren().addAll(outerCircle, innerCircle);
        
        return buttonContainer;
    }
    
    /**
     * Gets the CSS style for D-pad arms.
     * 
     * @return CSS style string for D-pad segments
     */
    private String getDPadArmStyle() {
        return String.format(
            "-fx-fill: %s;",
            PokedexTheme.DPAD_COLOR
        );
    }
    
    /**
     * Gets the D-pad component for testing purposes.
     * 
     * @return The D-pad Pane
     */
    public Pane getDPad() {
        return dPad;
    }
    
    /**
     * Gets the action buttons container for testing purposes.
     * 
     * @return The HBox containing action buttons
     */
    public HBox getActionButtons() {
        return actionButtons;
    }
    
    /**
     * Gets the number of action buttons.
     * 
     * @return The count of action buttons (should be 2)
     */
    public int getActionButtonCount() {
        return actionButtons != null ? actionButtons.getChildren().size() : 0;
    }
}
