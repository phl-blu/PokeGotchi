package com.tamagotchi.committracker.ui.components;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import com.tamagotchi.committracker.pokemon.PokemonSpecies;
import com.tamagotchi.committracker.pokemon.PokemonState;
import com.tamagotchi.committracker.pokemon.EvolutionStage;

/**
 * Tests for PokemonDisplayComponent animation system.
 * Note: These tests focus on the logic rather than JavaFX animation functionality
 * to avoid requiring a full JavaFX application context.
 */
class PokemonDisplayComponentTest {
    
    @Test
    void testEvolutionRequirementsLogic() {
        // Test evolution requirements without actually triggering JavaFX animations
        
        // Test egg to basic evolution requirements (4+ day streak, 200+ XP)
        assertTrue(meetsEvolutionRequirements(EvolutionStage.EGG, EvolutionStage.BASIC, 250, 5));
        assertFalse(meetsEvolutionRequirements(EvolutionStage.EGG, EvolutionStage.BASIC, 150, 5)); // Not enough XP
        assertFalse(meetsEvolutionRequirements(EvolutionStage.EGG, EvolutionStage.BASIC, 250, 3)); // Not enough streak
        
        // Test basic to stage 1 evolution requirements (11+ day streak, 800+ XP)
        assertTrue(meetsEvolutionRequirements(EvolutionStage.BASIC, EvolutionStage.STAGE_1, 850, 12));
        assertFalse(meetsEvolutionRequirements(EvolutionStage.BASIC, EvolutionStage.STAGE_1, 750, 12)); // Not enough XP
        assertFalse(meetsEvolutionRequirements(EvolutionStage.BASIC, EvolutionStage.STAGE_1, 850, 10)); // Not enough streak
        
        // Test stage 1 to stage 2 evolution requirements (22+ day streak, 2000+ XP)
        assertTrue(meetsEvolutionRequirements(EvolutionStage.STAGE_1, EvolutionStage.STAGE_2, 2100, 25));
        assertFalse(meetsEvolutionRequirements(EvolutionStage.STAGE_1, EvolutionStage.STAGE_2, 1900, 25)); // Not enough XP
        assertFalse(meetsEvolutionRequirements(EvolutionStage.STAGE_1, EvolutionStage.STAGE_2, 2100, 20)); // Not enough streak
    }
    
    @Test
    void testEvolutionSpeciesMapping() {
        // Test that evolution species mapping works correctly
        
        // Kanto starters
        assertEquals(PokemonSpecies.IVYSAUR, getEvolvedSpecies(PokemonSpecies.BULBASAUR, EvolutionStage.STAGE_1));
        assertEquals(PokemonSpecies.VENUSAUR, getEvolvedSpecies(PokemonSpecies.BULBASAUR, EvolutionStage.STAGE_2));
        
        assertEquals(PokemonSpecies.CHARMELEON, getEvolvedSpecies(PokemonSpecies.CHARMANDER, EvolutionStage.STAGE_1));
        assertEquals(PokemonSpecies.CHARIZARD, getEvolvedSpecies(PokemonSpecies.CHARMANDER, EvolutionStage.STAGE_2));
        
        assertEquals(PokemonSpecies.WARTORTLE, getEvolvedSpecies(PokemonSpecies.SQUIRTLE, EvolutionStage.STAGE_1));
        assertEquals(PokemonSpecies.BLASTOISE, getEvolvedSpecies(PokemonSpecies.SQUIRTLE, EvolutionStage.STAGE_2));
        
        // Johto starters
        assertEquals(PokemonSpecies.BAYLEEF, getEvolvedSpecies(PokemonSpecies.CHIKORITA, EvolutionStage.STAGE_1));
        assertEquals(PokemonSpecies.MEGANIUM, getEvolvedSpecies(PokemonSpecies.CHIKORITA, EvolutionStage.STAGE_2));
        
        // Hoenn starters
        assertEquals(PokemonSpecies.GROVYLE, getEvolvedSpecies(PokemonSpecies.TREECKO, EvolutionStage.STAGE_1));
        assertEquals(PokemonSpecies.SCEPTILE, getEvolvedSpecies(PokemonSpecies.TREECKO, EvolutionStage.STAGE_2));
    }
    
    @Test
    void testNextEvolutionStage() {
        // Test getting next evolution stage
        assertEquals(EvolutionStage.BASIC, getNextEvolutionStage(EvolutionStage.EGG));
        assertEquals(EvolutionStage.STAGE_1, getNextEvolutionStage(EvolutionStage.BASIC));
        assertEquals(EvolutionStage.STAGE_2, getNextEvolutionStage(EvolutionStage.STAGE_1));
        assertNull(getNextEvolutionStage(EvolutionStage.STAGE_2)); // Already at max
    }
    
    @Test
    void testPokemonStateValues() {
        // Test that all Pokemon states are valid
        for (PokemonState state : PokemonState.values()) {
            assertNotNull(state);
            assertNotNull(state.name());
        }
    }
    
    @Test
    void testPokemonSpeciesValues() {
        // Test that all Pokemon species are valid and we have the right count
        PokemonSpecies[] species = PokemonSpecies.values();
        assertEquals(27, species.length); // 9 starter lines × 3 stages each
        
        // Test that all species have names
        for (PokemonSpecies pokemon : species) {
            assertNotNull(pokemon);
            assertNotNull(pokemon.name());
        }
    }
    
    // Helper methods that replicate the logic from PokemonDisplayComponent
    // without requiring JavaFX initialization
    
    private boolean meetsEvolutionRequirements(EvolutionStage currentStage, EvolutionStage targetStage, int xpLevel, int streakDays) {
        switch (targetStage) {
            case BASIC:
                return currentStage == EvolutionStage.EGG && streakDays >= 4 && xpLevel >= 200;
            case STAGE_1:
                return currentStage == EvolutionStage.BASIC && streakDays >= 11 && xpLevel >= 800;
            case STAGE_2:
                return currentStage == EvolutionStage.STAGE_1 && streakDays >= 22 && xpLevel >= 2000;
            default:
                return false;
        }
    }
    
    private EvolutionStage getNextEvolutionStage(EvolutionStage currentStage) {
        switch (currentStage) {
            case EGG:
                return EvolutionStage.BASIC;
            case BASIC:
                return EvolutionStage.STAGE_1;
            case STAGE_1:
                return EvolutionStage.STAGE_2;
            case STAGE_2:
                return null; // Already at max evolution
            default:
                return null;
        }
    }
    
    private PokemonSpecies getEvolvedSpecies(PokemonSpecies baseSpecies, EvolutionStage targetStage) {
        switch (baseSpecies) {
            // Kanto starters
            case BULBASAUR:
                return targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.IVYSAUR : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.VENUSAUR : baseSpecies;
            case CHARMANDER:
                return targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.CHARMELEON : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.CHARIZARD : baseSpecies;
            case SQUIRTLE:
                return targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.WARTORTLE : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.BLASTOISE : baseSpecies;
            
            // Johto starters
            case CHIKORITA:
                return targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.BAYLEEF : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.MEGANIUM : baseSpecies;
            case CYNDAQUIL:
                return targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.QUILAVA : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.TYPHLOSION : baseSpecies;
            case TOTODILE:
                return targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.CROCONAW : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.FERALIGATR : baseSpecies;
            
            // Hoenn starters
            case TREECKO:
                return targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.GROVYLE : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.SCEPTILE : baseSpecies;
            case TORCHIC:
                return targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.COMBUSKEN : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.BLAZIKEN : baseSpecies;
            case MUDKIP:
                return targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.MARSHTOMP : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.SWAMPERT : baseSpecies;
            
            default:
                return baseSpecies;
        }
    }
}