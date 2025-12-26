package com.tamagotchi.committracker.util;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comprehensive performance monitoring for the Tamagotchi Commit Tracker application.
 * Provides memory usage tracking, trend analysis, cache statistics, and animation metrics.
 * 
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5
 */
public class PerformanceMonitor {
    private static final Logger logger = Logger.getLogger(PerformanceMonitor.class.getName());
    
    // Singleton instance
    private static volatile PerformanceMonitor instance;
    
    // Memory tracking
    private final ConcurrentLinkedDeque<MemorySnapshot> memorySnapshots;
    private static final int MAX_SNAPSHOTS = 100;
    
    // Animation metrics
    private final Map<String, AnimationMetrics> animationMetrics;
    private final AtomicLong totalAnimationsRecorded;
    
    // Repository scanning metrics
    private final Map<String, RepositoryScanMetrics> repositoryMetrics;
    private final AtomicLong totalScansRecorded;
    
    // Cache metrics tracking
    private volatile SpriteCache.CacheStats lastCacheStats;
    private final ConcurrentLinkedDeque<CacheStatsSnapshot> cacheStatsHistory;
    private static final int MAX_CACHE_HISTORY = 50;
    
    // Resource cleanup tracking
    private final AtomicLong successfulCleanups;
    private final AtomicLong failedCleanups;
    private final ConcurrentLinkedDeque<CleanupEvent> cleanupEvents;
    private static final int MAX_CLEANUP_EVENTS = 100;
    
    private PerformanceMonitor() {
        this.memorySnapshots = new ConcurrentLinkedDeque<>();
        this.animationMetrics = new ConcurrentHashMap<>();
        this.totalAnimationsRecorded = new AtomicLong(0);
        this.repositoryMetrics = new ConcurrentHashMap<>();
        this.totalScansRecorded = new AtomicLong(0);
        this.cacheStatsHistory = new ConcurrentLinkedDeque<>();
        this.successfulCleanups = new AtomicLong(0);
        this.failedCleanups = new AtomicLong(0);
        this.cleanupEvents = new ConcurrentLinkedDeque<>();
        
        logger.info("PerformanceMonitor initialized");
    }
    
    /**
     * Gets the singleton instance of the PerformanceMonitor.
     */
    public static PerformanceMonitor getInstance() {
        if (instance == null) {
            synchronized (PerformanceMonitor.class) {
                if (instance == null) {
                    instance = new PerformanceMonitor();
                }
            }
        }
        return instance;
    }

    
    // ==================== Memory Tracking ====================
    
    /**
     * Records current memory usage snapshot.
     * Requirements: 5.1 - provide memory usage statistics for monitoring
     */
    public void recordMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        MemorySnapshot snapshot = new MemorySnapshot(
            System.currentTimeMillis(),
            usedMemory,
            totalMemory,
            maxMemory,
            freeMemory
        );
        
        memorySnapshots.addLast(snapshot);
        
        // Maintain bounded size
        while (memorySnapshots.size() > MAX_SNAPSHOTS) {
            memorySnapshots.pollFirst();
        }
        
        logger.fine("Memory snapshot recorded: " + snapshot);
    }
    
    /**
     * Gets the most recent memory snapshot.
     */
    public MemorySnapshot getLatestMemorySnapshot() {
        MemorySnapshot latest = memorySnapshots.peekLast();
        if (latest == null) {
            recordMemoryUsage();
            latest = memorySnapshots.peekLast();
        }
        return latest;
    }
    
    /**
     * Gets all memory snapshots for trend analysis.
     */
    public List<MemorySnapshot> getMemorySnapshots() {
        return new ArrayList<>(memorySnapshots);
    }
    
    /**
     * Analyzes memory usage trend over recorded snapshots.
     * Requirements: 5.1 - memory usage tracking and trend analysis
     */
    public MemoryTrend analyzeMemoryTrend() {
        List<MemorySnapshot> snapshots = getMemorySnapshots();
        
        if (snapshots.size() < 2) {
            return new MemoryTrend(TrendDirection.STABLE, 0, 0, 0);
        }
        
        // Calculate average memory usage
        long totalUsed = 0;
        for (MemorySnapshot snapshot : snapshots) {
            totalUsed += snapshot.usedMemory;
        }
        long averageUsed = totalUsed / snapshots.size();
        
        // Calculate trend by comparing first half to second half
        int midpoint = snapshots.size() / 2;
        long firstHalfAvg = 0;
        long secondHalfAvg = 0;
        
        for (int i = 0; i < midpoint; i++) {
            firstHalfAvg += snapshots.get(i).usedMemory;
        }
        firstHalfAvg /= midpoint;
        
        for (int i = midpoint; i < snapshots.size(); i++) {
            secondHalfAvg += snapshots.get(i).usedMemory;
        }
        secondHalfAvg /= (snapshots.size() - midpoint);
        
        // Determine trend direction
        long difference = secondHalfAvg - firstHalfAvg;
        double percentChange = (double) difference / firstHalfAvg * 100;
        
        TrendDirection direction;
        if (percentChange > 5) {
            direction = TrendDirection.INCREASING;
        } else if (percentChange < -5) {
            direction = TrendDirection.DECREASING;
        } else {
            direction = TrendDirection.STABLE;
        }
        
        // Calculate peak memory
        long peakMemory = snapshots.stream()
            .mapToLong(s -> s.usedMemory)
            .max()
            .orElse(0);
        
        return new MemoryTrend(direction, averageUsed, peakMemory, percentChange);
    }
    
    /**
     * Checks if memory pressure is high.
     */
    public boolean isMemoryPressureHigh() {
        MemorySnapshot latest = getLatestMemorySnapshot();
        if (latest == null) {
            return false;
        }
        
        // Consider pressure high if using more than 80% of max memory
        double usageRatio = (double) latest.usedMemory / latest.maxMemory;
        return usageRatio > 0.8;
    }

    
    // ==================== Animation Metrics ====================
    
    /**
     * Records animation execution metrics.
     * Requirements: 5.3 - monitor Timeline lifecycle and cleanup
     */
    public void recordAnimationMetrics(String animationId, long durationMs, boolean success) {
        AnimationMetrics metrics = new AnimationMetrics(
            animationId,
            System.currentTimeMillis(),
            durationMs,
            success
        );
        
        animationMetrics.put(animationId, metrics);
        totalAnimationsRecorded.incrementAndGet();
        
        // Limit stored metrics to prevent unbounded growth
        if (animationMetrics.size() > 1000) {
            // Remove oldest entries
            animationMetrics.entrySet().stream()
                .sorted((a, b) -> Long.compare(a.getValue().timestamp, b.getValue().timestamp))
                .limit(100)
                .forEach(e -> animationMetrics.remove(e.getKey()));
        }
        
        logger.fine("Animation metrics recorded: " + animationId + " (duration: " + durationMs + "ms, success: " + success + ")");
    }
    
    /**
     * Gets animation statistics summary.
     */
    public AnimationStatsSummary getAnimationStatsSummary() {
        if (animationMetrics.isEmpty()) {
            return new AnimationStatsSummary(0, 0, 0, 0, 0);
        }
        
        long totalDuration = 0;
        int successCount = 0;
        int failureCount = 0;
        long maxDuration = 0;
        long minDuration = Long.MAX_VALUE;
        
        for (AnimationMetrics metrics : animationMetrics.values()) {
            totalDuration += metrics.durationMs;
            if (metrics.success) {
                successCount++;
            } else {
                failureCount++;
            }
            maxDuration = Math.max(maxDuration, metrics.durationMs);
            minDuration = Math.min(minDuration, metrics.durationMs);
        }
        
        int totalCount = animationMetrics.size();
        long avgDuration = totalCount > 0 ? totalDuration / totalCount : 0;
        
        return new AnimationStatsSummary(
            totalCount,
            avgDuration,
            maxDuration,
            successCount,
            failureCount
        );
    }
    
    // ==================== Repository Scanning Metrics ====================
    
    /**
     * Records repository scanning performance metrics.
     * Requirements: 5.4 - log performance metrics and timing data
     */
    public void recordRepositoryScanMetrics(String repositoryPath, long durationMs, 
                                            int commitsProcessed, boolean success) {
        RepositoryScanMetrics metrics = new RepositoryScanMetrics(
            repositoryPath,
            System.currentTimeMillis(),
            durationMs,
            commitsProcessed,
            success
        );
        
        repositoryMetrics.put(repositoryPath, metrics);
        totalScansRecorded.incrementAndGet();
        
        logger.fine("Repository scan metrics recorded: " + repositoryPath + 
                   " (duration: " + durationMs + "ms, commits: " + commitsProcessed + ")");
    }
    
    /**
     * Gets repository scanning statistics summary.
     */
    public RepositoryScanStatsSummary getRepositoryScanStatsSummary() {
        if (repositoryMetrics.isEmpty()) {
            return new RepositoryScanStatsSummary(0, 0, 0, 0, 0);
        }
        
        long totalDuration = 0;
        int totalCommits = 0;
        int successCount = 0;
        int failureCount = 0;
        
        for (RepositoryScanMetrics metrics : repositoryMetrics.values()) {
            totalDuration += metrics.durationMs;
            totalCommits += metrics.commitsProcessed;
            if (metrics.success) {
                successCount++;
            } else {
                failureCount++;
            }
        }
        
        int totalScans = repositoryMetrics.size();
        long avgDuration = totalScans > 0 ? totalDuration / totalScans : 0;
        
        return new RepositoryScanStatsSummary(
            totalScans,
            avgDuration,
            totalCommits,
            successCount,
            failureCount
        );
    }

    
    // ==================== Cache Metrics ====================
    
    /**
     * Records cache statistics for monitoring.
     * Requirements: 5.2 - track hit ratios and eviction statistics
     */
    public void recordCacheMetrics(SpriteCache.CacheStats stats) {
        if (stats == null) {
            return;
        }
        
        this.lastCacheStats = stats;
        
        CacheStatsSnapshot snapshot = new CacheStatsSnapshot(
            System.currentTimeMillis(),
            stats.currentSize,
            stats.maxSize,
            stats.hits,
            stats.misses,
            stats.evictions,
            stats.hitRatio
        );
        
        cacheStatsHistory.addLast(snapshot);
        
        // Maintain bounded size
        while (cacheStatsHistory.size() > MAX_CACHE_HISTORY) {
            cacheStatsHistory.pollFirst();
        }
        
        logger.fine("Cache metrics recorded: " + stats);
    }
    
    /**
     * Gets the latest cache statistics.
     */
    public SpriteCache.CacheStats getLatestCacheStats() {
        return lastCacheStats;
    }
    
    /**
     * Gets cache statistics history for trend analysis.
     */
    public List<CacheStatsSnapshot> getCacheStatsHistory() {
        return new ArrayList<>(cacheStatsHistory);
    }
    
    /**
     * Calculates average cache hit ratio over recorded history.
     */
    public double getAverageCacheHitRatio() {
        List<CacheStatsSnapshot> history = getCacheStatsHistory();
        if (history.isEmpty()) {
            return 0.0;
        }
        
        double totalHitRatio = 0;
        for (CacheStatsSnapshot snapshot : history) {
            totalHitRatio += snapshot.hitRatio;
        }
        
        return totalHitRatio / history.size();
    }
    
    // ==================== Resource Cleanup Tracking ====================
    
    /**
     * Records a resource cleanup event.
     * Requirements: 5.5 - verify successful disposal and log any failures
     */
    public void recordCleanupEvent(String resourceId, boolean success, String details) {
        CleanupEvent event = new CleanupEvent(
            System.currentTimeMillis(),
            resourceId,
            success,
            details
        );
        
        cleanupEvents.addLast(event);
        
        if (success) {
            successfulCleanups.incrementAndGet();
        } else {
            failedCleanups.incrementAndGet();
            logger.warning("Resource cleanup failed: " + resourceId + " - " + details);
        }
        
        // Maintain bounded size
        while (cleanupEvents.size() > MAX_CLEANUP_EVENTS) {
            cleanupEvents.pollFirst();
        }
        
        logger.fine("Cleanup event recorded: " + resourceId + " (success: " + success + ")");
    }
    
    /**
     * Gets cleanup statistics.
     */
    public CleanupStats getCleanupStats() {
        return new CleanupStats(
            successfulCleanups.get(),
            failedCleanups.get()
        );
    }
    
    /**
     * Gets recent cleanup events.
     */
    public List<CleanupEvent> getRecentCleanupEvents() {
        return new ArrayList<>(cleanupEvents);
    }

    
    // ==================== Performance Report ====================
    
    /**
     * Generates a comprehensive performance report.
     * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5
     */
    public PerformanceReport generateReport() {
        // Ensure we have fresh memory data
        recordMemoryUsage();
        
        // Collect cache stats if available
        try {
            SpriteCache cache = SpriteCache.getInstance();
            recordCacheMetrics(cache.getCacheStats());
        } catch (Exception e) {
            logger.fine("Could not collect cache stats: " + e.getMessage());
        }
        
        return new PerformanceReport(
            getLatestMemorySnapshot(),
            analyzeMemoryTrend(),
            getAnimationStatsSummary(),
            getRepositoryScanStatsSummary(),
            lastCacheStats,
            getAverageCacheHitRatio(),
            getCleanupStats(),
            System.currentTimeMillis()
        );
    }
    
    /**
     * Resets all collected metrics.
     */
    public void reset() {
        memorySnapshots.clear();
        animationMetrics.clear();
        totalAnimationsRecorded.set(0);
        repositoryMetrics.clear();
        totalScansRecorded.set(0);
        cacheStatsHistory.clear();
        lastCacheStats = null;
        successfulCleanups.set(0);
        failedCleanups.set(0);
        cleanupEvents.clear();
        
        logger.info("PerformanceMonitor metrics reset");
    }
    
    // ==================== Data Classes ====================
    
    /**
     * Memory snapshot at a point in time.
     */
    public static class MemorySnapshot {
        public final long timestamp;
        public final long usedMemory;
        public final long totalMemory;
        public final long maxMemory;
        public final long freeMemory;
        
        public MemorySnapshot(long timestamp, long usedMemory, long totalMemory, 
                             long maxMemory, long freeMemory) {
            this.timestamp = timestamp;
            this.usedMemory = usedMemory;
            this.totalMemory = totalMemory;
            this.maxMemory = maxMemory;
            this.freeMemory = freeMemory;
        }
        
        public double getUsagePercentage() {
            return maxMemory > 0 ? (double) usedMemory / maxMemory * 100 : 0;
        }
        
        @Override
        public String toString() {
            return String.format("MemorySnapshot{used=%dMB, total=%dMB, max=%dMB, usage=%.1f%%}",
                usedMemory / (1024 * 1024),
                totalMemory / (1024 * 1024),
                maxMemory / (1024 * 1024),
                getUsagePercentage());
        }
    }
    
    /**
     * Memory usage trend analysis result.
     */
    public static class MemoryTrend {
        public final TrendDirection direction;
        public final long averageUsed;
        public final long peakUsed;
        public final double percentChange;
        
        public MemoryTrend(TrendDirection direction, long averageUsed, 
                         long peakUsed, double percentChange) {
            this.direction = direction;
            this.averageUsed = averageUsed;
            this.peakUsed = peakUsed;
            this.percentChange = percentChange;
        }
        
        @Override
        public String toString() {
            return String.format("MemoryTrend{direction=%s, avg=%dMB, peak=%dMB, change=%.1f%%}",
                direction, averageUsed / (1024 * 1024), peakUsed / (1024 * 1024), percentChange);
        }
    }
    
    /**
     * Trend direction enumeration.
     */
    public enum TrendDirection {
        INCREASING,
        DECREASING,
        STABLE
    }
    
    /**
     * Animation execution metrics.
     */
    public static class AnimationMetrics {
        public final String animationId;
        public final long timestamp;
        public final long durationMs;
        public final boolean success;
        
        public AnimationMetrics(String animationId, long timestamp, 
                               long durationMs, boolean success) {
            this.animationId = animationId;
            this.timestamp = timestamp;
            this.durationMs = durationMs;
            this.success = success;
        }
    }
    
    /**
     * Animation statistics summary.
     */
    public static class AnimationStatsSummary {
        public final int totalAnimations;
        public final long averageDurationMs;
        public final long maxDurationMs;
        public final int successCount;
        public final int failureCount;
        
        public AnimationStatsSummary(int totalAnimations, long averageDurationMs,
                                    long maxDurationMs, int successCount, int failureCount) {
            this.totalAnimations = totalAnimations;
            this.averageDurationMs = averageDurationMs;
            this.maxDurationMs = maxDurationMs;
            this.successCount = successCount;
            this.failureCount = failureCount;
        }
        
        public double getSuccessRate() {
            return totalAnimations > 0 ? (double) successCount / totalAnimations * 100 : 0;
        }
        
        @Override
        public String toString() {
            return String.format("AnimationStats{total=%d, avgDuration=%dms, successRate=%.1f%%}",
                totalAnimations, averageDurationMs, getSuccessRate());
        }
    }

    
    /**
     * Repository scan metrics.
     */
    public static class RepositoryScanMetrics {
        public final String repositoryPath;
        public final long timestamp;
        public final long durationMs;
        public final int commitsProcessed;
        public final boolean success;
        
        public RepositoryScanMetrics(String repositoryPath, long timestamp,
                                    long durationMs, int commitsProcessed, boolean success) {
            this.repositoryPath = repositoryPath;
            this.timestamp = timestamp;
            this.durationMs = durationMs;
            this.commitsProcessed = commitsProcessed;
            this.success = success;
        }
    }
    
    /**
     * Repository scan statistics summary.
     */
    public static class RepositoryScanStatsSummary {
        public final int totalScans;
        public final long averageDurationMs;
        public final int totalCommitsProcessed;
        public final int successCount;
        public final int failureCount;
        
        public RepositoryScanStatsSummary(int totalScans, long averageDurationMs,
                                         int totalCommitsProcessed, int successCount, int failureCount) {
            this.totalScans = totalScans;
            this.averageDurationMs = averageDurationMs;
            this.totalCommitsProcessed = totalCommitsProcessed;
            this.successCount = successCount;
            this.failureCount = failureCount;
        }
        
        public double getSuccessRate() {
            return totalScans > 0 ? (double) successCount / totalScans * 100 : 0;
        }
        
        @Override
        public String toString() {
            return String.format("RepositoryScanStats{scans=%d, avgDuration=%dms, commits=%d, successRate=%.1f%%}",
                totalScans, averageDurationMs, totalCommitsProcessed, getSuccessRate());
        }
    }
    
    /**
     * Cache statistics snapshot.
     */
    public static class CacheStatsSnapshot {
        public final long timestamp;
        public final int currentSize;
        public final int maxSize;
        public final long hits;
        public final long misses;
        public final long evictions;
        public final double hitRatio;
        
        public CacheStatsSnapshot(long timestamp, int currentSize, int maxSize,
                                 long hits, long misses, long evictions, double hitRatio) {
            this.timestamp = timestamp;
            this.currentSize = currentSize;
            this.maxSize = maxSize;
            this.hits = hits;
            this.misses = misses;
            this.evictions = evictions;
            this.hitRatio = hitRatio;
        }
    }
    
    /**
     * Resource cleanup event.
     */
    public static class CleanupEvent {
        public final long timestamp;
        public final String resourceId;
        public final boolean success;
        public final String details;
        
        public CleanupEvent(long timestamp, String resourceId, 
                           boolean success, String details) {
            this.timestamp = timestamp;
            this.resourceId = resourceId;
            this.success = success;
            this.details = details;
        }
    }
    
    /**
     * Cleanup statistics.
     */
    public static class CleanupStats {
        public final long successfulCleanups;
        public final long failedCleanups;
        
        public CleanupStats(long successfulCleanups, long failedCleanups) {
            this.successfulCleanups = successfulCleanups;
            this.failedCleanups = failedCleanups;
        }
        
        public long getTotalCleanups() {
            return successfulCleanups + failedCleanups;
        }
        
        public double getSuccessRate() {
            long total = getTotalCleanups();
            return total > 0 ? (double) successfulCleanups / total * 100 : 100;
        }
        
        @Override
        public String toString() {
            return String.format("CleanupStats{successful=%d, failed=%d, rate=%.1f%%}",
                successfulCleanups, failedCleanups, getSuccessRate());
        }
    }
    
    /**
     * Comprehensive performance report.
     */
    public static class PerformanceReport {
        public final MemorySnapshot memorySnapshot;
        public final MemoryTrend memoryTrend;
        public final AnimationStatsSummary animationStats;
        public final RepositoryScanStatsSummary repositoryScanStats;
        public final SpriteCache.CacheStats cacheStats;
        public final double averageCacheHitRatio;
        public final CleanupStats cleanupStats;
        public final long generatedAt;
        
        public PerformanceReport(MemorySnapshot memorySnapshot, MemoryTrend memoryTrend,
                                AnimationStatsSummary animationStats, 
                                RepositoryScanStatsSummary repositoryScanStats,
                                SpriteCache.CacheStats cacheStats, double averageCacheHitRatio,
                                CleanupStats cleanupStats, long generatedAt) {
            this.memorySnapshot = memorySnapshot;
            this.memoryTrend = memoryTrend;
            this.animationStats = animationStats;
            this.repositoryScanStats = repositoryScanStats;
            this.cacheStats = cacheStats;
            this.averageCacheHitRatio = averageCacheHitRatio;
            this.cleanupStats = cleanupStats;
            this.generatedAt = generatedAt;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Performance Report ===\n");
            sb.append("Generated: ").append(new java.util.Date(generatedAt)).append("\n\n");
            
            sb.append("Memory:\n");
            if (memorySnapshot != null) {
                sb.append("  ").append(memorySnapshot).append("\n");
            }
            if (memoryTrend != null) {
                sb.append("  ").append(memoryTrend).append("\n");
            }
            
            sb.append("\nAnimations:\n");
            if (animationStats != null) {
                sb.append("  ").append(animationStats).append("\n");
            }
            
            sb.append("\nRepository Scanning:\n");
            if (repositoryScanStats != null) {
                sb.append("  ").append(repositoryScanStats).append("\n");
            }
            
            sb.append("\nCache:\n");
            if (cacheStats != null) {
                sb.append("  ").append(cacheStats).append("\n");
            }
            sb.append("  Average Hit Ratio: ").append(String.format("%.1f%%", averageCacheHitRatio * 100)).append("\n");
            
            sb.append("\nResource Cleanup:\n");
            if (cleanupStats != null) {
                sb.append("  ").append(cleanupStats).append("\n");
            }
            
            return sb.toString();
        }
    }
}
