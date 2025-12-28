package com.tamagotchi.committracker.ui.components;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import static org.junit.jupiter.api.Assertions.*;

import com.tamagotchi.committracker.pokemon.EvolutionStage;
import com.tamagotchi.committracker.pokemon.PokemonSpecies;

/**
 * Property-based tests for PokedexStatsCorner component.
 * 
 * **Feature: pokedex-ui-redesign, Property 3: Stats corner excludes Pokemon name**
 * **Validates: Requirements 4.5**
 * 
 * These tests use static methods to test the text formatting logic
 * without requiring JavaFX initialization.
 */
class PokedexStatsCornerProperties {

    /**
     * **Feature: pokedex-ui-redesign, Property 3: Stats corner excludes Pokemon name**
     * **Validates: Requirements 4.5**
     * 
     * For any stats corner render, the displayed content SHALL include XP, streak,
     * and evolution stage, and SHALL NOT include the Pokemon name.
     */
    @Property(tries = 100)
    @Label("Stats corner should exclude all Pokemon names")
    void statsCornerShouldExcludeAllPokemonNames(
            @ForAll("validXP") int currentXP,
            @ForAll("validXP") int nextThreshold,
            @ForAll("validStreak") int streak,
            @ForAll EvolutionStage stage) {
        
        // Act: Get all displayed text using static method (no JavaFX needed)
        String displayedText = PokedexStatsCorner.getAllDisplayedTextStatic(
            currentXP, nextThreshold, streak, stage).toUpperCase();
        
        // Assert: No Pokemon name should appear in the displayed text
        for (PokemonSpecies species : PokemonSpecies.values()) {
            String pokemonName = species.name();
            assertFalse(displayedText.contains(pokemonName),
                "Stats corner should NOT contain Pokemon name: " + pokemonName);
        }
    }

    /**
     * **Feature: pokedex-ui-redesign, Property 3: Stats corner excludes Pokemon name**
     * **Validates: Requirements 4.2, 4.3, 4.4**
     * 
     * For any stats corner render, the displayed content SHALL include XP, streak,
     * and evolution stage information.
     */
    @Property(tries = 100)
    @Label("Stats corner should include XP, streak, and stage information")
    void statsCornerShouldIncludeRequiredStats(
            @ForAll("validXP") int currentXP,
            @ForAll("validXP") int nextThreshold,
            @ForAll("validStreak") int streak,
            @ForAll EvolutionStage stage) {
        
        // Act: Get all displayed text using static method (no JavaFX needed)
        String displayedText = PokedexStatsCorner.getAllDisplayedTextStatic(
            currentXP, nextThreshold, streak, stage);
        
        // Assert: XP information should be present
        assertTrue(displayedText.contains("XP:"),
            "Stats corner should display XP label");
        assertTrue(displayedText.contains(String.valueOf(currentXP)),
            "Stats corner should display current XP value");
        assertTrue(displayedText.contains(String.valueOf(nextThreshold)),
            "Stats corner should display XP threshold value");
        
        // Assert: Streak information should be present
        assertTrue(displayedText.contains("Streak:"),
            "Stats corner should display Streak label");
        assertTrue(displayedText.contains(String.valueOf(streak)),
            "Stats corner should display streak value");
        
        // Assert: Stage information should be present
        assertTrue(displayedText.contains("Stage:"),
            "Stats corner should display Stage label");
    }

    /**
     * **Feature: pokedex-ui-redesign, Property 3: Stats corner excludes Pokemon name**
     * **Validates: Requirements 4.5**
     * 
     * Stats corner should have exactly 3 labels (XP, streak, stage) - no name label.
     * This tests the constant that defines the label count.
     */
    @Property(tries = 100)
    @Label("Stats corner should have exactly 3 labels")
    void statsCornerShouldHaveExactlyThreeLabels() {
        // Assert: The constant should be exactly 3
        assertEquals(3, PokedexStatsCorner.LABEL_COUNT,
            "Stats corner should have exactly 3 labels (XP, streak, stage)");
    }

    /**
     * Verifies that stage name formatting works correctly for all stages.
     */
    @Property(tries = 100)
    @Label("Stage name formatting should produce valid output for all stages")
    void stageNameFormattingShouldProduceValidOutput(@ForAll EvolutionStage stage) {
        // Act
        String stageName = PokedexStatsCorner.formatStageName(stage);
        
        // Assert: Stage name should not be null or empty
        assertNotNull(stageName, "Stage name should not be null");
        assertFalse(stageName.isEmpty(), "Stage name should not be empty");
        
        // Assert: Stage name should not contain any Pokemon names
        for (PokemonSpecies species : PokemonSpecies.values()) {
            assertFalse(stageName.toUpperCase().contains(species.name()),
                "Stage name should not contain Pokemon name: " + species.name());
        }
    }

    /**
     * Verifies that XP text formatting produces correct format.
     */
    @Property(tries = 100)
    @Label("XP text formatting should produce correct format")
    void xpTextFormattingShouldProduceCorrectFormat(
            @ForAll("validXP") int currentXP,
            @ForAll("validXP") int nextThreshold) {
        
        // Act
        String xpText = PokedexStatsCorner.formatXPText(currentXP, nextThreshold);
        
        // Assert: Should contain XP label and both values
        assertTrue(xpText.startsWith("XP:"), "XP text should start with 'XP:'");
        assertTrue(xpText.contains(String.valueOf(currentXP)), 
            "XP text should contain current XP value");
        assertTrue(xpText.contains(String.valueOf(nextThreshold)), 
            "XP text should contain threshold value");
        assertTrue(xpText.contains("/"), "XP text should contain separator '/'");
    }

    /**
     * Verifies that streak text formatting handles singular/plural correctly.
     */
    @Property(tries = 100)
    @Label("Streak text formatting should handle singular/plural correctly")
    void streakTextFormattingShouldHandleSingularPlural(
            @ForAll("validStreak") int streak) {
        
        // Act
        String streakText = PokedexStatsCorner.formatStreakText(streak);
        
        // Assert: Should contain Streak label and value
        assertTrue(streakText.startsWith("Streak:"), "Streak text should start with 'Streak:'");
        assertTrue(streakText.contains(String.valueOf(streak)), 
            "Streak text should contain streak value");
        
        // Assert: Singular/plural handling
        if (streak == 1) {
            assertTrue(streakText.contains("day") && !streakText.contains("days"),
                "Streak of 1 should use singular 'day'");
        } else {
            assertTrue(streakText.contains("days"),
                "Streak != 1 should use plural 'days'");
        }
    }

    /**
     * Provides valid XP values for testing.
     */
    @Provide
    Arbitrary<Integer> validXP() {
        return Arbitraries.integers().between(0, 10000);
    }

    /**
     * Provides valid streak values for testing.
     */
    @Provide
    Arbitrary<Integer> validStreak() {
        return Arbitraries.integers().between(0, 365);
    }
}
