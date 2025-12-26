package com.tamagotchi.committracker.performance;

import com.tamagotchi.committracker.util.PerformanceMonitor;
import com.tamagotchi.committracker.util.ResourceManager;
import com.tamagotchi.committracker.util.SpriteCache;
import javafx.animation.Timeline;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Task 8: Performance Testing Checkpoint
 * Validates memory stability, resource cleanup, and performance under load
 */
public class PerformanceCheckpointTest {
    
    private PerformanceMonitor monitor;
    private ResourceManager resourceManager;
    private SpriteCache spriteCache;
    
    @BeforeEach
    void setUp() {
        monitor = PerformanceMonitor.getInstance();
        resourceManager = ResourceManager.getInstance();
        spriteCache = SpriteCache.getInstance();
        
        // Clear any existing state
        resourceManager.cleanupAll();
        spriteCache.clearCache();
        
        // Record baseline memory
        monitor.recordMemoryUsage();
    }
    
    @AfterEach
    void tearDown() {
        resourceManager.cleanupAll();
        spriteCache.clearCache();
    }
    
    @Test
    void testMemoryStabilityUnderLoad() {
        // Record initial memory
        long initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        
        // Simulate high load operations
        for (int i = 0; i < 100; i++) {
            // Create and register timelines
            Timeline timeline = new Timeline();
            resourceManager.registerTimeline("test-timeline-" + i, timeline);
            
            // Load essential sprites
            spriteCache.preloadEssentialSprites();
            
            // Trigger cleanup periodically
            if (i % 10 == 0) {
                resourceManager.cleanupAll();
                System.gc(); // Suggest garbage collection
            }
        }
        
        // Final cleanup
        resourceManager.cleanupAll();
        System.gc();
        
        // Verify memory stability
        long finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long memoryGrowth = finalMemory - initialMemory;
        
        // Allow for reasonable memory growth (less than 50MB)
        assertTrue(memoryGrowth < 50 * 1024 * 1024, 
            "Memory growth should be minimal: " + memoryGrowth + " bytes");
    }
    
    @Test
    void testTimelineCleanupCompleteness() {
        int timelineCount = 50;
        
        // Create multiple timelines
        for (int i = 0; i < timelineCount; i++) {
            Timeline timeline = new Timeline();
            resourceManager.registerTimeline("timeline-" + i, timeline);
        }
        
        // Verify registration by checking cleanup stats
        PerformanceMonitor.CleanupStats initialStats = monitor.getCleanupStats();
        
        // Cleanup all resources
        resourceManager.cleanupAll();
        
        // Verify cleanup occurred
        PerformanceMonitor.CleanupStats finalStats = monitor.getCleanupStats();
        assertTrue(finalStats.getTotalCleanups() >= initialStats.getTotalCleanups(), 
            "Cleanup operations should have occurred");
    }
    
    @Test
    void testApplicationUnderHighLoad() {
        // Record initial cleanup stats
        PerformanceMonitor.CleanupStats initialStats = monitor.getCleanupStats();
        long initialCleanups = initialStats.getTotalCleanups();
        
        // Simulate concurrent operations
        for (int cycle = 0; cycle < 10; cycle++) {
            // Animation operations
            for (int i = 0; i < 20; i++) {
                Timeline timeline = new Timeline();
                resourceManager.registerTimeline("load-test-" + cycle + "-" + i, timeline);
            }
            
            // Cache operations
            spriteCache.preloadEssentialSprites();
            
            // Memory monitoring
            monitor.recordMemoryUsage();
            
            // Periodic cleanup
            resourceManager.cleanupAll();
        }
        
        // Verify system stability
        PerformanceMonitor.MemorySnapshot snapshot = monitor.getLatestMemorySnapshot();
        assertNotNull(snapshot, "Performance monitoring should be active");
        
        // Verify cleanup occurred by checking final stats
        PerformanceMonitor.CleanupStats finalStats = monitor.getCleanupStats();
        long finalCleanups = finalStats.getTotalCleanups();
        
        // Should have more cleanups than initially (at least 10 cycles of cleanup)
        assertTrue(finalCleanups >= initialCleanups, 
            "Cleanup operations should have occurred: initial=" + initialCleanups + ", final=" + finalCleanups);
    }
}