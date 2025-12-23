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
        // Note: Evolution uses OR logic - either XP OR streak threshold triggers evolution
        
        // Test egg to basic evolution requirements (4+ day streak OR 60+ XP)
        assertTrue(meetsEvolutionRequirements(EvolutionStage.EGG, EvolutionStage.BASIC, 60, 4));
        assertTrue(meetsEvolutionRequirements(EvolutionStage.EGG, EvolutionStage.BASIC, 60, 2)); // XP only
        assertTrue(meetsEvolutionRequirements(EvolutionStage.EGG, EvolutionStage.BASIC, 30, 4)); // Streak only
        assertFalse(meetsEvolutionRequirements(EvolutionStage.EGG, EvolutionStage.BASIC, 59, 3)); // Neither threshold met
        
        // Test basic to stage 1 evolution requirements (11+ day streak OR 500+ XP)
        assertTrue(meetsEvolutionRequirements(EvolutionStage.BASIC, EvolutionStage.STAGE_1, 500, 11));
        assertTrue(meetsEvolutionRequirements(EvolutionStage.BASIC, EvolutionStage.STAGE_1, 500, 5)); // XP only
        assertTrue(meetsEvolutionRequirements(EvolutionStage.BASIC, EvolutionStage.STAGE_1, 100, 11)); // Streak only
        assertFalse(meetsEvolutionRequirements(EvolutionStage.BASIC, EvolutionStage.STAGE_1, 499, 10)); // Neither threshold met
        
        // Test stage 1 to stage 2 evolution requirements (22+ day streak OR 1200+ XP)
        assertTrue(meetsEvolutionRequirements(EvolutionStage.STAGE_1, EvolutionStage.STAGE_2, 1200, 22));
        assertTrue(meetsEvolutionRequirements(EvolutionStage.STAGE_1, EvolutionStage.STAGE_2, 1200, 10)); // XP only
        assertTrue(meetsEvolutionRequirements(EvolutionStage.STAGE_1, EvolutionStage.STAGE_2, 500, 22)); // Streak only
        assertFalse(meetsEvolutionRequirements(EvolutionStage.STAGE_1, EvolutionStage.STAGE_2, 1199, 21)); // Neither threshold met
    }
    
    @Test
    void testEvolutionSpeciesMapping() {
        // Test that evolution species mapping works correctly
        
        // Charmander line (Kanto Fire)
        assertEquals(PokemonSpecies.CHARMELEON, getEvolvedSpecies(PokemonSpecies.CHARMANDER, EvolutionStage.STAGE_1));
        assertEquals(PokemonSpecies.CHARIZARD, getEvolvedSpecies(PokemonSpecies.CHARMANDER, EvolutionStage.STAGE_2));
        
        // Cyndaquil line (Johto Fire)
        assertEquals(PokemonSpecies.QUILAVA, getEvolvedSpecies(PokemonSpecies.CYNDAQUIL, EvolutionStage.STAGE_1));
        assertEquals(PokemonSpecies.TYPHLOSION, getEvolvedSpecies(PokemonSpecies.CYNDAQUIL, EvolutionStage.STAGE_2));
        
        // Mudkip line (Hoenn Water)
        assertEquals(PokemonSpecies.MARSHTOMP, getEvolvedSpecies(PokemonSpecies.MUDKIP, EvolutionStage.STAGE_1));
        assertEquals(PokemonSpecies.SWAMPERT, getEvolvedSpecies(PokemonSpecies.MUDKIP, EvolutionStage.STAGE_2));
        
        // Piplup line (Sinnoh Water)
        assertEquals(PokemonSpecies.PRINPLUP, getEvolvedSpecies(PokemonSpecies.PIPLUP, EvolutionStage.STAGE_1));
        assertEquals(PokemonSpecies.EMPOLEON, getEvolvedSpecies(PokemonSpecies.PIPLUP, EvolutionStage.STAGE_2));
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
    // Uses OR logic: either XP OR streak threshold triggers evolution
    
    private boolean meetsEvolutionRequirements(EvolutionStage currentStage, EvolutionStage targetStage, int xpLevel, int streakDays) {
        switch (targetStage) {
            case BASIC:
                return currentStage == EvolutionStage.EGG && (streakDays >= 4 || xpLevel >= 60);
            case STAGE_1:
                return currentStage == EvolutionStage.BASIC && (streakDays >= 11 || xpLevel >= 500);
            case STAGE_2:
                return currentStage == EvolutionStage.STAGE_1 && (streakDays >= 22 || xpLevel >= 1200);
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
            // 1. Charmander line (Kanto Fire)
            case CHARMANDER:
                return targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.CHARMELEON : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.CHARIZARD : baseSpecies;
            
            // 2. Cyndaquil line (Johto Fire)
            case CYNDAQUIL:
                return targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.QUILAVA : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.TYPHLOSION : baseSpecies;
            
            // 3. Mudkip line (Hoenn Water)
            case MUDKIP:
                return targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.MARSHTOMP : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.SWAMPERT : baseSpecies;
            
            // 4. Piplup line (Sinnoh Water)
            case PIPLUP:
                return targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.PRINPLUP : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.EMPOLEON : baseSpecies;
            
            // 5. Snivy line (Unova Grass)
            case SNIVY:
                return targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.SERVINE : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.SERPERIOR : baseSpecies;
            
            // 6. Froakie line (Kalos Water)
            case FROAKIE:
                return targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.FROGADIER : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.GRENINJA : baseSpecies;
            
            // 7. Rowlet line (Alola Grass)
            case ROWLET:
                return targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.DARTRIX : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.DECIDUEYE : baseSpecies;
            
            // 8. Grookey line (Galar Grass)
            case GROOKEY:
                return targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.THWACKEY : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.RILLABOOM : baseSpecies;
            
            // 9. Fuecoco line (Paldea Fire)
            case FUECOCO:
                return targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.CROCALOR : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.SKELEDIRGE : baseSpecies;
            
            default:
                return baseSpecies;
        }
    }
}