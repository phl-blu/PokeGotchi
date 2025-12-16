package com.tamagotchi.committracker.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import com.tamagotchi.committracker.ui.components.PokemonDisplayComponent;
import com.tamagotchi.committracker.pokemon.PokemonSpecies;
import com.tamagotchi.committracker.pokemon.PokemonState;
import com.tamagotchi.committracker.pokemon.EvolutionStage;

/**
 * Simple test application to verify Pokemon sprites are loading and displaying correctly.
 * Run this to see your Pokemon images in action!
 */
public class PokemonDisplayTest extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        // Create Pokemon display components for testing
        
        // Test 1: Egg state (should show your egg image)
        PokemonDisplayComponent eggDisplay = new PokemonDisplayComponent(
            PokemonSpecies.CHARMANDER, EvolutionStage.EGG, PokemonState.CONTENT
        );
        
        // Test 2: Basic Charmander (should show your Charmander image)
        PokemonDisplayComponent charmanderDisplay = new PokemonDisplayComponent(
            PokemonSpecies.CHARMANDER, EvolutionStage.BASIC, PokemonState.CONTENT
        );
        
        // Layout
        VBox root = new VBox(20);
        root.getChildren().addAll(eggDisplay, charmanderDisplay);
        root.setStyle("-fx-padding: 20; -fx-alignment: center;");
        
        // Scene and stage
        Scene scene = new Scene(root, 300, 200);
        primaryStage.setTitle("Pokemon Sprite Test");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Test evolution after 3 seconds
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                // Trigger evolution from egg to basic
                javafx.application.Platform.runLater(() -> {
                    eggDisplay.checkEvolutionRequirements(250, 5); // Should evolve egg to basic
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}