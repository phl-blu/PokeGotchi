package com.tamagotchi.committracker.git;

import com.tamagotchi.committracker.domain.Commit;
import com.tamagotchi.committracker.domain.CommitHistory;
import com.tamagotchi.committracker.domain.Repository;
import com.tamagotchi.committracker.config.AppConfig;
import com.tamagotchi.committracker.util.WindowsIntegration;
import com.tamagotchi.committracker.util.ErrorHandler;
import com.tamagotchi.committracker.util.RetryMechanism;
import com.tamagotchi.committracker.util.ResourceManager;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Orchestrates repository monitoring and data collection.
 * Manages 5-minute polling cycle and processes new commits.
 * Implements graceful error handling and retry mechanisms for resilience.
 * 
 * Requirements: 1.5, 3.1, 4.3, 4.5 - Proper resource cleanup, configurable timeouts,
 * and background repository discovery with progress reporting
 */
public class CommitService {
    private static final Logger logger = Logger.getLogger(CommitService.class.getName());
    
    // Timeout constants for cleanup operations
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 10;
    private static final int TASK_TIMEOUT_SECONDS = 30;
    
    private final RepositoryScanner repositoryScanner;
    private final BackgroundRepositoryDiscoveryService discoveryService;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService scanExecutor;
    private final CommitHistory commitHistory;
    private final List<CommitListener> listeners;
    private final RetryMechanism retryMechanism;
    
    // Progress listener for UI updates during discovery
    private DiscoveryProgressListener discoveryProgressListener;
    
    private volatile boolean isMonitoring = false;
    private volatile boolean isShutdown = false;
    private ScheduledFuture<?> pollingTask;
    
    /**
     * Listener interface for discovery progress updates.
     * Allows UI to show progress during repository scanning.
     * 
     * Requirements: 4.3 - Add progress reporting for long-running operations
     */
    public interface DiscoveryProgressListener {
        void onProgressUpdate(int directoriesScanned, int repositoriesFound, 
                            String currentPath, boolean isComplete);
    }
    
    public CommitService() {
        this.repositoryScanner = new RepositoryScanner();
        this.discoveryService = new BackgroundRepositoryDiscoveryService(repositoryScanner);
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.scanExecutor = Executors.newFixedThreadPool(4);
        this.commitHistory = new CommitHistory();
        this.listeners = new ArrayList<>();
        this.retryMechanism = RetryMechanism.builder()
            .maxRetries(3)
            .initialDelay(2000)
            .maxDelay(30000)
            .backoffMultiplier(2.0)
            .build();
        
        // Register thread pools with ResourceManager
        ResourceManager resourceManager = ResourceManager.getInstance();
        resourceManager.registerExecutorService("commitService-scheduler", scheduler);
        resourceManager.registerExecutorService("commitService-scanExecutor", scanExecutor);
        
        // Set up discovery progress forwarding
        setupDiscoveryProgressForwarding();
    }
    
    public CommitService(RepositoryScanner repositoryScanner) {
        this.repositoryScanner = repositoryScanner;
        this.discoveryService = new BackgroundRepositoryDiscoveryService(repositoryScanner);
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.scanExecutor = Executors.newFixedThreadPool(4);
        this.commitHistory = new CommitHistory();
        this.listeners = new ArrayList<>();
        this.retryMechanism = RetryMechanism.builder()
            .maxRetries(3)
            .initialDelay(2000)
            .maxDelay(30000)
            .backoffMultiplier(2.0)
            .build();
        
        // Register thread pools with ResourceManager
        ResourceManager resourceManager = ResourceManager.getInstance();
        resourceManager.registerExecutorService("commitService-scheduler", scheduler);
        resourceManager.registerExecutorService("commitService-scanExecutor", scanExecutor);
        
        // Set up discovery progress forwarding
        setupDiscoveryProgressForwarding();
    }
    
    /**
     * Sets up progress forwarding from the discovery service.
     * 
     * Requirements: 4.3 - Add progress reporting for long-running operations
     */
    private void setupDiscoveryProgressForwarding() {
        discoveryService.setProgressCallback(progress -> {
            if (discoveryProgressListener != null) {
                discoveryProgressListener.onProgressUpdate(
                    progress.getDirectoriesScanned(),
                    progress.getRepositoriesFound(),
                    progress.getCurrentPath(),
                    progress.isComplete()
                );
            }
        });
    }
    
    /**
     * Sets the discovery progress listener for UI updates.
     * 
     * @param listener Listener to receive progress updates
     * 
     * Requirements: 4.3 - Add progress reporting for long-running operations
     */
    public void setDiscoveryProgressListener(DiscoveryProgressListener listener) {
        this.discoveryProgressListener = listener;
    }
    
    /**
     * Starts the monitoring service with configurable polling interval.
     */
    public void startMonitoring() {
        if (isMonitoring) {
            logger.warning("Monitoring is already active");
            return;
        }
        
        logger.info("Starting commit monitoring service");
        isMonitoring = true;
        
        // Perform initial scan
        performInitialScan();
        
        // Schedule periodic polling with configurable interval
        int pollingInterval = AppConfig.getPollingIntervalSeconds();
        pollingTask = scheduler.scheduleAtFixedRate(
            this::scanAllRepositories,
            pollingInterval,
            pollingInterval,
            TimeUnit.SECONDS
        );
        
        logger.info("Commit monitoring started with " + pollingInterval + "-second intervals");
    }
    
    /**
     * Stops the monitoring service and performs comprehensive cleanup.
     * Ensures all resources are properly released to prevent memory leaks.
     * 
     * Requirements: 1.5 - Clean up all resources including threads, caches, and file handles
     */
    public void stopMonitoring() {
        if (!isMonitoring && !isShutdown) {
            return;
        }
        
        logger.info("Stopping commit monitoring service");
        isMonitoring = false;
        isShutdown = true;
        
        // Cancel the polling task first
        if (pollingTask != null) {
            pollingTask.cancel(false);
            pollingTask = null;
        }
        
        // Cancel any ongoing repository discovery
        if (discoveryService != null) {
            discoveryService.shutdown();
        }
        
        // Also shutdown the scanner directly (in case it was used independently)
        if (repositoryScanner != null) {
            repositoryScanner.cancelDiscovery();
            repositoryScanner.shutdown();
        }
        
        // Shutdown scheduler with timeout
        shutdownExecutorService(scheduler, "scheduler");
        
        // Shutdown scan executor with timeout
        shutdownExecutorService(scanExecutor, "scanExecutor");
        
        // Shutdown retry mechanism
        if (retryMechanism != null) {
            retryMechanism.shutdown();
        }
        
        // Clear all listeners to prevent memory leaks from listener references
        clearListeners();
        
        // Unregister from ResourceManager
        ResourceManager resourceManager = ResourceManager.getInstance();
        resourceManager.cleanupResource("commitService-scheduler");
        resourceManager.cleanupResource("commitService-scanExecutor");
        
        logger.info("Commit monitoring stopped and all resources cleaned up");
    }
    
    /**
     * Shuts down an ExecutorService with proper timeout handling.
     * 
     * @param executor The executor service to shutdown
     * @param name Name for logging purposes
     */
    private void shutdownExecutorService(ExecutorService executor, String name) {
        if (executor == null || executor.isShutdown()) {
            return;
        }
        
        logger.fine("Shutting down " + name);
        executor.shutdown();
        
        try {
            if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                logger.warning(name + " did not terminate gracefully, forcing shutdown");
                List<Runnable> pendingTasks = executor.shutdownNow();
                if (!pendingTasks.isEmpty()) {
                    logger.warning(name + " had " + pendingTasks.size() + " pending tasks that were cancelled");
                }
                
                // Wait a bit more for tasks to respond to being cancelled
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.severe(name + " did not terminate after forced shutdown");
                }
            }
        } catch (InterruptedException e) {
            logger.warning(name + " shutdown interrupted, forcing immediate shutdown");
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Clears all registered listeners to prevent memory leaks.
     */
    private void clearListeners() {
        synchronized (listeners) {
            int listenerCount = listeners.size();
            listeners.clear();
            if (listenerCount > 0) {
                logger.fine("Cleared " + listenerCount + " commit listeners");
            }
        }
    }
    
    /**
     * Performs initial repository discovery and commit scanning with timeout.
     * Uses background threads to keep UI responsive.
     * 
     * Requirements: 4.3, 4.5 - Perform initial scanning in background threads
     *                         with progress reporting
     */
    private void performInitialScan() {
        logger.info("Performing initial repository scan in background");
        
        // Use configurable timeout for repository discovery
        int timeoutSeconds = AppConfig.getScanTimeoutSeconds();
        
        // Use the background discovery service for non-blocking discovery
        discoveryService.startDiscoveryWithTimeout(timeoutSeconds)
            .thenAccept(repositories -> {
                if (isShutdown) {
                    return;
                }
                
                // Limit repositories to prevent performance issues
                int maxRepos = AppConfig.getMaxRepositories();
                List<Repository> limitedRepos = repositories;
                if (repositories.size() > maxRepos) {
                    logger.info("Found " + repositories.size() + " repositories, limiting to " + maxRepos + " for performance");
                    limitedRepos = repositories.subList(0, maxRepos);
                }
                
                logger.info("Discovered " + limitedRepos.size() + " repositories");
                
                // Process commits in background to keep UI responsive
                processRepositoriesInBackground(limitedRepos);
            })
            .exceptionally(e -> {
                logger.log(Level.SEVERE, "Initial scan failed", e);
                return null;
            });
    }
    
    /**
     * Processes repositories in background threads to avoid blocking UI.
     * 
     * Requirements: 4.3, 4.5 - Background processing with progress reporting
     */
    private void processRepositoriesInBackground(List<Repository> repositories) {
        if (isShutdown || repositories.isEmpty()) {
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            List<Commit> allCommits = new ArrayList<>();
            LocalDateTime since = LocalDateTime.now().minusDays(30); // Get last 30 days of commits
            
            for (Repository repo : repositories) {
                if (isShutdown) {
                    break;
                }
                try {
                    List<Commit> repoCommits = getCommitsSince(repo, since);
                    allCommits.addAll(repoCommits);
                    repo.setLastScanned(LocalDateTime.now());
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to scan repository: " + repo.getName(), e);
                    repo.setAccessible(false);
                }
            }
            
            if (!isShutdown) {
                processNewCommits(allCommits);
                logger.info("Initial scan completed. Found " + allCommits.size() + " commits");
            }
        }, scanExecutor).exceptionally(e -> {
            logger.log(Level.WARNING, "Background repository processing failed", e);
            return null;
        });
    }
    
    /**
     * Scans all repositories for new commits.
     * Implements retry mechanism for transient failures and graceful error handling.
     * 
     * Requirements: 3.1 - Use configurable timeouts to prevent hanging operations
     */
    public void scanAllRepositories() {
        if (!isMonitoring || isShutdown) {
            return;
        }
        
        logger.fine("Starting periodic repository scan");
        
        try {
            List<Repository> repositories = repositoryScanner.getDiscoveredRepositories();
            List<Future<List<Commit>>> futures = new ArrayList<>();
            
            for (Repository repo : repositories) {
                if (repo.isAccessible() && !isShutdown) {
                    Future<List<Commit>> future = scanExecutor.submit(() -> {
                        // Check for shutdown before starting work
                        if (isShutdown) {
                            return Collections.<Commit>emptyList();
                        }
                        
                        try {
                            LocalDateTime since = repo.getLastScanned() != null ? 
                                repo.getLastScanned() : LocalDateTime.now().minusHours(6);
                            
                            // Use retry mechanism for potentially transient failures
                            RetryMechanism.RetryResult<List<Commit>> result = retryMechanism.executeWithRetry(
                                () -> getCommitsSince(repo, since),
                                "scan repository " + repo.getName(),
                                e -> RetryMechanism.isRetryableException(e) && !RetryMechanism.isAuthenticationException(e)
                            );
                            
                            if (result.isSuccess()) {
                                repo.setLastScanned(LocalDateTime.now());
                                return result.getResult();
                            } else {
                                // Mark repository as inaccessible after all retries failed
                                if (RetryMechanism.isAuthenticationException(result.getLastError())) {
                                    ErrorHandler.handleGitAuthenticationError(repo.getName(), result.getLastError());
                                }
                                repo.setAccessible(false);
                                return Collections.<Commit>emptyList();
                            }
                            
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "Failed to scan repository: " + repo.getName(), e);
                            repo.setAccessible(false);
                            return Collections.<Commit>emptyList();
                        }
                    });
                    futures.add(future);
                }
            }
            
            // Collect results with timeout to prevent hanging
            List<Commit> newCommits = new ArrayList<>();
            for (Future<List<Commit>> future : futures) {
                if (isShutdown) {
                    // Cancel remaining futures if shutting down
                    future.cancel(true);
                    continue;
                }
                
                try {
                    List<Commit> commits = future.get(TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    newCommits.addAll(commits);
                } catch (TimeoutException e) {
                    logger.log(Level.WARNING, "Repository scan task timed out after " + TASK_TIMEOUT_SECONDS + " seconds");
                    future.cancel(true); // Cancel the timed-out task
                    ErrorHandler.handleNetworkError("repository scan", e, false);
                } catch (CancellationException e) {
                    logger.fine("Repository scan task was cancelled");
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Repository scan task failed", e);
                }
            }
            
            if (!newCommits.isEmpty() && !isShutdown) {
                processNewCommits(newCommits);
                logger.info("Periodic scan found " + newCommits.size() + " new commits");
            }
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Periodic scan failed", e);
            ErrorHandler.handleNetworkError("periodic repository scan", e, true);
        }
    }
    
    /**
     * Gets commits from a repository since a specific date.
     * Uses Windows credential storage for authentication when available.
     * Implements graceful error handling for authentication and access failures.
     */
    public List<Commit> getCommitsSince(Repository repository, LocalDateTime since) {
        List<Commit> commits = new ArrayList<>();
        
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            org.eclipse.jgit.lib.Repository jgitRepo = builder
                .setGitDir(repository.getPath().resolve(".git").toFile())
                .readEnvironment()
                .findGitDir()
                .build();
            
            try (Git git = new Git(jgitRepo)) {
                // Set up credentials provider if available
                CredentialsProvider credentialsProvider = null;
                if (repository.getRemoteUrl() != null) {
                    credentialsProvider = createCredentialsProvider(repository.getRemoteUrl());
                }
                
                LogCommand logCommand = git.log().setMaxCount(AppConfig.getMaxCommitsPerRepo());
                
                Iterable<RevCommit> revCommits = logCommand.call();
                
                for (RevCommit revCommit : revCommits) {
                    LocalDateTime commitTime = LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(revCommit.getCommitTime()),
                        ZoneId.systemDefault()
                    );
                    
                    if (commitTime.isAfter(since)) {
                        PersonIdent author = revCommit.getAuthorIdent();
                        
                        Commit commit = new Commit(
                            revCommit.getName(),
                            revCommit.getShortMessage(),
                            author.getName(),
                            commitTime,
                            repository.getName(),
                            repository.getPath().toString()
                        );
                        
                        commits.add(commit);
                    }
                }
            }
            
            jgitRepo.close();
            
        } catch (Exception e) {
            // Classify and handle the error appropriately
            if (RetryMechanism.isAuthenticationException(e)) {
                // Handle authentication failure
                ErrorHandler.handleGitAuthenticationError(repository.getName(), e);
                
                // Try to handle authentication failure with stored credentials
                if (handleAuthenticationFailure(repository)) {
                    logger.info("Authentication credentials found, repository may be accessible on retry");
                } else {
                    logger.warning("No authentication credentials available for repository: " + repository.getName());
                }
            } else if (RetryMechanism.isRetryableException(e)) {
                // Network or transient error - will be retried by caller
                ErrorHandler.handleNetworkError("reading commits from " + repository.getName(), e, true);
            } else {
                // Repository access error
                ErrorHandler.handleRepositoryAccessError(repository.getPath().toString(), e);
            }
            
            logger.log(Level.WARNING, "Failed to get commits from repository: " + repository.getName(), e);
            throw new RuntimeException("Failed to read commits from repository", e);
        }
        
        return commits;
    }
    
    /**
     * Processes new commits and updates the commit history.
     */
    public void processNewCommits(List<Commit> newCommits) {
        if (newCommits.isEmpty()) {
            return;
        }
        
        // Sort commits by timestamp
        newCommits.sort(Comparator.comparing(Commit::getTimestamp));
        
        // Add commits to history
        for (Commit commit : newCommits) {
            commitHistory.addCommit(commit);
        }
        
        // Update calculated fields
        commitHistory.calculateCurrentStreak();
        commitHistory.calculateAverageCommitsPerDay();
        
        // Notify listeners
        notifyListeners(newCommits);
        
        logger.fine("Processed " + newCommits.size() + " new commits");
    }
    
    /**
     * Gets the current commit history.
     */
    public CommitHistory getCommitHistory() {
        return commitHistory;
    }
    
    /**
     * Adds a commit listener.
     */
    public void addCommitListener(CommitListener listener) {
        if (listener != null && !isShutdown) {
            synchronized (listeners) {
                listeners.add(listener);
            }
        }
    }
    
    /**
     * Removes a commit listener.
     */
    public void removeCommitListener(CommitListener listener) {
        if (listener != null) {
            synchronized (listeners) {
                listeners.remove(listener);
            }
        }
    }
    
    /**
     * Notifies all listeners of new commits.
     */
    private void notifyListeners(List<Commit> newCommits) {
        if (isShutdown) {
            return;
        }
        
        List<CommitListener> listenersCopy;
        synchronized (listeners) {
            listenersCopy = new ArrayList<>(listeners);
        }
        
        for (CommitListener listener : listenersCopy) {
            try {
                listener.onNewCommits(newCommits);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Commit listener failed", e);
            }
        }
    }
    
    /**
     * Checks if the service is currently monitoring.
     */
    public boolean isMonitoring() {
        return isMonitoring && !isShutdown;
    }
    
    /**
     * Checks if the service has been shutdown.
     */
    public boolean isShutdown() {
        return isShutdown;
    }
    
    /**
     * Gets the repository scanner.
     */
    public RepositoryScanner getRepositoryScanner() {
        return repositoryScanner;
    }
    
    /**
     * Gets the background repository discovery service.
     * Useful for monitoring discovery progress.
     * 
     * Requirements: 4.3 - Add progress reporting for long-running operations
     */
    public BackgroundRepositoryDiscoveryService getDiscoveryService() {
        return discoveryService;
    }
    
    /**
     * Creates a credentials provider for Git operations using Windows credential storage.
     * Attempts to retrieve stored credentials for the repository URL.
     * 
     * @param repositoryUrl The Git repository URL
     * @return CredentialsProvider if credentials are found, null otherwise
     */
    private CredentialsProvider createCredentialsProvider(String repositoryUrl) {
        if (!WindowsIntegration.isWindows()) {
            return null;
        }
        
        try {
            String[] credentials = WindowsIntegration.retrieveGitCredentials(repositoryUrl);
            if (credentials != null && credentials.length >= 2) {
                String username = credentials[0];
                String token = credentials.length > 1 ? credentials[1] : "";
                
                if (username != null && !username.isEmpty()) {
                    logger.fine("Using stored credentials for repository: " + repositoryUrl);
                    return new UsernamePasswordCredentialsProvider(username, token);
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to retrieve credentials for: " + repositoryUrl, e);
        }
        
        return null;
    }
    
    /**
     * Stores Git credentials securely using Windows credential storage.
     * This method can be called when authentication is required for a repository.
     * 
     * @param repositoryUrl The Git repository URL
     * @param username The username for authentication
     * @param token The access token or password
     * @return true if credentials were stored successfully
     */
    public boolean storeGitCredentials(String repositoryUrl, String username, String token) {
        if (!WindowsIntegration.isWindows()) {
            logger.warning("Windows credential storage not available on this platform");
            return false;
        }
        
        try {
            boolean success = WindowsIntegration.storeGitCredentials(repositoryUrl, username, token);
            if (success) {
                logger.info("Git credentials stored successfully for: " + repositoryUrl);
            } else {
                logger.warning("Failed to store Git credentials for: " + repositoryUrl);
            }
            return success;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error storing Git credentials for: " + repositoryUrl, e);
            return false;
        }
    }
    
    /**
     * Handles authentication failures by attempting to use Windows credential storage.
     * This method is called when Git operations fail due to authentication issues.
     * 
     * @param repository The repository that failed authentication
     * @return true if credentials were found and can be retried
     */
    private boolean handleAuthenticationFailure(Repository repository) {
        if (!WindowsIntegration.isWindows()) {
            return false;
        }
        
        try {
            String repositoryUrl = repository.getRemoteUrl();
            if (repositoryUrl == null || repositoryUrl.isEmpty()) {
                return false;
            }
            
            String[] credentials = WindowsIntegration.retrieveGitCredentials(repositoryUrl);
            if (credentials != null && credentials.length >= 1) {
                logger.info("Found stored credentials for repository: " + repository.getName());
                return true;
            } else {
                logger.warning("No stored credentials found for repository: " + repository.getName());
                return false;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to handle authentication for: " + repository.getName(), e);
            return false;
        }
    }
    
    /**
     * Interface for listening to commit events.
     */
    public interface CommitListener {
        void onNewCommits(List<Commit> commits);
    }
}