package com.tamagotchi.committracker.ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.input.MouseEvent;

import com.tamagotchi.committracker.ui.theme.PokedexTheme;

/**
 * Functional D-pad and action buttons component for the Pokedex UI.
 * Creates a retro handheld gaming device control layout with:
 * - A cross-shaped D-pad on the left for navigation
 * - Two circular action buttons (A and B) on the right
 * 
 * The D-pad supports left/right navigation between screens.
 * Button A confirms selections, Button B returns to the Pokemon screen.
 * 
 * Requirements: 6.1, 6.2, 6.3, 6.4, 7.4, 7.5, 7.6
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
    
    // D-pad pressed color
    private static final String DPAD_PRESSED = "#E0E0E0";
    private static final String DPAD_HOVER = "#FAFAFA";
    
    private Pane dPad;
    private HBox actionButtons;
    
    // D-pad direction regions for click handling
    private Rectangle leftRegion;
    private Rectangle rightRegion;
    private Rectangle upRegion;
    private Rectangle downRegion;
    
    // Action buttons
    private StackPane buttonA;
    private StackPane buttonB;
    
    // Event handlers
    private Runnable onLeftPressed;
    private Runnable onRightPressed;
    private Runnable onUpPressed;
    private Runnable onDownPressed;
    private Runnable onButtonAPressed;
    private Runnable onButtonBPressed;
    
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
     * Creates a cross-shaped D-pad control element with clickable regions.
     * The D-pad consists of four separate directional segments
     * forming a plus/cross shape with white/light colored segments.
     * Each direction (left, right, up, down) is clickable with independent visual feedback.
     * 
     * Requirements: 6.1, 6.2, 7.5, 7.6
     * 
     * @return A Pane containing the D-pad shape
     */
    Pane createDPad() {
        StackPane dPadContainer = new StackPane();
        dPadContainer.setPrefSize(DPAD_SIZE, DPAD_SIZE);
        dPadContainer.setMaxSize(DPAD_SIZE, DPAD_SIZE);
        dPadContainer.setMinSize(DPAD_SIZE, DPAD_SIZE);
        
        // Create center square for the D-pad hub
        Rectangle centerSquare = new Rectangle(DPAD_ARM_WIDTH, DPAD_ARM_WIDTH);
        centerSquare.setStyle(getDPadArmStyle());
        centerSquare.setArcWidth(2);
        centerSquare.setArcHeight(2);
        
        // Create separate visual elements for each direction
        // Left arm
        Rectangle leftArm = new Rectangle(DPAD_ARM_LENGTH, DPAD_ARM_WIDTH);
        leftArm.setStyle(getDPadArmStyle());
        leftArm.setArcWidth(4);
        leftArm.setArcHeight(4);
        leftArm.setTranslateX(-DPAD_SIZE / 2 + DPAD_ARM_LENGTH / 2);
        
        // Right arm
        Rectangle rightArm = new Rectangle(DPAD_ARM_LENGTH, DPAD_ARM_WIDTH);
        rightArm.setStyle(getDPadArmStyle());
        rightArm.setArcWidth(4);
        rightArm.setArcHeight(4);
        rightArm.setTranslateX(DPAD_SIZE / 2 - DPAD_ARM_LENGTH / 2);
        
        // Up arm
        Rectangle upArm = new Rectangle(DPAD_ARM_WIDTH, DPAD_ARM_LENGTH);
        upArm.setStyle(getDPadArmStyle());
        upArm.setArcWidth(4);
        upArm.setArcHeight(4);
        upArm.setTranslateY(-DPAD_SIZE / 2 + DPAD_ARM_LENGTH / 2);
        
        // Down arm
        Rectangle downArm = new Rectangle(DPAD_ARM_WIDTH, DPAD_ARM_LENGTH);
        downArm.setStyle(getDPadArmStyle());
        downArm.setArcWidth(4);
        downArm.setArcHeight(4);
        downArm.setTranslateY(DPAD_SIZE / 2 - DPAD_ARM_LENGTH / 2);
        
        // Create center circle for depth effect
        Circle centerCircle = new Circle(DPAD_ARM_WIDTH / 2.5);
        centerCircle.setStyle(String.format(
            "-fx-fill: %s;",
            PokedexTheme.DPAD_SHADOW
        ));
        
        // Create invisible clickable regions for each direction
        // Left region - uses leftArm for visual feedback
        leftRegion = new Rectangle(DPAD_ARM_LENGTH, DPAD_ARM_WIDTH);
        leftRegion.setStyle("-fx-fill: transparent;");
        leftRegion.setTranslateX(-DPAD_SIZE / 2 + DPAD_ARM_LENGTH / 2);
        setupDPadRegion(leftRegion, leftArm, () -> {
            if (onLeftPressed != null) onLeftPressed.run();
        });
        
        // Right region - uses rightArm for visual feedback
        rightRegion = new Rectangle(DPAD_ARM_LENGTH, DPAD_ARM_WIDTH);
        rightRegion.setStyle("-fx-fill: transparent;");
        rightRegion.setTranslateX(DPAD_SIZE / 2 - DPAD_ARM_LENGTH / 2);
        setupDPadRegion(rightRegion, rightArm, () -> {
            if (onRightPressed != null) onRightPressed.run();
        });
        
        // Up region - uses upArm for visual feedback
        upRegion = new Rectangle(DPAD_ARM_WIDTH, DPAD_ARM_LENGTH);
        upRegion.setStyle("-fx-fill: transparent;");
        upRegion.setTranslateY(-DPAD_SIZE / 2 + DPAD_ARM_LENGTH / 2);
        setupDPadRegion(upRegion, upArm, () -> {
            if (onUpPressed != null) onUpPressed.run();
        });
        
        // Down region - uses downArm for visual feedback
        downRegion = new Rectangle(DPAD_ARM_WIDTH, DPAD_ARM_LENGTH);
        downRegion.setStyle("-fx-fill: transparent;");
        downRegion.setTranslateY(DPAD_SIZE / 2 - DPAD_ARM_LENGTH / 2);
        setupDPadRegion(downRegion, downArm, () -> {
            if (onDownPressed != null) onDownPressed.run();
        });
        
        // Stack the arms to form a cross, then add clickable regions on top
        dPadContainer.getChildren().addAll(
            centerSquare, leftArm, rightArm, upArm, downArm, centerCircle,
            leftRegion, rightRegion, upRegion, downRegion
        );
        
        return dPadContainer;
    }
    
    /**
     * Sets up a D-pad region with hover and click handlers.
     * 
     * @param region The clickable region
     * @param arm The visual arm to highlight
     * @param action The action to run on click
     */
    private void setupDPadRegion(Rectangle region, Rectangle arm, Runnable action) {
        region.setOnMouseEntered(e -> {
            arm.setStyle(String.format("-fx-fill: %s;", DPAD_HOVER));
            region.setCursor(javafx.scene.Cursor.HAND);
        });
        
        region.setOnMouseExited(e -> {
            arm.setStyle(getDPadArmStyle());
            region.setCursor(javafx.scene.Cursor.DEFAULT);
        });
        
        region.setOnMousePressed(e -> {
            arm.setStyle(String.format("-fx-fill: %s;", DPAD_PRESSED));
            e.consume();
        });
        
        region.setOnMouseReleased(e -> {
            arm.setStyle(String.format("-fx-fill: %s;", DPAD_HOVER));
            if (action != null) action.run();
            e.consume();
        });

        // Consume click events so double-clicking d-pad doesn't bubble to root and trigger toggleMode
        region.setOnMouseClicked(e -> e.consume());
    }
    
    /**
     * Creates two circular action buttons labeled A and B.
     * Each button has an outer circle, inner detail circle, and label.
     * Buttons have hover and pressed state visual feedback.
     * 
     * Requirements: 6.3, 6.4, 7.4, 7.5
     * 
     * @return An HBox containing the two action buttons
     */
    HBox createActionButtons() {
        HBox buttonsContainer = new HBox(BUTTON_SPACING);
        buttonsContainer.setAlignment(Pos.CENTER);
        
        // Create two action buttons with labels
        buttonA = createActionButton("A", () -> {
            if (onButtonAPressed != null) onButtonAPressed.run();
        });
        buttonB = createActionButton("B", () -> {
            if (onButtonBPressed != null) onButtonBPressed.run();
        });
        
        buttonsContainer.getChildren().addAll(buttonA, buttonB);
        
        return buttonsContainer;
    }
    
    /**
     * Creates a single circular action button with label and visual feedback.
     * 
     * @param label The button label (A or B)
     * @param action The action to run on click
     * @return A StackPane containing the button
     */
    private StackPane createActionButton(String label, Runnable action) {
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
        
        // Label for the button
        Label buttonLabel = new Label(label);
        buttonLabel.setStyle(String.format(
            "-fx-font-size: 6px; -fx-font-weight: bold; -fx-text-fill: %s;",
            PokedexTheme.TEXT_DARK
        ));
        
        buttonContainer.getChildren().addAll(outerCircle, innerCircle, buttonLabel);
        
        // Add hover and click handlers
        buttonContainer.setOnMouseEntered(e -> {
            outerCircle.setStyle(String.format("-fx-fill: %s;", DPAD_HOVER));
            buttonContainer.setCursor(javafx.scene.Cursor.HAND);
        });
        
        buttonContainer.setOnMouseExited(e -> {
            outerCircle.setStyle(String.format("-fx-fill: %s;", PokedexTheme.DPAD_COLOR));
            buttonContainer.setCursor(javafx.scene.Cursor.DEFAULT);
        });
        
        buttonContainer.setOnMousePressed(e -> {
            outerCircle.setStyle(String.format("-fx-fill: %s;", DPAD_PRESSED));
        });
        
        buttonContainer.setOnMouseReleased(e -> {
            outerCircle.setStyle(String.format("-fx-fill: %s;", DPAD_HOVER));
            if (action != null) action.run();
        });
        
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
    
    // ========== Event Handler Setters ==========
    
    /**
     * Sets the handler for D-pad left press.
     * 
     * @param handler The handler to run when left is pressed
     */
    public void setOnLeftPressed(Runnable handler) {
        this.onLeftPressed = handler;
    }
    
    /**
     * Sets the handler for D-pad right press.
     * 
     * @param handler The handler to run when right is pressed
     */
    public void setOnRightPressed(Runnable handler) {
        this.onRightPressed = handler;
    }
    
    /**
     * Sets the handler for D-pad up press.
     * 
     * @param handler The handler to run when up is pressed
     */
    public void setOnUpPressed(Runnable handler) {
        this.onUpPressed = handler;
    }
    
    /**
     * Sets the handler for D-pad down press.
     * 
     * @param handler The handler to run when down is pressed
     */
    public void setOnDownPressed(Runnable handler) {
        this.onDownPressed = handler;
    }
    
    /**
     * Sets the handler for Button A press.
     * 
     * @param handler The handler to run when Button A is pressed
     */
    public void setOnButtonAPressed(Runnable handler) {
        this.onButtonAPressed = handler;
    }
    
    /**
     * Sets the handler for Button B press.
     * 
     * @param handler The handler to run when Button B is pressed
     */
    public void setOnButtonBPressed(Runnable handler) {
        this.onButtonBPressed = handler;
    }
    
    /**
     * Gets Button A for testing purposes.
     * 
     * @return The Button A StackPane
     */
    public StackPane getButtonA() {
        return buttonA;
    }
    
    /**
     * Gets Button B for testing purposes.
     * 
     * @return The Button B StackPane
     */
    public StackPane getButtonB() {
        return buttonB;
    }
    
    /**
     * Gets the left D-pad region for testing purposes.
     * 
     * @return The left region Rectangle
     */
    public Rectangle getLeftRegion() {
        return leftRegion;
    }
    
    /**
     * Gets the right D-pad region for testing purposes.
     * 
     * @return The right region Rectangle
     */
    public Rectangle getRightRegion() {
        return rightRegion;
    }
}
