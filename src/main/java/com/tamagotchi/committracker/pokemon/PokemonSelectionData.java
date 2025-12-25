package com.tamagotchi.committracker.pokemon;

import com.tamagotchi.committracker.util.FileUtils;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * Manages Pokemon selection persistence.
 * Stores the user's selected starter Pokemon and ensures the selection screen
 * is only shown once (on first application launch).
 */
public class PokemonSelectionData {
    
    private static final String SELECTION_FILE = "pokemon-selection.properties";
    private static final String SELECTED_STARTER_KEY = "selected.starter";
    private static final String HAS_SELECTED_KEY = "has.selected";
    private static final String SELECTION_TIMESTAMP_KEY = "selection.timestamp";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    private PokemonSpecies selectedStarter;
    private boolean hasSelectedStarter;
    private LocalDateTime selectionTimestamp;
    
    /**
     * Creates a new PokemonSelectionData instance and loads any existing selection.
     */
    public PokemonSelectionData() {
        loadSelection();
    }
    
    /**
     * Loads the saved Pokemon selection from persistent storage.
     */
    private void loadSelection() {
        try {
            Properties props = FileUtils.loadProperties(SELECTION_FILE);
            
            String hasSelected = props.getProperty(HAS_SELECTED_KEY, "false");
            this.hasSelectedStarter = Boolean.parseBoolean(hasSelected);
            
            if (hasSelectedStarter) {
                String starterName = props.getProperty(SELECTED_STARTER_KEY);
                if (starterName != null && !starterName.isEmpty()) {
                    try {
                        this.selectedStarter = PokemonSpecies.valueOf(starterName);
                    } catch (IllegalArgumentException e) {
                        // Invalid species name, reset selection
                        this.hasSelectedStarter = false;
                        this.selectedStarter = null;
                    }
                }
                
                String timestamp = props.getProperty(SELECTION_TIMESTAMP_KEY);
                if (timestamp != null && !timestamp.isEmpty()) {
                    try {
                        this.selectionTimestamp = LocalDateTime.parse(timestamp, TIMESTAMP_FORMAT);
                    } catch (Exception e) {
                        this.selectionTimestamp = null;
                    }
                }
            }
        } catch (IOException e) {
            // No saved selection, user is first-time
            this.hasSelectedStarter = false;
            this.selectedStarter = null;
            this.selectionTimestamp = null;
        }
    }
    
    /**
     * Saves the Pokemon selection to persistent storage.
     * This prevents the selection screen from showing again.
     * 
     * @param starter The selected starter Pokemon
     */
    public void saveSelection(PokemonSpecies starter) {
        if (starter == null) {
            return;
        }
        
        this.selectedStarter = starter;
        this.hasSelectedStarter = true;
        this.selectionTimestamp = LocalDateTime.now();
        
        try {
            Properties props = new Properties();
            props.setProperty(HAS_SELECTED_KEY, "true");
            props.setProperty(SELECTED_STARTER_KEY, starter.name());
            props.setProperty(SELECTION_TIMESTAMP_KEY, selectionTimestamp.format(TIMESTAMP_FORMAT));
            
            FileUtils.saveProperties(props, SELECTION_FILE);
            System.out.println("✅ Pokemon selection saved: " + starter.name());
        } catch (IOException e) {
            System.err.println("Failed to save Pokemon selection: " + e.getMessage());
        }
    }
    
    /**
     * Checks if this is a first-time user who hasn't selected a Pokemon yet.
     * 
     * @return true if no Pokemon has been selected, false otherwise
     */
    public boolean isFirstTimeUser() {
        return !hasSelectedStarter;
    }
    
    /**
     * Gets the selected starter Pokemon.
     * 
     * @return The selected Pokemon species, or null if none selected
     */
    public PokemonSpecies getSelectedStarter() {
        return selectedStarter;
    }
    
    /**
     * Checks if a starter has been selected.
     * 
     * @return true if a starter has been selected
     */
    public boolean hasSelectedStarter() {
        return hasSelectedStarter;
    }
    
    /**
     * Gets the timestamp when the selection was made.
     * 
     * @return The selection timestamp, or null if none
     */
    public LocalDateTime getSelectionTimestamp() {
        return selectionTimestamp;
    }
    
    /**
     * Gets the list of all 9 starter Pokemon options.
     * 
     * @return Array of starter Pokemon species
     */
    public static PokemonSpecies[] getStarterOptions() {
        return new PokemonSpecies[] {
            PokemonSpecies.CHARMANDER,  // Kanto Fire
            PokemonSpecies.CYNDAQUIL,   // Johto Fire
            PokemonSpecies.FUECOCO,     // Paldea Fire
            PokemonSpecies.PIPLUP,      // Sinnoh Water
            PokemonSpecies.MUDKIP,      // Hoenn Water
            PokemonSpecies.FROAKIE,     // Kalos Water
            PokemonSpecies.ROWLET,      // Alola Grass
            PokemonSpecies.GROOKEY,     // Galar Grass
            PokemonSpecies.SNIVY        // Unova Grass
        };
    }
    
    /**
     * Gets the Pokemon type for display purposes.
     * 
     * @param species The Pokemon species
     * @return The type string (Fire, Water, or Grass)
     */
    public static String getPokemonType(PokemonSpecies species) {
        switch (species) {
            case CHARMANDER:
            case CYNDAQUIL:
            case FUECOCO:
                return "Fire";
            case MUDKIP:
            case PIPLUP:
            case FROAKIE:
                return "Water";
            case SNIVY:
            case ROWLET:
            case GROOKEY:
                return "Grass";
            default:
                return "Unknown";
        }
    }
    
    /**
     * Gets a formatted display name for the Pokemon.
     * 
     * @param species The Pokemon species
     * @return Capitalized name (e.g., "Charmander")
     */
    public static String getDisplayName(PokemonSpecies species) {
        String name = species.name();
        return name.charAt(0) + name.substring(1).toLowerCase();
    }
}
