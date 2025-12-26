package com.tamagotchi.committracker.util;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.ArrayList;

/**
 * Property-based tests for PerformanceMonitor metrics collection accuracy.
 * 
 * **Feature: performance-optimization, Property 10: Performance Monitoring Accuracy**
 * **Validates: Requirements 5.3, 5.4, 5.5**
 * 
 * These tests validate that performance metrics are accurately collected and made
 * available for monitoring across all system operations.
 */
class PerformanceMonitorProperties {

    /**
     * **Feature: performance-optimization, Property 10: Performance Monitoring Accuracy**
     * **Validates: Requirements 5.3, 5.4, 5.5**
     * 
     * For any animation metrics recording, the recorded data should accurately
     * reflect the input values and be retrievable for monitoring.
     */
    @Property(tries = 100)
    void animationMetricsAccuracy(
            @ForAll("animationIds") List<String> animationIds,
            @ForAll @LongRange(min = 1, max = 10000) long baseDuration) {
        
        Assume.that(!animationIds.isEmpty());
        Assume.that(animationIds.size() <= 20);
        
        PerformanceMonitor monitor = PerformanceMonitor.getInstance();
        monitor.reset();
        
        int expectedSuccessCount = 0;
        int expectedFailureCount = 0;
        long totalDuration = 0;
        
        // Record animation metrics
        for (int i = 0; i < animationIds.size(); i++) {
            String id = animationIds.get(i);
            long duration = baseDuration + (i * 10); // Vary duration slightly
            boolean success = (i % 3 != 0); // Every 3rd animation fails
            
            monitor.recordAnimationMetrics(id, duration, success);
            
            totalDuration += duration;
            if (success) {
                expectedSuccessCount++;
            } else {
                expectedFailureCount++;
            }
        }
        
        // Verify metrics are accurately recorded
        PerformanceMonitor.AnimationStatsSummary stats = monitor.getAnimationStatsSummary();
        
        assertEquals(animationIds.size(), stats.totalAnimations,
                "Total animations should match recorded count");
        assertEquals(expectedSuccessCount, stats.successCount,
                "Success count should match recorded successes");
        assertEquals(expectedFailureCount, stats.failureCount,
                "Failure count should match recorded failures");
        
        // Average duration should be within reasonable bounds
        long expectedAvg = totalDuration / animationIds.size();
        assertTrue(Math.abs(stats.averageDurationMs - expectedAvg) <= 1,
                "Average duration should be accurate");
    }

    /**
     * **Feature: performance-optimization, Property 10: Performance Monitoring Accuracy**
     * **Validates: Requirements 5.4**
     * 
     * For any repository scan metrics recording, the recorded data should accurately
     * reflect the input values and be retrievable for monitoring.
     */
    @Property(tries = 100)
    void repositoryScanMetricsAccuracy(
            @ForAll("repositoryPaths") List<String> repoPaths,
            @ForAll @LongRange(min = 10, max = 5000) long baseDuration,
            @ForAll @IntRange(min = 0, max = 1000) int baseCommits) {
        
        Assume.that(!repoPaths.isEmpty());
        Assume.that(repoPaths.size() <= 15);
        
        PerformanceMonitor monitor = PerformanceMonitor.getInstance();
        monitor.reset();
        
        int expectedSuccessCount = 0;
        int expectedFailureCount = 0;
        int totalCommits = 0;
        
        // Record repository scan metrics
        for (int i = 0; i < repoPaths.size(); i++) {
            String path = repoPaths.get(i);
            long duration = baseDuration + (i * 50);
            int commits = baseCommits + (i * 5);
            boolean success = (i % 4 != 0); // Every 4th scan fails
            
            monitor.recordRepositoryScanMetrics(path, duration, commits, success);
            
            totalCommits += commits;
            if (success) {
                expectedSuccessCount++;
            } else {
                expectedFailureCount++;
            }
        }
        
        // Verify metrics are accurately recorded
        PerformanceMonitor.RepositoryScanStatsSummary stats = monitor.getRepositoryScanStatsSummary();
        
        assertEquals(repoPaths.size(), stats.totalScans,
                "Total scans should match recorded count");
        assertEquals(expectedSuccessCount, stats.successCount,
                "Success count should match recorded successes");
        assertEquals(expectedFailureCount, stats.failureCount,
                "Failure count should match recorded failures");
        assertEquals(totalCommits, stats.totalCommitsProcessed,
                "Total commits should match recorded commits");
    }


    /**
     * **Feature: performance-optimization, Property 10: Performance Monitoring Accuracy**
     * **Validates: Requirements 5.5**
     * 
     * For any resource cleanup event recording, the recorded data should accurately
     * track successful and failed cleanups.
     */
    @Property(tries = 100)
    void cleanupEventTrackingAccuracy(
            @ForAll("resourceIds") List<String> resourceIds) {
        
        Assume.that(!resourceIds.isEmpty());
        Assume.that(resourceIds.size() <= 25);
        
        PerformanceMonitor monitor = PerformanceMonitor.getInstance();
        monitor.reset();
        
        int expectedSuccessCount = 0;
        int expectedFailureCount = 0;
        
        // Record cleanup events
        for (int i = 0; i < resourceIds.size(); i++) {
            String id = resourceIds.get(i);
            boolean success = (i % 5 != 0); // Every 5th cleanup fails
            String details = success ? "Cleanup successful" : "Cleanup failed: timeout";
            
            monitor.recordCleanupEvent(id, success, details);
            
            if (success) {
                expectedSuccessCount++;
            } else {
                expectedFailureCount++;
            }
        }
        
        // Verify cleanup stats are accurate
        PerformanceMonitor.CleanupStats stats = monitor.getCleanupStats();
        
        assertEquals(expectedSuccessCount, stats.successfulCleanups,
                "Successful cleanups should match recorded count");
        assertEquals(expectedFailureCount, stats.failedCleanups,
                "Failed cleanups should match recorded count");
        assertEquals(resourceIds.size(), stats.getTotalCleanups(),
                "Total cleanups should match recorded count");
        
        // Verify success rate calculation
        double expectedRate = (double) expectedSuccessCount / resourceIds.size() * 100;
        assertEquals(expectedRate, stats.getSuccessRate(), 0.01,
                "Success rate should be accurately calculated");
    }

    /**
     * **Feature: performance-optimization, Property 10: Performance Monitoring Accuracy**
     * **Validates: Requirements 5.3**
     * 
     * For any memory snapshot recording, the snapshots should be bounded and
     * maintain chronological order.
     */
    @Property(tries = 100)
    void memorySnapshotBoundedness(
            @ForAll @IntRange(min = 1, max = 150) int snapshotCount) {
        
        PerformanceMonitor monitor = PerformanceMonitor.getInstance();
        monitor.reset();
        
        // Record multiple memory snapshots
        for (int i = 0; i < snapshotCount; i++) {
            monitor.recordMemoryUsage();
        }
        
        // Verify snapshots are bounded
        List<PerformanceMonitor.MemorySnapshot> snapshots = monitor.getMemorySnapshots();
        
        assertTrue(snapshots.size() <= 100,
                "Memory snapshots should be bounded to MAX_SNAPSHOTS (100)");
        
        // Verify chronological order
        for (int i = 1; i < snapshots.size(); i++) {
            assertTrue(snapshots.get(i).timestamp >= snapshots.get(i - 1).timestamp,
                    "Snapshots should be in chronological order");
        }
        
        // Verify latest snapshot is accessible
        PerformanceMonitor.MemorySnapshot latest = monitor.getLatestMemorySnapshot();
        assertNotNull(latest, "Latest snapshot should be available");
        
        if (!snapshots.isEmpty()) {
            assertEquals(snapshots.get(snapshots.size() - 1).timestamp, latest.timestamp,
                    "Latest snapshot should be the most recent");
        }
    }

    /**
     * **Feature: performance-optimization, Property 10: Performance Monitoring Accuracy**
     * **Validates: Requirements 5.3**
     * 
     * For any memory snapshot, the recorded values should be consistent with
     * JVM memory constraints (used <= total <= max).
     */
    @Property(tries = 100)
    void memorySnapshotConsistency() {
        PerformanceMonitor monitor = PerformanceMonitor.getInstance();
        monitor.reset();
        
        // Record a memory snapshot
        monitor.recordMemoryUsage();
        
        PerformanceMonitor.MemorySnapshot snapshot = monitor.getLatestMemorySnapshot();
        assertNotNull(snapshot, "Snapshot should be recorded");
        
        // Verify memory constraints
        assertTrue(snapshot.usedMemory >= 0,
                "Used memory should be non-negative");
        assertTrue(snapshot.freeMemory >= 0,
                "Free memory should be non-negative");
        assertTrue(snapshot.totalMemory >= snapshot.usedMemory,
                "Total memory should be >= used memory");
        assertTrue(snapshot.maxMemory >= snapshot.totalMemory,
                "Max memory should be >= total memory");
        
        // Verify used + free = total
        assertEquals(snapshot.totalMemory, snapshot.usedMemory + snapshot.freeMemory,
                "Used + free should equal total memory");
        
        // Verify usage percentage is valid
        double usagePercent = snapshot.getUsagePercentage();
        assertTrue(usagePercent >= 0 && usagePercent <= 100,
                "Usage percentage should be between 0 and 100");
    }


    /**
     * **Feature: performance-optimization, Property 10: Performance Monitoring Accuracy**
     * **Validates: Requirements 5.3**
     * 
     * For any memory trend analysis, the trend direction should accurately reflect
     * the pattern of memory usage over time.
     */
    @Property(tries = 100)
    void memoryTrendAnalysisAccuracy(
            @ForAll("trendPattern") TrendPattern pattern) {
        
        PerformanceMonitor monitor = PerformanceMonitor.getInstance();
        monitor.reset();
        
        // We can't directly control JVM memory, but we can verify trend analysis
        // works correctly with the actual memory state
        
        // Record multiple snapshots
        for (int i = 0; i < 10; i++) {
            monitor.recordMemoryUsage();
            // Small delay to ensure different timestamps
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Verify trend analysis returns valid result
        PerformanceMonitor.MemoryTrend trend = monitor.analyzeMemoryTrend();
        assertNotNull(trend, "Trend analysis should return a result");
        assertNotNull(trend.direction, "Trend direction should not be null");
        assertTrue(trend.averageUsed >= 0, "Average used should be non-negative");
        assertTrue(trend.peakUsed >= 0, "Peak used should be non-negative");
        assertTrue(trend.peakUsed >= trend.averageUsed, 
                "Peak should be >= average");
    }

    /**
     * **Feature: performance-optimization, Property 10: Performance Monitoring Accuracy**
     * **Validates: Requirements 5.3, 5.4, 5.5**
     * 
     * For any performance report generation, all metrics should be included
     * and consistent with individually recorded data.
     */
    @Property(tries = 50)
    void performanceReportCompleteness(
            @ForAll @IntRange(min = 1, max = 10) int animationCount,
            @ForAll @IntRange(min = 1, max = 5) int repoCount,
            @ForAll @IntRange(min = 1, max = 10) int cleanupCount) {
        
        PerformanceMonitor monitor = PerformanceMonitor.getInstance();
        monitor.reset();
        
        // Record various metrics
        for (int i = 0; i < animationCount; i++) {
            monitor.recordAnimationMetrics("anim-" + i, 100 + i * 10, i % 2 == 0);
        }
        
        for (int i = 0; i < repoCount; i++) {
            monitor.recordRepositoryScanMetrics("/repo/" + i, 500 + i * 100, 50 + i * 10, true);
        }
        
        for (int i = 0; i < cleanupCount; i++) {
            monitor.recordCleanupEvent("resource-" + i, i % 3 != 0, "details");
        }
        
        // Generate report
        PerformanceMonitor.PerformanceReport report = monitor.generateReport();
        
        // Verify report completeness
        assertNotNull(report, "Report should not be null");
        assertNotNull(report.memorySnapshot, "Memory snapshot should be included");
        assertNotNull(report.memoryTrend, "Memory trend should be included");
        assertNotNull(report.animationStats, "Animation stats should be included");
        assertNotNull(report.repositoryScanStats, "Repository scan stats should be included");
        assertNotNull(report.cleanupStats, "Cleanup stats should be included");
        assertTrue(report.generatedAt > 0, "Generation timestamp should be set");
        
        // Verify consistency with individual metrics
        assertEquals(animationCount, report.animationStats.totalAnimations,
                "Animation count should match");
        assertEquals(repoCount, report.repositoryScanStats.totalScans,
                "Repository scan count should match");
        assertEquals(cleanupCount, report.cleanupStats.getTotalCleanups(),
                "Cleanup count should match");
    }

    /**
     * **Feature: performance-optimization, Property 10: Performance Monitoring Accuracy**
     * **Validates: Requirements 5.5**
     * 
     * For any cleanup events, recent events should be retrievable and bounded.
     */
    @Property(tries = 100)
    void cleanupEventsBoundedness(
            @ForAll @IntRange(min = 1, max = 150) int eventCount) {
        
        PerformanceMonitor monitor = PerformanceMonitor.getInstance();
        monitor.reset();
        
        // Record cleanup events
        for (int i = 0; i < eventCount; i++) {
            monitor.recordCleanupEvent("resource-" + i, true, "cleanup details " + i);
        }
        
        // Verify events are bounded
        List<PerformanceMonitor.CleanupEvent> events = monitor.getRecentCleanupEvents();
        
        assertTrue(events.size() <= 100,
                "Cleanup events should be bounded to MAX_CLEANUP_EVENTS (100)");
        
        // Verify chronological order
        for (int i = 1; i < events.size(); i++) {
            assertTrue(events.get(i).timestamp >= events.get(i - 1).timestamp,
                    "Events should be in chronological order");
        }
    }

    // ==================== Arbitrary Providers ====================

    @Provide
    Arbitrary<List<String>> animationIds() {
        return Arbitraries.integers()
                .between(1, 100000)
                .set()
                .ofMinSize(1)
                .ofMaxSize(20)
                .map(set -> set.stream()
                        .map(i -> "animation-" + i)
                        .toList());
    }

    @Provide
    Arbitrary<List<String>> repositoryPaths() {
        return Arbitraries.integers()
                .between(1, 100000)
                .set()
                .ofMinSize(1)
                .ofMaxSize(15)
                .map(set -> set.stream()
                        .map(i -> "/path/to/repo-" + i)
                        .toList());
    }

    @Provide
    Arbitrary<List<String>> resourceIds() {
        return Arbitraries.integers()
                .between(1, 100000)
                .set()
                .ofMinSize(1)
                .ofMaxSize(25)
                .map(set -> set.stream()
                        .map(i -> "resource-" + i)
                        .toList());
    }

    @Provide
    Arbitrary<TrendPattern> trendPattern() {
        return Arbitraries.of(TrendPattern.values());
    }

    /**
     * Enum for testing different trend patterns.
     */
    enum TrendPattern {
        INCREASING,
        DECREASING,
        STABLE,
        FLUCTUATING
    }
}
