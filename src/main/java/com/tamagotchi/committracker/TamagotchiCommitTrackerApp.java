package com.tamagotchi.committracker;

import javafx.application.Application;
import javafx.stage.Stage;
import com.tamagotchi.committracker.config.AppConfig;

/**
 * Main JavaFX Application class for the Tamagotchi Commit Tracker.
 * This desktop widget monitors Git repositories and displays a Pokemon
 * whose evolution and state reflect the user's coding activity.
 */
public class TamagotchiCommitTrackerApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // TODO: Initialize the widget window and Pokemon display
        // This will be implemented in task 4
        primaryStage.setTitle("Tamagotchi Commit Tracker");
        primaryStage.setWidth(AppConfig.COMPACT_WIDTH);
        primaryStage.setHeight(AppConfig.COMPACT_HEIGHT);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}