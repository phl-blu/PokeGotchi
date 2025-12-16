package com.tamagotchi.committracker.ui.widget;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import com.tamagotchi.committracker.config.AppConfig;
import com.tamagotchi.committracker.util.FileUtils;

import java.io.IOException;
import java.util.Properties;

/**
 * Main UI container with transparent background and drag functionality.
 * Handles switching between compact and expanded modes.
 */
public class WidgetWindow {
    private Stage stage;
    private Scene scene;
    private StackPane root;
    private boolean isCompactMode = true;
    
    // For dragging functionality
    private double xOffset = 0;
    private double yOffset = 0;
    
    // Position persistence
    private static final String POSITION_FILE = "widget-position.properties";
    private static final String X_KEY = "widget.x";
    private static final String Y_KEY = "widget.y";
    
    public WidgetWindow(Stage primaryStage) {
        this.stage = primaryStage;
        initialize();
    }
    
    /**
     * Set up transparent stage and initial pet display
     */
    public void initialize() {
        // Configure stage properties
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);
        stage.setResizable(false);
        
        // Create root container
        root = new StackPane();
        root.setStyle("-fx-background-color: transparent;");
        
        // Create scene with transparent background
        scene = new Scene(root, AppConfig.COMPACT_WIDTH, AppConfig.COMPACT_HEIGHT);
        scene.setFill(Color.TRANSPARENT);
        
        stage.setScene(scene);
        
        // Enable dragging
        enableDragging();
        
        // Restore saved position
        restorePosition();
        
        // Save position when window is moved
        stage.xProperty().addListener((obs, oldVal, newVal) -> savePosition());
        stage.yProperty().addListener((obs, oldVal, newVal) -> savePosition());
        
        // Handle window closing to save position
        stage.setOnCloseRequest(e -> {
            savePosition();
            Platform.exit();
        });
    }
    
    /**
     * Allow user to drag widget to new desktop position
     */
    public void enableDragging() {
        root.setOnMousePressed((MouseEvent event) -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        
        root.setOnMouseDragged((MouseEvent event) -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
    }
    
    /**
     * Switch between compact and expanded modes
     */
    public void toggleMode() {
        if (isCompactMode) {
            // Switch to expanded mode
            root.setPrefSize(AppConfig.EXPANDED_WIDTH, AppConfig.EXPANDED_HEIGHT);
            stage.setWidth(AppConfig.EXPANDED_WIDTH);
            stage.setHeight(AppConfig.EXPANDED_HEIGHT);
            isCompactMode = false;
        } else {
            // Switch to compact mode
            root.setPrefSize(AppConfig.COMPACT_WIDTH, AppConfig.COMPACT_HEIGHT);
            stage.setWidth(AppConfig.COMPACT_WIDTH);
            stage.setHeight(AppConfig.COMPACT_HEIGHT);
            isCompactMode = true;
        }
    }
    
    /**
     * Persist widget position for next startup
     */
    public void savePosition() {
        try {
            Properties props = new Properties();
            props.setProperty(X_KEY, String.valueOf(stage.getX()));
            props.setProperty(Y_KEY, String.valueOf(stage.getY()));
            FileUtils.saveProperties(props, POSITION_FILE);
        } catch (IOException e) {
            // Log error but don't fail - position saving is not critical
            System.err.println("Failed to save widget position: " + e.getMessage());
        }
    }
    
    /**
     * Load saved widget position on startup
     */
    public void restorePosition() {
        try {
            Properties props = FileUtils.loadProperties(POSITION_FILE);
            if (props.containsKey(X_KEY) && props.containsKey(Y_KEY)) {
                double x = Double.parseDouble(props.getProperty(X_KEY));
                double y = Double.parseDouble(props.getProperty(Y_KEY));
                
                // Ensure position is within screen bounds
                if (x >= 0 && y >= 0) {
                    stage.setX(x);
                    stage.setY(y);
                }
            }
        } catch (IOException | NumberFormatException e) {
            // Use default position if loading fails
            stage.centerOnScreen();
        }
    }
    
    /**
     * Get the root container for adding UI components
     */
    public StackPane getRoot() {
        return root;
    }
    
    /**
     * Get the stage for external configuration
     */
    public Stage getStage() {
        return stage;
    }
    
    /**
     * Check if widget is in compact mode
     */
    public boolean isCompactMode() {
        return isCompactMode;
    }
    
    /**
     * Show the widget window
     */
    public void show() {
        stage.show();
    }
}