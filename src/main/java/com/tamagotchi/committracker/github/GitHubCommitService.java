package com.tamagotchi.committracker.github;

import com.tamagotchi.committracker.domain.Commit;
import com.tamagotchi.committracker.domain.CommitHistory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for tracking commits via GitHub API.
 * Integrates with existing CommitService and provides GitHub-based commit tracking.
 * 
 * Requirements: 4.1, 4.2, 4.3, 4.5, 4.6
 */
public class GitHubCommitService {
    
    private static final Logger LOGGER = Logger.getLogger(GitHubCommitService.class.getName());
    
    private final GitHubApiClient apiClient;
    private final RateLimitManager rateLimitManager;
    private final CommitCache commitCache;
    private final RepositoryCache repositoryCache;
    private final CommitBaselineManager baselineManager;
    
    private final ScheduledExecutorService scheduler;
    private final List<GitHubCommitListener> listeners;
    private final AtomicBoolean isPolling;
    
    private ScheduledFuture<?> pollingTask;
    private volatile boolean isInitialized;
    
    /**
     * Listener interface for GitHub commit events.
     */
    public interface GitHubCommitListener {
        /**
         * Called when new commits are detected.
         * @param commits the new commits (converted to domain Commit objects)
         */
        void onNewCommits(List<Commit> commits);
        
        /**
         * Called when sync completes.
         * @param totalCommits total number of commits synced
         */
        void onSyncComplete(int totalCommits);
        
        /**
         * Called when an error occurs.
         * @param error the error message
         */
        void onError(String error);
    }
    
    /**
     * Creates a GitHubCommitService with default components.
     */
    public GitHubCommitService() {
        this(new GitHubApiClientImpl(), new RateLimitManagerImpl(), 
             new CommitCache(), new RepositoryCache(), new CommitBaselineManager());
    }
    
    /**
     * Creates a GitHubCommitService with custom components.
     */
    public GitHubCommitService(GitHubApiClient apiClient, RateLimitManager rateLimitManager,
                               CommitCache commitCache, RepositoryCache repositoryCache,
                               CommitBaselineManager baselineManager) {
        this.apiClient = apiClient;
        this.rateLimitManager = rateLimitManager;
        this.commitCache = commitCache;
        this.repositoryCache = repositoryCache;
        this.baselineManager = baselineManager;
        
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "GitHubCommitService-Poller");
            t.setDaemon(true);
            return t;
        });
        this.listeners = Collections.synchronizedList(new ArrayList<>());
        this.isPolling = new AtomicBoolean(false);
        this.isInitialized = false;
    }
    
    /**
     * Initializes the service with an access token.
     * Sets the commit baseline if not already set.
     * 
     * @param accessToken the GitHub OAuth access token
     */
    public void initialize(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Access token cannot be null or blank");
        }
        
        apiClient.setAccessToken(accessToken);
        
        // Set baseline to 30 days ago if not already set, to capture recent commit history
        if (!baselineManager.hasBaseline()) {
            baselineManager.setBaseline(Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS));
            LOGGER.info("Set commit baseline to 30 days ago");
        }
        
        isInitialized = true;
        LOGGER.info("GitHubCommitService initialized");
    }
    
    /**
     * Performs initial sync of commits since baseline date.
     * Fetches all repositories and their commits since the baseline.
     * 
     * Requirements: 4.1, 4.3, 4.6
     * 
     * @return CompletableFuture containing the sync result
     */
    public CompletableFuture<SyncResult> performInitialSync() {
        if (!isInitialized) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Service not initialized. Call initialize() first."));
        }
        
        LOGGER.info("Starting initial sync...");
        
        return apiClient.fetchUserRepositories()
            .thenCompose(repositories -> {
                LOGGER.info("Found " + repositories.size() + " repositories");
                
                // Cache repositories
                repositoryCache.cacheRepositories(repositories);
                final int repoCount = repositories.size();
                
                // Fetch commits from each repository
                return fetchCommitsFromAllRepositories(repositories)
                    .thenApply(allCommits -> {
                        // Filter by baseline and cache
                        Instant baseline = getCommitBaseline();
                        List<GitHubCommit> filteredCommits = filterCommitsByBaseline(allCommits, baseline);
                        
                        // Cache commits
                        cacheCommitsByRepository(filteredCommits);
                        
                        // Convert to domain commits and notify listeners
                        List<Commit> domainCommits = convertToDomainCommits(filteredCommits);
                        notifyNewCommits(domainCommits);
                        notifySyncComplete(domainCommits.size());
                        
                        LOGGER.info("Initial sync complete. Found " + domainCommits.size() + " commits since baseline.");
                        return new SyncResult(domainCommits.size(), repoCount, true, null);
                    });
            })
            .exceptionally(e -> {
                String error = "Initial sync failed: " + e.getMessage();
                LOGGER.log(Level.SEVERE, error, e);
                notifyError(error);
                return new SyncResult(0, 0, false, error);
            });
    }

    
    /**
     * Polls for new commits across all repositories.
     * Uses conditional requests (ETag) to minimize API usage.
     * 
     * Requirements: 4.1, 4.5, 6.1, 6.2
     * 
     * @return CompletableFuture containing list of new commits
     */
    public CompletableFuture<List<Commit>> pollForNewCommits() {
        if (!isInitialized) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Service not initialized. Call initialize() first."));
        }
        
        // Check rate limit before polling
        if (!rateLimitManager.canMakeRequest()) {
            LOGGER.warning("Rate limit reached, skipping poll");
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        
        LOGGER.fine("Polling for new commits...");
        
        // Get cached repositories or fetch fresh
        List<GitHubRepository> repositories = repositoryCache.getCachedRepositories();
        if (repositories.isEmpty()) {
            return apiClient.fetchUserRepositories()
                .thenCompose(repos -> {
                    repositoryCache.cacheRepositories(repos);
                    return fetchNewCommitsFromRepositories(repos);
                });
        }
        
        return fetchNewCommitsFromRepositories(repositories);
    }
    
    /**
     * Starts automatic polling for new commits.
     * Uses adaptive polling intervals based on rate limit status.
     * 
     * Requirements: 5.2, 6.3, 6.4
     */
    public void startPolling() {
        if (!isInitialized) {
            throw new IllegalStateException("Service not initialized. Call initialize() first.");
        }
        
        if (isPolling.getAndSet(true)) {
            LOGGER.warning("Polling already started");
            return;
        }
        
        scheduleNextPoll();
        LOGGER.info("Started GitHub commit polling");
    }
    
    /**
     * Stops automatic polling.
     */
    public void stopPolling() {
        isPolling.set(false);
        
        if (pollingTask != null) {
            pollingTask.cancel(false);
            pollingTask = null;
        }
        
        LOGGER.info("Stopped GitHub commit polling");
    }
    
    /**
     * Schedules the next poll based on current rate limit status.
     */
    private void scheduleNextPoll() {
        if (!isPolling.get()) {
            return;
        }
        
        long intervalMs = rateLimitManager.getCurrentPollingInterval().toMillis();
        
        pollingTask = scheduler.schedule(() -> {
            pollForNewCommits()
                .thenAccept(commits -> {
                    if (!commits.isEmpty()) {
                        LOGGER.info("Found " + commits.size() + " new commits");
                    }
                })
                .exceptionally(e -> {
                    LOGGER.log(Level.WARNING, "Poll failed", e);
                    notifyError("Poll failed: " + e.getMessage());
                    return null;
                })
                .thenRun(this::scheduleNextPoll);
        }, intervalMs, TimeUnit.MILLISECONDS);
        
        LOGGER.fine("Next poll scheduled in " + intervalMs + "ms");
    }
    
    /**
     * Gets the commit baseline date.
     * 
     * Requirement 4.2: Authentication date as baseline
     * 
     * @return the baseline instant
     */
    public Instant getCommitBaseline() {
        return baselineManager.getBaseline().orElse(Instant.now());
    }
    
    /**
     * Sets a new commit baseline date.
     * 
     * @param baseline the new baseline
     */
    public void setCommitBaseline(Instant baseline) {
        baselineManager.setBaseline(baseline);
        LOGGER.info("Commit baseline updated to: " + baseline);
    }
    
    /**
     * Gets cached commits for offline viewing.
     * 
     * Requirement 7.1: Continue displaying cached commit data when offline
     * 
     * @return list of cached commits converted to domain objects
     */
    public List<Commit> getCachedCommits() {
        List<GitHubCommit> cached = commitCache.getAllCachedCommits();
        return convertToDomainCommits(cached);
    }
    
    /**
     * Gets cached commits since a specific date.
     * 
     * @param since only return commits after this date
     * @return list of commits after the specified date
     */
    public List<Commit> getCachedCommitsSince(Instant since) {
        List<GitHubCommit> cached = commitCache.getCommitsSince(since);
        return convertToDomainCommits(cached);
    }
    
    /**
     * Adds a listener for commit events.
     */
    public void addListener(GitHubCommitListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
    
    /**
     * Removes a listener.
     */
    public void removeListener(GitHubCommitListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Checks if the service is initialized.
     */
    public boolean isInitialized() {
        return isInitialized;
    }
    
    /**
     * Checks if polling is active.
     */
    public boolean isPolling() {
        return isPolling.get();
    }
    
    /**
     * Shuts down the service and releases resources.
     */
    public void shutdown() {
        stopPolling();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        listeners.clear();
        isInitialized = false;
        LOGGER.info("GitHubCommitService shutdown complete");
    }

    
    // ==================== Private Helper Methods ====================
    
    /**
     * Fetches commits from all repositories.
     */
    private CompletableFuture<List<GitHubCommit>> fetchCommitsFromAllRepositories(
            List<GitHubRepository> repositories) {
        
        List<CompletableFuture<List<GitHubCommit>>> futures = new ArrayList<>();
        Instant baseline = getCommitBaseline();
        
        for (GitHubRepository repo : repositories) {
            if (!rateLimitManager.canMakeRequest()) {
                LOGGER.warning("Rate limit reached, stopping repository fetch");
                break;
            }
            
            String etag = commitCache.getEtag(repo.fullName()).orElse(null);
            
            CompletableFuture<List<GitHubCommit>> future = apiClient
                .fetchCommits(repo.owner(), repo.name(), baseline, etag)
                .thenApply(result -> {
                    if (result.wasModified() && result.hasCommits()) {
                        // Cache the commits with new ETag
                        commitCache.cacheCommits(repo.fullName(), result.commits(), result.newEtag());
                        return result.commits();
                    }
                    return Collections.<GitHubCommit>emptyList();
                })
                .exceptionally(e -> {
                    LOGGER.log(Level.WARNING, "Failed to fetch commits for " + repo.fullName(), e);
                    return Collections.emptyList();
                });
            
            futures.add(future);
        }
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                List<GitHubCommit> allCommits = new ArrayList<>();
                for (CompletableFuture<List<GitHubCommit>> future : futures) {
                    allCommits.addAll(future.join());
                }
                return allCommits;
            });
    }
    
    /**
     * Fetches new commits from repositories using conditional requests.
     */
    private CompletableFuture<List<Commit>> fetchNewCommitsFromRepositories(
            List<GitHubRepository> repositories) {
        
        List<CompletableFuture<List<GitHubCommit>>> futures = new ArrayList<>();
        Instant baseline = getCommitBaseline();
        
        for (GitHubRepository repo : repositories) {
            if (!rateLimitManager.canMakeRequest()) {
                break;
            }
            
            String etag = commitCache.getEtag(repo.fullName()).orElse(null);
            
            CompletableFuture<List<GitHubCommit>> future = apiClient
                .fetchCommits(repo.owner(), repo.name(), baseline, etag)
                .thenApply(result -> {
                    if (result.wasModified() && result.hasCommits()) {
                        // Find truly new commits (not already cached)
                        List<GitHubCommit> cached = commitCache.getCachedCommits(repo.fullName());
                        List<GitHubCommit> newCommits = findNewCommits(result.commits(), cached);
                        
                        if (!newCommits.isEmpty()) {
                            // Cache all commits with new ETag
                            commitCache.cacheCommits(repo.fullName(), result.commits(), result.newEtag());
                        }
                        
                        return newCommits;
                    }
                    return Collections.<GitHubCommit>emptyList();
                })
                .exceptionally(e -> {
                    LOGGER.log(Level.WARNING, "Failed to fetch commits for " + repo.fullName(), e);
                    return Collections.emptyList();
                });
            
            futures.add(future);
        }
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                List<GitHubCommit> allNewCommits = new ArrayList<>();
                for (CompletableFuture<List<GitHubCommit>> future : futures) {
                    allNewCommits.addAll(future.join());
                }
                
                // Filter by baseline and convert
                List<GitHubCommit> filtered = filterCommitsByBaseline(allNewCommits, baseline);
                List<Commit> domainCommits = convertToDomainCommits(filtered);
                
                if (!domainCommits.isEmpty()) {
                    notifyNewCommits(domainCommits);
                }
                
                return domainCommits;
            });
    }
    
    /**
     * Finds commits that are not already in the cached list.
     */
    private List<GitHubCommit> findNewCommits(List<GitHubCommit> fetched, List<GitHubCommit> cached) {
        if (cached.isEmpty()) {
            return fetched;
        }
        
        java.util.Set<String> cachedShas = new java.util.HashSet<>();
        for (GitHubCommit c : cached) {
            cachedShas.add(c.sha());
        }
        
        List<GitHubCommit> newCommits = new ArrayList<>();
        for (GitHubCommit c : fetched) {
            if (!cachedShas.contains(c.sha())) {
                newCommits.add(c);
            }
        }
        
        return newCommits;
    }
    
    /**
     * Filters commits to only include those after the baseline date.
     * 
     * Requirement 4.3: Only retrieve commits made after the baseline date
     */
    private List<GitHubCommit> filterCommitsByBaseline(List<GitHubCommit> commits, Instant baseline) {
        if (baseline == null) {
            return commits;
        }
        
        List<GitHubCommit> filtered = new ArrayList<>();
        for (GitHubCommit commit : commits) {
            if (commit.committedAt() != null && !commit.committedAt().isBefore(baseline)) {
                filtered.add(commit);
            }
        }
        return filtered;
    }
    
    /**
     * Caches commits grouped by repository.
     */
    private void cacheCommitsByRepository(List<GitHubCommit> commits) {
        java.util.Map<String, List<GitHubCommit>> byRepo = new java.util.HashMap<>();
        
        for (GitHubCommit commit : commits) {
            byRepo.computeIfAbsent(commit.repositoryFullName(), k -> new ArrayList<>()).add(commit);
        }
        
        for (java.util.Map.Entry<String, List<GitHubCommit>> entry : byRepo.entrySet()) {
            commitCache.cacheCommits(entry.getKey(), entry.getValue(), null);
        }
    }
    
    /**
     * Converts GitHub commits to domain Commit objects.
     */
    private List<Commit> convertToDomainCommits(List<GitHubCommit> githubCommits) {
        List<Commit> commits = new ArrayList<>();
        
        for (GitHubCommit gc : githubCommits) {
            LocalDateTime timestamp = gc.committedAt() != null 
                ? LocalDateTime.ofInstant(gc.committedAt(), ZoneId.systemDefault())
                : LocalDateTime.now();
            
            Commit commit = new Commit(
                gc.sha(),
                gc.message(),
                gc.authorName(),
                timestamp,
                gc.repositoryFullName(),
                gc.url()  // Use URL as repository path for GitHub commits
            );
            
            commits.add(commit);
        }
        
        // Sort by timestamp (newest first)
        commits.sort((a, b) -> {
            if (a.getTimestamp() == null && b.getTimestamp() == null) return 0;
            if (a.getTimestamp() == null) return 1;
            if (b.getTimestamp() == null) return -1;
            return b.getTimestamp().compareTo(a.getTimestamp());
        });
        
        return commits;
    }
    
    // ==================== Notification Methods ====================
    
    private void notifyNewCommits(List<Commit> commits) {
        if (commits.isEmpty()) return;
        
        for (GitHubCommitListener listener : listeners) {
            try {
                listener.onNewCommits(commits);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Listener error on new commits", e);
            }
        }
    }
    
    private void notifySyncComplete(int totalCommits) {
        for (GitHubCommitListener listener : listeners) {
            try {
                listener.onSyncComplete(totalCommits);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Listener error on sync complete", e);
            }
        }
    }
    
    private void notifyError(String error) {
        for (GitHubCommitListener listener : listeners) {
            try {
                listener.onError(error);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Listener error on error notification", e);
            }
        }
    }
    
    // ==================== Inner Classes ====================
    
    /**
     * Result of a sync operation.
     */
    public record SyncResult(
        int commitCount,
        int repositoryCount,
        boolean success,
        String error
    ) {
        public boolean hasError() {
            return error != null && !error.isBlank();
        }
    }
}
