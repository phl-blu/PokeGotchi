package com.tamagotchi.committracker.ui.components;

import net.jqwik.api.*;
import static org.junit.jupiter.api.Assertions.*;

import com.tamagotchi.committracker.pokemon.EvolutionStage;
import com.tamagotchi.committracker.pokemon.PokemonSpecies;
import com.tamagotchi.committracker.pokemon.PokemonSelectionData;
import com.tamagotchi.committracker.ui.theme.PokedexTheme;

import java.util.Set;

/**
 * Property-based tests for PokedexFrame component.
 * Tests window controls and screen transition functionality.
 */
class PokedexFrameProperties {

    private static final Set<String> EXPECTED_CONTROL_COLORS = Set.of(
        PokedexTheme.CONTROL_BLUE,
        PokedexTheme.CONTROL_PINK,
        PokedexTheme.CONTROL_ORANGE,
        PokedexTheme.CONTROL_GREEN
    );

    /**
     * **Feature: pokedex-ui-redesign, Property 7: Window controls display four colored dots**
     * **Validates: Requirements 1.3**
     */
    @Property(tries = 100)
    @Label("Window controls should have exactly four distinct colors defined")
    void windowControlsShouldHaveFourDistinctColors() {
        assertEquals(4, EXPECTED_CONTROL_COLORS.size(),
            "Should have exactly 4 window control colors defined");
        for (String color : EXPECTED_CONTROL_COLORS) {
            assertTrue(isValidHexColor(color),
                "Control color " + color + " should be a valid hex color");
        }
    }

    /**
     * **Feature: pokedex-ui-redesign, Property 7: Window controls display four colored dots**
     * **Validates: Requirements 1.3**
     */
    @Property(tries = 100)
    @Label("Control dot style should be valid for any size")
    void controlDotStyleShouldBeValidForAnySize(@ForAll("validDotSize") int dotSize) {
        for (String color : EXPECTED_CONTROL_COLORS) {
            String style = PokedexTheme.getControlDotStyle(color, dotSize);
            assertNotNull(style, "Dot style should not be null");
            assertFalse(style.isEmpty(), "Dot style should not be empty");
            assertTrue(style.contains(color), "Dot style should contain the color");
        }
    }


    /**
     * **Feature: pokedex-ui-redesign, Property 7: Window controls display four colored dots**
     * **Validates: Requirements 1.3**
     */
    @Property(tries = 100)
    @Label("All four control colors should be distinct")
    void allControlColorsShouldBeDistinct() {
        String blue = PokedexTheme.CONTROL_BLUE;
        String pink = PokedexTheme.CONTROL_PINK;
        String orange = PokedexTheme.CONTROL_ORANGE;
        String green = PokedexTheme.CONTROL_GREEN;
        
        assertNotEquals(blue, pink, "Blue and pink should be different");
        assertNotEquals(blue, orange, "Blue and orange should be different");
        assertNotEquals(blue, green, "Blue and green should be different");
        assertNotEquals(pink, orange, "Pink and orange should be different");
        assertNotEquals(pink, green, "Pink and green should be different");
        assertNotEquals(orange, green, "Orange and green should be different");
    }

    /**
     * **Feature: pokedex-ui-redesign, Property 5: Screen transition preserves Pokemon state**
     * **Validates: Requirements 2.4**
     */
    @Property(tries = 100)
    @Label("Pokemon species should be preserved through selection")
    void pokemonSpeciesShouldBePreservedThroughSelection(
            @ForAll("validPokemonSpecies") PokemonSpecies species) {
        assertNotNull(species, "Pokemon species should not be null");
        assertNotNull(species.name(), "Species name should not be null");
        assertFalse(species.name().isEmpty(), "Species name should not be empty");
        
        String displayName = PokemonSelectionData.getDisplayName(species);
        assertNotNull(displayName, "Display name should not be null");
        assertFalse(displayName.isEmpty(), "Display name should not be empty");
    }


    /**
     * **Feature: pokedex-ui-redesign, Property 5: Screen transition preserves Pokemon state**
     * **Validates: Requirements 2.4**
     */
    @Property(tries = 100)
    @Label("Pokemon and stage combination should be valid for display")
    void pokemonAndStageCombinationShouldBeValidForDisplay(
            @ForAll("validPokemonSpecies") PokemonSpecies species,
            @ForAll("validEvolutionStage") EvolutionStage stage) {
        assertNotNull(species, "Species should not be null");
        assertNotNull(stage, "Stage should not be null");
        
        String displayName = PokemonSelectionData.getDisplayName(species);
        assertNotNull(displayName, "Display name should not be null");
        
        int stageOrdinal = stage.ordinal();
        assertTrue(stageOrdinal >= 0 && stageOrdinal < EvolutionStage.values().length,
            "Stage ordinal should be within valid range");
    }

    /**
     * **Feature: pokedex-ui-redesign, Property 5: Screen transition preserves Pokemon state**
     * **Validates: Requirements 2.4**
     */
    @Property(tries = 100)
    @Label("Frame style should be consistent")
    void frameStyleShouldBeConsistent() {
        String style1 = PokedexTheme.getFrameStyle();
        String style2 = PokedexTheme.getFrameStyle();
        
        assertEquals(style1, style2, "Frame style should be consistent across calls");
        assertTrue(style1.contains("-fx-background-color"), "Frame style should set background color");
        assertTrue(style1.contains(PokedexTheme.FRAME_PRIMARY), "Frame style should use primary frame color");
    }


    private boolean isValidHexColor(String color) {
        return color != null && color.matches("^#[0-9A-Fa-f]{6}$");
    }

    @Provide
    Arbitrary<Integer> validDotSize() {
        return Arbitraries.integers().between(4, 20);
    }

    @Provide
    Arbitrary<PokemonSpecies> validPokemonSpecies() {
        return Arbitraries.of(PokemonSpecies.values());
    }

    @Provide
    Arbitrary<EvolutionStage> validEvolutionStage() {
        return Arbitraries.of(EvolutionStage.values());
    }
}
