package com.tamagotchi.committracker.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.lang.ref.WeakReference;
import java.util.Iterator;

/**
 * Manages the lifecycle of all application resources including Timeline objects,
 * thread pools, and file handles. Provides automatic cleanup on application shutdown
 * and resource tracking for memory leak detection.
 * 
 * Requirements: 1.2, 1.5
 */
public class ResourceManager implements AutoCloseable {
    private static final Logger logger = Logger.getLogger(ResourceManager.class.getName());
    
    // Singleton instance
    private static volatile ResourceManager instance;
    
    // Resource tracking
    private final ConcurrentHashMap<String, ResourceInfo> activeResources;
    private final Set<WeakReference<AutoCloseable>> weakReferences;
    private final AtomicLong totalResourcesCreated;
    private final AtomicLong totalResourcesDisposed;
    
    // Shutdown hook for automatic cleanup
    private final Thread shutdownHook;
    private volatile boolean isShutdown = false;
    
    private ResourceManager() {
        this.activeResources = new ConcurrentHashMap<>();
        this.weakReferences = new CopyOnWriteArraySet<>();
        this.totalResourcesCreated = new AtomicLong(0);
        this.totalResourcesDisposed = new AtomicLong(0);
        
        // Register shutdown hook for automatic cleanup
        this.shutdownHook = new Thread(this::performShutdownCleanup, "ResourceManager-Shutdown");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        
        logger.info("ResourceManager initialized with automatic shutdown cleanup");
    }
    
    /**
     * Gets the singleton instance of the ResourceManager.
     */
    public static ResourceManager getInstance() {
        if (instance == null) {
            synchronized (ResourceManager.class) {
                if (instance == null) {
                    instance = new ResourceManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Registers a resource for lifecycle management.
     * 
     * @param id Unique identifier for the resource
     * @param resource The resource to track (must implement AutoCloseable)
     * @param type The type of resource for categorization
     */
    public void registerResource(String id, AutoCloseable resource, ResourceType type) {
        if (isShutdown) {
            logger.warning("Cannot register resource after shutdown: " + id);
            return;
        }
        
        if (id == null || resource == null || type == null) {
            throw new IllegalArgumentException("Resource ID, resource, and type cannot be null");
        }
        
        ResourceInfo info = new ResourceInfo(resource, type, System.currentTimeMillis());
        ResourceInfo existing = activeResources.put(id, info);
        
        if (existing != null) {
            logger.warning("Resource ID already exists, replacing: " + id);
            // Clean up the existing resource
            cleanupResourceSafely(existing.resource, id);
        }
        
        // Also track with weak reference for additional safety
        weakReferences.add(new WeakReference<>(resource));
        
        totalResourcesCreated.incrementAndGet();
        logger.fine("Registered resource: " + id + " (type: " + type + ")");
    }
    
    /**
     * Convenience method for registering Timeline resources.
     */
    public void registerTimeline(String id, javafx.animation.Timeline timeline) {
        registerResource(id, () -> {
            if (timeline != null) {
                timeline.stop();
                logger.fine("Timeline stopped: " + id);
            }
        }, ResourceType.TIMELINE);
    }
    
    /**
     * Convenience method for registering ExecutorService resources.
     */
    public void registerExecutorService(String id, java.util.concurrent.ExecutorService executor) {
        registerResource(id, () -> {
            if (executor != null && !executor.isShutdown()) {
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
                logger.fine("ExecutorService shutdown: " + id);
            }
        }, ResourceType.THREAD_POOL);
    }
    
    /**
     * Convenience method for registering file handle resources.
     */
    public void registerFileHandle(String id, java.io.Closeable fileHandle) {
        registerResource(id, fileHandle::close, ResourceType.FILE_HANDLE);
    }
    
    /**
     * Cleans up a specific resource by ID.
     * 
     * @param id The resource ID to cleanup
     * @return true if the resource was found and cleaned up
     */
    public boolean cleanupResource(String id) {
        if (id == null) {
            return false;
        }
        
        ResourceInfo info = activeResources.remove(id);
        if (info != null) {
            cleanupResourceSafely(info.resource, id);
            totalResourcesDisposed.incrementAndGet();
            logger.fine("Cleaned up resource: " + id);
            return true;
        }
        
        logger.fine("Resource not found for cleanup: " + id);
        return false;
    }
    
    /**
     * Cleans up all registered resources.
     * This method is called automatically during application shutdown.
     */
    public void cleanupAll() {
        if (isShutdown) {
            return;
        }
        
        logger.info("Starting cleanup of all resources (" + activeResources.size() + " active)");
        
        // Clean up all registered resources
        for (Map.Entry<String, ResourceInfo> entry : activeResources.entrySet()) {
            String id = entry.getKey();
            ResourceInfo info = entry.getValue();
            cleanupResourceSafely(info.resource, id);
            totalResourcesDisposed.incrementAndGet();
        }
        
        activeResources.clear();
        
        // Clean up any remaining weak references
        cleanupWeakReferences();
        
        logger.info("Resource cleanup completed. Total created: " + totalResourcesCreated.get() + 
                   ", Total disposed: " + totalResourcesDisposed.get());
    }
    
    /**
     * Gets current resource statistics for monitoring.
     */
    public ResourceStats getResourceStats() {
        // Clean up dead weak references first
        cleanupDeadWeakReferences();
        
        Map<ResourceType, Integer> countsByType = new ConcurrentHashMap<>();
        for (ResourceInfo info : activeResources.values()) {
            countsByType.merge(info.type, 1, Integer::sum);
        }
        
        return new ResourceStats(
            activeResources.size(),
            weakReferences.size(),
            totalResourcesCreated.get(),
            totalResourcesDisposed.get(),
            countsByType
        );
    }
    
    /**
     * Checks if there are any potential resource leaks.
     * A leak is suspected if there are significantly more created than disposed resources.
     */
    public boolean hasPotentialLeaks() {
        long created = totalResourcesCreated.get();
        long disposed = totalResourcesDisposed.get();
        long active = activeResources.size();
        
        // Consider it a potential leak if we have more than 50% undisposed resources
        // and more than 10 active resources
        return active > 10 && (disposed < created * 0.5);
    }
    
    /**
     * Gets a list of currently active resource IDs for debugging.
     */
    public Set<String> getActiveResourceIds() {
        return Set.copyOf(activeResources.keySet());
    }
    
    /**
     * Safely cleans up a resource, catching and logging any exceptions.
     */
    private void cleanupResourceSafely(AutoCloseable resource, String id) {
        try {
            if (resource != null) {
                resource.close();
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to cleanup resource: " + id, e);
        }
    }
    
    /**
     * Cleans up weak references to resources that may not have been properly disposed.
     */
    private void cleanupWeakReferences() {
        int cleaned = 0;
        List<WeakReference<AutoCloseable>> toRemove = new ArrayList<>();
        
        for (WeakReference<AutoCloseable> ref : weakReferences) {
            AutoCloseable resource = ref.get();
            
            if (resource != null) {
                try {
                    resource.close();
                    cleaned++;
                } catch (Exception e) {
                    logger.log(Level.FINE, "Exception during weak reference cleanup", e);
                }
            }
            
            toRemove.add(ref);
        }
        
        // Remove all references after iteration
        weakReferences.removeAll(toRemove);
        
        if (cleaned > 0) {
            logger.info("Cleaned up " + cleaned + " resources via weak references");
        }
    }
    
    /**
     * Removes dead weak references to free memory.
     */
    private void cleanupDeadWeakReferences() {
        weakReferences.removeIf(ref -> ref.get() == null);
    }
    
    /**
     * Performs shutdown cleanup and removes the shutdown hook.
     */
    private void performShutdownCleanup() {
        if (!isShutdown) {
            isShutdown = true;
            logger.info("Performing automatic resource cleanup on shutdown");
            cleanupAll();
        }
    }
    
    /**
     * Closes the ResourceManager and performs cleanup.
     * This method is called automatically if used in try-with-resources.
     */
    @Override
    public void close() {
        if (!isShutdown) {
            isShutdown = true;
            
            // Remove shutdown hook to prevent double cleanup
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException e) {
                // Shutdown already in progress, ignore
            }
            
            cleanupAll();
            logger.info("ResourceManager closed");
        }
    }
    
    /**
     * Resource type enumeration for categorization.
     */
    public enum ResourceType {
        TIMELINE("JavaFX Timeline"),
        THREAD_POOL("Thread Pool/ExecutorService"),
        FILE_HANDLE("File Handle"),
        CACHE("Cache"),
        OTHER("Other Resource");
        
        private final String description;
        
        ResourceType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Internal class to hold resource information.
     */
    private static class ResourceInfo {
        final AutoCloseable resource;
        final ResourceType type;
        final long createdTime;
        
        ResourceInfo(AutoCloseable resource, ResourceType type, long createdTime) {
            this.resource = resource;
            this.type = type;
            this.createdTime = createdTime;
        }
    }
    
    /**
     * Resource statistics data class.
     */
    public static class ResourceStats {
        public final int activeCount;
        public final int weakReferenceCount;
        public final long totalCreated;
        public final long totalDisposed;
        public final Map<ResourceType, Integer> countsByType;
        
        public ResourceStats(int activeCount, int weakReferenceCount, long totalCreated, 
                           long totalDisposed, Map<ResourceType, Integer> countsByType) {
            this.activeCount = activeCount;
            this.weakReferenceCount = weakReferenceCount;
            this.totalCreated = totalCreated;
            this.totalDisposed = totalDisposed;
            this.countsByType = Map.copyOf(countsByType);
        }
        
        @Override
        public String toString() {
            return String.format("ResourceStats{active=%d, weakRefs=%d, created=%d, disposed=%d, byType=%s}", 
                               activeCount, weakReferenceCount, totalCreated, totalDisposed, countsByType);
        }
    }
}