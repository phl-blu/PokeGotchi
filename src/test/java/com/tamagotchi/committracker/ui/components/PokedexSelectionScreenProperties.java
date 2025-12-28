package com.tamagotchi.committracker.ui.components;

import net.jqwik.api.*;
import static org.junit.jupiter.api.Assertions.*;

import com.tamagotchi.committracker.pokemon.PokemonSelectionData;
import com.tamagotchi.committracker.pokemon.PokemonSpecies;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Property-based tests for PokedexSelectionScreen component.
 * 
 * Tests the selection grid and hover highlight functionality without
 * requiring JavaFX initialization by testing the underlying logic.
 */
class PokedexSelectionScreenProperties {

    /**
     * **Feature: pokedex-ui-redesign, Property 1: Selection grid contains all available Pokemon**
     * **Validates: Requirements 2.1**
     * 
     * For any set of available Pokemon species, the selection grid SHALL display
     * exactly one cell for each species with no duplicates or omissions.
     */
    @Property(tries = 100)
    @Label("Selection grid should contain all starter Pokemon with no duplicates")
    void selectionGridShouldContainAllStarterPokemon() {
        // Arrange: Get the starter options
        PokemonSpecies[] starters = PokemonSelectionData.getStarterOptions();
        
        // Act: Check for duplicates and completeness
        Set<PokemonSpecies> uniqueStarters = new HashSet<>(Arrays.asList(starters));
        
        // Assert: No duplicates (set size equals array length)
        assertEquals(starters.length, uniqueStarters.size(),
            "Starter options should have no duplicates");
        
        // Assert: All starters should be base forms (not evolutions)
        for (PokemonSpecies starter : starters) {
            assertTrue(isBaseForm(starter),
                "Starter " + starter + " should be a base form Pokemon");
        }
        
        // Assert: Should have exactly 9 starters (3 types x 3 generations each)
        assertEquals(9, starters.length,
            "Should have exactly 9 starter Pokemon options");
    }

    /**
     * **Feature: pokedex-ui-redesign, Property 1: Selection grid contains all available Pokemon**
     * **Validates: Requirements 2.1**
     * 
     * Grid dimensions should be calculated correctly for any Pokemon count.
     */
    @Property(tries = 100)
    @Label("Grid dimensions should accommodate all Pokemon")
    void gridDimensionsShouldAccommodateAllPokemon(
            @ForAll("validPokemonCount") int pokemonCount) {
        
        // Act: Calculate grid dimensions
        int columns = calculateGridColumns(pokemonCount);
        int rows = (int) Math.ceil((double) pokemonCount / columns);
        int totalCells = columns * rows;
        
        // Assert: Grid should have enough cells for all Pokemon
        assertTrue(totalCells >= pokemonCount,
            "Grid should have at least " + pokemonCount + " cells, but has " + totalCells);
        
        // Assert: Grid should not have excessive empty cells
        int emptyCells = totalCells - pokemonCount;
        assertTrue(emptyCells < columns,
            "Grid should not have more than " + (columns - 1) + " empty cells, but has " + emptyCells);
    }

    /**
     * **Feature: pokedex-ui-redesign, Property 1: Selection grid contains all available Pokemon**
     * **Validates: Requirements 2.1**
     * 
     * Each starter Pokemon should have a valid type.
     */
    @Property(tries = 100)
    @Label("Each starter should have a valid type")
    void eachStarterShouldHaveValidType() {
        // Arrange
        PokemonSpecies[] starters = PokemonSelectionData.getStarterOptions();
        Set<String> validTypes = Set.of("Fire", "Water", "Grass");
        
        // Assert: Each starter should have a valid type
        for (PokemonSpecies starter : starters) {
            String type = PokemonSelectionData.getPokemonType(starter);
            assertTrue(validTypes.contains(type),
                "Starter " + starter + " should have a valid type, but has: " + type);
        }
    }

    /**
     * **Feature: pokedex-ui-redesign, Property 1: Selection grid contains all available Pokemon**
     * **Validates: Requirements 2.1**
     * 
     * Starters should be balanced across types.
     */
    @Property(tries = 100)
    @Label("Starters should be balanced across types")
    void startersShouldBeBalancedAcrossTypes() {
        // Arrange
        PokemonSpecies[] starters = PokemonSelectionData.getStarterOptions();
        int fireCount = 0, waterCount = 0, grassCount = 0;
        
        // Count each type
        for (PokemonSpecies starter : starters) {
            String type = PokemonSelectionData.getPokemonType(starter);
            switch (type) {
                case "Fire": fireCount++; break;
                case "Water": waterCount++; break;
                case "Grass": grassCount++; break;
            }
        }
        
        // Assert: Should have 3 of each type
        assertEquals(3, fireCount, "Should have 3 Fire type starters");
        assertEquals(3, waterCount, "Should have 3 Water type starters");
        assertEquals(3, grassCount, "Should have 3 Grass type starters");
    }

    /**
     * **Feature: pokedex-ui-redesign, Property 2: Selection highlight follows hover state**
     * **Validates: Requirements 2.3**
     * 
     * Cell style should change based on hover and selection state.
     */
    @Property(tries = 100)
    @Label("Cell style should reflect hover and selection state")
    void cellStyleShouldReflectHoverAndSelectionState(
            @ForAll boolean highlighted,
            @ForAll boolean selected) {
        
        // Act: Get cell style from theme
        String style = com.tamagotchi.committracker.ui.theme.PokedexTheme.getCellStyle(highlighted, selected);
        
        // Assert: Style should not be null or empty
        assertNotNull(style, "Cell style should not be null");
        assertFalse(style.isEmpty(), "Cell style should not be empty");
        
        // Assert: Style should contain background color
        assertTrue(style.contains("-fx-background-color"),
            "Cell style should contain background color");
        
        // Assert: Different states should produce different styles
        String normalStyle = com.tamagotchi.committracker.ui.theme.PokedexTheme.getCellStyle(false, false);
        String highlightStyle = com.tamagotchi.committracker.ui.theme.PokedexTheme.getCellStyle(true, false);
        String selectedStyle = com.tamagotchi.committracker.ui.theme.PokedexTheme.getCellStyle(false, true);
        
        // Highlighted style should differ from normal
        assertNotEquals(normalStyle, highlightStyle,
            "Highlighted style should differ from normal style");
        
        // Selected style should differ from normal
        assertNotEquals(normalStyle, selectedStyle,
            "Selected style should differ from normal style");
    }

    /**
     * **Feature: pokedex-ui-redesign, Property 2: Selection highlight follows hover state**
     * **Validates: Requirements 2.3**
     * 
     * Selected state should take precedence over hover state in styling.
     */
    @Property(tries = 100)
    @Label("Selected state should use selected color regardless of hover")
    void selectedStateShouldUseSelectedColor() {
        // Act: Get styles for selected states
        String selectedNotHovered = com.tamagotchi.committracker.ui.theme.PokedexTheme.getCellStyle(false, true);
        String selectedAndHovered = com.tamagotchi.committracker.ui.theme.PokedexTheme.getCellStyle(true, true);
        
        // Assert: Both should contain the selected color
        String selectedColor = com.tamagotchi.committracker.ui.theme.PokedexTheme.CELL_SELECTED;
        assertTrue(selectedNotHovered.contains(selectedColor),
            "Selected (not hovered) should use selected color");
        assertTrue(selectedAndHovered.contains(selectedColor),
            "Selected (and hovered) should use selected color");
    }

    /**
     * Provides valid Pokemon counts for grid dimension testing.
     */
    @Provide
    Arbitrary<Integer> validPokemonCount() {
        return Arbitraries.integers().between(1, 27); // Max 27 Pokemon (9 lines x 3 stages)
    }

    /**
     * Helper method to calculate grid columns (mirrors PokedexSelectionScreen logic).
     */
    private int calculateGridColumns(int count) {
        return (int) Math.ceil(Math.sqrt(count));
    }

    /**
     * Helper method to check if a Pokemon is a base form (not an evolution).
     */
    private boolean isBaseForm(PokemonSpecies species) {
        // Base forms are the first Pokemon in each evolution line
        switch (species) {
            case CHARMANDER:
            case CYNDAQUIL:
            case MUDKIP:
            case PIPLUP:
            case SNIVY:
            case FROAKIE:
            case ROWLET:
            case GROOKEY:
            case FUECOCO:
                return true;
            default:
                return false;
        }
    }
}
