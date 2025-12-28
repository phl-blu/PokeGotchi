package com.tamagotchi.committracker.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import com.tamagotchi.committracker.pokemon.EvolutionStage;
import com.tamagotchi.committracker.pokemon.PokemonSpecies;
import com.tamagotchi.committracker.pokemon.PokemonState;
import com.tamagotchi.committracker.ui.theme.PokedexTheme;

/**
 * Main display component for the Pokedex UI.
 * Combines the stats corner (top-left), Pokemon display (center), and name label (bottom).
 * 
 * Layout:
 * - Top-left: PokedexStatsCorner (XP, streak, stage)
 * - Center: PokemonDisplayComponent (animated Pokemon sprite)
 * - Bottom: PokedexNameLabel (Pokemon name)
 * 
 * Requirements: 3.1, 3.2, 4.1, 5.1
 */
public class PokedexMainDisplay extends BorderPane {
    
    // Child components
    private PokedexStatsCorner statsCorner;
    private PokemonDisplayComponent pokemonDisplay;
    private PokedexNameLabel nameLabel;
    
    // Current Pokemon data
    private PokemonSpecies currentSpecies;
    private EvolutionStage currentStage;
    
    /**
     * Creates a new PokedexMainDisplay with the specified Pokemon.
     * 
     * @param species The Pokemon species to display
     * @param stage The current evolution stage
     */
    public PokedexMainDisplay(PokemonSpecies species, EvolutionStage stage) {
        this.currentSpecies = species;
        this.currentStage = stage;
        initializeComponent();
    }
    
    /**
     * Initializes the main display layout and components.
     */
    private void initializeComponent() {
        // Apply screen background styling
        this.setStyle(PokedexTheme.getScreenStyle());
        this.setPadding(new Insets(8));
        
        // Create stats corner (top-left)
        statsCorner = new PokedexStatsCorner();
        
        // Wrap stats corner in a StackPane for top-left alignment
        StackPane topLeftContainer = new StackPane(statsCorner);
        topLeftContainer.setAlignment(Pos.TOP_LEFT);
        this.setTop(topLeftContainer);
        BorderPane.setAlignment(topLeftContainer, Pos.TOP_LEFT);
        
        // Create Pokemon display (center)
        if (currentSpecies != null) {
            pokemonDisplay = new PokemonDisplayComponent(
                currentSpecies, 
                currentStage, 
                PokemonState.CONTENT
            );
        } else {
            // Fallback: create empty display component
            pokemonDisplay = new PokemonDisplayComponent();
        }
        
        // Center the Pokemon display
        StackPane centerContainer = new StackPane(pokemonDisplay);
        centerContainer.setAlignment(Pos.CENTER);
        this.setCenter(centerContainer);
        
        // Create name label (bottom)
        nameLabel = new PokedexNameLabel();
        updateNameFromStage();
        
        // Wrap name label in a StackPane for bottom-center alignment
        StackPane bottomContainer = new StackPane(nameLabel);
        bottomContainer.setAlignment(Pos.CENTER);
        bottomContainer.setPadding(new Insets(4, 0, 0, 0));
        this.setBottom(bottomContainer);
        BorderPane.setAlignment(bottomContainer, Pos.CENTER);
    }
    
    /**
     * Updates the name label based on current species and stage.
     */
    private void updateNameFromStage() {
        if (currentSpecies != null && currentStage != null) {
            String name = PokedexNameLabel.getPokemonNameForStage(currentSpecies, currentStage);
            nameLabel.setName(name);
        }
    }
    
    /**
     * Updates the stats display with new values.
     * 
     * Requirements: 4.2, 4.3, 4.4
     * 
     * @param xp Current XP amount
     * @param nextThreshold XP needed for next evolution
     * @param streak Current commit streak in days
     * @param stage Current evolution stage
     */
    public void updateStats(int xp, int nextThreshold, int streak, EvolutionStage stage) {
        if (statsCorner != null) {
            statsCorner.updateXP(xp, nextThreshold);
            statsCorner.updateStreak(streak);
            statsCorner.updateStage(stage);
        }
        
        // Update current stage if changed
        if (stage != null && stage != this.currentStage) {
            this.currentStage = stage;
            updateNameFromStage();
        }
    }
    
    /**
     * Updates the Pokemon display with a new species and stage.
     * 
     * Requirements: 3.1, 3.2
     * 
     * @param species The new Pokemon species
     * @param stage The new evolution stage
     */
    public void updatePokemon(PokemonSpecies species, EvolutionStage stage) {
        this.currentSpecies = species;
        this.currentStage = stage;
        
        if (pokemonDisplay != null && species != null) {
            // Change species if different
            if (pokemonDisplay.getCurrentSpecies() != species) {
                pokemonDisplay.changeSpecies(species);
            }
            
            // Check evolution requirements if stage changed
            if (pokemonDisplay.getCurrentStage() != stage) {
                // Use instant evolution for direct stage changes
                pokemonDisplay.checkEvolutionRequirements(0, 0, true);
            }
        }
        
        // Update name label
        updateNameFromStage();
    }
    
    /**
     * Updates the displayed Pokemon name.
     * 
     * Requirements: 5.2, 5.3
     * 
     * @param name The Pokemon name to display
     */
    public void updateName(String name) {
        if (nameLabel != null) {
            nameLabel.setName(name);
        }
    }
    
    /**
     * Gets the Pokemon display component for direct access.
     * Useful for triggering animations or checking evolution status.
     * 
     * @return The PokemonDisplayComponent
     */
    public PokemonDisplayComponent getPokemonDisplay() {
        return pokemonDisplay;
    }
    
    /**
     * Gets the stats corner component for direct access.
     * 
     * @return The PokedexStatsCorner
     */
    public PokedexStatsCorner getStatsCorner() {
        return statsCorner;
    }
    
    /**
     * Gets the name label component for direct access.
     * 
     * @return The PokedexNameLabel
     */
    public PokedexNameLabel getNameLabel() {
        return nameLabel;
    }
    
    /**
     * Gets the current Pokemon species.
     * 
     * @return The current PokemonSpecies
     */
    public PokemonSpecies getCurrentSpecies() {
        return currentSpecies;
    }
    
    /**
     * Gets the current evolution stage.
     * 
     * @return The current EvolutionStage
     */
    public EvolutionStage getCurrentStage() {
        return currentStage;
    }
    
    /**
     * Cleans up resources when the display is no longer needed.
     */
    public void cleanup() {
        if (pokemonDisplay != null) {
            pokemonDisplay.cleanup();
        }
    }
}
