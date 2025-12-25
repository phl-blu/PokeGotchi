package com.tamagotchi.committracker.pokemon;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PokemonSelectionData class.
 * Tests the persistence and retrieval of Pokemon selection.
 */
class PokemonSelectionDataTest {
    
    @Test
    void testGetStarterOptions_ReturnsNineStarters() {
        PokemonSpecies[] starters = PokemonSelectionData.getStarterOptions();
        
        assertEquals(9, starters.length, "Should have exactly 9 starter options");
        
        // Verify all expected starters are present
        assertArrayEquals(new PokemonSpecies[] {
            PokemonSpecies.CHARMANDER,
            PokemonSpecies.CYNDAQUIL,
            PokemonSpecies.FUECOCO,
            PokemonSpecies.PIPLUP,
            PokemonSpecies.MUDKIP,
            PokemonSpecies.FROAKIE,
            PokemonSpecies.ROWLET,
            PokemonSpecies.GROOKEY,
            PokemonSpecies.SNIVY
        }, starters);
    }
    
    @Test
    void testGetPokemonType_FireTypes() {
        assertEquals("Fire", PokemonSelectionData.getPokemonType(PokemonSpecies.CHARMANDER));
        assertEquals("Fire", PokemonSelectionData.getPokemonType(PokemonSpecies.CYNDAQUIL));
        assertEquals("Fire", PokemonSelectionData.getPokemonType(PokemonSpecies.FUECOCO));
    }
    
    @Test
    void testGetPokemonType_WaterTypes() {
        assertEquals("Water", PokemonSelectionData.getPokemonType(PokemonSpecies.MUDKIP));
        assertEquals("Water", PokemonSelectionData.getPokemonType(PokemonSpecies.PIPLUP));
        assertEquals("Water", PokemonSelectionData.getPokemonType(PokemonSpecies.FROAKIE));
    }
    
    @Test
    void testGetPokemonType_GrassTypes() {
        assertEquals("Grass", PokemonSelectionData.getPokemonType(PokemonSpecies.SNIVY));
        assertEquals("Grass", PokemonSelectionData.getPokemonType(PokemonSpecies.ROWLET));
        assertEquals("Grass", PokemonSelectionData.getPokemonType(PokemonSpecies.GROOKEY));
    }
    
    @Test
    void testGetDisplayName_FormatsCorrectly() {
        assertEquals("Charmander", PokemonSelectionData.getDisplayName(PokemonSpecies.CHARMANDER));
        assertEquals("Cyndaquil", PokemonSelectionData.getDisplayName(PokemonSpecies.CYNDAQUIL));
        assertEquals("Mudkip", PokemonSelectionData.getDisplayName(PokemonSpecies.MUDKIP));
        assertEquals("Piplup", PokemonSelectionData.getDisplayName(PokemonSpecies.PIPLUP));
        assertEquals("Snivy", PokemonSelectionData.getDisplayName(PokemonSpecies.SNIVY));
        assertEquals("Froakie", PokemonSelectionData.getDisplayName(PokemonSpecies.FROAKIE));
        assertEquals("Rowlet", PokemonSelectionData.getDisplayName(PokemonSpecies.ROWLET));
        assertEquals("Grookey", PokemonSelectionData.getDisplayName(PokemonSpecies.GROOKEY));
        assertEquals("Fuecoco", PokemonSelectionData.getDisplayName(PokemonSpecies.FUECOCO));
    }
    
    @Test
    void testAllStartersHaveValidTypes() {
        PokemonSpecies[] starters = PokemonSelectionData.getStarterOptions();
        
        for (PokemonSpecies starter : starters) {
            String type = PokemonSelectionData.getPokemonType(starter);
            assertTrue(
                type.equals("Fire") || type.equals("Water") || type.equals("Grass"),
                "Starter " + starter + " should have a valid type (Fire, Water, or Grass)"
            );
        }
    }
    
    @Test
    void testStarterDistribution_ThreeOfEachType() {
        PokemonSpecies[] starters = PokemonSelectionData.getStarterOptions();
        
        int fireCount = 0;
        int waterCount = 0;
        int grassCount = 0;
        
        for (PokemonSpecies starter : starters) {
            String type = PokemonSelectionData.getPokemonType(starter);
            switch (type) {
                case "Fire": fireCount++; break;
                case "Water": waterCount++; break;
                case "Grass": grassCount++; break;
            }
        }
        
        assertEquals(3, fireCount, "Should have 3 Fire type starters");
        assertEquals(3, waterCount, "Should have 3 Water type starters");
        assertEquals(3, grassCount, "Should have 3 Grass type starters");
    }
}
