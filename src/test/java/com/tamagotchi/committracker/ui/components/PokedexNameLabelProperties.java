package com.tamagotchi.committracker.ui.components;

import net.jqwik.api.*;
import static org.junit.jupiter.api.Assertions.*;

import com.tamagotchi.committracker.pokemon.EvolutionStage;
import com.tamagotchi.committracker.pokemon.PokemonSpecies;

/**
 * Property-based tests for PokedexNameLabel component.
 * 
 * **Feature: pokedex-ui-redesign, Property 4: Name label displays correct Pokemon name**
 * **Validates: Requirements 5.2, 5.3**
 * 
 * These tests use static methods to test the name formatting logic
 * without requiring JavaFX initialization.
 */
class PokedexNameLabelProperties {

    /**
     * **Feature: pokedex-ui-redesign, Property 4: Name label displays correct Pokemon name**
     * **Validates: Requirements 5.2, 5.3**
     * 
     * For any Pokemon species and evolution stage, the name label SHALL display
     * the correct species name corresponding to the current evolution stage.
     */
    @Property(tries = 100)
    @Label("Name label should display correct Pokemon name for each evolution stage")
    void nameLabelShouldDisplayCorrectPokemonNameForStage(
            @ForAll("starterPokemon") PokemonSpecies species,
            @ForAll EvolutionStage stage) {
        
        // Act: Get the Pokemon name for the given species and stage
        String displayedName = PokedexNameLabel.getPokemonNameForStage(species, stage);
        
        // Assert: Name should not be null or empty
        assertNotNull(displayedName, "Displayed name should not be null");
        assertFalse(displayedName.isEmpty(), "Displayed name should not be empty");
        
        // Assert: Name should be in Title Case format (first letter uppercase, rest lowercase)
        if (!displayedName.equals("Egg")) {
            assertTrue(Character.isUpperCase(displayedName.charAt(0)),
                "Name should start with uppercase letter: " + displayedName);
            for (int i = 1; i < displayedName.length(); i++) {
                assertTrue(Character.isLowerCase(displayedName.charAt(i)),
                    "Name should have lowercase letters after first: " + displayedName);
            }
        }
    }

    /**
     * **Feature: pokedex-ui-redesign, Property 4: Name label displays correct Pokemon name**
     * **Validates: Requirements 5.3**
     * 
     * When the Pokemon evolves, the name label SHALL update to show the new evolved Pokemon name.
     * This tests that different stages produce different names for the same starter.
     */
    @Property(tries = 100)
    @Label("Name label should show different names for different evolution stages")
    void nameLabelShouldShowDifferentNamesForDifferentStages(
            @ForAll("starterPokemon") PokemonSpecies species) {
        
        // Get names for each stage
        String eggName = PokedexNameLabel.getPokemonNameForStage(species, EvolutionStage.EGG);
        String basicName = PokedexNameLabel.getPokemonNameForStage(species, EvolutionStage.BASIC);
        String stage1Name = PokedexNameLabel.getPokemonNameForStage(species, EvolutionStage.STAGE_1);
        String stage2Name = PokedexNameLabel.getPokemonNameForStage(species, EvolutionStage.STAGE_2);
        
        // Assert: EGG stage should show "Egg"
        assertEquals("Egg", eggName, "EGG stage should display 'Egg'");
        
        // Assert: Each evolved stage should have a different name
        assertNotEquals(basicName, stage1Name,
            "BASIC and STAGE_1 should have different names");
        assertNotEquals(stage1Name, stage2Name,
            "STAGE_1 and STAGE_2 should have different names");
        assertNotEquals(basicName, stage2Name,
            "BASIC and STAGE_2 should have different names");
    }

    /**
     * **Feature: pokedex-ui-redesign, Property 4: Name label displays correct Pokemon name**
     * **Validates: Requirements 5.2**
     * 
     * The name label SHALL display the Pokemon species name in pixel font.
     * This tests that the name formatting produces valid display names.
     */
    @Property(tries = 100)
    @Label("Pokemon name formatting should produce valid display names")
    void pokemonNameFormattingShouldProduceValidDisplayNames(
            @ForAll PokemonSpecies species) {
        
        // Act: Format the Pokemon name
        String formattedName = PokedexNameLabel.formatPokemonName(species);
        
        // Assert: Name should not be null or empty
        assertNotNull(formattedName, "Formatted name should not be null");
        assertFalse(formattedName.isEmpty(), "Formatted name should not be empty");
        
        // Assert: Name should be in Title Case
        assertTrue(Character.isUpperCase(formattedName.charAt(0)),
            "Name should start with uppercase letter: " + formattedName);
        
        // Assert: Name should match the species name (case-insensitive)
        assertEquals(species.name().toLowerCase(), formattedName.toLowerCase(),
            "Formatted name should match species name");
    }

    /**
     * **Feature: pokedex-ui-redesign, Property 4: Name label displays correct Pokemon name**
     * **Validates: Requirements 5.2, 5.3**
     * 
     * For BASIC stage, the name should match the base Pokemon of the evolution line.
     */
    @Property(tries = 100)
    @Label("BASIC stage should show base Pokemon name")
    void basicStageShouldShowBasePokemonName(
            @ForAll("starterPokemon") PokemonSpecies species) {
        
        // Act: Get the name for BASIC stage
        String basicName = PokedexNameLabel.getPokemonNameForStage(species, EvolutionStage.BASIC);
        
        // Assert: Name should be the formatted version of the starter species
        String expectedName = PokedexNameLabel.formatPokemonName(species);
        assertEquals(expectedName, basicName,
            "BASIC stage should show the base Pokemon name");
    }

    /**
     * Verifies that null inputs are handled gracefully.
     */
    @Property(tries = 10)
    @Label("Null inputs should be handled gracefully")
    void nullInputsShouldBeHandledGracefully() {
        // Test null species
        String nullSpeciesName = PokedexNameLabel.getPokemonNameForStage(null, EvolutionStage.BASIC);
        assertEquals("", nullSpeciesName, "Null species should return empty string");
        
        // Test null stage
        String nullStageName = PokedexNameLabel.getPokemonNameForStage(PokemonSpecies.CHARMANDER, null);
        assertEquals("", nullStageName, "Null stage should return empty string");
        
        // Test null for formatPokemonName
        String nullFormatted = PokedexNameLabel.formatPokemonName(null);
        assertEquals("", nullFormatted, "Null species in formatPokemonName should return empty string");
    }

    /**
     * Provides starter Pokemon (base forms only) for testing.
     * These are the 9 starter Pokemon that users can select.
     */
    @Provide
    Arbitrary<PokemonSpecies> starterPokemon() {
        return Arbitraries.of(
            PokemonSpecies.CHARMANDER,
            PokemonSpecies.CYNDAQUIL,
            PokemonSpecies.MUDKIP,
            PokemonSpecies.PIPLUP,
            PokemonSpecies.SNIVY,
            PokemonSpecies.FROAKIE,
            PokemonSpecies.ROWLET,
            PokemonSpecies.GROOKEY,
            PokemonSpecies.FUECOCO
        );
    }
}
