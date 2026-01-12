package com.tamagotchi.committracker.github;

import com.tamagotchi.committracker.domain.Commit;
import com.tamagotchi.committracker.domain.CommitHistory;
import com.tamagotchi.committracker.git.CommitService;
import com.tamagotchi.committracker.pokemon.PokemonEvolutionManager;
import com.tamagotchi.committracker.pokemon.PokemonSpecies;
import com.tamagotchi.committracker.pokemon.EvolutionStage;
import com.tamagotchi.committracker.pokemon.PokemonState;
import com.tamagotchi.committracker.ui.components.PokemonDisplayComponent;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Integration layer that connects GitHub commit tracking with the Pokemon and XP systems.
 * This class bridges the GitHubCommitService with the existing CommitService and
 * PokemonEvolutionManager to provide seamless GitHub-based commit tracking.
 * 
 * Requirements: 4.1, 4.5
 * - 4.1: Track commits authored by the authenticated user
 * - 4.5: Trigger Pokemon animations and XP updates on new commits
 */
public class GitHubCommitIntegration implements GitHubCommitService.GitHubCommitListener {
    
    private static final Logger LOGGER = Logger.getLogger(GitHubCommitIntegration.class.getName());
    
    private final GitHubCommitService gitHubCommitService;
    private final CommitHistory commitHistory;
    private final SecureTokenStorage tokenStorage;
    
    // Pokemon integration
    private PokemonEvolutionManager evolutionManager;
    private PokemonDisplayComponent displayComponent;
    
    // Callbacks for UI updates
    private Consumer<List<Commit>> onNewCommitsCallback;
    private Consumer<Integer> onXPGainedCallback;
    private Consumer<PokemonState> onStateChangedCallback;
    private Consumer<EvolutionStage> onEvolutionCallback;
    private Consumer<String> onErrorCallback;
    
    // State tracking
    private volatile boolean isInitialized;
    private volatile boolean isGitHubMode;
    
    /**
     * Creates a GitHubCommitIntegration with default components.
     */
    public GitHubCommitIntegration() {
        this(new GitHubCommitService(), new CommitHistory(), new SecureTokenStorageImpl());
    }
    
    /**
     * Creates a GitHubCommitIntegration with custom components.
     */
    public GitHubCommitIntegration(GitHubCommitService gitHubCommitService,
                                   CommitHistory commitHistory,
                                   SecureTokenStorage tokenStorage) {
        this.gitHubCommitService = gitHubCommitService;
        this.commitHistory = commitHistory;
        this.tokenStorage = tokenStorage;
        this.isInitialized = false;
        this.isGitHubMode = false;
        
        // Register as listener for GitHub commit events
        gitHubCommitService.addListener(this);
    }
    
    /**
     * Initializes the integration with stored credentials.
     * Attempts to use stored access token for authentication.
     * 
     * @return CompletableFuture that completes when initialization is done
     */
    public CompletableFuture<Boolean> initializeWithStoredCredentials() {
        Optional<String> storedToken = tokenStorage.getAccessToken();
        
        if (storedToken.isEmpty()) {
            LOGGER.info("No stored GitHub credentials found");
            return CompletableFuture.completedFuture(false);
        }
        
        return initialize(storedToken.get());
    }
    
    /**
     * Initializes the integration with a new access token.
     * 
     * @param accessToken the GitHub OAuth access token
     * @return CompletableFuture that completes when initialization is done
     */
    public CompletableFuture<Boolean> initialize(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return CompletableFuture.completedFuture(false);
        }
        
        try {
            gitHubCommitService.initialize(accessToken);
            isInitialized = true;
            isGitHubMode = true;
            
            LOGGER.info("GitHub commit integration initialized");
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize GitHub integration", e);
            notifyError("Failed to initialize GitHub integration: " + e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * Performs initial sync and starts polling for new commits.
     * 
     * @return CompletableFuture containing the sync result
     */
    public CompletableFuture<GitHubCommitService.SyncResult> startTracking() {
        if (!isInitialized) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Integration not initialized"));
        }
        
        return gitHubCommitService.performInitialSync()
            .thenApply(result -> {
                if (result.success()) {
                    // Start polling for new commits
                    gitHubCommitService.startPolling();
                    LOGGER.info("GitHub commit tracking started");
                }
                return result;
            });
    }
    
    /**
     * Stops GitHub commit tracking.
     */
    public void stopTracking() {
        gitHubCommitService.stopPolling();
        LOGGER.info("GitHub commit tracking stopped");
    }
    
    /**
     * Sets the Pokemon evolution manager for XP and evolution integration.
     * 
     * Requirement 4.5: Trigger XP updates on new commits
     * 
     * @param evolutionManager the evolution manager
     */
    public void setEvolutionManager(PokemonEvolutionManager evolutionManager) {
        this.evolutionManager = evolutionManager;
    }
    
    /**
     * Sets the Pokemon display component for animation triggers.
     * 
     * Requirement 4.5: Trigger Pokemon animations on commit detection
     * 
     * @param displayComponent the display component
     */
    public void setDisplayComponent(PokemonDisplayComponent displayComponent) {
        this.displayComponent = displayComponent;
        
        if (evolutionManager != null) {
            evolutionManager.setDisplayComponent(displayComponent);
        }
    }
    
    /**
     * Sets callback for new commits.
     */
    public void setOnNewCommitsCallback(Consumer<List<Commit>> callback) {
        this.onNewCommitsCallback = callback;
    }
    
    /**
     * Sets callback for XP gained.
     */
    public void setOnXPGainedCallback(Consumer<Integer> callback) {
        this.onXPGainedCallback = callback;
    }
    
    /**
     * Sets callback for Pokemon state changes.
     */
    public void setOnStateChangedCallback(Consumer<PokemonState> callback) {
        this.onStateChangedCallback = callback;
    }
    
    /**
     * Sets callback for evolution events.
     */
    public void setOnEvolutionCallback(Consumer<EvolutionStage> callback) {
        this.onEvolutionCallback = callback;
        
        if (evolutionManager != null) {
            evolutionManager.setEvolutionCallback(callback);
        }
    }
    
    /**
     * Sets callback for error events.
     */
    public void setOnErrorCallback(Consumer<String> callback) {
        this.onErrorCallback = callback;
    }
    
    // ==================== GitHubCommitListener Implementation ====================
    
    /**
     * Called when new commits are detected from GitHub.
     * Triggers XP updates and Pokemon animations.
     * 
     * Requirement 4.5: Trigger XP updates and Pokemon animations on new commits
     */
    @Override
    public void onNewCommits(List<Commit> commits) {
        if (commits == null || commits.isEmpty()) {
            return;
        }
        
        LOGGER.info("Processing " + commits.size() + " new GitHub commits");
        
        // Add commits to history
        for (Commit commit : commits) {
            commitHistory.addCommit(commit);
        }
        
        // Update streak tracking
        commitHistory.calculateCurrentStreak();
        commitHistory.calculateAverageCommitsPerDay();
        
        // Process commits through Pokemon evolution manager
        if (evolutionManager != null) {
            boolean stateChanged = evolutionManager.processCommitActivity(commitHistory, commits);
            
            if (stateChanged) {
                // Notify state change callback
                if (onStateChangedCallback != null) {
                    onStateChangedCallback.accept(evolutionManager.getCurrentState());
                }
            }
            
            // Notify XP gained callback
            if (onXPGainedCallback != null) {
                onXPGainedCallback.accept(evolutionManager.getCurrentXP());
            }
        } else {
            // No evolution manager - just trigger display animation if available
            if (displayComponent != null) {
                int streak = commitHistory.getCurrentStreak();
                displayComponent.triggerCommitAnimation(0, streak);
            }
        }
        
        // Notify new commits callback
        if (onNewCommitsCallback != null) {
            onNewCommitsCallback.accept(commits);
        }
        
        LOGGER.fine("Processed " + commits.size() + " commits, streak: " + commitHistory.getCurrentStreak());
    }
    
    @Override
    public void onSyncComplete(int totalCommits) {
        LOGGER.info("GitHub sync complete: " + totalCommits + " commits");
    }
    
    @Override
    public void onError(String error) {
        LOGGER.warning("GitHub commit error: " + error);
        notifyError(error);
    }
    
    // ==================== Getters ====================
    
    /**
     * Gets the commit history.
     */
    public CommitHistory getCommitHistory() {
        return commitHistory;
    }
    
    /**
     * Gets the commit baseline.
     */
    public Instant getCommitBaseline() {
        return gitHubCommitService.getCommitBaseline();
    }
    
    /**
     * Sets the commit baseline.
     */
    public void setCommitBaseline(Instant baseline) {
        gitHubCommitService.setCommitBaseline(baseline);
    }
    
    /**
     * Checks if the integration is initialized.
     */
    public boolean isInitialized() {
        return isInitialized;
    }
    
    /**
     * Checks if GitHub mode is active.
     */
    public boolean isGitHubMode() {
        return isGitHubMode;
    }
    
    /**
     * Checks if polling is active.
     */
    public boolean isPolling() {
        return gitHubCommitService.isPolling();
    }
    
    /**
     * Gets cached commits for offline viewing.
     */
    public List<Commit> getCachedCommits() {
        return gitHubCommitService.getCachedCommits();
    }
    
    /**
     * Gets the underlying GitHub commit service.
     */
    public GitHubCommitService getGitHubCommitService() {
        return gitHubCommitService;
    }
    
    // ==================== Cleanup ====================
    
    /**
     * Shuts down the integration and releases resources.
     */
    public void shutdown() {
        stopTracking();
        gitHubCommitService.removeListener(this);
        gitHubCommitService.shutdown();
        
        isInitialized = false;
        isGitHubMode = false;
        
        LOGGER.info("GitHub commit integration shutdown complete");
    }
    
    // ==================== Private Helpers ====================
    
    private void notifyError(String error) {
        if (onErrorCallback != null) {
            onErrorCallback.accept(error);
        }
    }
}
