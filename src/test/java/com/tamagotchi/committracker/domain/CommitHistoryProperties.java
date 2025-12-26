package com.tamagotchi.committracker.domain;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import static org.junit.jupiter.api.Assertions.*;

import com.tamagotchi.committracker.config.AppConfig;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * Property-based tests for CommitHistory memory management.
 * 
 * **Feature: performance-optimization, Property 1: Memory Stability**
 * **Validates: Requirements 1.1, 1.4, 5.1**
 * 
 * These tests validate that memory usage remains stable and bounded
 * regardless of the number of commits added over time.
 */
class CommitHistoryProperties {

    /**
     * **Feature: performance-optimization, Property 1: Memory Stability**
     * **Validates: Requirements 1.1, 1.4, 5.1**
     * 
     * For any extended application runtime, memory usage should remain within
     * configured bounds and not exhibit continuous growth patterns.
     * 
     * This property verifies that adding commits beyond the configured limit
     * results in automatic pruning to maintain bounded memory usage.
     */
    @Property(tries = 100)
    void commitHistoryRemainsWithinBounds(
            @ForAll @IntRange(min = 1, max = 2000) int numberOfCommits) {
        
        CommitHistory history = new CommitHistory();
        int limit = AppConfig.getCommitHistoryLimit();
        
        // Add commits beyond the limit
        for (int i = 0; i < numberOfCommits; i++) {
            Commit commit = createTestCommit(i);
            history.addCommit(commit);
        }
        
        // Verify the history size never exceeds the configured limit
        List<Commit> recentCommits = history.getRecentCommits();
        assertTrue(recentCommits.size() <= limit,
                "Commit history size (" + recentCommits.size() + 
                ") should not exceed limit (" + limit + ")");
        
        // Verify memory stats reflect the bounded size
        Map<String, Integer> stats = history.getMemoryUsageStats();
        assertTrue(stats.get("recentCommitsCount") <= limit,
                "Memory stats should reflect bounded commit count");
    }

    /**
     * **Feature: performance-optimization, Property 1: Memory Stability**
     * **Validates: Requirements 1.1, 1.4, 5.1**
     * 
     * For any sequence of commit additions, the oldest commits should be
     * pruned first (FIFO order) when the limit is exceeded.
     */
    @Property(tries = 100)
    void oldestCommitsArePrunedFirst(
            @ForAll @IntRange(min = 10, max = 100) int numberOfCommits) {
        
        // Use a small limit for testing
        int testLimit = 5;
        
        // Create a fresh history with controlled limit
        CommitHistory history = new CommitHistory();
        
        // Add commits with sequential timestamps
        List<Commit> addedCommits = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().minusDays(numberOfCommits);
        
        for (int i = 0; i < numberOfCommits; i++) {
            Commit commit = new Commit(
                "hash-" + i,
                "Message " + i,
                "Author",
                baseTime.plusHours(i),
                "test-repo",
                "/test/path"
            );
            history.addCommit(commit);
            addedCommits.add(commit);
        }
        
        // Get the current commits in history
        List<Commit> currentCommits = history.getRecentCommits();
        int limit = AppConfig.getCommitHistoryLimit();
        
        // Verify size is bounded
        assertTrue(currentCommits.size() <= limit,
                "History should be bounded by limit");
        
        // If we added more than the limit, verify oldest were pruned
        if (numberOfCommits > limit) {
            // The remaining commits should be the most recent ones
            int expectedStartIndex = numberOfCommits - limit;
            
            for (int i = 0; i < currentCommits.size(); i++) {
                Commit expected = addedCommits.get(expectedStartIndex + i);
                Commit actual = currentCommits.get(i);
                assertEquals(expected.getHash(), actual.getHash(),
                        "Commit at index " + i + " should be from the most recent additions");
            }
        }
    }

    /**
     * **Feature: performance-optimization, Property 1: Memory Stability**
     * **Validates: Requirements 1.1, 1.4, 5.1**
     * 
     * For any commit history, daily commit counts older than 90 days
     * should be automatically pruned to prevent unbounded growth.
     */
    @Property(tries = 100)
    void dailyCommitCountsArePruned(
            @ForAll @IntRange(min = 1, max = 200) int daysOfHistory) {
        
        CommitHistory history = new CommitHistory();
        LocalDateTime now = LocalDateTime.now();
        
        // Add commits spanning the specified number of days
        for (int day = 0; day < daysOfHistory; day++) {
            LocalDateTime commitTime = now.minusDays(day);
            Commit commit = new Commit(
                "hash-day-" + day,
                "Message for day " + day,
                "Author",
                commitTime,
                "test-repo",
                "/test/path"
            );
            history.addCommit(commit);
        }
        
        // Get daily commit counts
        Map<java.time.LocalDate, Integer> dailyCounts = history.getDailyCommitCounts();
        
        // Verify no entries older than 90 days exist
        java.time.LocalDate cutoffDate = java.time.LocalDate.now().minusDays(90);
        
        for (java.time.LocalDate date : dailyCounts.keySet()) {
            assertFalse(date.isBefore(cutoffDate),
                    "Daily commit counts should not contain entries older than 90 days: " + date);
        }
        
        // Verify the count is bounded
        assertTrue(dailyCounts.size() <= 91,
                "Daily commit counts should be bounded to ~90 days");
    }

    /**
     * **Feature: performance-optimization, Property 1: Memory Stability**
     * **Validates: Requirements 1.1, 1.4, 5.1**
     * 
     * For any commit history constructed with initial data, the pruning
     * should be applied immediately to ensure bounded memory from the start.
     */
    @Property(tries = 100)
    void constructorEnforcesBounds(
            @ForAll @IntRange(min = 1, max = 2000) int initialCommitCount) {
        
        int limit = AppConfig.getCommitHistoryLimit();
        
        // Create initial commits list larger than limit
        List<Commit> initialCommits = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().minusDays(initialCommitCount);
        
        for (int i = 0; i < initialCommitCount; i++) {
            initialCommits.add(new Commit(
                "hash-" + i,
                "Message " + i,
                "Author",
                baseTime.plusHours(i),
                "test-repo",
                "/test/path"
            ));
        }
        
        // Create history with initial data
        CommitHistory history = new CommitHistory(
            initialCommits,
            null,
            LocalDateTime.now(),
            0,
            0.0
        );
        
        // Verify bounds are enforced immediately
        List<Commit> recentCommits = history.getRecentCommits();
        assertTrue(recentCommits.size() <= limit,
                "Constructor should enforce commit history limit");
    }

    /**
     * **Feature: performance-optimization, Property 1: Memory Stability**
     * **Validates: Requirements 1.1, 1.4, 5.1**
     * 
     * For any sequence of setRecentCommits calls, the bounds should be
     * enforced after each call.
     */
    @Property(tries = 100)
    void setRecentCommitsEnforcesBounds(
            @ForAll @IntRange(min = 1, max = 2000) int commitCount) {
        
        CommitHistory history = new CommitHistory();
        int limit = AppConfig.getCommitHistoryLimit();
        
        // Create commits list
        List<Commit> commits = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now();
        
        for (int i = 0; i < commitCount; i++) {
            commits.add(new Commit(
                "hash-" + i,
                "Message " + i,
                "Author",
                baseTime.minusHours(i),
                "test-repo",
                "/test/path"
            ));
        }
        
        // Set commits
        history.setRecentCommits(commits);
        
        // Verify bounds are enforced
        List<Commit> recentCommits = history.getRecentCommits();
        assertTrue(recentCommits.size() <= limit,
                "setRecentCommits should enforce commit history limit");
    }

    /**
     * Helper method to create a test commit with a given index.
     */
    private Commit createTestCommit(int index) {
        return new Commit(
            "hash-" + index,
            "Test commit message " + index,
            "Test Author",
            LocalDateTime.now().minusMinutes(index),
            "test-repository",
            "/test/path/repo"
        );
    }
}
