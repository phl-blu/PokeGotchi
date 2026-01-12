package com.tamagotchi.committracker.github;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Queue for sync operations that need to be performed when connectivity is restored.
 * Persists queued operations to disk to survive application restarts.
 * 
 * Requirements: 7.3
 * - 7.3: Sync missed commits automatically when connectivity is restored
 */
public class SyncOperationQueue {
    
    private static final Logger LOGGER = Logger.getLogger(SyncOperationQueue.class.getName());
    private static final String QUEUE_FILE_NAME = "sync_queue.json";
    private static final int MAX_QUEUE_SIZE = 100;
    
    private final Path storageDirectory;
    private final Gson gson;
    private final ReadWriteLock lock;
    
    private Queue<SyncOperation> operationQueue;
    
    /**
     * Types of sync operations that can be queued.
     */
    public enum OperationType {
        FULL_SYNC,           // Full repository and commit sync
        COMMIT_POLL,         // Poll for new commits
        REPOSITORY_REFRESH,  // Refresh repository list
        BASELINE_UPDATE      // Update commit baseline
    }
    
    /**
     * Represents a queued sync operation.
     */
    public static class SyncOperation {
        public OperationType type;
        public Instant queuedAt;
        public String repositoryFullName; // Optional, for repo-specific operations
        public int retryCount;
        public String lastError;
        
        public SyncOperation() {
            this.queuedAt = Instant.now();
            this.retryCount = 0;
        }
        
        public SyncOperation(OperationType type) {
            this.type = type;
            this.queuedAt = Instant.now();
            this.retryCount = 0;
        }
        
        public SyncOperation(OperationType type, String repositoryFullName) {
            this.type = type;
            this.repositoryFullName = repositoryFullName;
            this.queuedAt = Instant.now();
            this.retryCount = 0;
        }
    }

    
    /**
     * Creates a SyncOperationQueue with the default storage directory.
     */
    public SyncOperationQueue() {
        this(getDefaultStorageDirectory());
    }
    
    /**
     * Creates a SyncOperationQueue with a custom storage directory.
     * 
     * @param storageDirectory the directory to store queue data
     */
    public SyncOperationQueue(Path storageDirectory) {
        this.storageDirectory = storageDirectory;
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
        this.lock = new ReentrantReadWriteLock();
        this.operationQueue = new LinkedList<>();
        
        // Load queue from disk
        loadFromDisk();
    }
    
    /**
     * Gets the default storage directory.
     */
    private static Path getDefaultStorageDirectory() {
        String userHome = System.getProperty("user.home");
        return Path.of(userHome, ".pokemon-commit-tracker", "cache");
    }
    
    /**
     * Queues a sync operation to be performed when online.
     * 
     * @param operation the operation to queue
     * @return true if operation was queued, false if queue is full
     */
    public boolean enqueue(SyncOperation operation) {
        if (operation == null) {
            return false;
        }
        
        lock.writeLock().lock();
        try {
            // Check queue size limit
            if (operationQueue.size() >= MAX_QUEUE_SIZE) {
                LOGGER.warning("Sync queue is full, cannot add operation: " + operation.type);
                return false;
            }
            
            // Check for duplicate operations
            if (hasDuplicateOperation(operation)) {
                LOGGER.fine("Duplicate operation already queued: " + operation.type);
                return true; // Consider it successful since operation is already queued
            }
            
            operationQueue.add(operation);
            saveToDisk();
            
            LOGGER.info("Queued sync operation: " + operation.type);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Queues a full sync operation.
     * 
     * @return true if operation was queued
     */
    public boolean queueFullSync() {
        return enqueue(new SyncOperation(OperationType.FULL_SYNC));
    }
    
    /**
     * Queues a commit poll operation.
     * 
     * @return true if operation was queued
     */
    public boolean queueCommitPoll() {
        return enqueue(new SyncOperation(OperationType.COMMIT_POLL));
    }
    
    /**
     * Queues a repository refresh operation.
     * 
     * @return true if operation was queued
     */
    public boolean queueRepositoryRefresh() {
        return enqueue(new SyncOperation(OperationType.REPOSITORY_REFRESH));
    }
    
    /**
     * Dequeues the next operation to process.
     * 
     * @return the next operation, or null if queue is empty
     */
    public SyncOperation dequeue() {
        lock.writeLock().lock();
        try {
            SyncOperation operation = operationQueue.poll();
            if (operation != null) {
                saveToDisk();
                LOGGER.fine("Dequeued sync operation: " + operation.type);
            }
            return operation;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Peeks at the next operation without removing it.
     * 
     * @return the next operation, or null if queue is empty
     */
    public SyncOperation peek() {
        lock.readLock().lock();
        try {
            return operationQueue.peek();
        } finally {
            lock.readLock().unlock();
        }
    }

    
    /**
     * Re-queues an operation that failed (for retry).
     * 
     * @param operation the operation to re-queue
     * @param error the error message
     * @return true if re-queued, false if max retries exceeded
     */
    public boolean requeue(SyncOperation operation, String error) {
        if (operation == null) {
            return false;
        }
        
        operation.retryCount++;
        operation.lastError = error;
        
        // Max 3 retries
        if (operation.retryCount > 3) {
            LOGGER.warning("Max retries exceeded for operation: " + operation.type);
            return false;
        }
        
        lock.writeLock().lock();
        try {
            operationQueue.add(operation);
            saveToDisk();
            LOGGER.info("Re-queued sync operation (retry " + operation.retryCount + "): " + operation.type);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Gets all queued operations.
     * 
     * @return list of queued operations
     */
    public List<SyncOperation> getQueuedOperations() {
        lock.readLock().lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(operationQueue));
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Gets the number of queued operations.
     * 
     * @return queue size
     */
    public int size() {
        lock.readLock().lock();
        try {
            return operationQueue.size();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Checks if the queue is empty.
     * 
     * @return true if empty
     */
    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            return operationQueue.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Checks if the queue has pending operations.
     * 
     * @return true if there are pending operations
     */
    public boolean hasPendingOperations() {
        return !isEmpty();
    }
    
    /**
     * Clears all queued operations.
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            operationQueue.clear();
            saveToDisk();
            LOGGER.info("Sync queue cleared");
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Consolidates the queue by removing redundant operations.
     * For example, if there's a FULL_SYNC, remove individual COMMIT_POLL operations.
     */
    public void consolidate() {
        lock.writeLock().lock();
        try {
            boolean hasFullSync = operationQueue.stream()
                .anyMatch(op -> op.type == OperationType.FULL_SYNC);
            
            if (hasFullSync) {
                // Remove individual operations that would be covered by full sync
                operationQueue.removeIf(op -> 
                    op.type == OperationType.COMMIT_POLL || 
                    op.type == OperationType.REPOSITORY_REFRESH);
                
                saveToDisk();
                LOGGER.info("Consolidated sync queue");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    
    // ==================== Private Methods ====================
    
    /**
     * Checks if a similar operation is already queued.
     */
    private boolean hasDuplicateOperation(SyncOperation newOp) {
        for (SyncOperation existing : operationQueue) {
            if (existing.type == newOp.type) {
                // For repo-specific operations, check the repo name too
                if (newOp.repositoryFullName != null) {
                    if (newOp.repositoryFullName.equals(existing.repositoryFullName)) {
                        return true;
                    }
                } else {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Saves the queue to disk.
     */
    private void saveToDisk() {
        try {
            Files.createDirectories(storageDirectory);
            Path file = storageDirectory.resolve(QUEUE_FILE_NAME);
            
            List<SyncOperation> operations = new ArrayList<>(operationQueue);
            String json = gson.toJson(operations);
            
            Files.writeString(file, json);
            LOGGER.fine("Saved sync queue to disk (" + operations.size() + " operations)");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to save sync queue", e);
        }
    }
    
    /**
     * Loads the queue from disk.
     */
    private void loadFromDisk() {
        Path file = storageDirectory.resolve(QUEUE_FILE_NAME);
        
        if (!Files.exists(file)) {
            LOGGER.fine("No sync queue file found");
            return;
        }
        
        try {
            String json = Files.readString(file);
            Type type = new TypeToken<List<SyncOperation>>(){}.getType();
            List<SyncOperation> operations = gson.fromJson(json, type);
            
            if (operations != null) {
                operationQueue = new LinkedList<>(operations);
                LOGGER.info("Loaded " + operationQueue.size() + " sync operations from disk");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load sync queue", e);
            operationQueue = new LinkedList<>();
        }
    }
    
    /**
     * Gets a summary of the queue for display.
     * 
     * @return queue summary
     */
    public QueueSummary getSummary() {
        lock.readLock().lock();
        try {
            int fullSyncCount = 0;
            int commitPollCount = 0;
            int repoRefreshCount = 0;
            int otherCount = 0;
            
            for (SyncOperation op : operationQueue) {
                switch (op.type) {
                    case FULL_SYNC -> fullSyncCount++;
                    case COMMIT_POLL -> commitPollCount++;
                    case REPOSITORY_REFRESH -> repoRefreshCount++;
                    default -> otherCount++;
                }
            }
            
            return new QueueSummary(
                operationQueue.size(),
                fullSyncCount,
                commitPollCount,
                repoRefreshCount,
                otherCount
            );
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Summary of queued operations.
     */
    public record QueueSummary(
        int totalOperations,
        int fullSyncCount,
        int commitPollCount,
        int repoRefreshCount,
        int otherCount
    ) {
        public String getDisplayText() {
            if (totalOperations == 0) {
                return "No pending operations";
            }
            return totalOperations + " pending sync operation" + (totalOperations > 1 ? "s" : "");
        }
    }
}
