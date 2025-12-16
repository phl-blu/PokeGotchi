package com.tamagotchi.committracker.debug;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import com.tamagotchi.committracker.util.AnimationUtils;
import com.tamagotchi.committracker.pokemon.PokemonSpecies;
import com.tamagotchi.committracker.pokemon.PokemonState;
import com.tamagotchi.committracker.pokemon.EvolutionStage;

import java.util.List;

/**
 * Debug application to test image loading step by step
 */
public class ImageDebugTest extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(10);
        root.setStyle("-fx-padding: 20; -fx-background-color: white;");
        
        // Test 1: Direct image loading
        Label status1 = new Label("Testing direct image loading...");
        root.getChildren().add(status1);
        
        try {
            // Try to load the egg image directly
            String eggPath = "/pokemon/sprites/charmander/egg/content/frame1.png";
            System.out.println("Trying to load: " + eggPath);
            
            Image eggImage = new Image(ImageDebugTest.class.getResourceAsStream(eggPath));
            if (eggImage.isError()) {
                status1.setText("❌ Failed to load egg image directly");
                System.out.println("Error loading egg image: " + eggImage.getException());
            } else {
                status1.setText("✅ Egg image loaded directly: " + eggImage.getWidth() + "x" + eggImage.getHeight());
                ImageView eggView = new ImageView(eggImage);
                eggView.setFitWidth(64);
                eggView.setFitHeight(64);
                root.getChildren().add(eggView);
            }
        } catch (Exception e) {
            status1.setText("❌ Exception loading egg image: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Test 2: AnimationUtils loading
        Label status2 = new Label("Testing AnimationUtils loading...");
        root.getChildren().add(status2);
        
        try {
            List<Image> frames = AnimationUtils.loadSpriteFrames(
                PokemonSpecies.CHARMANDER, EvolutionStage.EGG, PokemonState.CONTENT
            );
            
            if (frames.isEmpty()) {
                status2.setText("❌ AnimationUtils returned empty frames list");
            } else {
                Image firstFrame = frames.get(0);
                if (firstFrame.isError()) {
                    status2.setText("❌ AnimationUtils frame has error: " + firstFrame.getException());
                } else {
                    status2.setText("✅ AnimationUtils loaded: " + firstFrame.getWidth() + "x" + firstFrame.getHeight());
                    ImageView frameView = new ImageView(firstFrame);
                    frameView.setFitWidth(64);
                    frameView.setFitHeight(64);
                    root.getChildren().add(frameView);
                }
            }
        } catch (Exception e) {
            status2.setText("❌ Exception in AnimationUtils: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Test 3: Basic Charmander
        Label status3 = new Label("Testing basic Charmander loading...");
        root.getChildren().add(status3);
        
        try {
            String charmanderPath = "/pokemon/sprites/charmander/basic/content/frame1.png";
            System.out.println("Trying to load: " + charmanderPath);
            
            Image charmanderImage = new Image(ImageDebugTest.class.getResourceAsStream(charmanderPath));
            if (charmanderImage.isError()) {
                status3.setText("❌ Failed to load Charmander image");
                System.out.println("Error loading Charmander: " + charmanderImage.getException());
            } else {
                status3.setText("✅ Charmander loaded: " + charmanderImage.getWidth() + "x" + charmanderImage.getHeight());
                ImageView charmanderView = new ImageView(charmanderImage);
                charmanderView.setFitWidth(64);
                charmanderView.setFitHeight(64);
                root.getChildren().add(charmanderView);
            }
        } catch (Exception e) {
            status3.setText("❌ Exception loading Charmander: " + e.getMessage());
            e.printStackTrace();
        }
        
        Scene scene = new Scene(root, 400, 600);
        primaryStage.setTitle("Image Debug Test");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}