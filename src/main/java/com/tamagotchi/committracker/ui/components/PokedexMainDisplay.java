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
        
        // Create stats corner (top-left) - don't span full width or height
        statsCorner = new PokedexStatsCorner();
        // Set max width and height to prevent expanding beyond content
        statsCorner.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        statsCorner.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        
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
        
        // Make Pokemon sprite larger
        pokemonDisplay.setScaleX(1.3);
        pokemonDisplay.setScaleY(1.3);
        
        // Create name label (bottom-right)
        nameLabel = new PokedexNameLabel();
        updateNameFromStage();
        
        // Use a StackPane as the center to layer stats corner over Pokemon
        StackPane contentStack = new StackPane();
        
        // Add Pokemon display - position slightly lower to avoid stats overlap
        contentStack.getChildren().add(pokemonDisplay);
        StackPane.setAlignment(pokemonDisplay, Pos.CENTER);
        StackPane.setMargin(pokemonDisplay, new Insets(28, 0, 0, 0)); // Push down 28px (moved up 3px)
        
        // Add stats corner in top-left (overlaid on Pokemon area)
        contentStack.getChildren().add(statsCorner);
        StackPane.setAlignment(statsCorner, Pos.TOP_LEFT);
        StackPane.setMargin(statsCorner, new Insets(-2, 0, 0, 0)); // Move up 2px
        
        // Add name label in bottom-right (overlaid on Pokemon area, like stats corner)
        nameLabel.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        nameLabel.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        contentStack.getChildren().add(nameLabel);
        StackPane.setAlignment(nameLabel, Pos.BOTTOM_RIGHT);
        // Shift name label: down 3px (negative bottom margin), right margin reduced to -3px
        StackPane.setMargin(nameLabel, new Insets(0, -3, -3, 0));
        
        this.setCenter(contentStack);
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
     * Updates the Pokemon state animation (happy, sad, thriving, content).
     * Triggers the appropriate animation based on the new state.
     * 
     * Requirements: 9.3
     * 
     * @param state The new Pokemon state
     */
    public void updatePokemonState(PokemonState state) {
        if (pokemonDisplay != null && state != null) {
            pokemonDisplay.updateState(state);
        }
    }
    
    /**
     * Triggers a commit animation on the Pokemon display.
     * For eggs: plays shake animation
     * For Pokemon: plays happy animation
     * 
     * Requirements: 9.1
     * 
     * @param totalXP Total accumulated XP
     * @param streakDays Current commit streak in days
     */
    public void triggerCommitAnimation(int totalXP, int streakDays) {
        if (pokemonDisplay != null) {
            pokemonDisplay.triggerCommitAnimation(totalXP, streakDays);
        }
    }
    
    /**
     * Checks evolution requirements and triggers evolution if conditions are met.
     * 
     * Requirements: 9.2
     * 
     * @param xpLevel Current XP level
     * @param streakDays Current commit streak in days
     * @return true if evolution was triggered
     */
    public boolean checkEvolutionRequirements(int xpLevel, int streakDays) {
        if (pokemonDisplay != null) {
            boolean evolved = pokemonDisplay.checkEvolutionRequirements(xpLevel, streakDays);
            if (evolved) {
                // Update current stage and name after evolution
                this.currentStage = pokemonDisplay.getCurrentStage();
                this.currentSpecies = pokemonDisplay.getCurrentSpecies();
                updateNameFromStage();
            }
            return evolved;
        }
        return false;
    }
    
    /**
     * Sets the evolution listener to be notified when evolution completes.
     * 
     * @param listener The evolution listener
     */
    public void setEvolutionListener(PokemonDisplayComponent.EvolutionListener listener) {
        if (pokemonDisplay != null) {
            pokemonDisplay.setEvolutionListener((newSpecies, newStage) -> {
                // Update internal state
                this.currentSpecies = newSpecies;
                this.currentStage = newStage;
                updateNameFromStage();
                
                // Notify external listener
                if (listener != null) {
                    listener.onEvolutionComplete(newSpecies, newStage);
                }
            });
        }
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
