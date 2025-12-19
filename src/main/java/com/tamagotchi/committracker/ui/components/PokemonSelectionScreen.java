package com.tamagotchi.committracker.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import com.tamagotchi.committracker.pokemon.PokemonSelectionData;
import com.tamagotchi.committracker.pokemon.PokemonSpecies;
import com.tamagotchi.committracker.util.AnimationUtils;

import java.util.function.Consumer;

/**
 * Pokemon selection screen displayed on first application launch.
 * Shows a 3x3 grid of 9 starter Pokemon for the user to choose from.
 * The selection is saved permanently and this screen never shows again.
 * Uses flexible sizing that adapts to content with minimum constraints.
 */
public class PokemonSelectionScreen {
    
    // Compact dimensions for smaller window
    private static final int MIN_WIDTH = 320;
    private static final int MIN_HEIGHT = 300;
    private static final int SPRITE_SIZE = 48;
    
    private Stage stage;
    private PokemonSpecies selectedPokemon;
    private Consumer<PokemonSpecies> onSelectionConfirmed;
    private Button confirmButton;
    private VBox selectedCard;
    
    /**
     * Creates a new Pokemon selection screen.
     * 
     * @param owner The owner stage (main application window)
     * @param onSelectionConfirmed Callback when selection is confirmed
     */
    public PokemonSelectionScreen(Stage owner, Consumer<PokemonSpecies> onSelectionConfirmed) {
        this.onSelectionConfirmed = onSelectionConfirmed;
        this.selectedPokemon = null;
        
        createStage(owner);
    }
    
    /**
     * Creates the selection stage and UI components.
     * Uses flexible sizing that adapts to content.
     */
    private void createStage(Stage owner) {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle("Choose Your Starter Pokemon");
        
        // Allow resizing but set minimum constraints
        stage.setResizable(true);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        
        // Main container - uses VBox.setVgrow for flexible children
        VBox mainContainer = new VBox(10);
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.setPadding(new Insets(15));
        mainContainer.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #2c3e50, #1a252f);" +
            "-fx-background-radius: 10;"
        );
        
        // Title
        Label titleLabel = new Label("Choose Your Starter Pokemon");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 22));
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setAlignment(Pos.CENTER);
        
        // Subtitle
        Label subtitleLabel = new Label("Select your companion for your coding journey!");
        subtitleLabel.setFont(Font.font("System", 12));
        subtitleLabel.setTextFill(Color.LIGHTGRAY);
        subtitleLabel.setMaxWidth(Double.MAX_VALUE);
        subtitleLabel.setAlignment(Pos.CENTER);
        
        // Pokemon grid (3x3) - grows to fill available space
        GridPane pokemonGrid = createPokemonGrid();
        VBox.setVgrow(pokemonGrid, Priority.ALWAYS);
        
        // Confirm button (disabled until selection)
        confirmButton = new Button("Confirm Selection");
        confirmButton.setDisable(true);
        confirmButton.setFont(Font.font("System", FontWeight.BOLD, 14));
        confirmButton.setMaxWidth(Double.MAX_VALUE); // Button stretches horizontally
        confirmButton.setStyle(
            "-fx-background-color: #27ae60;" +
            "-fx-text-fill: white;" +
            "-fx-padding: 10 30;" +
            "-fx-background-radius: 5;" +
            "-fx-cursor: hand;"
        );
        confirmButton.setOnAction(e -> confirmSelection());
        
        // Disable styling when button is disabled
        confirmButton.disabledProperty().addListener((obs, wasDisabled, isDisabled) -> {
            if (isDisabled) {
                confirmButton.setStyle(
                    "-fx-background-color: #7f8c8d;" +
                    "-fx-text-fill: #bdc3c7;" +
                    "-fx-padding: 10 30;" +
                    "-fx-background-radius: 5;"
                );
            } else {
                confirmButton.setStyle(
                    "-fx-background-color: #27ae60;" +
                    "-fx-text-fill: white;" +
                    "-fx-padding: 10 30;" +
                    "-fx-background-radius: 5;" +
                    "-fx-cursor: hand;"
                );
            }
        });
        
        mainContainer.getChildren().addAll(titleLabel, subtitleLabel, pokemonGrid, confirmButton);
        
        // Create scene without fixed dimensions - will auto-size to content
        Scene scene = new Scene(mainContainer);
        stage.setScene(scene);
        
        // Size to fit content, then center on screen
        stage.sizeToScene();
        
        // Always center on screen (not on owner, since owner widget is tiny)
        stage.setOnShown(e -> {
            // Get screen dimensions
            javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
            
            // Center the stage on screen
            stage.setX((screenBounds.getWidth() - stage.getWidth()) / 2);
            stage.setY((screenBounds.getHeight() - stage.getHeight()) / 2);
        });
    }
    
    /**
     * Creates the 3x3 grid of Pokemon options.
     * Grid uses flexible column/row constraints to adapt to available space.
     */
    private GridPane createPokemonGrid() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(5));
        
        // Set up flexible column constraints (3 columns, each grows equally)
        for (int i = 0; i < 3; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setHgrow(Priority.ALWAYS);
            col.setFillWidth(true);
            grid.getColumnConstraints().add(col);
        }
        
        // Set up flexible row constraints (3 rows, each grows equally)
        for (int i = 0; i < 3; i++) {
            RowConstraints row = new RowConstraints();
            row.setVgrow(Priority.ALWAYS);
            row.setFillHeight(true);
            grid.getRowConstraints().add(row);
        }
        
        PokemonSpecies[] starters = PokemonSelectionData.getStarterOptions();
        
        for (int i = 0; i < starters.length; i++) {
            int row = i / 3;
            int col = i % 3;
            
            VBox pokemonCard = createPokemonCard(starters[i]);
            grid.add(pokemonCard, col, row);
        }
        
        return grid;
    }
    
    /**
     * Creates a card for a single Pokemon option.
     * Card uses flexible sizing with min/pref constraints.
     */
    private VBox createPokemonCard(PokemonSpecies species) {
        VBox card = new VBox(5);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(10));
        
        // Compact sizing for smaller window
        card.setMinSize(80, 90);
        card.setPrefSize(90, 100);
        card.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        
        card.setStyle(
            "-fx-background-color: #34495e;" +
            "-fx-background-radius: 8;" +
            "-fx-cursor: hand;"
        );
        
        // Pokemon sprite (egg image)
        ImageView spriteView = new ImageView();
        spriteView.setFitWidth(SPRITE_SIZE);
        spriteView.setFitHeight(SPRITE_SIZE);
        spriteView.setPreserveRatio(true);
        
        // Load egg sprite for this Pokemon
        Image eggSprite = AnimationUtils.loadEggPreviewSprite(species);
        if (eggSprite != null) {
            spriteView.setImage(eggSprite);
        }
        
        // Pokemon name
        Label nameLabel = new Label(PokemonSelectionData.getDisplayName(species));
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        nameLabel.setTextFill(Color.WHITE);
        
        // Pokemon type
        String type = PokemonSelectionData.getPokemonType(species);
        Label typeLabel = new Label(type + " Type");
        typeLabel.setFont(Font.font("System", 10));
        typeLabel.setTextFill(getTypeColor(type));
        
        card.getChildren().addAll(spriteView, nameLabel, typeLabel);
        
        // Click handler
        card.setOnMouseClicked(e -> handlePokemonSelection(species, card));
        
        // Hover effects
        card.setOnMouseEntered(e -> {
            if (selectedPokemon != species) {
                card.setStyle(
                    "-fx-background-color: #4a6278;" +
                    "-fx-background-radius: 8;" +
                    "-fx-cursor: hand;"
                );
            }
        });
        
        card.setOnMouseExited(e -> {
            if (selectedPokemon != species) {
                card.setStyle(
                    "-fx-background-color: #34495e;" +
                    "-fx-background-radius: 8;" +
                    "-fx-cursor: hand;"
                );
            }
        });
        
        return card;
    }
    
    /**
     * Gets the color for a Pokemon type.
     */
    private Color getTypeColor(String type) {
        switch (type) {
            case "Fire":
                return Color.ORANGE;
            case "Water":
                return Color.DEEPSKYBLUE;
            case "Grass":
                return Color.LIGHTGREEN;
            default:
                return Color.LIGHTGRAY;
        }
    }
    
    /**
     * Handles Pokemon selection when a card is clicked.
     * Highlights the selected card and enables the confirm button.
     * User must click "Confirm Selection" to finalize their choice.
     */
    private void handlePokemonSelection(PokemonSpecies species, VBox card) {
        // Deselect previous selection
        if (selectedCard != null) {
            selectedCard.setStyle(
                "-fx-background-color: #34495e;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;"
            );
            selectedCard.setEffect(null);
        }
        
        // Select new Pokemon
        selectedPokemon = species;
        selectedCard = card;
        
        // Highlight selected card
        card.setStyle(
            "-fx-background-color: #2980b9;" +
            "-fx-background-radius: 8;" +
            "-fx-cursor: hand;" +
            "-fx-border-color: #f1c40f;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 8;"
        );
        
        // Add glow effect
        DropShadow glow = new DropShadow();
        glow.setColor(Color.GOLD);
        glow.setRadius(10);
        card.setEffect(glow);
        
        // Enable confirm button - user must click to confirm
        confirmButton.setDisable(false);
        
        System.out.println("🎮 Selected: " + PokemonSelectionData.getDisplayName(species));
    }
    
    /**
     * Confirms the selection and closes the screen.
     */
    private void confirmSelection() {
        if (selectedPokemon != null) {
            System.out.println("✅ Confirmed selection: " + PokemonSelectionData.getDisplayName(selectedPokemon));
            
            // Save selection
            PokemonSelectionData selectionData = new PokemonSelectionData();
            selectionData.saveSelection(selectedPokemon);
            
            // Close screen
            stage.close();
            
            // Notify callback
            if (onSelectionConfirmed != null) {
                onSelectionConfirmed.accept(selectedPokemon);
            }
        }
    }
    
    /**
     * Shows the selection screen and waits for user selection.
     */
    public void show() {
        stage.showAndWait();
    }
    
    /**
     * Shows the selection screen without blocking.
     */
    public void showNonBlocking() {
        stage.show();
    }
    
    /**
     * Gets the selected Pokemon (may be null if not yet selected).
     */
    public PokemonSpecies getSelectedPokemon() {
        return selectedPokemon;
    }
    
    /**
     * Checks if a Pokemon has been selected.
     */
    public boolean hasSelection() {
        return selectedPokemon != null;
    }
}
