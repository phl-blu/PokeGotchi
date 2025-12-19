package com.tamagotchi.committracker.pokemon;

import com.tamagotchi.committracker.domain.CommitHistory;
import com.tamagotchi.committracker.domain.Commit;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PokemonEvolutionManager.
 */
class PokemonEvolutionManagerTest {

    @Test
    void testInitialization() {
        PokemonEvolutionManager manager = new PokemonEvolutionManager(PokemonSpecies.CHARMANDER);
        
        assertEquals(PokemonSpecies.CHARMANDER, manager.getCurrentSpecies());
        assertEquals(EvolutionStage.EGG, manager.getCurrentStage());
        assertEquals(PokemonState.CONTENT, manager.getCurrentState());
        assertEquals(0, manager.getCurrentXP());
        assertFalse(manager.isEvolutionInProgress());
    }
    
    @Test
    void testInitializationWithExistingState() {
        PokemonEvolutionManager manager = new PokemonEvolutionManager(
            PokemonSpecies.CYNDAQUIL, EvolutionStage.BASIC, 300);
        
        assertEquals(PokemonSpecies.CYNDAQUIL, manager.getCurrentSpecies());
        assertEquals(EvolutionStage.BASIC, manager.getCurrentStage());
        assertEquals(300, manager.getCurrentXP());
    }
    
    @Test
    void testProcessCommitActivity() {
        PokemonEvolutionManager manager = new PokemonEvolutionManager(PokemonSpecies.MUDKIP);
        
        CommitHistory history = new CommitHistory();
        history.setLastCommitTime(LocalDateTime.now());
        history.setCurrentStreak(2);
        history.setAverageCommitsPerDay(1.0);
        
        List<Commit> newCommits = List.of(
            new Commit("hash1", "feat: add feature", "author", LocalDateTime.now(), "repo", "/path"),
            new Commit("hash2", "fix: bug fix", "author", LocalDateTime.now(), "repo", "/path")
        );
        
        int initialXP = manager.getCurrentXP();
        boolean stateChanged = manager.processCommitActivity(history, newCommits);
        
        assertTrue(stateChanged);
        assertTrue(manager.getCurrentXP() > initialXP);
    }
    
    @Test
    void testEvolutionCallback() {
        PokemonEvolutionManager manager = new PokemonEvolutionManager(PokemonSpecies.PIPLUP);
        
        AtomicReference<EvolutionStage> callbackStage = new AtomicReference<>();
        manager.setEvolutionCallback(callbackStage::set);
        
        // Force evolution by setting high XP and triggering with sufficient streak
        manager.getXPSystem().setCurrentXP(50); // Exactly enough to evolve from egg
        
        CommitHistory history = new CommitHistory();
        history.setCurrentStreak(5); // Sufficient streak for egg evolution
        
        boolean evolutionTriggered = manager.checkAndTriggerEvolution(5);
        
        assertTrue(evolutionTriggered);
        assertEquals(EvolutionStage.BASIC, callbackStage.get());
        assertEquals(EvolutionStage.BASIC, manager.getCurrentStage());
    }
    
    @Test
    void testUpdateStateFromHistory() {
        PokemonEvolutionManager manager = new PokemonEvolutionManager(PokemonSpecies.SNIVY);
        
        // Create history that should result in SAD state
        CommitHistory sadHistory = new CommitHistory();
        sadHistory.setLastCommitTime(LocalDateTime.now().minusDays(5));
        sadHistory.setCurrentStreak(0);
        sadHistory.setAverageCommitsPerDay(0.1);
        
        boolean stateChanged = manager.updateStateFromHistory(sadHistory);
        
        assertTrue(stateChanged);
        assertEquals(PokemonState.SAD, manager.getCurrentState());
    }
    
    @Test
    void testHealthMetricsCalculation() {
        PokemonEvolutionManager manager = new PokemonEvolutionManager(PokemonSpecies.FROAKIE);
        
        // Healthy commit pattern
        CommitHistory healthyHistory = new CommitHistory();
        healthyHistory.setLastCommitTime(LocalDateTime.now().minusHours(1));
        healthyHistory.setCurrentStreak(10);
        healthyHistory.setAverageCommitsPerDay(2.0);
        
        double healthMetrics = manager.calculateHealthMetrics(healthyHistory);
        assertTrue(healthMetrics > 0.6); // Should be healthy
        
        // Unhealthy commit pattern
        CommitHistory unhealthyHistory = new CommitHistory();
        unhealthyHistory.setLastCommitTime(LocalDateTime.now().minusDays(10));
        unhealthyHistory.setCurrentStreak(0);
        unhealthyHistory.setAverageCommitsPerDay(0.1);
        
        double unhealthyMetrics = manager.calculateHealthMetrics(unhealthyHistory);
        assertTrue(unhealthyMetrics < 0.5); // Should be unhealthy
    }
    
    @Test
    void testEvolutionProgression() {
        PokemonEvolutionManager manager = new PokemonEvolutionManager(PokemonSpecies.ROWLET);
        
        // Start at egg stage
        assertEquals(EvolutionStage.EGG, manager.getCurrentStage());
        
        // Add enough XP to evolve from egg
        manager.getXPSystem().setCurrentXP(50);
        
        // Trigger evolution with sufficient streak
        boolean evolved1 = manager.checkAndTriggerEvolution(5);
        assertTrue(evolved1);
        assertEquals(EvolutionStage.BASIC, manager.getCurrentStage());
        
        // Evolve to Stage 1 with longer streak
        boolean evolved2 = manager.checkAndTriggerEvolution(12);
        assertTrue(evolved2);
        assertEquals(EvolutionStage.STAGE_1, manager.getCurrentStage());
        
        // Evolve to Stage 2 with even longer streak
        boolean evolved3 = manager.checkAndTriggerEvolution(25);
        assertTrue(evolved3);
        assertEquals(EvolutionStage.STAGE_2, manager.getCurrentStage());
        
        // Should not evolve further (already at max)
        boolean evolved4 = manager.checkAndTriggerEvolution(30);
        assertFalse(evolved4);
        assertEquals(EvolutionStage.STAGE_2, manager.getCurrentStage());
    }
    
    @Test
    void testCommitActivityWithEvolution() {
        PokemonEvolutionManager manager = new PokemonEvolutionManager(PokemonSpecies.GROOKEY);
        
        CommitHistory history = new CommitHistory();
        history.setLastCommitTime(LocalDateTime.now());
        history.setCurrentStreak(5); // Sufficient for egg evolution
        history.setAverageCommitsPerDay(1.5);
        
        // Add commits that will provide enough XP for evolution
        List<Commit> commits = List.of(
            new Commit("hash1", "feat: major feature implementation", "author", LocalDateTime.now(), "repo", "/path"),
            new Commit("hash2", "feat: another major feature", "author", LocalDateTime.now(), "repo", "/path"),
            new Commit("hash3", "feat: third major feature", "author", LocalDateTime.now(), "repo", "/path")
        );
        
        // Process commits - should gain XP and potentially evolve
        boolean stateChanged = manager.processCommitActivity(history, commits);
        
        assertTrue(stateChanged);
        assertTrue(manager.getCurrentXP() > 0);
        
        // Check if evolution occurred (depends on XP gained)
        if (manager.getCurrentXP() >= 50 || history.getCurrentStreak() >= 4) {
            assertEquals(EvolutionStage.BASIC, manager.getCurrentStage());
        }
    }
    
    @Test
    void testNoStateChangeWhenEvolutionInProgress() {
        PokemonEvolutionManager manager = new PokemonEvolutionManager(PokemonSpecies.FUECOCO);
        
        // Set up for evolution from EGG to BASIC
        manager.getXPSystem().setCurrentXP(50);
        
        boolean evolved1 = manager.checkAndTriggerEvolution(5);
        assertTrue(evolved1);
        assertEquals(EvolutionStage.BASIC, manager.getCurrentStage());
        
        // Now at BASIC stage, need higher streak for next evolution
        // Should not evolve with insufficient streak
        boolean evolved2 = manager.checkAndTriggerEvolution(10); // Need 11+ for BASIC -> STAGE_1
        assertFalse(evolved2);
        assertEquals(EvolutionStage.BASIC, manager.getCurrentStage());
        
        // Should evolve with sufficient streak
        boolean evolved3 = manager.checkAndTriggerEvolution(12);
        assertTrue(evolved3);
        assertEquals(EvolutionStage.STAGE_1, manager.getCurrentStage());
    }
    
    @Test
    void testToString() {
        PokemonEvolutionManager manager = new PokemonEvolutionManager(
            PokemonSpecies.CHARMANDER, EvolutionStage.BASIC, 250);
        
        String toString = manager.toString();
        
        assertTrue(toString.contains("CHARMANDER"));
        assertTrue(toString.contains("BASIC"));
        assertTrue(toString.contains("250"));
        assertTrue(toString.contains("evolutionInProgress=false"));
    }
}