package com.tamagotchi.committracker;

import javafx.application.Application;
import javafx.stage.Stage;
import com.tamagotchi.committracker.ui.widget.WidgetWindow;

/**
 * Main JavaFX Application class for the Tamagotchi Commit Tracker.
 * This desktop widget monitors Git repositories and displays a Pokemon
 * whose evolution and state reflect the user's coding activity.
 */
public class TamagotchiCommitTrackerApp extends Application {

    private WidgetWindow widgetWindow;

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Tamagotchi Commit Tracker");
        
        // Initialize the widget window with transparency and dragging
        widgetWindow = new WidgetWindow(primaryStage);
        widgetWindow.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}