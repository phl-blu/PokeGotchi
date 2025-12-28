package com.tamagotchi.committracker.ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import com.tamagotchi.committracker.pokemon.EvolutionStage;
import com.tamagotchi.committracker.pokemon.PokemonSpecies;
import com.tamagotchi.committracker.ui.theme.PokedexTheme;

/**
 * Name label component for the Pokedex UI.
 * Displays the Pokemon name at the bottom center of the main display screen.
 * 
 * The name displayed corresponds to the current evolution stage:
 * - EGG: Shows "Egg" (generic)
 * - BASIC: Shows the base Pokemon name (e.g., "Charmander")
 * - STAGE_1: Shows the first evolution name (e.g., "Charmeleon")
 * - STAGE_2: Shows the final evolution name (e.g., "Charizard")
 * 
 * Requirements: 5.1, 5.2, 5.3
 */
public class PokedexNameLabel extends HBox {
    
    // Styling constants
    private static final int FONT_SIZE = 12;
    
    // Label for name display
    private Label nameLabel;
    
    // Current name value (for testing without JavaFX)
    private String currentName = "";
    
    /**
     * Creates a new PokedexNameLabel component.
     * Initializes with an empty name.
     */
    public PokedexNameLabel() {
        initializeComponent();
    }
    
    /**
     * Initializes the name label layout and styling.
     */
    private void initializeComponent() {
        // Apply container styling
        this.setStyle(PokedexTheme.getNameLabelStyle());
        this.setAlignment(Pos.CENTER);
        
        // Create name label
        nameLabel = new Label(currentName);
        nameLabel.setStyle(PokedexTheme.combineStyles(
            PokedexTheme.getPixelFontStyle(FONT_SIZE),
            PokedexTheme.getTextStyle(PokedexTheme.TEXT_WHITE)
        ));
        
        // Add label to container
        this.getChildren().add(nameLabel);
    }
    
    /**
     * Sets the displayed Pokemon name.
     * 
     * Requirements: 5.2
     * 
     * @param name The Pokemon name to display
     */
    public void setName(String name) {
        this.currentName = name != null ? name : "";
        if (nameLabel != null) {
            nameLabel.setText(this.currentName);
        }
    }
    
    /**
     * Gets the currently displayed name.
     * 
     * @return The current Pokemon name
     */
    public String getName() {
        return currentName;
    }
    
    /**
     * Gets the name label for testing purposes.
     * 
     * @return The name Label
     */
    public Label getNameLabel() {
        return nameLabel;
    }
    
    /**
     * Gets the Pokemon name for a given species and evolution stage.
     * This static method can be used for testing without JavaFX.
     * 
     * Requirements: 5.2, 5.3
     * 
     * @param species The Pokemon species (base form)
     * @param stage The current evolution stage
     * @return The correct Pokemon name for the given stage
     */
    public static String getPokemonNameForStage(PokemonSpecies species, EvolutionStage stage) {
        if (species == null || stage == null) {
            return "";
        }
        
        if (stage == EvolutionStage.EGG) {
            return "Egg";
        }
        
        // Get the evolution line for this species
        PokemonSpecies[] evolutionLine = getEvolutionLine(species);
        if (evolutionLine == null) {
            return formatPokemonName(species);
        }
        
        // Return the appropriate name based on stage
        switch (stage) {
            case BASIC:
                return formatPokemonName(evolutionLine[0]);
            case STAGE_1:
                return formatPokemonName(evolutionLine[1]);
            case STAGE_2:
                return formatPokemonName(evolutionLine[2]);
            default:
                return formatPokemonName(species);
        }
    }
    
    /**
     * Gets the evolution line for a given Pokemon species.
     * Returns the 3-Pokemon evolution line that contains this species.
     * 
     * @param species Any Pokemon in the evolution line
     * @return Array of [BASIC, STAGE_1, STAGE_2] Pokemon, or null if not found
     */
    private static PokemonSpecies[] getEvolutionLine(PokemonSpecies species) {
        // Define all evolution lines
        PokemonSpecies[][] evolutionLines = {
            {PokemonSpecies.CHARMANDER, PokemonSpecies.CHARMELEON, PokemonSpecies.CHARIZARD},
            {PokemonSpecies.CYNDAQUIL, PokemonSpecies.QUILAVA, PokemonSpecies.TYPHLOSION},
            {PokemonSpecies.MUDKIP, PokemonSpecies.MARSHTOMP, PokemonSpecies.SWAMPERT},
            {PokemonSpecies.PIPLUP, PokemonSpecies.PRINPLUP, PokemonSpecies.EMPOLEON},
            {PokemonSpecies.SNIVY, PokemonSpecies.SERVINE, PokemonSpecies.SERPERIOR},
            {PokemonSpecies.FROAKIE, PokemonSpecies.FROGADIER, PokemonSpecies.GRENINJA},
            {PokemonSpecies.ROWLET, PokemonSpecies.DARTRIX, PokemonSpecies.DECIDUEYE},
            {PokemonSpecies.GROOKEY, PokemonSpecies.THWACKEY, PokemonSpecies.RILLABOOM},
            {PokemonSpecies.FUECOCO, PokemonSpecies.CROCALOR, PokemonSpecies.SKELEDIRGE}
        };
        
        // Find the evolution line containing this species
        for (PokemonSpecies[] line : evolutionLines) {
            for (PokemonSpecies pokemon : line) {
                if (pokemon == species) {
                    return line;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Formats a Pokemon species name for display.
     * Converts from UPPER_CASE enum to Title Case display name.
     * 
     * @param species The Pokemon species
     * @return The formatted display name
     */
    public static String formatPokemonName(PokemonSpecies species) {
        if (species == null) {
            return "";
        }
        String name = species.name();
        // Convert UPPER_CASE to Title Case
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }
}
