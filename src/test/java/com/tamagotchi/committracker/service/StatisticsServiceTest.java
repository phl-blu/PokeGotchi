package com.tamagotchi.committracker.service;

import com.tamagotchi.committracker.domain.Commit;
import com.tamagotchi.committracker.pokemon.EvolutionStage;
import com.tamagotchi.committracker.pokemon.PokemonSpecies;
import com.tamagotchi.committracker.service.StatisticsService.DailyStats;
import com.tamagotchi.committracker.service.StatisticsService.WeeklyStats;
import com.tamagotchi.committracker.service.StatisticsService.ProductivityMetrics;
import com.tamagotchi.committracker.service.StatisticsService.EvolutionEntry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StatisticsService functionality.
 * Tests commit frequency graphs, productivity metrics, and evolution history.
 * 
 * Requirements: 5.5
 */
public class StatisticsServiceTest {
    
    private StatisticsService statisticsService;
    private List<Commit> testCommits;
    
    @BeforeEach
    void setUp() {
        statisticsService = new StatisticsService();
        testCommits = createTestCommits();
    }
    
    /**
     * Creates a set of test commits for testing statistics calculations.
     */
    private List<Commit> createTestCommits() {
        List<Commit> commits = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // Create commits over the last 10 days
        for (int i = 0; i < 10; i++) {
            LocalDateTime commitTime = now.minusDays(i);
            
            // Create 1-3 commits per day
            int commitsPerDay = 1 + (i % 3);
            for (int j = 0; j < commitsPerDay; j++) {
                Commit commit = new Commit();
                commit.setMessage("Test commit " + i + "-" + j);
                commit.setAuthor("Test Author");
                commit.setRepositoryName("test-repo-" + (i % 2)); // Alternate between 2 repos
                commit.setTimestamp(commitTime.minusHours(j));
                commits.add(commit);
            }
        }
        
        return commits;
    }
    
    @Test
    void testCalculateDailyStats() {
        List<DailyStats> dailyStats = statisticsService.calculateDailyStats(testCommits, 30);
        
        assertNotNull(dailyStats);
        assertEquals(30, dailyStats.size()); // Should return 30 days of data
        
        // Check that recent days have commits
        boolean foundCommitsInRecentDays = false;
        for (int i = 0; i < 10; i++) {
            DailyStats stats = dailyStats.get(dailyStats.size() - 1 - i); // Last 10 days
            if (stats.getCommitCount() > 0) {
                foundCommitsInRecentDays = true;
                assertTrue(stats.getXpGained() > 0);
                assertTrue(stats.getRepositoryCount() > 0);
                break;
            }
        }
        assertTrue(foundCommitsInRecentDays, "Should find commits in recent days");
    }
    
    @Test
    void testCalculateWeeklyStats() {
        List<WeeklyStats> weeklyStats = statisticsService.calculateWeeklyStats(testCommits, 12);
        
        assertNotNull(weeklyStats);
        assertEquals(12, weeklyStats.size()); // Should return 12 weeks of data
        
        // Check that recent weeks have commits
        boolean foundCommitsInRecentWeeks = false;
        for (WeeklyStats stats : weeklyStats) {
            if (stats.getCommitCount() > 0) {
                foundCommitsInRecentWeeks = true;
                assertTrue(stats.getXpGained() > 0);
                assertTrue(stats.getActiveDays() > 0);
                assertTrue(stats.getRepositoryCount() > 0);
                break;
            }
        }
        assertTrue(foundCommitsInRecentWeeks, "Should find commits in recent weeks");
    }
    
    @Test
    void testCalculateProductivityMetrics() {
        ProductivityMetrics metrics = statisticsService.calculateProductivityMetrics(testCommits, 5);
        
        assertNotNull(metrics);
        assertTrue(metrics.getTotalCommits() > 0);
        assertTrue(metrics.getTotalXp() > 0);
        assertTrue(metrics.getTotalRepositories() > 0);
        assertTrue(metrics.getActiveDays() > 0);
        assertEquals(5, metrics.getCurrentStreak()); // Should match input
        assertTrue(metrics.getLongestStreak() > 0);
        assertTrue(metrics.getAverageCommitsPerDay() > 0);
        assertTrue(metrics.getAverageXpPerDay() > 0);
        assertNotNull(metrics.getFirstCommitDate());
        assertNotNull(metrics.getLastCommitDate());
    }
    
    @Test
    void testCalculateProductivityMetricsWithEmptyCommits() {
        ProductivityMetrics metrics = statisticsService.calculateProductivityMetrics(new ArrayList<>(), 0);
        
        assertNotNull(metrics);
        assertEquals(0, metrics.getTotalCommits());
        assertEquals(0, metrics.getTotalXp());
        assertEquals(0, metrics.getTotalRepositories());
        assertEquals(0, metrics.getActiveDays());
        assertEquals(0, metrics.getCurrentStreak());
        assertEquals(0, metrics.getLongestStreak());
        assertEquals(0.0, metrics.getAverageCommitsPerDay());
        assertEquals(0.0, metrics.getAverageXpPerDay());
        assertNull(metrics.getFirstCommitDate());
        assertNull(metrics.getLastCommitDate());
    }
    
    @Test
    void testGetEvolutionHistory() {
        // Clear any existing history first
        statisticsService.clearEvolutionHistory();
        
        // Record some evolutions
        statisticsService.recordEvolution(PokemonSpecies.CHARMANDER, EvolutionStage.EGG, EvolutionStage.BASIC, 60, 4);
        
        // Test with BASIC stage - should have 1 evolution
        List<EvolutionEntry> basicHistory = statisticsService.getEvolutionHistory(
            PokemonSpecies.CHARMANDER, EvolutionStage.BASIC
        );
        assertEquals(1, basicHistory.size()); // Should have egg -> basic evolution
        
        // Record more evolutions
        statisticsService.recordEvolution(PokemonSpecies.CHARMANDER, EvolutionStage.BASIC, EvolutionStage.STAGE_1, 200, 11);
        
        List<EvolutionEntry> stage1History = statisticsService.getEvolutionHistory(
            PokemonSpecies.CHARMANDER, EvolutionStage.STAGE_1
        );
        assertEquals(2, stage1History.size()); // Should have egg -> basic -> stage1
        
        // Record final evolution
        statisticsService.recordEvolution(PokemonSpecies.CHARMANDER, EvolutionStage.STAGE_1, EvolutionStage.STAGE_2, 800, 22);
        
        List<EvolutionEntry> stage2History = statisticsService.getEvolutionHistory(
            PokemonSpecies.CHARMANDER, EvolutionStage.STAGE_2
        );
        assertEquals(3, stage2History.size()); // Should have all evolutions
        
        // Verify evolution entry data
        EvolutionEntry entry = stage2History.get(0);
        assertNotNull(entry.getTimestamp());
        assertEquals(PokemonSpecies.CHARMANDER, entry.getSpecies());
        assertEquals(EvolutionStage.EGG, entry.getFromStage());
        assertEquals(EvolutionStage.BASIC, entry.getToStage());
        assertTrue(entry.getXpAtEvolution() > 0);
        assertTrue(entry.getStreakAtEvolution() > 0);
    }
    
    @Test
    void testDailyStatsDataIntegrity() {
        List<DailyStats> dailyStats = statisticsService.calculateDailyStats(testCommits, 7);
        
        for (DailyStats stats : dailyStats) {
            assertNotNull(stats.getDate());
            assertTrue(stats.getCommitCount() >= 0);
            assertTrue(stats.getXpGained() >= 0);
            assertTrue(stats.getRepositoryCount() >= 0);
            assertNotNull(stats.getRepositories());
            
            // If there are commits, there should be XP and repositories
            if (stats.getCommitCount() > 0) {
                assertTrue(stats.getXpGained() > 0);
                assertTrue(stats.getRepositoryCount() > 0);
            }
        }
    }
    
    @Test
    void testWeeklyStatsDataIntegrity() {
        List<WeeklyStats> weeklyStats = statisticsService.calculateWeeklyStats(testCommits, 4);
        
        for (WeeklyStats stats : weeklyStats) {
            assertNotNull(stats.getWeekStart());
            assertTrue(stats.getCommitCount() >= 0);
            assertTrue(stats.getXpGained() >= 0);
            assertTrue(stats.getActiveDays() >= 0);
            assertTrue(stats.getRepositoryCount() >= 0);
            assertNotNull(stats.getRepositories());
            
            // Active days should not exceed 7
            assertTrue(stats.getActiveDays() <= 7);
            
            // If there are commits, there should be active days
            if (stats.getCommitCount() > 0) {
                assertTrue(stats.getActiveDays() > 0);
                assertTrue(stats.getXpGained() > 0);
            }
        }
    }
}