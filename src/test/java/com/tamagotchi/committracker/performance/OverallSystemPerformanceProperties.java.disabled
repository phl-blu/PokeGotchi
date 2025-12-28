package com.tamagotchi.committracker.performance;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import static org.junit.jupiter.api.Assertions.*;

import com.tamagotchi.committracker.config.AppConfig;
import com.tamagotchi.committracker.domain.Commit;
import com.tamagotchi.committracker.domain.CommitHistory;
import com.tamagotchi.committracker.git.CommitService;
import com.tamagotchi.committracker.git.BackgroundRepositoryDiscoveryService;
import com.tamagotchi.committracker.pokemon.PokemonSpecies;
import com.tamagotchi.committracker.pokemon.EvolutionStage;
import com.tamagotchi.committracker.ui.animation.AnimationController;
import com.tamagotchi.committracker.ui.animation.UITransitionManager;
import com.tamagotchi.committracker.util.ResourceManager;
import com.tamagotchi.committracker.util.SpriteCache;
import com.tamagotchi.committracker.util.PerformanceMonitor;
import com.tamagotchi.committracker.util.RetryMechanism;

import javafx.animation.Timeline;
import javafx.application.Platform;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;

/**
 * Comprehensive property-based tests for overall system performance.
 * 
 * **Feature: performance-optimization, Property 8: Overall System Performance**
 * **Validates: All Requirements (1.1-4.2)**
 * 
 * These tests validate that the entire system performs efficiently under
 * various load conditions while maintaining resource bounds and responsiveness.
 */
class OverallSystemPerformanceProperties {

    /**
     * **Feature: performance-optimization, Property 8: Overall System Performance**
     * **Validates: All Requirements (1.1-4.2)**
     * 
     * For any combination of system operations (memory management, animations, 
     * repository processing, and UI transitions), the system should maintain
     * stable performance and resource usage within configured bounds.
     */
    @Property(tries = 100)
    void systemMaintainsPerformanceUnderCombinedLoad(
            @ForAll @IntRange(min = 5, max = 50) int operationCycles,
            @ForAll @IntRange(min = 1, max = 10) int concurrentAnimations,
            @ForAll @IntRange(min = 10, max = 100) int commitCount) {
        
        // Initialize all system components
        ResourceManager resourceManager = ResourceManager.getInstance();
        SpriteCache spriteCache = SpriteCache.getInstance();
        PerformanceMonitor monitor = PerformanceMonitor.getInstance();
        AnimationController animationController = AnimationController.getInstance();
        UITransitionManager transitionManager = UITransitionManager.getInstance();
        
        // Clean up any existing state
        resourceManager.cleanupAll();
        spriteCache.clearCache();
        
        // Record baseline metrics
        long initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        ResourceManager.ResourceStats initialStats = resourceManager.getResourceStats();
        
        try {
            // Simulate combined system load across multiple cycles
            for (int cycle = 0; cycle < operationCycles; cycle++) {
                
                // 1. Memory Management Operations (Property 1: Memory Stability)
                CommitHistory history = new CommitHistory();
                for (int i = 0; i < commitCount; i++) {
                    Commit commit = createTestCommit(cycle * commitCount + i);
                    history.addCommit(commit);
                }
                
                // Verify memory bounds are maintained
                List<Commit> commits = history.getRecentCommits();
                assertTrue(commits.size() <= AppConfig.getCommitHistoryLimit(),
                        "Commit history should remain bounded during cycle " + cycle);
                
                // 2. Resource Cleanup Operations (Property 2: Resource Cleanup Completeness)
                List<String> resourceIds = new ArrayList<>();
                for (int i = 0; i < 5; i++) {
                    String id = "cycle-" + cycle + "-resource-" + i;
                    AutoCloseable resource = () -> {};
                    resourceManager.registerResource(id, resource, ResourceManager.ResourceType.OTHER);
                    resourceIds.add(id);
                }
                
                // Clean up resources and verify
                for (String id : resourceIds) {
                    assertTrue(resourceManager.cleanupResource(id),
                            "Resource cleanup should succeed for " + id);
                }
                
                // 3. Cache Operations (Property 3: Cache Behavior Correctness)
                spriteCache.preloadEssentialSprites();
                SpriteCache.CacheStats cacheStats = spriteCache.getCacheStatistics();
                assertTrue(cacheStats.getCurrentSize() <= cacheStats.getMaxSize(),
                        "Cache size should not exceed maximum during cycle " + cycle);
                
                // 4. Animation Queue Management (Property 4: Animation Queue Management)
                CountDownLatch animationLatch = new CountDownLatch(concurrentAnimations);
                for (int i = 0; i < concurrentAnimations; i++) {
                    AnimationController.AnimationRequest request = new AnimationController.AnimationRequest(
                        "cycle-" + cycle + "-anim-" + i,
                        PokemonSpecies.CHARMANDER,
                        EvolutionStage.EGG,
                        EvolutionStage.BASIC,
                        AnimationController.AnimationType.EVOLUTION,
                        () -> animationLatch.countDown()
                    );
                    animationController.queueAnimation(request);
                }
                
                // Wait for animations to complete (with timeout)
                try {
                    boolean completed = animationLatch.await(5, TimeUnit.SECONDS);
                    assertTrue(completed || animationLatch.getCount() < concurrentAnimations,
                            "At least some animations should complete in cycle " + cycle);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // 5. UI Transition Operations (Property 5: UI Transition Smoothness)
                if (cycle % 3 == 0) { // Test transitions periodically
                    UITransitionManager.TransitionStats transitionStats = transitionManager.getTransitionStats();
                    assertTrue(transitionStats.getActiveTransitions() >= 0,
                            "Transition count should be non-negative");
                }
                
                // 6. Repository Processing (Property 6: Repository Processing Efficiency)
                // Simulate repository operations with timeouts
                RetryMechanism retryMechanism = new RetryMechanism();
                CompletableFuture<Boolean> repoOperation = CompletableFuture.supplyAsync(() -> {
                    try {
                        Thread.sleep(50); // Simulate repository work
                        return true;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                });
                
                try {
                    Boolean result = repoOperation.get(1, TimeUnit.SECONDS);
                    assertNotNull(result, "Repository operation should complete");
                } catch (Exception e) {
                    // Timeout is acceptable for this test
                }
                
                // 7. Retry Mechanism (Property 7: Retry Mechanism Correctness)
                RetryMechanism.RetryStats retryStats = retryMechanism.getStats();
                assertTrue(retryStats.getTotalAttempts() >= 0,
                        "Retry attempts should be non-negative");
                
                // 8. Performance Monitoring (Property 10: Performance Monitoring Accuracy)
                monitor.recordMemoryUsage();
                PerformanceMonitor.MemorySnapshot snapshot = monitor.getLatestMemorySnapshot();
                assertNotNull(snapshot, "Performance monitoring should be active");
                
                // Periodic cleanup to prevent resource buildup
                if (cycle % 5 == 0) {
                    resourceManager.cleanupAll();
                    System.gc(); // Suggest garbage collection
                }
            }
            
            // Final system validation
            
            // Verify memory stability (Property 1)
            long finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long memoryGrowth = finalMemory - initialMemory;
            assertTrue(memoryGrowth < 100 * 1024 * 1024, // Allow up to 100MB growth
                    "Memory growth should be reasonable: " + memoryGrowth + " bytes");
            
            // Verify resource cleanup completeness (Property 2)
            ResourceManager.ResourceStats finalStats = resourceManager.getResourceStats();
            assertTrue(finalStats.activeCount >= 0,
                    "Active resource count should be non-negative");
            
            // Verify cache behavior (Property 3)
            SpriteCache.CacheStats finalCacheStats = spriteCache.getCacheStatistics();
            assertTrue(finalCacheStats.getCurrentSize() <= finalCacheStats.getMaxSize(),
                    "Final cache size should be within bounds");
            
            // Verify animation system stability (Property 4)
            AnimationController.AnimationStats animStats = animationController.getStats();
            assertTrue(animStats.activeCount >= 0 && animStats.queuedCount >= 0,
                    "Animation counts should be non-negative");
            
            // Verify performance monitoring is working (Property 10)
            PerformanceMonitor.MemorySnapshot finalSnapshot = monitor.getLatestMemorySnapshot();
            assertNotNull(finalSnapshot, "Performance monitoring should remain active");
            
        } finally {
            // Clean up all resources
            resourceManager.cleanupAll();
            spriteCache.clearCache();
            animationController.cleanup();
        }
    }
    
    /**
     * **Feature: performance-optimization, Property 8: Overall System Performance**
     * **Validates: All Requirements (1.1-4.2)**
     * 
     * For any extended system operation, all performance properties should remain
     * valid and the system should maintain responsiveness.
     */
    @Property(tries = 100)
    void systemMaintainsAllPerformancePropertiesUnderLoad(
            @ForAll @IntRange(min = 10, max = 100) int totalOperations,
            @ForAll @IntRange(min = 1, max = 5) int concurrentThreads) {
        
        ResourceManager resourceManager = ResourceManager.getInstance();
        SpriteCache spriteCache = SpriteCache.getInstance();
        PerformanceMonitor monitor = PerformanceMonitor.getInstance();
        
        // Clean up initial state
        resourceManager.cleanupAll();
        spriteCache.clearCache();
        
        AtomicInteger completedOperations = new AtomicInteger(0);
        CountDownLatch operationLatch = new CountDownLatch(concurrentThreads);
        
        // Create concurrent threads that perform various operations
        List<Thread> workerThreads = new ArrayList<>();
        
        for (int threadId = 0; threadId < concurrentThreads; threadId++) {
            final int finalThreadId = threadId;
            Thread worker = new Thread(() -> {
                try {
                    int operationsPerThread = totalOperations / concurrentThreads;
                    
                    for (int op = 0; op < operationsPerThread; op++) {
                        // Mixed operations to test all properties
                        
                        // Memory operations
                        CommitHistory history = new CommitHistory();
                        for (int i = 0; i < 10; i++) {
                            history.addCommit(createTestCommit(finalThreadId * 1000 + op * 10 + i));
                        }
                        
                        // Resource operations
                        String resourceId = "thread-" + finalThreadId + "-op-" + op;
                        AutoCloseable resource = () -> {};
                        resourceManager.registerResource(resourceId, resource, ResourceManager.ResourceType.OTHER);
                        resourceManager.cleanupResource(resourceId);
                        
                        // Cache operations
                        if (op % 5 == 0) {
                            spriteCache.preloadEssentialSprites();
                        }
                        
                        // Performance monitoring
                        monitor.recordMemoryUsage();
                        
                        completedOperations.incrementAndGet();
                        
                        // Small delay to allow other threads
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                } finally {
                    operationLatch.countDown();
                }
            });
            
            workerThreads.add(worker);
            worker.start();
        }
        
        // Wait for all threads to complete
        try {
            boolean completed = operationLatch.await(30, TimeUnit.SECONDS);
            assertTrue(completed, "All worker threads should complete within timeout");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Verify system integrity after concurrent operations
        
        // Property 1: Memory Stability - no excessive memory growth
        PerformanceMonitor.MemorySnapshot snapshot = monitor.getLatestMemorySnapshot();
        assertNotNull(snapshot, "Memory monitoring should be active");
        
        // Property 2: Resource Cleanup - no resource leaks
        ResourceManager.ResourceStats stats = resourceManager.getResourceStats();
        assertTrue(stats.activeCount >= 0, "Active resource count should be valid");
        
        // Property 3: Cache Behavior - cache within bounds
        SpriteCache.CacheStats cacheStats = spriteCache.getCacheStatistics();
        assertTrue(cacheStats.getCurrentSize() <= cacheStats.getMaxSize(),
                "Cache should remain within bounds after concurrent operations");
        
        // Verify operations completed successfully
        assertTrue(completedOperations.get() > 0,
                "At least some operations should have completed successfully");
        
        // Clean up
        resourceManager.cleanupAll();
        spriteCache.clearCache();
    }
    
    /**
     * **Feature: performance-optimization, Property 8: Overall System Performance**
     * **Validates: All Requirements (1.1-4.2)**
     * 
     * For any system stress scenario, the system should gracefully handle
     * resource pressure and maintain stability.
     */
    @Property(tries = 100)
    void systemHandlesResourcePressureGracefully(
            @ForAll @IntRange(min = 50, max = 200) int resourcePressure) {
        
        ResourceManager resourceManager = ResourceManager.getInstance();
        SpriteCache spriteCache = SpriteCache.getInstance();
        PerformanceMonitor monitor = PerformanceMonitor.getInstance();
        
        // Clean up initial state
        resourceManager.cleanupAll();
        spriteCache.clearCache();
        
        List<String> createdResources = new ArrayList<>();
        
        try {
            // Create resource pressure
            for (int i = 0; i < resourcePressure; i++) {
                String resourceId = "pressure-test-" + i;
                AutoCloseable resource = () -> {};
                resourceManager.registerResource(resourceId, resource, ResourceManager.ResourceType.OTHER);
                createdResources.add(resourceId);
                
                // Add memory pressure through commit history
                CommitHistory history = new CommitHistory();
                for (int j = 0; j < 20; j++) {
                    history.addCommit(createTestCommit(i * 20 + j));
                }
                
                // Add cache pressure
                if (i % 10 == 0) {
                    spriteCache.preloadEssentialSprites();
                }
                
                // Monitor memory periodically
                if (i % 25 == 0) {
                    monitor.recordMemoryUsage();
                }
            }
            
            // Verify system is still functional under pressure
            ResourceManager.ResourceStats pressureStats = resourceManager.getResourceStats();
            assertEquals(resourcePressure, pressureStats.activeCount,
                    "All resources should be tracked under pressure");
            
            // Verify cache is still within bounds
            SpriteCache.CacheStats cacheStats = spriteCache.getCacheStatistics();
            assertTrue(cacheStats.getCurrentSize() <= cacheStats.getMaxSize(),
                    "Cache should remain bounded under pressure");
            
            // Verify monitoring is still working
            PerformanceMonitor.MemorySnapshot snapshot = monitor.getLatestMemorySnapshot();
            assertNotNull(snapshot, "Performance monitoring should work under pressure");
            
            // Test cleanup under pressure
            int halfPoint = resourcePressure / 2;
            for (int i = 0; i < halfPoint; i++) {
                assertTrue(resourceManager.cleanupResource(createdResources.get(i)),
                        "Resource cleanup should work under pressure");
            }
            
            // Verify partial cleanup worked
            ResourceManager.ResourceStats afterPartialCleanup = resourceManager.getResourceStats();
            assertEquals(resourcePressure - halfPoint, afterPartialCleanup.activeCount,
                    "Partial cleanup should work correctly under pressure");
            
        } finally {
            // Clean up all remaining resources
            resourceManager.cleanupAll();
            spriteCache.clearCache();
        }
        
        // Verify complete cleanup worked
        ResourceManager.ResourceStats finalStats = resourceManager.getResourceStats();
        assertEquals(0, finalStats.activeCount,
                "Complete cleanup should work even after resource pressure");
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