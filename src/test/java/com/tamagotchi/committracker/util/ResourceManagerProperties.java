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
            @ForAll("resourcePairs") List<ResourcePair> resourcePairs) {
        
        Assume.that(!resourcePairs.isEmpty());
        Assume.that(resourcePairs.size() <= 10); // Limit size for performance
        
        // Create a fresh ResourceManager instance for this test
        // Note: We can't easily create a new instance due to singleton pattern,
        // so we'll work with the existing one and clean up afterward
        ResourceManager resourceManager = ResourceManager.getInstance();
        
        // Clean up any existing state first
        resourceManager.cleanupAll();
        
        // Track cleanup calls for verification
        List<AtomicBoolean> cleanupCalled = new ArrayList<>();
        List<String> registeredIds = new ArrayList<>();
        
        // Register resources with cleanup tracking
        for (int i = 0; i < resourcePairs.size(); i++) {
            ResourcePair pair = resourcePairs.get(i);
            String id = pair.id;
            ResourceManager.ResourceType type = pair.type;
            AtomicBoolean wasCleanedUp = new AtomicBoolean(false);
            
            // Create a test resource that tracks cleanup
            AutoCloseable testResource = () -> wasCleanedUp.set(true);
            
            // Register the resource
            resourceManager.registerResource(id, testResource, type);
            cleanupCalled.add(wasCleanedUp);
            registeredIds.add(id);
        }
        
        // Verify all resources are tracked
        ResourceManager.ResourceStats initialStats = resourceManager.getResourceStats();
        assertTrue(initialStats.activeCount >= registeredIds.size(),
                "All registered resources should be tracked");
        
        // Clean up each resource individually
        for (int i = 0; i < registeredIds.size(); i++) {
            String id = registeredIds.get(i);
            AtomicBoolean wasCleanedUp = cleanupCalled.get(i);
            
            // Cleanup should succeed
            boolean cleanupResult = resourceManager.cleanupResource(id);
            assertTrue(cleanupResult, "Cleanup should succeed for registered resource: " + id);
            
            // Verify the resource's close method was called
            assertTrue(wasCleanedUp.get(), 
                    "Resource cleanup method should be called for: " + id);
        }
        
        // Verify resources are no longer tracked
        Set<String> activeIds = resourceManager.getActiveResourceIds();
        for (String id : registeredIds) {
            assertFalse(activeIds.contains(id), 
                    "Resource should no longer be tracked after cleanup: " + id);
        }
        
        // Verify statistics reflect the cleanup
        ResourceManager.ResourceStats finalStats = resourceManager.getResourceStats();
        assertEquals(initialStats.totalDisposed + registeredIds.size(), finalStats.totalDisposed,
                "Disposed count should increase by number of cleaned up resources");
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
            @ForAll("resourcePairs") List<ResourcePair> resourcePairs) {
        
        Assume.that(!resourcePairs.isEmpty());
        Assume.that(resourcePairs.size() <= 10); // Limit size for performance
        
        // Create a fresh ResourceManager instance for this test
        ResourceManager resourceManager = ResourceManager.getInstance();
        
        // Clean up any existing state first
        resourceManager.cleanupAll();
        
        // Track cleanup calls for verification
        List<AtomicBoolean> cleanupCalled = new ArrayList<>();
        
        // Register resources with cleanup tracking
        for (int i = 0; i < resourcePairs.size(); i++) {
            ResourcePair pair = resourcePairs.get(i);
            String id = pair.id;
            ResourceManager.ResourceType type = pair.type;
            AtomicBoolean wasCleanedUp = new AtomicBoolean(false);
            
            // Create a test resource that tracks cleanup
            AutoCloseable testResource = () -> wasCleanedUp.set(true);
            
            // Register the resource
            resourceManager.registerResource(id, testResource, type);
            cleanupCalled.add(wasCleanedUp);
        }
        
        // Verify resources are tracked before cleanup
        ResourceManager.ResourceStats beforeStats = resourceManager.getResourceStats();
        assertTrue(beforeStats.activeCount >= resourcePairs.size(),
                "All registered resources should be tracked before cleanup");
        
        // Call cleanupAll()
        resourceManager.cleanupAll();
        
        // Verify all resources were cleaned up
        for (int i = 0; i < cleanupCalled.size(); i++) {
            AtomicBoolean wasCleanedUp = cleanupCalled.get(i);
            assertTrue(wasCleanedUp.get(), 
                    "Resource " + i + " cleanup method should be called during cleanupAll");
        }
        
        // Verify no resources are tracked after cleanupAll
        Set<String> activeIds = resourceManager.getActiveResourceIds();
        for (ResourcePair pair : resourcePairs) {
            assertFalse(activeIds.contains(pair.id), 
                    "No resources should be tracked after cleanupAll: " + pair.id);
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
            @ForAll("resourceIds") List<String> timelineIds) {
        
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
        assertTrue(stats.activeCount >= timelineIds.size(),
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
     * Provides lists of resource pairs (ID + Type) for testing.
     */
    @Provide
    Arbitrary<List<ResourcePair>> resourcePairs() {
        return Arbitraries.integers()
                .between(1, 10)
                .flatMap(size -> {
                    // Generate a list of unique IDs first
                    return Arbitraries.integers().between(1, 10000)
                            .set()
                            .ofMinSize(size)
                            .ofMaxSize(size)
                            .flatMap(idSet -> {
                                // For each unique ID, pair it with a random type
                                List<Arbitrary<ResourcePair>> pairArbitraries = idSet.stream()
                                        .map(id -> Arbitraries.of(ResourceManager.ResourceType.values())
                                                .map(type -> new ResourcePair("resource-" + id, type)))
                                        .toList();
                                return Combinators.combine(pairArbitraries).as(list -> list);
                            });
                });
    }
    
    /**
     * Provides lists of unique resource IDs for testing.
     */
    @Provide
    Arbitrary<List<String>> resourceIds() {
        return Arbitraries.integers()
                .between(1, 1000)
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
     * Resource pair for testing.
     */
    private static class ResourcePair {
        final String id;
        final ResourceManager.ResourceType type;
        
        ResourcePair(String id, ResourceManager.ResourceType type) {
            this.id = id;
            this.type = type;
        }
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