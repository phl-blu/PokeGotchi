package com.tamagotchi.committracker.util;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;
import net.jqwik.api.Combinators;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map;

/**
 * Property-based tests for ResourceManager resource lifecycle management.
 * 
 * **Feature: performance-optimization, Property 2: Resource Cleanup Completeness**
 * **Validates: Requirements 1.2, 1.5**
 * 
 * These tests validate that resources are properly tracked, registered, and cleaned up
 * to prevent memory leaks and ensure proper resource lifecycle management.
 */
class ResourceManagerProperties {

    /**
     * **Feature: performance-optimization, Property 2: Resource Cleanup Completeness**
     * **Validates: Requirements 1.2, 1.5**
     * 
     * For any resource creation operation, when the resource is no longer needed,
     * it should be properly disposed and removed from tracking.
     */
    @Property(tries = 100)
    void resourceCleanupCompleteness(
            @ForAll("uniqueResourceIds") List<String> uniqueIds) {
        
        Assume.that(!uniqueIds.isEmpty());
        Assume.that(uniqueIds.size() <= 10); // Limit size for performance
        
        ResourceManager resourceManager = ResourceManager.getInstance();
        
        // Clean up any existing state first
        resourceManager.cleanupAll();
        
        // Track cleanup calls for verification - map ID to cleanup flag
        Map<String, AtomicBoolean> cleanupTracking = new java.util.HashMap<>();
        
        // Register resources with cleanup tracking
        for (String id : uniqueIds) {
            AtomicBoolean wasCleanedUp = new AtomicBoolean(false);
            
            // Create a test resource that tracks cleanup
            AutoCloseable testResource = () -> wasCleanedUp.set(true);
            
            // Register the resource
            resourceManager.registerResource(id, testResource, ResourceManager.ResourceType.OTHER);
            cleanupTracking.put(id, wasCleanedUp);
        }
        
        // Verify all resources are tracked
        ResourceManager.ResourceStats initialStats = resourceManager.getResourceStats();
        assertEquals(uniqueIds.size(), initialStats.activeCount,
                "All registered resources should be tracked");
        
        // Clean up each resource individually
        for (String id : uniqueIds) {
            AtomicBoolean wasCleanedUp = cleanupTracking.get(id);
            
            // Cleanup should succeed
            boolean cleanupResult = resourceManager.cleanupResource(id);
            assertTrue(cleanupResult, "Cleanup should succeed for registered resource: " + id);
            
            // Verify the resource's close method was called
            assertTrue(wasCleanedUp.get(), 
                    "Resource cleanup method should be called for: " + id);
        }
        
        // Verify resources are no longer tracked
        Set<String> activeIds = resourceManager.getActiveResourceIds();
        for (String id : uniqueIds) {
            assertFalse(activeIds.contains(id), 
                    "Resource should no longer be tracked after cleanup: " + id);
        }
        
        // Verify statistics reflect the cleanup
        ResourceManager.ResourceStats finalStats = resourceManager.getResourceStats();
        assertEquals(0, finalStats.activeCount,
                "Active count should be 0 after all cleanups");
    }
    
    /**
     * **Feature: performance-optimization, Property 2: Resource Cleanup Completeness**
     * **Validates: Requirements 1.2, 1.5**
     * 
     * For any set of resources, when cleanupAll() is called, all registered resources
     * should be properly disposed and tracking should be cleared.
     */
    @Property(tries = 100)
    void cleanupAllCompleteness(
            @ForAll("uniqueResourceIds") List<String> resourceIds) {
        
        Assume.that(!resourceIds.isEmpty());
        Assume.that(resourceIds.size() <= 10); // Limit size for performance
        
        ResourceManager resourceManager = ResourceManager.getInstance();
        
        // Clean up any existing state first
        resourceManager.cleanupAll();
        
        // Track cleanup calls for verification - map ID to cleanup flag
        Map<String, AtomicBoolean> cleanupTracking = new java.util.HashMap<>();
        
        // Register resources with cleanup tracking
        for (String id : resourceIds) {
            AtomicBoolean wasCleanedUp = new AtomicBoolean(false);
            
            // Create a test resource that tracks cleanup
            AutoCloseable testResource = () -> wasCleanedUp.set(true);
            
            // Register the resource
            resourceManager.registerResource(id, testResource, ResourceManager.ResourceType.OTHER);
            cleanupTracking.put(id, wasCleanedUp);
        }
        
        // Verify resources are tracked before cleanup
        ResourceManager.ResourceStats beforeStats = resourceManager.getResourceStats();
        assertEquals(resourceIds.size(), beforeStats.activeCount,
                "All registered resources should be tracked before cleanup");
        
        // Call cleanupAll()
        resourceManager.cleanupAll();
        
        // Verify all resources were cleaned up
        for (String id : resourceIds) {
            AtomicBoolean wasCleanedUp = cleanupTracking.get(id);
            assertTrue(wasCleanedUp.get(), 
                    "Resource cleanup method should be called during cleanupAll: " + id);
        }
        
        // Verify no resources are tracked after cleanupAll
        Set<String> activeIds = resourceManager.getActiveResourceIds();
        for (String id : resourceIds) {
            assertFalse(activeIds.contains(id), 
                    "No resources should be tracked after cleanupAll: " + id);
        }
        
        // Verify statistics reflect complete cleanup
        ResourceManager.ResourceStats afterStats = resourceManager.getResourceStats();
        assertEquals(0, afterStats.activeCount,
                "Active count should be 0 after cleanupAll");
    }
    
    /**
     * **Feature: performance-optimization, Property 2: Resource Cleanup Completeness**
     * **Validates: Requirements 1.2, 1.5**
     * 
     * For any Timeline resource registration, the Timeline should be properly stopped
     * and cleaned up when the resource is disposed.
     */
    @Property(tries = 100)
    void timelineResourceCleanup(
            @ForAll("uniqueResourceIds") List<String> timelineIds) {
        
        Assume.that(!timelineIds.isEmpty());
        Assume.that(timelineIds.size() <= 5); // Limit for performance
        
        ResourceManager resourceManager = ResourceManager.getInstance();
        
        // Clean up any existing state first
        resourceManager.cleanupAll();
        
        // Track Timeline states
        List<MockTimeline> mockTimelines = new ArrayList<>();
        
        // Register Timeline resources
        for (String id : timelineIds) {
            MockTimeline mockTimeline = new MockTimeline();
            resourceManager.registerResource(id, mockTimeline, ResourceManager.ResourceType.TIMELINE);
            mockTimelines.add(mockTimeline);
        }
        
        // Verify Timelines are tracked
        ResourceManager.ResourceStats stats = resourceManager.getResourceStats();
        assertEquals(timelineIds.size(), stats.activeCount,
                "All Timeline resources should be tracked");
        
        // Clean up all resources
        resourceManager.cleanupAll();
        
        // Verify all Timelines were stopped
        for (int i = 0; i < mockTimelines.size(); i++) {
            MockTimeline timeline = mockTimelines.get(i);
            assertTrue(timeline.wasStopped(), 
                    "Timeline " + i + " should be stopped during cleanup");
        }
        
        // Verify no Timeline resources are tracked
        ResourceManager.ResourceStats finalStats = resourceManager.getResourceStats();
        assertEquals(0, finalStats.activeCount,
                "No Timeline resources should remain after cleanup");
    }
    
    /**
     * Provides lists of unique resource IDs for testing.
     * Ensures no duplicate IDs are generated.
     */
    @Provide
    Arbitrary<List<String>> uniqueResourceIds() {
        return Arbitraries.integers()
                .between(1, 100000)
                .set()  // Use set to ensure uniqueness
                .ofMinSize(1)
                .ofMaxSize(10)
                .map(set -> set.stream()
                        .map(i -> "resource-" + i)
                        .toList());
    }
    
    /**
     * Provides lists of resource types for testing.
     */
    @Provide
    Arbitrary<List<ResourceManager.ResourceType>> resourceTypes() {
        return Arbitraries.integers()
                .between(1, 10)
                .flatMap(size -> 
                    Arbitraries.of(ResourceManager.ResourceType.values())
                            .list()
                            .ofSize(size)
                );
    }
    
    /**
     * Mock Timeline class for testing Timeline-specific cleanup behavior.
     */
    private static class MockTimeline implements AutoCloseable {
        private final AtomicBoolean stopped = new AtomicBoolean(false);
        
        public void stop() {
            stopped.set(true);
        }
        
        public boolean wasStopped() {
            return stopped.get();
        }
        
        @Override
        public void close() throws Exception {
            stop();
        }
    }
}