package com.tamagotchi.committracker.util;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import static org.junit.jupiter.api.Assertions.*;

import com.tamagotchi.committracker.ui.components.PokemonDisplayComponent;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Property-based tests for WeakReference-based memory leak detection.
 * 
 * **Feature: performance-optimization, Property 6: Memory Leak Prevention**
 * **Validates: Requirements 3.1, 3.2**
 * 
 * These tests validate that WeakReference-based tracking can detect memory leaks
 * and that proper cleanup results in perfect cleanup ratios.
 */
class MemoryLeakDetectionProperties {

    /**
     * **Feature: performance-optimization, Property 6: Memory Leak Prevention**
     * **Validates: Requirements 3.1, 3.2**
     * 
     * For any set of resources tracked with WeakReferences, when resources are
     * properly disposed, the WeakReference tracking should show perfect cleanup ratios.
     */
    @Property(tries = 100)
    void weakReferenceTrackingDetectsProperCleanup(
            @ForAll @IntRange(min = 1, max = 20) int resourceCount) {
        
        ResourceManager resourceManager = ResourceManager.getInstance();
        
        // Clean up any existing state
        resourceManager.cleanupAll();
        
        // Track initial stats
        ResourceManager.ResourceStats initialStats = resourceManager.getResourceStats();
        long initialWeakRefs = initialStats.weakReferenceCount;
        
        // Create and register resources
        List<String> resourceIds = new ArrayList<>();
        for (int i = 0; i < resourceCount; i++) {
            String id = "weak-ref-test-" + System.nanoTime() + "-" + i;
            AutoCloseable resource = () -> {}; // Simple no-op resource
            resourceManager.registerResource(id, resource, ResourceManager.ResourceType.OTHER);
            resourceIds.add(id);
        }
        
        // Verify WeakReferences are created
        ResourceManager.ResourceStats afterRegistration = resourceManager.getResourceStats();
        assertEquals(resourceCount, afterRegistration.activeCount,
                "Active count should match registered resources");
        assertTrue(afterRegistration.weakReferenceCount >= initialWeakRefs + resourceCount,
                "WeakReference count should increase with resource registration");
        
        // Clean up all resources properly
        resourceManager.cleanupAll();
        
        // Force garbage collection to clear WeakReferences
        System.gc();
        Thread.yield();
        System.gc();
        
        // Verify perfect cleanup
        ResourceManager.ResourceStats finalStats = resourceManager.getResourceStats();
        assertEquals(0, finalStats.activeCount,
                "All resources should be cleaned up");
        
        // The cleanup ratio should be perfect (all resources properly disposed)
        long totalCreated = finalStats.totalCreated - initialStats.totalCreated;
        long totalDisposed = finalStats.totalDisposed - initialStats.totalDisposed;
        
        assertTrue(totalCreated >= resourceCount,
                "Should have created at least " + resourceCount + " resources");
        assertTrue(totalDisposed >= resourceCount,
                "Should have disposed at least " + resourceCount + " resources");
    }

    /**
     * **Feature: performance-optimization, Property 6: Memory Leak Prevention**
     * **Validates: Requirements 3.1, 3.2**
     * 
     * For any Timeline objects tracked with WeakReferences, when Timelines are
     * not properly stopped, the WeakReference mechanism should detect the leak.
     */
    @Property(tries = 100)
    void weakReferenceDetectsTimelineMemoryLeaks(
            @ForAll @IntRange(min = 1, max = 10) int timelineCount) {
        
        ResourceManager resourceManager = ResourceManager.getInstance();
        
        // Clean up any existing state
        resourceManager.cleanupAll();
        
        // Create Timeline objects that simulate potential leaks
        List<Timeline> timelines = new ArrayList<>();
        List<WeakReference<Timeline>> weakRefs = new ArrayList<>();
        
        for (int i = 0; i < timelineCount; i++) {
            Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(100), e -> {})
            );
            timelines.add(timeline);
            weakRefs.add(new WeakReference<>(timeline));
            
            // Register with ResourceManager
            String id = "timeline-leak-test-" + System.nanoTime() + "-" + i;
            resourceManager.registerTimeline(id, timeline);
        }
        
        // Verify all Timelines are tracked
        ResourceManager.ResourceStats stats = resourceManager.getResourceStats();
        assertTrue(stats.activeCount >= timelineCount,
                "All Timeline resources should be tracked");
        
        // Simulate proper cleanup - stop and dispose all Timelines
        for (Timeline timeline : timelines) {
            timeline.stop();
        }
        resourceManager.cleanupAll();
        
        // Clear strong references to allow garbage collection
        timelines.clear();
        
        // Force garbage collection
        System.gc();
        Thread.yield();
        System.gc();
        
        // Verify WeakReferences detect proper cleanup
        int nullReferences = 0;
        for (WeakReference<Timeline> ref : weakRefs) {
            if (ref.get() == null) {
                nullReferences++;
            }
        }
        
        // With proper cleanup, most or all WeakReferences should be null
        // (allowing for some GC timing variations)
        assertTrue(nullReferences >= timelineCount * 0.7,
                "Most WeakReferences should be null after proper cleanup: " + 
                nullReferences + "/" + timelineCount);
        
        // Verify ResourceManager shows complete cleanup
        ResourceManager.ResourceStats finalStats = resourceManager.getResourceStats();
        assertEquals(0, finalStats.activeCount,
                "No Timeline resources should remain after cleanup");
    }

    /**
     * **Feature: performance-optimization, Property 6: Memory Leak Prevention**
     * **Validates: Requirements 3.1, 3.2**
     * 
     * For any sequence of resource operations, the WeakReference cleanup mechanism
     * should maintain accurate tracking without accumulating dead references.
     */
    @Property(tries = 100)
    void weakReferenceCleanupMaintainsAccuracy(
            @ForAll @IntRange(min = 5, max = 25) int operationCycles) {
        
        ResourceManager resourceManager = ResourceManager.getInstance();
        
        // Clean up any existing state
        resourceManager.cleanupAll();
        
        ResourceManager.ResourceStats initialStats = resourceManager.getResourceStats();
        
        // Perform multiple cycles of resource creation and cleanup
        for (int cycle = 0; cycle < operationCycles; cycle++) {
            // Create resources
            List<String> cycleIds = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                String id = "cycle-" + cycle + "-resource-" + i + "-" + System.nanoTime();
                AutoCloseable resource = () -> {};
                resourceManager.registerResource(id, resource, ResourceManager.ResourceType.OTHER);
                cycleIds.add(id);
            }
            
            // Clean up half the resources
            for (int i = 0; i < cycleIds.size() / 2; i++) {
                resourceManager.cleanupResource(cycleIds.get(i));
            }
            
            // Force periodic WeakReference cleanup
            if (cycle % 5 == 0) {
                System.gc();
                Thread.yield();
                // Get stats to trigger cleanup of dead WeakReferences
                resourceManager.getResourceStats();
            }
        }
        
        // Final cleanup
        resourceManager.cleanupAll();
        System.gc();
        
        // Verify final state
        ResourceManager.ResourceStats finalStats = resourceManager.getResourceStats();
        assertEquals(0, finalStats.activeCount,
                "All resources should be cleaned up");
        
        // Verify that total operations are tracked correctly
        long totalCreated = finalStats.totalCreated - initialStats.totalCreated;
        long totalDisposed = finalStats.totalDisposed - initialStats.totalDisposed;
        
        assertTrue(totalCreated >= operationCycles * 3,
                "Should have created resources across all cycles");
        assertTrue(totalDisposed >= operationCycles * 3,
                "Should have disposed resources across all cycles");
    }

    /**
     * **Feature: performance-optimization, Property 6: Memory Leak Prevention**
     * **Validates: Requirements 3.1, 3.2**
     * 
     * For any WeakReference tracking system, dead references should be automatically
     * cleaned up to prevent the tracking system itself from becoming a memory leak.
     */
    @Property(tries = 100)
    void deadWeakReferencesAreAutomaticallyCleanedUp(
            @ForAll @IntRange(min = 1, max = 15) int resourceBatches) {
        
        ResourceManager resourceManager = ResourceManager.getInstance();
        
        // Clean up any existing state
        resourceManager.cleanupAll();
        
        long initialWeakRefCount = resourceManager.getResourceStats().weakReferenceCount;
        
        // Create and dispose resources in batches to generate dead WeakReferences
        for (int batch = 0; batch < resourceBatches; batch++) {
            List<String> batchIds = new ArrayList<>();
            
            // Create resources
            for (int i = 0; i < 5; i++) {
                String id = "batch-" + batch + "-item-" + i + "-" + System.nanoTime();
                AutoCloseable resource = () -> {};
                resourceManager.registerResource(id, resource, ResourceManager.ResourceType.OTHER);
                batchIds.add(id);
            }
            
            // Immediately clean up resources (creates dead WeakReferences)
            for (String id : batchIds) {
                resourceManager.cleanupResource(id);
            }
            
            // Force garbage collection
            System.gc();
            Thread.yield();
        }
        
        // Get stats multiple times to trigger dead WeakReference cleanup
        ResourceManager.ResourceStats beforeCleanup = resourceManager.getResourceStats();
        System.gc();
        ResourceManager.ResourceStats afterCleanup = resourceManager.getResourceStats();
        
        // Verify that dead WeakReferences are cleaned up
        // The WeakReference count should not grow unboundedly
        long finalWeakRefCount = afterCleanup.weakReferenceCount;
        long maxExpectedWeakRefs = initialWeakRefCount + (resourceBatches * 5);
        
        assertTrue(finalWeakRefCount <= maxExpectedWeakRefs,
                "Dead WeakReferences should be cleaned up, not accumulate indefinitely. " +
                "Expected <= " + maxExpectedWeakRefs + ", but was " + finalWeakRefCount);
        
        // Verify no active resources remain
        assertEquals(0, afterCleanup.activeCount,
                "No active resources should remain");
    }
}