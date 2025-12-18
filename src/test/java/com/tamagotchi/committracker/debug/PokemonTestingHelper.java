package com.tamagotchi.committracker.debug;

import com.tamagotchi.committracker.ui.components.PokemonDisplayComponent;
import com.tamagotchi.committracker.pokemon.XPSystem;
import com.tamagotchi.committracker.pokemon.PokemonSpecies;
import com.tamagotchi.committracker.pokemon.EvolutionStage;
import com.tamagotchi.committracker.pokemon.PokemonState;

/**
 * FOR TESTING ONLY: Helper class for testing Pokemon evolution and egg animations.
 * Provides convenient methods to reset Pokemon state and test different scenarios.
 * TODO: REMOVE THIS CLASS BEFORE PRODUCTION - See TODO.md
 */
public class PokemonTestingHelper {
    
    /**
     * Creates a Pokemon display component in egg stage for testing egg animations.
     */
    public static PokemonDisplayComponent createTestEgg() {
        System.out.println("🧪 TESTING: Creating test egg");
        return new PokemonDisplayComponent(PokemonSpecies.CHARMANDER, EvolutionStage.EGG, PokemonState.CONTENT);
    }
    
    /**
     * Creates a Pokemon display component in basic stage for testing Pokemon animations.
     */
    public static PokemonDisplayComponent createTestBasicPokemon() {
        System.out.println("🧪 TESTING: Creating test basic Pokemon");
        return new PokemonDisplayComponent(PokemonSpecies.CHARMANDER, EvolutionStage.BASIC, PokemonState.CONTENT);
    }
    
    /**
     * Tests egg animation by triggering a commit animation.
     */
    public static void testEggAnimation(PokemonDisplayComponent pokemon, int xp) {
        if (pokemon.getCurrentStage() != EvolutionStage.EGG) {
            System.out.println("🧪 TESTING: Pokemon is not an egg, resetting to egg first");
            pokemon.forceDeevolutionToEggForTesting();
        }
        
        System.out.println("🧪 TESTING: Triggering egg animation with " + xp + " XP");
        pokemon.triggerCommitAnimation(xp);
    }
    
    /**
     * Tests evolution progression by forcing evolution to next stage.
     */
    public static void testEvolution(PokemonDisplayComponent pokemon) {
        System.out.println("🧪 TESTING: Testing evolution progression");
        pokemon.forceEvolutionForTesting();
    }
    
    /**
     * Tests complete reset cycle: egg -> basic -> egg.
     */
    public static void testResetCycle(PokemonDisplayComponent pokemon) {
        System.out.println("🧪 TESTING: Starting reset cycle test");
        
        // Show current state
        System.out.println("🧪 Current stage: " + pokemon.getCurrentStage());
        
        // Force evolution if egg
        if (pokemon.getCurrentStage() == EvolutionStage.EGG) {
            System.out.println("🧪 Evolving from egg to basic");
            pokemon.forceEvolutionForTesting();
            
            // Wait a moment for evolution to complete
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Show evolved state
        System.out.println("🧪 After evolution: " + pokemon.getCurrentStage());
        
        // Reset back to egg
        System.out.println("🧪 Resetting back to egg");
        pokemon.forceDeevolutionToEggForTesting();
        
        // Show final state
        System.out.println("🧪 After reset: " + pokemon.getCurrentStage());
        System.out.println("🧪 Reset cycle test completed");
    }
    
    /**
     * Creates an XP system for testing with specific XP amount.
     */
    public static XPSystem createTestXPSystem(int initialXP) {
        System.out.println("🧪 TESTING: Creating XP system with " + initialXP + " XP");
        return new XPSystem(initialXP);
    }
    
    /**
     * Tests XP system reset functionality.
     */
    public static void testXPReset(XPSystem xpSystem) {
        System.out.println("🧪 TESTING: XP system before reset: " + xpSystem);
        xpSystem.resetForTesting();
        System.out.println("🧪 TESTING: XP system after reset: " + xpSystem);
    }
    
    /**
     * Prints current Pokemon state for debugging.
     */
    public static void printPokemonState(PokemonDisplayComponent pokemon) {
        System.out.println("🧪 POKEMON STATE:");
        System.out.println("  Species: " + pokemon.getCurrentSpecies());
        System.out.println("  Stage: " + pokemon.getCurrentStage());
        System.out.println("  State: " + pokemon.getCurrentState());
        System.out.println("  Evolution in progress: " + pokemon.isEvolutionInProgress());
    }
}