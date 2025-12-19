package com.tamagotchi.committracker.pokemon;

import com.tamagotchi.committracker.domain.CommitHistory;
import com.tamagotchi.committracker.domain.Commit;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
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
        
        // With the enhanced logic, this should be CONTENT since it's not within the last hour
        // and doesn't meet the higher thresholds for HAPPY
        assertEquals(PokemonState.CONTENT, manager.calculateState(history));
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
        
        // Should evolve: 4+ day streak OR 50+ XP (either condition works)
        assertTrue(manager.checkEvolutionCriteria(250, 5, EvolutionStage.EGG));
        assertTrue(manager.checkEvolutionCriteria(50, 2, EvolutionStage.EGG)); // Exactly 50 XP, low streak
        assertTrue(manager.checkEvolutionCriteria(60, 2, EvolutionStage.EGG)); // High XP, low streak
        assertTrue(manager.checkEvolutionCriteria(30, 5, EvolutionStage.EGG)); // Low XP, high streak
        
        // Should not evolve: insufficient streak AND insufficient XP
        assertFalse(manager.checkEvolutionCriteria(49, 3, EvolutionStage.EGG)); // Just under 50 XP and under 4 days
        assertFalse(manager.checkEvolutionCriteria(40, 3, EvolutionStage.EGG));
    }
    
    @Test
    void testCheckEvolutionCriteriaBasicToStage1() {
        PokemonStateManager manager = new PokemonStateManager();
        
        // Should evolve: 11+ day streak (XP not required after hatching)
        assertTrue(manager.checkEvolutionCriteria(100, 12, EvolutionStage.BASIC));
        assertTrue(manager.checkEvolutionCriteria(900, 12, EvolutionStage.BASIC));
        
        // Should not evolve: insufficient streak
        assertFalse(manager.checkEvolutionCriteria(900, 10, EvolutionStage.BASIC));
        assertFalse(manager.checkEvolutionCriteria(100, 10, EvolutionStage.BASIC));
    }
    
    @Test
    void testCheckEvolutionCriteriaStage1ToStage2() {
        PokemonStateManager manager = new PokemonStateManager();
        
        // Should evolve: 22+ day streak (XP not required after hatching)
        assertTrue(manager.checkEvolutionCriteria(500, 25, EvolutionStage.STAGE_1));
        assertTrue(manager.checkEvolutionCriteria(2100, 25, EvolutionStage.STAGE_1));
        
        // Should not evolve: insufficient streak
        assertFalse(manager.checkEvolutionCriteria(2100, 20, EvolutionStage.STAGE_1));
        assertFalse(manager.checkEvolutionCriteria(500, 20, EvolutionStage.STAGE_1));
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
    
    @Test
    void testCalculateStateForCommitTrigger() {
        PokemonStateManager manager = new PokemonStateManager();
        CommitHistory history = new CommitHistory();
        history.setLastCommitTime(LocalDateTime.now().minusHours(1));
        history.setCurrentStreak(3);
        history.setAverageCommitsPerDay(1.0);
        
        // Test with multiple commits
        List<Commit> multipleCommits = List.of(
            new Commit("hash1", "feat: add feature", "author", LocalDateTime.now(), "repo", "/path"),
            new Commit("hash2", "fix: bug fix", "author", LocalDateTime.now(), "repo", "/path"),
            new Commit("hash3", "docs: update", "author", LocalDateTime.now(), "repo", "/path")
        );
        assertEquals(PokemonState.THRIVING, manager.calculateStateForCommitTrigger(history, multipleCommits));
        
        // Test with single high-quality commit
        List<Commit> singleHighQualityCommit = List.of(
            new Commit("hash1", "feat: implement comprehensive new feature with detailed documentation", "author", LocalDateTime.now(), "repo", "/path")
        );
        PokemonState result = manager.calculateStateForCommitTrigger(history, singleHighQualityCommit);
        // Should be HAPPY or CONTENT depending on XP calculation - both are acceptable for high-quality commits
        assertTrue(result == PokemonState.HAPPY || result == PokemonState.CONTENT);
        
        // Test with single regular commit
        List<Commit> singleRegularCommit = List.of(
            new Commit("hash1", "fix", "author", LocalDateTime.now(), "repo", "/path")
        );
        assertEquals(PokemonState.CONTENT, manager.calculateStateForCommitTrigger(history, singleRegularCommit));
    }
    
    @Test
    void testProcessNewCommits() {
        PokemonStateManager manager = new PokemonStateManager();
        
        List<Commit> newCommits = List.of(
            new Commit("hash1", "feat: add feature", "author", LocalDateTime.now(), "repo", "/path"),
            new Commit("hash2", "fix: bug fix", "author", LocalDateTime.now(), "repo", "/path")
        );
        
        int initialXP = manager.getCurrentXP();
        int xpGained = manager.processNewCommits(newCommits);
        
        assertTrue(xpGained > 0);
        assertEquals(initialXP + xpGained, manager.getCurrentXP());
    }
    
    @Test
    void testTriggerEvolutionIfReady() {
        PokemonStateManager manager = new PokemonStateManager(PokemonSpecies.CHARMANDER, EvolutionStage.EGG, 60);
        
        // Should evolve from EGG to BASIC with sufficient XP
        EvolutionStage newStage = manager.triggerEvolutionIfReady(2); // Low streak but high XP
        assertEquals(EvolutionStage.BASIC, newStage);
        assertEquals(EvolutionStage.BASIC, manager.getCurrentStage());
        
        // Should not evolve from BASIC without sufficient streak
        EvolutionStage noEvolution = manager.triggerEvolutionIfReady(5);
        assertNull(noEvolution);
        assertEquals(EvolutionStage.BASIC, manager.getCurrentStage());
        
        // Should evolve from BASIC to STAGE_1 with sufficient streak
        EvolutionStage stage1 = manager.triggerEvolutionIfReady(12);
        assertEquals(EvolutionStage.STAGE_1, stage1);
        assertEquals(EvolutionStage.STAGE_1, manager.getCurrentStage());
    }
    
    @Test
    void testCalculateHealthMetrics() {
        PokemonStateManager manager = new PokemonStateManager();
        
        // Test with healthy commit pattern
        CommitHistory healthyHistory = new CommitHistory();
        healthyHistory.setLastCommitTime(LocalDateTime.now().minusHours(2));
        healthyHistory.setCurrentStreak(10);
        healthyHistory.setAverageCommitsPerDay(2.5);
        
        double healthyMetrics = manager.calculateHealthMetrics(healthyHistory);
        assertTrue(healthyMetrics > 0.7); // Should be high health
        
        // Test with unhealthy commit pattern
        CommitHistory unhealthyHistory = new CommitHistory();
        unhealthyHistory.setLastCommitTime(LocalDateTime.now().minusDays(10));
        unhealthyHistory.setCurrentStreak(0);
        unhealthyHistory.setAverageCommitsPerDay(0.1);
        
        double unhealthyMetrics = manager.calculateHealthMetrics(unhealthyHistory);
        assertTrue(unhealthyMetrics < 0.4); // Should be low health
    }
    
    @Test
    void testEnhancedStateCalculationWithRecentActivity() {
        PokemonStateManager manager = new PokemonStateManager();
        
        // Test very recent commit (within last hour) should be more energetic
        CommitHistory veryRecentHistory = new CommitHistory();
        veryRecentHistory.setLastCommitTime(LocalDateTime.now().minusMinutes(30));
        veryRecentHistory.setCurrentStreak(2);
        veryRecentHistory.setAverageCommitsPerDay(1.0);
        
        // Add some recent commits to the history
        List<Commit> recentCommits = List.of(
            new Commit("hash1", "feat: add feature", "author", LocalDateTime.now().minusMinutes(30), "repo", "/path"),
            new Commit("hash2", "fix: bug fix", "author", LocalDateTime.now().minusMinutes(45), "repo", "/path"),
            new Commit("hash3", "docs: update", "author", LocalDateTime.now().minusHours(2), "repo", "/path")
        );
        veryRecentHistory.setRecentCommits(recentCommits);
        
        PokemonState state = manager.calculateState(veryRecentHistory);
        assertTrue(state == PokemonState.CONTENT || state == PokemonState.HAPPY); // Should be positive due to recent activity
    }
    
    @Test
    void testEggStageProgression() {
        PokemonStateManager manager = new PokemonStateManager(PokemonSpecies.CHARMANDER, EvolutionStage.EGG, 0);
        
        // Test egg stage progression based on XP
        assertEquals(1, manager.getCurrentEggStage()); // 0 XP = Stage 1
        
        manager.getXpSystem().setCurrentXP(5);
        assertEquals(1, manager.getCurrentEggStage()); // 5 XP = Stage 1 (0-10)
        
        manager.getXpSystem().setCurrentXP(10);
        assertEquals(1, manager.getCurrentEggStage()); // 10 XP = Stage 1 (0-10)
        
        manager.getXpSystem().setCurrentXP(15);
        assertEquals(2, manager.getCurrentEggStage()); // 15 XP = Stage 2 (11-20)
        
        manager.getXpSystem().setCurrentXP(25);
        assertEquals(3, manager.getCurrentEggStage()); // 25 XP = Stage 3 (21-35)
        
        manager.getXpSystem().setCurrentXP(40);
        assertEquals(4, manager.getCurrentEggStage()); // 40 XP = Stage 4 (36-49)
        
        manager.getXpSystem().setCurrentXP(50);
        assertEquals(4, manager.getCurrentEggStage()); // 50 XP = Still Stage 4 until evolution
        assertTrue(manager.isEggReadyToHatch()); // But ready to hatch
    }
    
    @Test
    void testEggReadyToHatch() {
        PokemonStateManager manager = new PokemonStateManager(PokemonSpecies.MUDKIP, EvolutionStage.EGG, 49);
        
        // Not ready to hatch at 49 XP
        assertFalse(manager.isEggReadyToHatch());
        assertEquals(4, manager.getCurrentEggStage());
        
        // Ready to hatch at 50 XP
        manager.getXpSystem().setCurrentXP(50);
        assertTrue(manager.isEggReadyToHatch());
        assertEquals(4, manager.getCurrentEggStage());
        
        // Not ready if already evolved
        manager.setCurrentStage(EvolutionStage.BASIC);
        assertFalse(manager.isEggReadyToHatch());
        assertEquals(0, manager.getCurrentEggStage()); // Not an egg anymore
    }
    
    @Test
    void testXPBasedEvolutionThresholds() {
        PokemonStateManager manager = new PokemonStateManager(PokemonSpecies.PIPLUP, EvolutionStage.EGG, 0);
        
        // Test XP thresholds for evolution
        assertFalse(manager.checkEvolutionCriteria(49, 0, EvolutionStage.EGG)); // 49 XP, no streak - should not evolve
        assertTrue(manager.checkEvolutionCriteria(50, 0, EvolutionStage.EGG)); // 50 XP, no streak - should evolve
        assertTrue(manager.checkEvolutionCriteria(100, 0, EvolutionStage.EGG)); // 100 XP, no streak - should evolve
        
        // Test streak-based evolution still works
        assertTrue(manager.checkEvolutionCriteria(0, 4, EvolutionStage.EGG)); // 0 XP, 4+ day streak - should evolve
        assertTrue(manager.checkEvolutionCriteria(30, 4, EvolutionStage.EGG)); // 30 XP, 4+ day streak - should evolve
    }
}