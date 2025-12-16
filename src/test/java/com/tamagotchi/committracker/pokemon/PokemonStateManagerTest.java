package com.tamagotchi.committracker.pokemon;

import com.tamagotchi.committracker.domain.CommitHistory;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PokemonStateManager.
 */
class PokemonStateManagerTest {

    @Test
    void testCalculateStateWithNullHistory() {
        PokemonStateManager manager = new PokemonStateManager();
        assertEquals(PokemonState.CONTENT, manager.calculateState(null));
    }
    
    @Test
    void testCalculateStateWithNoCommits() {
        PokemonStateManager manager = new PokemonStateManager();
        CommitHistory history = new CommitHistory();
        
        assertEquals(PokemonState.NEGLECTED, manager.calculateState(history));
    }
    
    @Test
    void testCalculateStateWithRecentCommit() {
        PokemonStateManager manager = new PokemonStateManager();
        CommitHistory history = new CommitHistory();
        history.setLastCommitTime(LocalDateTime.now().minusHours(1));
        history.setCurrentStreak(5);
        history.setAverageCommitsPerDay(1.5);
        
        assertEquals(PokemonState.HAPPY, manager.calculateState(history));
    }
    
    @Test
    void testCalculateStateWithOldCommit() {
        PokemonStateManager manager = new PokemonStateManager();
        CommitHistory history = new CommitHistory();
        history.setLastCommitTime(LocalDateTime.now().minusDays(10));
        
        assertEquals(PokemonState.NEGLECTED, manager.calculateState(history));
    }
    
    @Test
    void testCheckEvolutionCriteriaEggToBasic() {
        PokemonStateManager manager = new PokemonStateManager();
        
        // Should evolve: 4+ day streak and 200+ XP
        assertTrue(manager.checkEvolutionCriteria(250, 5, EvolutionStage.EGG));
        
        // Should not evolve: insufficient streak
        assertFalse(manager.checkEvolutionCriteria(250, 3, EvolutionStage.EGG));
        
        // Should not evolve: insufficient XP
        assertFalse(manager.checkEvolutionCriteria(150, 5, EvolutionStage.EGG));
    }
    
    @Test
    void testCheckEvolutionCriteriaBasicToStage1() {
        PokemonStateManager manager = new PokemonStateManager();
        
        // Should evolve: 11+ day streak and 800+ XP
        assertTrue(manager.checkEvolutionCriteria(900, 12, EvolutionStage.BASIC));
        
        // Should not evolve: insufficient streak
        assertFalse(manager.checkEvolutionCriteria(900, 10, EvolutionStage.BASIC));
        
        // Should not evolve: insufficient XP
        assertFalse(manager.checkEvolutionCriteria(700, 12, EvolutionStage.BASIC));
    }
    
    @Test
    void testCheckEvolutionCriteriaStage1ToStage2() {
        PokemonStateManager manager = new PokemonStateManager();
        
        // Should evolve: 22+ day streak and 2000+ XP
        assertTrue(manager.checkEvolutionCriteria(2100, 25, EvolutionStage.STAGE_1));
        
        // Should not evolve: insufficient streak
        assertFalse(manager.checkEvolutionCriteria(2100, 20, EvolutionStage.STAGE_1));
        
        // Should not evolve: insufficient XP
        assertFalse(manager.checkEvolutionCriteria(1900, 25, EvolutionStage.STAGE_1));
    }
    
    @Test
    void testCheckEvolutionCriteriaMaxLevel() {
        PokemonStateManager manager = new PokemonStateManager();
        
        // Should not evolve: already at max level
        assertFalse(manager.checkEvolutionCriteria(3000, 30, EvolutionStage.STAGE_2));
    }
    
    @Test
    void testGetNextEvolutionStage() {
        PokemonStateManager manager = new PokemonStateManager();
        
        assertEquals(EvolutionStage.BASIC, manager.getNextEvolutionStage(EvolutionStage.EGG));
        assertEquals(EvolutionStage.STAGE_1, manager.getNextEvolutionStage(EvolutionStage.BASIC));
        assertEquals(EvolutionStage.STAGE_2, manager.getNextEvolutionStage(EvolutionStage.STAGE_1));
        assertEquals(EvolutionStage.STAGE_2, manager.getNextEvolutionStage(EvolutionStage.STAGE_2)); // Max level
    }
}