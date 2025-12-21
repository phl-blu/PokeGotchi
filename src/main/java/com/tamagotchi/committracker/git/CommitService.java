package com.tamagotchi.committracker.git;

import com.tamagotchi.committracker.domain.Commit;
import com.tamagotchi.committracker.domain.CommitHistory;
import com.tamagotchi.committracker.domain.Repository;
import com.tamagotchi.committracker.config.AppConfig;
import com.tamagotchi.committracker.util.WindowsIntegration;
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
 */
public class CommitService {
    private static final Logger logger = Logger.getLogger(CommitService.class.getName());
    private static final int MAX_COMMITS_PER_REPO = 100;
    
    private final RepositoryScanner repositoryScanner;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService scanExecutor;
    private final CommitHistory commitHistory;
    private final List<CommitListener> listeners;
    
    private volatile boolean isMonitoring = false;
    private ScheduledFuture<?> pollingTask;
    
    public CommitService() {
        this.repositoryScanner = new RepositoryScanner();
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.scanExecutor = Executors.newFixedThreadPool(4);
        this.commitHistory = new CommitHistory();
        this.listeners = new ArrayList<>();
    }
    
    public CommitService(RepositoryScanner repositoryScanner) {
        this.repositoryScanner = repositoryScanner;
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.scanExecutor = Executors.newFixedThreadPool(4);
        this.commitHistory = new CommitHistory();
        this.listeners = new ArrayList<>();
    }
    
    /**
     * Starts the monitoring service with 5-minute polling.
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
        
        // Schedule periodic polling
        pollingTask = scheduler.scheduleAtFixedRate(
            this::scanAllRepositories,
            AppConfig.POLLING_INTERVAL_SECONDS,
            AppConfig.POLLING_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );
        
        logger.info("Commit monitoring started with " + AppConfig.POLLING_INTERVAL_SECONDS + "-second intervals");
    }
    
    /**
     * Stops the monitoring service.
     */
    public void stopMonitoring() {
        if (!isMonitoring) {
            return;
        }
        
        logger.info("Stopping commit monitoring service");
        isMonitoring = false;
        
        if (pollingTask != null) {
            pollingTask.cancel(false);
        }
        
        scheduler.shutdown();
        scanExecutor.shutdown();
        
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            if (!scanExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                scanExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            scanExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("Commit monitoring stopped");
    }
    
    /**
     * Performs initial repository discovery and commit scanning.
     */
    private void performInitialScan() {
        logger.info("Performing initial repository scan");
        
        try {
            // Use a timeout for repository discovery to avoid hanging
            CompletableFuture<List<Repository>> discoveryFuture = CompletableFuture.supplyAsync(
                () -> repositoryScanner.discoverRepositories(),
                scanExecutor
            );
            
            List<Repository> repositories = discoveryFuture.get(60, TimeUnit.SECONDS);
            logger.info("Discovered " + repositories.size() + " repositories");
            
            List<Commit> allCommits = new ArrayList<>();
            LocalDateTime since = LocalDateTime.now().minusDays(30); // Get last 30 days of commits
            
            for (Repository repo : repositories) {
                try {
                    List<Commit> repoCommits = getCommitsSince(repo, since);
                    allCommits.addAll(repoCommits);
                    repo.setLastScanned(LocalDateTime.now());
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to scan repository: " + repo.getName(), e);
                    repo.setAccessible(false);
                }
            }
            
            processNewCommits(allCommits);
            logger.info("Initial scan completed. Found " + allCommits.size() + " commits");
            
        } catch (TimeoutException e) {
            logger.log(Level.WARNING, "Initial repository discovery timed out after 60 seconds", e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Initial scan failed", e);
        }
    }
    
    /**
     * Scans all repositories for new commits.
     */
    public void scanAllRepositories() {
        if (!isMonitoring) {
            return;
        }
        
        logger.fine("Starting periodic repository scan");
        
        try {
            List<Repository> repositories = repositoryScanner.getDiscoveredRepositories();
            List<Future<List<Commit>>> futures = new ArrayList<>();
            
            for (Repository repo : repositories) {
                if (repo.isAccessible()) {
                    Future<List<Commit>> future = scanExecutor.submit(() -> {
                        try {
                            LocalDateTime since = repo.getLastScanned() != null ? 
                                repo.getLastScanned() : LocalDateTime.now().minusHours(6);
                            
                            List<Commit> commits = getCommitsSince(repo, since);
                            repo.setLastScanned(LocalDateTime.now());
                            return commits;
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "Failed to scan repository: " + repo.getName(), e);
                            repo.setAccessible(false);
                            return Collections.emptyList();
                        }
                    });
                    futures.add(future);
                }
            }
            
            // Collect results
            List<Commit> newCommits = new ArrayList<>();
            for (Future<List<Commit>> future : futures) {
                try {
                    newCommits.addAll(future.get(30, TimeUnit.SECONDS));
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Repository scan task failed", e);
                }
            }
            
            if (!newCommits.isEmpty()) {
                processNewCommits(newCommits);
                logger.info("Periodic scan found " + newCommits.size() + " new commits");
            }
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Periodic scan failed", e);
        }
    }
    
    /**
     * Gets commits from a repository since a specific date.
     * Uses Windows credential storage for authentication when available.
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
                
                LogCommand logCommand = git.log().setMaxCount(MAX_COMMITS_PER_REPO);
                
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
            logger.log(Level.WARNING, "Failed to get commits from repository: " + repository.getName(), e);
            
            // Try to handle authentication failure
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("auth")) {
                if (handleAuthenticationFailure(repository)) {
                    logger.info("Authentication credentials found, repository may be accessible on retry");
                } else {
                    logger.warning("No authentication credentials available for repository: " + repository.getName());
                }
            }
            
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
        listeners.add(listener);
    }
    
    /**
     * Removes a commit listener.
     */
    public void removeCommitListener(CommitListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Notifies all listeners of new commits.
     */
    private void notifyListeners(List<Commit> newCommits) {
        for (CommitListener listener : listeners) {
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
        return isMonitoring;
    }
    
    /**
     * Gets the repository scanner.
     */
    public RepositoryScanner getRepositoryScanner() {
        return repositoryScanner;
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