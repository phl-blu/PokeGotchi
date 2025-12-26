package com.tamagotchi.committracker.git;

import com.tamagotchi.committracker.domain.Repository;
import com.tamagotchi.committracker.config.AppConfig;
import com.tamagotchi.committracker.util.ResourceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for performing repository discovery in background threads.
 * Ensures UI remains responsive during long-running scanning operations.
 * 
 * Requirements: 4.3, 4.5 - Background repository discovery with progress reporting
 * 
 * This service provides:
 * - Non-blocking repository discovery
 * - Progress reporting for UI updates
 * - Cancellation support
 * - Timeout handling
 * - Thread pool isolation from UI thread
 */
public class BackgroundRepositoryDiscoveryService {
    private static final Logger logger = Logger.getLogger(BackgroundRepositoryDiscoveryService.class.getName());
    
    // Thread pool configuration - separate from UI thread pool
    private static final int DISCOVERY_THREAD_POOL_SIZE = 2;
    private static final String EXECUTOR_RESOURCE_ID = "backgroundDiscovery-executor";
    
    private final RepositoryScanner repositoryScanner;
    private final ExecutorService discoveryExecutor;
    private final AtomicBoolean isDiscovering = new AtomicBoolean(false);
    private final AtomicBoolean isCancelled = new AtomicBoolean(false);
    private final AtomicInteger discoveryCount = new AtomicInteger(0);
    
    // Progress tracking
    private volatile DiscoveryProgress currentProgress;
    private Consumer<DiscoveryProgress> progressCallback;
    private Consumer<List<Repository>> completionCallback;
    private Consumer<Throwable> errorCallback;
    
    // Current discovery task
    private CompletableFuture<List<Repository>> currentDiscoveryTask;
    
    /**
     * Progress information for repository discovery.
     * Designed for UI consumption without blocking.
     */
    public static class DiscoveryProgress {
        private final int directoriesScanned;
        private final int repositoriesFound;
        private final String currentPath;
        private final DiscoveryState state;
        private final double progressPercentage;
        private final long elapsedTimeMs;
        
        public enum DiscoveryState {
            NOT_STARTED,
            IN_PROGRESS,
            COMPLETED,
            CANCELLED,
            TIMED_OUT,
            ERROR
        }
        
        public DiscoveryProgress(int directoriesScanned, int repositoriesFound, 
                                String currentPath, DiscoveryState state,
                                double progressPercentage, long elapsedTimeMs) {
            this.directoriesScanned = directoriesScanned;
            this.repositoriesFound = repositoriesFound;
            this.currentPath = currentPath;
            this.state = state;
            this.progressPercentage = progressPercentage;
            this.elapsedTimeMs = elapsedTimeMs;
        }
        
        public int getDirectoriesScanned() { return directoriesScanned; }
        public int getRepositoriesFound() { return repositoriesFound; }
        public String getCurrentPath() { return currentPath; }
        public DiscoveryState getState() { return state; }
        public double getProgressPercentage() { return progressPercentage; }
        public long getElapsedTimeMs() { return elapsedTimeMs; }
        public boolean isComplete() { 
            return state == DiscoveryState.COMPLETED || 
                   state == DiscoveryState.CANCELLED ||
                   state == DiscoveryState.TIMED_OUT ||
                   state == DiscoveryState.ERROR;
        }
        
        @Override
        public String toString() {
            return String.format("DiscoveryProgress[state=%s, dirs=%d, repos=%d, progress=%.1f%%, elapsed=%dms]",
                    state, directoriesScanned, repositoriesFound, progressPercentage, elapsedTimeMs);
        }
    }
    
    /**
     * Creates a new background discovery service with its own thread pool.
     */
    public BackgroundRepositoryDiscoveryService() {
        this(new RepositoryScanner());
    }
    
    /**
     * Creates a new background discovery service with a provided scanner.
     * Useful for testing.
     */
    public BackgroundRepositoryDiscoveryService(RepositoryScanner scanner) {
        this.repositoryScanner = scanner;
        this.discoveryExecutor = Executors.newFixedThreadPool(DISCOVERY_THREAD_POOL_SIZE, r -> {
            Thread t = new Thread(r, "BackgroundDiscovery-Worker-" + discoveryCount.incrementAndGet());
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY); // Lower priority to not compete with UI
            return t;
        });
        
        this.currentProgress = new DiscoveryProgress(0, 0, "", 
                DiscoveryProgress.DiscoveryState.NOT_STARTED, 0.0, 0);
        
        // Register with ResourceManager for cleanup
        ResourceManager.getInstance().registerExecutorService(EXECUTOR_RESOURCE_ID, discoveryExecutor);
        
        // Set up progress forwarding from scanner
        setupProgressForwarding();
    }
    
    /**
     * Sets up progress forwarding from the repository scanner.
     */
    private void setupProgressForwarding() {
        repositoryScanner.setProgressListener(scanProgress -> {
            if (!isCancelled.get()) {
                updateProgress(
                    scanProgress.getScannedCount(),
                    scanProgress.getRepositoriesFound(),
                    scanProgress.getCurrentPath(),
                    scanProgress.isComplete() ? 
                        DiscoveryProgress.DiscoveryState.COMPLETED : 
                        DiscoveryProgress.DiscoveryState.IN_PROGRESS,
                    scanProgress.getProgressPercentage()
                );
            }
        });
    }

    
    /**
     * Updates the current progress and notifies the callback.
     */
    private void updateProgress(int directoriesScanned, int repositoriesFound,
                               String currentPath, DiscoveryProgress.DiscoveryState state,
                               double progressPercentage) {
        long elapsed = System.currentTimeMillis() - discoveryStartTime;
        currentProgress = new DiscoveryProgress(
            directoriesScanned, repositoriesFound, currentPath, state, progressPercentage, elapsed
        );
        
        if (progressCallback != null) {
            try {
                progressCallback.accept(currentProgress);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Progress callback failed", e);
            }
        }
    }
    
    private volatile long discoveryStartTime;
    
    /**
     * Starts repository discovery in background threads.
     * This method returns immediately and does not block the calling thread.
     * 
     * @return CompletableFuture that completes when discovery is done
     * 
     * Requirements: 4.3 - Perform initial scanning in background threads
     */
    public CompletableFuture<List<Repository>> startDiscoveryAsync() {
        if (isDiscovering.getAndSet(true)) {
            logger.warning("Discovery already in progress, returning existing task");
            return currentDiscoveryTask != null ? currentDiscoveryTask : 
                   CompletableFuture.completedFuture(new ArrayList<>());
        }
        
        isCancelled.set(false);
        discoveryStartTime = System.currentTimeMillis();
        
        updateProgress(0, 0, "", DiscoveryProgress.DiscoveryState.IN_PROGRESS, 0.0);
        
        currentDiscoveryTask = CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Starting background repository discovery");
                List<Repository> repositories = repositoryScanner.discoverRepositories();
                
                if (isCancelled.get()) {
                    updateProgress(currentProgress.getDirectoriesScanned(), 
                                  repositories.size(), "",
                                  DiscoveryProgress.DiscoveryState.CANCELLED, 100.0);
                    return repositories;
                }
                
                // Apply repository limit
                int maxRepos = AppConfig.getMaxRepositories();
                if (repositories.size() > maxRepos) {
                    logger.info("Limiting repositories from " + repositories.size() + " to " + maxRepos);
                    repositories = repositories.subList(0, maxRepos);
                }
                
                updateProgress(currentProgress.getDirectoriesScanned(),
                              repositories.size(), "",
                              DiscoveryProgress.DiscoveryState.COMPLETED, 100.0);
                
                logger.info("Background discovery completed: found " + repositories.size() + " repositories");
                return repositories;
                
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Background discovery failed", e);
                updateProgress(currentProgress.getDirectoriesScanned(),
                              currentProgress.getRepositoriesFound(), "",
                              DiscoveryProgress.DiscoveryState.ERROR, 0.0);
                throw new CompletionException(e);
            } finally {
                isDiscovering.set(false);
            }
        }, discoveryExecutor);
        
        // Set up completion handlers
        currentDiscoveryTask.whenComplete((repos, error) -> {
            if (error != null) {
                if (errorCallback != null) {
                    errorCallback.accept(error);
                }
            } else if (completionCallback != null && !isCancelled.get()) {
                completionCallback.accept(repos);
            }
        });
        
        return currentDiscoveryTask;
    }
    
    /**
     * Starts repository discovery with a timeout.
     * Returns partial results if timeout is reached.
     * 
     * @param timeoutSeconds Maximum time to wait for discovery
     * @return CompletableFuture that completes when discovery is done or times out
     * 
     * Requirements: 3.1 - Use configurable timeouts to prevent hanging operations
     */
    public CompletableFuture<List<Repository>> startDiscoveryWithTimeout(int timeoutSeconds) {
        CompletableFuture<List<Repository>> discoveryFuture = startDiscoveryAsync();
        
        // Create a timeout future
        CompletableFuture<List<Repository>> timeoutFuture = new CompletableFuture<>();
        
        // Schedule timeout
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "DiscoveryTimeout");
            t.setDaemon(true);
            return t;
        });
        
        scheduler.schedule(() -> {
            if (!discoveryFuture.isDone()) {
                logger.warning("Discovery timed out after " + timeoutSeconds + " seconds");
                cancelDiscovery();
                
                // Return whatever we found so far
                List<Repository> partialResults = repositoryScanner.getDiscoveredRepositories();
                updateProgress(currentProgress.getDirectoriesScanned(),
                              partialResults.size(), "",
                              DiscoveryProgress.DiscoveryState.TIMED_OUT, 100.0);
                
                timeoutFuture.complete(partialResults);
            }
            scheduler.shutdown();
        }, timeoutSeconds, TimeUnit.SECONDS);
        
        // Return whichever completes first
        return discoveryFuture.applyToEither(timeoutFuture, repos -> repos);
    }
    
    /**
     * Cancels any ongoing discovery operation.
     * The discovery will stop as soon as possible and return partial results.
     */
    public void cancelDiscovery() {
        if (isDiscovering.get()) {
            logger.info("Cancelling repository discovery");
            isCancelled.set(true);
            repositoryScanner.cancelDiscovery();
            
            updateProgress(currentProgress.getDirectoriesScanned(),
                          currentProgress.getRepositoriesFound(), "",
                          DiscoveryProgress.DiscoveryState.CANCELLED, 100.0);
        }
    }
    
    /**
     * Sets the progress callback for UI updates.
     * The callback is invoked on the discovery thread, so UI updates
     * should use Platform.runLater() or similar.
     * 
     * @param callback Consumer that receives progress updates
     * 
     * Requirements: 4.3 - Add progress reporting for long-running operations
     */
    public void setProgressCallback(Consumer<DiscoveryProgress> callback) {
        this.progressCallback = callback;
    }
    
    /**
     * Sets the completion callback.
     * Called when discovery completes successfully.
     */
    public void setCompletionCallback(Consumer<List<Repository>> callback) {
        this.completionCallback = callback;
    }
    
    /**
     * Sets the error callback.
     * Called when discovery fails with an error.
     */
    public void setErrorCallback(Consumer<Throwable> callback) {
        this.errorCallback = callback;
    }
    
    /**
     * Gets the current discovery progress.
     * Safe to call from any thread.
     */
    public DiscoveryProgress getCurrentProgress() {
        return currentProgress;
    }
    
    /**
     * Checks if discovery is currently in progress.
     */
    public boolean isDiscovering() {
        return isDiscovering.get();
    }
    
    /**
     * Gets the underlying repository scanner.
     * Useful for adding custom search paths.
     */
    public RepositoryScanner getRepositoryScanner() {
        return repositoryScanner;
    }
    
    /**
     * Shuts down the discovery service and releases resources.
     * 
     * Requirements: 1.5 - Clean up all resources including threads
     */
    public void shutdown() {
        logger.info("Shutting down BackgroundRepositoryDiscoveryService");
        
        // Cancel any ongoing discovery
        cancelDiscovery();
        
        // Shutdown the scanner
        if (repositoryScanner != null) {
            repositoryScanner.shutdown();
        }
        
        // Shutdown executor
        if (discoveryExecutor != null && !discoveryExecutor.isShutdown()) {
            discoveryExecutor.shutdown();
            try {
                if (!discoveryExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    discoveryExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                discoveryExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // Unregister from ResourceManager
        ResourceManager.getInstance().cleanupResource(EXECUTOR_RESOURCE_ID);
        
        logger.info("BackgroundRepositoryDiscoveryService shutdown complete");
    }
}
