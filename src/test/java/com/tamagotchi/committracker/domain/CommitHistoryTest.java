package com.tamagotchi.committracker.domain;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CommitHistory domain model.
 */
class CommitHistoryTest {

    @Test
    void testCommitHistoryInitialization() {
        CommitHistory history = new CommitHistory();
        
        assertNotNull(history.getRecentCommits());
        assertTrue(history.getRecentCommits().isEmpty());
        assertNotNull(history.getDailyCommitCounts());
        assertTrue(history.getDailyCommitCounts().isEmpty());
        assertNull(history.getLastCommitTime());
        assertEquals(0, history.getCurrentStreak());
        assertEquals(0.0, history.getAverageCommitsPerDay(), 0.01);
    }
    
    @Test
    void testAddCommit() {
        CommitHistory history = new CommitHistory();
        LocalDateTime now = LocalDateTime.now();
        
        Commit commit = new Commit("hash1", "message", "author", now, "repo", "/path");
        history.addCommit(commit);
        
        assertEquals(1, history.getRecentCommits().size());
        assertEquals(commit, history.getRecentCommits().get(0));
        assertEquals(now, history.getLastCommitTime());
        
        // Check daily commit count was updated
        LocalDate today = now.toLocalDate();
        assertEquals(1, history.getDailyCommitCounts().get(today).intValue());
    }
    
    @Test
    void testAddMultipleCommitsSameDay() {
        CommitHistory history = new CommitHistory();
        // Use a fixed time in the middle of the day to avoid midnight crossing issues
        LocalDateTime baseTime = LocalDate.now().atTime(12, 0);
        LocalDate today = baseTime.toLocalDate();
        
        Commit commit1 = new Commit("hash1", "message1", "author", baseTime, "repo", "/path");
        Commit commit2 = new Commit("hash2", "message2", "author", baseTime.plusHours(1), "repo", "/path");
        
        history.addCommit(commit1);
        history.addCommit(commit2);
        
        assertEquals(2, history.getRecentCommits().size());
        assertEquals(2, history.getDailyCommitCounts().get(today).intValue());
        assertEquals(baseTime.plusHours(1), history.getLastCommitTime()); // Should be the later commit
    }
    
    @Test
    void testCalculateCurrentStreak() {
        CommitHistory history = new CommitHistory();
        Map<LocalDate, Integer> dailyCounts = new HashMap<>();
        
        LocalDate today = LocalDate.now();
        dailyCounts.put(today, 2);
        dailyCounts.put(today.minusDays(1), 1);
        dailyCounts.put(today.minusDays(2), 3);
        // Gap at day 3
        dailyCounts.put(today.minusDays(4), 1);
        
        history.setDailyCommitCounts(dailyCounts);
        history.calculateCurrentStreak();
        
        assertEquals(3, history.getCurrentStreak()); // Today + 2 previous consecutive days
    }
    
    @Test
    void testCalculateCurrentStreakNoCommits() {
        CommitHistory history = new CommitHistory();
        history.calculateCurrentStreak();
        
        assertEquals(0, history.getCurrentStreak());
    }
    
    @Test
    void testCalculateAverageCommitsPerDay() {
        CommitHistory history = new CommitHistory();
        Map<LocalDate, Integer> dailyCounts = new HashMap<>();
        
        // Add commits for the last 5 days
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 5; i++) {
            dailyCounts.put(today.minusDays(i), 2); // 2 commits per day
        }
        
        history.setDailyCommitCounts(dailyCounts);
        history.calculateAverageCommitsPerDay();
        
        assertEquals(2.0, history.getAverageCommitsPerDay(), 0.01);
    }
    
    @Test
    void testSetCurrentStreakNegative() {
        CommitHistory history = new CommitHistory();
        history.setCurrentStreak(-5);
        
        assertEquals(0, history.getCurrentStreak()); // Should be clamped to 0
    }
    
    @Test
    void testSetAverageCommitsPerDayNegative() {
        CommitHistory history = new CommitHistory();
        history.setAverageCommitsPerDay(-1.5);
        
        assertEquals(0.0, history.getAverageCommitsPerDay(), 0.01); // Should be clamped to 0
    }
}