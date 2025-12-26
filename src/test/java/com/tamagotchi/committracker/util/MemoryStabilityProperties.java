package com.tamagotchi.committracker.util;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import static org.junit.jupiter.api.Assertions.*;

import com.tamagotchi.committracker.config.AppConfig;
import com.tamagotchi.committracker.domain.Commit;
import com.tamagotchi.committracker.domain.CommitHistory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Property-based tests for overall memory stability across the application.
 * 
 * **Feature: performance-optimization, Property 1: Memory Stability**
 * **Validates: Requirements 1.1, 1.4, 5.1**
 * 
 * These tests validate that memory usage remains stable and bounded
 * regardless of the number of operations performed over time.
 */
class MemoryStabilityProperties {

    /**
     * **Feature: performance-optimization, Property 1: Memory Stability**
     * **Validates: Requirements 1.1, 1.4, 5.1**
     * 
     * For any extended application runtime, memory usage should remain within
     * configured bounds and not exhibit continuous growth patterns.
     * 
     * This property verifies that repeated operations don't cause unbounded memory growth.
     */
    @Property(tries = 100)
    void repeatedOperationsDoNotCauseMemoryGrowth(
            @ForAll @IntRange(min = 10, max = 100) int operationCount) {
        
        CommitHistory history = new CommitHistory();
        int limit = AppConfig.getCommitHistoryLimit();
        
        // Perform many operations
        for (int i = 0; i < operationCount; i++) {
            // Add commits
            for (int j = 0; j < 10; j++) {
                Commit commit = createTestCommit(i * 10 + j);
                history.addCommit(commit);
            }
            
            // Verify bounds are maintained after each batch
            List<Commit> commits = history.getRecentCommits();
            assertTrue(commits.size() <= limit,
                    "Commit history should remain bounded after operation " + i);
        }
        
        // Final verification
        Map<String, Integer> stats = history.getMemoryUsageStats();
        assertTrue(stats.get("recentCommitsCount") <= limit,
                "Final commit count should be within bounds");
        assertTrue(stats.get("dailyCommitCountsSize") <= 91,
                "Daily commit counts should be bounded to ~90 days");
    }

    /**
     * **Feature: performance-optimization, Property 1: Memory Stability**
     * **Validates: Requirements 1.1, 1.4, 5.1**
     * 
     * For any sequence of resource registrations and cleanups, the resource
     * manager should maintain accurate tracking without memory leaks.
     */
    @Property(tries = 100)
    void resourceManagerMaintainsAccurateTracking(
            @ForAll @IntRange(min = 1, max = 50) int resourceCount) {
        
        ResourceManager resourceManager = ResourceManager.getInstance();
        
        // Clean up any existing state
        resourceManager.cleanupAll();
        
        // Track resources we create
        List<String> createdIds = new ArrayList<>();
        
        // Register resources
        for (int i = 0; i < resourceCount; i++) {
            String id = "memory-test-resource-" + System.nanoTime() + "-" + i;
            AutoCloseable resource = () -> {}; // Simple no-op resource
            resourceManager.registerResource(id, resource, ResourceManager.ResourceType.OTHER);
            createdIds.add(id);
        }
        
        // Verify tracking is accurate
        ResourceManager.ResourceStats stats = resourceManager.getResourceStats();
        assertEquals(resourceCount, stats.activeCount,
                "Active count should match registered resources");
        
        // Clean up half the resources
        int halfCount = resourceCount / 2;
        for (int i = 0; i < halfCount; i++) {
            resourceManager.cleanupResource(createdIds.get(i));
        }
        
        // Verify tracking is updated
        ResourceManager.ResourceStats afterHalfCleanup = resourceManager.getResourceStats();
        assertEquals(resourceCount - halfCount, afterHalfCleanup.activeCount,
                "Active count should reflect partial cleanup");
        
        // Clean up remaining resources
        resourceManager.cleanupAll();
        
        // Verify complete cleanup
        ResourceManager.ResourceStats finalStats = resourceManager.getResourceStats();
        assertEquals(0, finalStats.activeCount,
                "Active count should be 0 after complete cleanup");
    }

    /**
     * **Feature: performance-optimization, Property 1: Memory Stability**
     * **Validates: Requirements 1.1, 1.4, 5.1**
     * 
     * For any bounded collection, the size should never exceed the configured limit
     * regardless of how many items are added.
     */
    @Property(tries = 100)
    void boundedCollectionsRespectLimits(
            @ForAll @IntRange(min = 1, max = 5000) int itemCount) {
        
        int limit = AppConfig.getCommitHistoryLimit();
        CommitHistory history = new CommitHistory();
        
        // Add many items
        LocalDateTime baseTime = LocalDateTime.now();
        for (int i = 0; i < itemCount; i++) {
            Commit commit = new Commit(
                "hash-" + i,
                "Message " + i,
                "Author",
                baseTime.minusMinutes(i),
                "test-repo",
                "/test/path"
            );
            history.addCommit(commit);
            
            // Verify bounds are maintained at each step
            assertTrue(history.getRecentCommits().size() <= limit,
                    "Size should never exceed limit at step " + i);
        }
        
        // Final verification
        List<Commit> finalCommits = history.getRecentCommits();
        assertTrue(finalCommits.size() <= limit,
                "Final size should be within bounds");
        
        // If we added more than the limit, verify we have exactly the limit
        if (itemCount > limit) {
            assertEquals(limit, finalCommits.size(),
                    "Should have exactly limit items when more were added");
        }
    }

    /**
     * **Feature: performance-optimization, Property 1: Memory Stability**
     * **Validates: Requirements 1.1, 1.4, 5.1**
     * 
     * For any memory pressure scenario, the system should be able to detect
     * and respond appropriately.
     */
    @Property(tries = 100)
    void memoryPressureDetectionWorks(
            @ForAll @IntRange(min = 1, max = 100) int resourceCount) {
        
        ResourceManager resourceManager = ResourceManager.getInstance();
        
        // Clean up any existing state
        resourceManager.cleanupAll();
        
        // Register many resources without cleaning up
        for (int i = 0; i < resourceCount; i++) {
            String id = "pressure-test-" + System.nanoTime() + "-" + i;
            AutoCloseable resource = () -> {};
            resourceManager.registerResource(id, resource, ResourceManager.ResourceType.OTHER);
        }
        
        // Get stats
        ResourceManager.ResourceStats stats = resourceManager.getResourceStats();
        
        // Verify stats are accurate
        assertEquals(resourceCount, stats.activeCount,
                "Stats should accurately reflect resource count");
        assertTrue(stats.totalCreated >= resourceCount,
                "Total created should be at least the current count");
        
        // Clean up
        resourceManager.cleanupAll();
        
        // Verify cleanup
        ResourceManager.ResourceStats afterCleanup = resourceManager.getResourceStats();
        assertEquals(0, afterCleanup.activeCount,
                "All resources should be cleaned up");
    }

    /**
     * **Feature: performance-optimization, Property 1: Memory Stability**
     * **Validates: Requirements 1.1, 1.4, 5.1**
     * 
     * For any commit history with daily counts, old entries should be pruned
     * to prevent unbounded growth of the daily counts map.
     */
    @Property(tries = 100)
    void dailyCountsPruningPreventsUnboundedGrowth(
            @ForAll @IntRange(min = 1, max = 365) int daysOfHistory) {
        
        CommitHistory history = new CommitHistory();
        LocalDateTime now = LocalDateTime.now();
        
        // Add commits spanning many days
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
        
        // Verify no entries older than 90 days
        java.time.LocalDate cutoffDate = java.time.LocalDate.now().minusDays(90);
        for (java.time.LocalDate date : dailyCounts.keySet()) {
            assertFalse(date.isBefore(cutoffDate),
                    "Daily counts should not contain entries older than 90 days: " + date);
        }
        
        // Verify the count is bounded
        assertTrue(dailyCounts.size() <= 91,
                "Daily commit counts should be bounded to ~90 days, but was " + dailyCounts.size());
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
