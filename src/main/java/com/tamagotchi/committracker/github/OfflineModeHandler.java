package com.tamagotchi.committracker.github;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main handler for offline mode functionality.
 * Coordinates between connectivity monitoring, caching, and sync operations.
 * 
 * Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6
 * - 7.1: Continue displaying cached commit data when offline
 * - 7.2: Display offline indicator without disrupting Pokemon display
 * - 7.3: Sync missed commits automatically when connectivity is restored
 * - 7.4: Load cached data when application starts offline
 * - 7.5: Preserve Pokemon state and XP progress locally
 * - 7.6: Reconcile local and remote commit data when syncing
 */
public class OfflineModeHandler implements NetworkConnectivityManager.ConnectivityListener {
    
    private static final Logger LOGGER = Logger.getLogger(OfflineModeHandler.class.getName());
    
    private final NetworkConnectivityManager connectivityManager;
    private final OfflineCacheManager cacheManager;
    private final SyncOperationQueue syncQueue;
    
    private final List<OfflineModeListener> listeners;
    private final AtomicBoolean isProcessingQueue;
    
    // Reference to GitHubCommitService for sync operations
    private GitHubCommitService commitService;
    
    /**
     * Listener interface for offline mode events.
     */
    public interface OfflineModeListener {
        /**
         * Called when offline mode is entered.
         */
        void onOfflineModeEntered();
        
        /**
         * Called when online mode is restored.
         * 
         * @param offlineDuration how long the system was offline
         */
        void onOnlineModeRestored(Duration offlineDuration);
        
        /**
         * Called when sync operations are queued.
         * 
         * @param pendingCount number of pending operations
         */
        void onSyncQueued(int pendingCount);
        
        /**
         * Called when sync operations complete.
         * 
         * @param success true if all operations succeeded
         */
        void onSyncComplete(boolean success);
    }

    
    /**
     * Creates an OfflineModeHandler with default components.
     */
    public OfflineModeHandler() {
        this(new NetworkConnectivityManager(), new OfflineCacheManager(), new SyncOperationQueue());
    }
    
    /**
     * Creates an OfflineModeHandler with custom components.
     */
    public OfflineModeHandler(NetworkConnectivityManager connectivityManager,
                              OfflineCacheManager cacheManager,
                              SyncOperationQueue syncQueue) {
        this.connectivityManager = connectivityManager;
        this.cacheManager = cacheManager;
        this.syncQueue = syncQueue;
        this.listeners = Collections.synchronizedList(new ArrayList<>());
        this.isProcessingQueue = new AtomicBoolean(false);
        
        // Register as connectivity listener
        connectivityManager.addListener(this);
    }
    
    /**
     * Sets the GitHubCommitService for sync operations.
     * 
     * @param commitService the commit service
     */
    public void setCommitService(GitHubCommitService commitService) {
        this.commitService = commitService;
    }
    
    /**
     * Initializes offline mode handling.
     * Starts connectivity monitoring and loads cached data.
     */
    public void initialize() {
        // Start connectivity monitoring
        connectivityManager.startMonitoring();
        
        // Check if we have cached data for offline use
        if (cacheManager.hasCachedData()) {
            LOGGER.info("Cached data available for offline use");
        }
        
        // Check for pending sync operations
        if (syncQueue.hasPendingOperations()) {
            LOGGER.info("Found " + syncQueue.size() + " pending sync operations");
            
            // If online, process the queue
            if (connectivityManager.isOnline()) {
                processQueuedOperations();
            }
        }
        
        LOGGER.info("OfflineModeHandler initialized");
    }
    
    /**
     * Checks if the system is currently online.
     * 
     * @return true if online
     */
    public boolean isOnline() {
        return connectivityManager.isOnline();
    }
    
    /**
     * Checks if the system is currently offline.
     * 
     * @return true if offline
     */
    public boolean isOffline() {
        return connectivityManager.isOffline();
    }
    
    /**
     * Gets the current connectivity status.
     * 
     * @return connectivity status
     */
    public NetworkConnectivityManager.ConnectivityStatus getConnectivityStatus() {
        return connectivityManager.getStatus();
    }
    
    /**
     * Gets the offline mode status for display.
     * 
     * Requirement 7.2: Display offline indicator
     * 
     * @return offline mode status
     */
    public OfflineModeStatus getStatus() {
        return new OfflineModeStatus(
            connectivityManager.isOnline(),
            connectivityManager.getOfflineDuration(),
            cacheManager.hasCachedData(),
            syncQueue.size(),
            cacheManager.getCacheSummary()
        );
    }

    
    // ==================== Sync Queue Management ====================
    
    /**
     * Queues a sync operation for when connectivity is restored.
     * 
     * Requirement 7.3: Queue sync operations for when online
     * 
     * @param operationType the type of operation to queue
     */
    public void queueSyncOperation(SyncOperationQueue.OperationType operationType) {
        if (connectivityManager.isOnline()) {
            // If online, execute immediately instead of queuing
            LOGGER.fine("Online - executing sync operation immediately: " + operationType);
            executeSyncOperation(new SyncOperationQueue.SyncOperation(operationType));
        } else {
            // Queue for later
            syncQueue.enqueue(new SyncOperationQueue.SyncOperation(operationType));
            notifySyncQueued(syncQueue.size());
        }
    }
    
    /**
     * Processes all queued sync operations.
     * Called when connectivity is restored.
     * 
     * Requirement 7.3: Sync missed commits automatically when connectivity is restored
     */
    public CompletableFuture<Boolean> processQueuedOperations() {
        if (!connectivityManager.isOnline()) {
            LOGGER.warning("Cannot process queue while offline");
            return CompletableFuture.completedFuture(false);
        }
        
        if (isProcessingQueue.getAndSet(true)) {
            LOGGER.fine("Queue processing already in progress");
            return CompletableFuture.completedFuture(true);
        }
        
        // Consolidate queue to remove redundant operations
        syncQueue.consolidate();
        
        if (syncQueue.isEmpty()) {
            isProcessingQueue.set(false);
            return CompletableFuture.completedFuture(true);
        }
        
        LOGGER.info("Processing " + syncQueue.size() + " queued sync operations");
        
        return processNextOperation()
            .thenApply(success -> {
                isProcessingQueue.set(false);
                notifySyncComplete(success);
                return success;
            });
    }
    
    /**
     * Processes the next operation in the queue recursively.
     */
    private CompletableFuture<Boolean> processNextOperation() {
        SyncOperationQueue.SyncOperation operation = syncQueue.dequeue();
        
        if (operation == null) {
            return CompletableFuture.completedFuture(true);
        }
        
        return executeSyncOperation(operation)
            .thenCompose(success -> {
                if (!success) {
                    // Re-queue failed operation
                    syncQueue.requeue(operation, "Operation failed");
                }
                
                // Process next operation
                return processNextOperation();
            })
            .exceptionally(e -> {
                LOGGER.log(Level.WARNING, "Error processing sync operation", e);
                syncQueue.requeue(operation, e.getMessage());
                return false;
            });
    }
    
    /**
     * Executes a single sync operation.
     */
    private CompletableFuture<Boolean> executeSyncOperation(SyncOperationQueue.SyncOperation operation) {
        if (commitService == null || !commitService.isInitialized()) {
            LOGGER.warning("CommitService not available for sync operation");
            return CompletableFuture.completedFuture(false);
        }
        
        LOGGER.info("Executing sync operation: " + operation.type);
        
        switch (operation.type) {
            case FULL_SYNC:
                return commitService.performInitialSync()
                    .thenApply(result -> {
                        if (result.success()) {
                            cacheManager.updateLastSyncTime(Instant.now());
                        }
                        return result.success();
                    });
                    
            case COMMIT_POLL:
                return commitService.pollForNewCommits()
                    .thenApply(commits -> {
                        cacheManager.updateLastSyncTime(Instant.now());
                        return true;
                    });
                    
            case REPOSITORY_REFRESH:
                // Repository refresh is part of full sync
                return commitService.performInitialSync()
                    .thenApply(result -> result.success());
                    
            default:
                LOGGER.warning("Unknown operation type: " + operation.type);
                return CompletableFuture.completedFuture(false);
        }
    }

    
    // ==================== ConnectivityListener Implementation ====================
    
    @Override
    public void onConnectivityChanged(boolean online) {
        if (online) {
            LOGGER.info("Connectivity restored - entering online mode");
        } else {
            LOGGER.info("Connectivity lost - entering offline mode");
            
            // Queue a sync operation for when we come back online
            syncQueue.queueCommitPoll();
            
            notifyOfflineModeEntered();
        }
    }
    
    @Override
    public void onConnectivityRestored(Duration offlineDuration) {
        LOGGER.info("Connectivity restored after " + offlineDuration.toMinutes() + " minutes offline");
        
        notifyOnlineModeRestored(offlineDuration);
        
        // Process any queued operations
        processQueuedOperations();
    }
    
    // ==================== Cache Access ====================
    
    /**
     * Gets the cache manager for direct cache access.
     * 
     * @return the cache manager
     */
    public OfflineCacheManager getCacheManager() {
        return cacheManager;
    }
    
    /**
     * Gets the sync queue for inspection.
     * 
     * @return the sync queue
     */
    public SyncOperationQueue getSyncQueue() {
        return syncQueue;
    }
    
    // ==================== Listener Management ====================
    
    /**
     * Adds an offline mode listener.
     * 
     * @param listener the listener to add
     */
    public void addListener(OfflineModeListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
    
    /**
     * Removes an offline mode listener.
     * 
     * @param listener the listener to remove
     */
    public void removeListener(OfflineModeListener listener) {
        listeners.remove(listener);
    }
    
    private void notifyOfflineModeEntered() {
        for (OfflineModeListener listener : listeners) {
            try {
                listener.onOfflineModeEntered();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Listener error on offline mode entered", e);
            }
        }
    }
    
    private void notifyOnlineModeRestored(Duration offlineDuration) {
        for (OfflineModeListener listener : listeners) {
            try {
                listener.onOnlineModeRestored(offlineDuration);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Listener error on online mode restored", e);
            }
        }
    }
    
    private void notifySyncQueued(int pendingCount) {
        for (OfflineModeListener listener : listeners) {
            try {
                listener.onSyncQueued(pendingCount);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Listener error on sync queued", e);
            }
        }
    }
    
    private void notifySyncComplete(boolean success) {
        for (OfflineModeListener listener : listeners) {
            try {
                listener.onSyncComplete(success);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Listener error on sync complete", e);
            }
        }
    }

    
    // ==================== Shutdown ====================
    
    /**
     * Shuts down the offline mode handler.
     */
    public void shutdown() {
        connectivityManager.removeListener(this);
        connectivityManager.shutdown();
        listeners.clear();
        LOGGER.info("OfflineModeHandler shutdown complete");
    }
    
    // ==================== Status Record ====================
    
    /**
     * Status information for offline mode display.
     * 
     * Requirement 7.2: Display offline indicator
     */
    public record OfflineModeStatus(
        boolean online,
        Duration offlineDuration,
        boolean hasCachedData,
        int pendingSyncOperations,
        OfflineCacheManager.CacheSummary cacheSummary
    ) {
        /**
         * Gets display text for the offline indicator.
         */
        public String getDisplayText() {
            if (online) {
                if (pendingSyncOperations > 0) {
                    return "Online (syncing...)";
                }
                return "Online";
            } else {
                long minutes = offlineDuration.toMinutes();
                if (minutes < 1) {
                    return "Offline";
                } else if (minutes < 60) {
                    return "Offline (" + minutes + " min)";
                } else {
                    return "Offline (" + (minutes / 60) + " hr)";
                }
            }
        }
        
        /**
         * Gets a detailed status message.
         */
        public String getDetailedStatus() {
            StringBuilder sb = new StringBuilder();
            
            if (online) {
                sb.append("Connected to GitHub");
            } else {
                sb.append("Offline - using cached data");
            }
            
            if (hasCachedData) {
                sb.append("\n").append(cacheSummary.totalCommits()).append(" commits cached");
                sb.append("\n").append(cacheSummary.totalRepositories()).append(" repositories cached");
            }
            
            if (pendingSyncOperations > 0) {
                sb.append("\n").append(pendingSyncOperations).append(" sync operations pending");
            }
            
            return sb.toString();
        }
    }
}
