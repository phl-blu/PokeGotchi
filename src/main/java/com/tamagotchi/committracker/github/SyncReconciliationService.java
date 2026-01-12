package com.tamagotchi.committracker.github;

import com.tamagotchi.committracker.domain.Commit;
import com.tamagotchi.committracker.pokemon.EvolutionStage;
import com.tamagotchi.committracker.pokemon.PokemonSpecies;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for reconciling local cached data with remote GitHub data.
 * Handles duplicate detection, conflict resolution, and state preservation.
 * 
 * Requirements: 7.5, 7.6
 * - 7.5: Preserve Pokemon state and XP progress locally during offline periods
 * - 7.6: Reconcile local and remote commit data when syncing after offline period
 */
public class SyncReconciliationService {
    
    private static final Logger LOGGER = Logger.getLogger(SyncReconciliationService.class.getName());
    
    private final CommitCache commitCache;
    private final OfflineCacheManager cacheManager;
    
    /**
     * Result of a reconciliation operation.
     */
    public record ReconciliationResult(
        int newCommitsAdded,
        int duplicatesSkipped,
        int conflictsResolved,
        List<GitHubCommit> newCommits,
        List<String> warnings
    ) {
        public boolean hasNewCommits() {
            return newCommitsAdded > 0;
        }
        
        public boolean hasWarnings() {
            return warnings != null && !warnings.isEmpty();
        }
    }
    
    /**
     * Creates a SyncReconciliationService with default components.
     */
    public SyncReconciliationService() {
        this(new CommitCache(), new OfflineCacheManager());
    }
    
    /**
     * Creates a SyncReconciliationService with custom components.
     */
    public SyncReconciliationService(CommitCache commitCache, OfflineCacheManager cacheManager) {
        this.commitCache = commitCache;
        this.cacheManager = cacheManager;
    }

    
    /**
     * Reconciles remote commits with locally cached commits.
     * Identifies new commits, detects duplicates, and merges data.
     * 
     * Requirement 7.6: Reconcile local and remote commit data
     * 
     * @param remoteCommits commits fetched from GitHub API
     * @param repositoryFullName the repository full name (owner/repo)
     * @return reconciliation result
     */
    public ReconciliationResult reconcileCommits(List<GitHubCommit> remoteCommits, 
                                                  String repositoryFullName) {
        if (remoteCommits == null || remoteCommits.isEmpty()) {
            return new ReconciliationResult(0, 0, 0, List.of(), List.of());
        }
        
        LOGGER.info("Reconciling " + remoteCommits.size() + " remote commits for " + repositoryFullName);
        
        // Get cached commits for this repository
        List<GitHubCommit> cachedCommits = commitCache.getCachedCommits(repositoryFullName);
        
        // Build a set of cached commit SHAs for fast lookup
        Set<String> cachedShas = new HashSet<>();
        for (GitHubCommit cached : cachedCommits) {
            cachedShas.add(cached.sha());
        }
        
        // Identify new commits (not in cache)
        List<GitHubCommit> newCommits = new ArrayList<>();
        int duplicatesSkipped = 0;
        List<String> warnings = new ArrayList<>();
        
        for (GitHubCommit remote : remoteCommits) {
            if (cachedShas.contains(remote.sha())) {
                duplicatesSkipped++;
            } else {
                newCommits.add(remote);
            }
        }
        
        // Cache the new commits
        if (!newCommits.isEmpty()) {
            commitCache.cacheCommits(repositoryFullName, newCommits, null);
            LOGGER.info("Added " + newCommits.size() + " new commits to cache");
        }
        
        return new ReconciliationResult(
            newCommits.size(),
            duplicatesSkipped,
            0, // No conflicts in commit reconciliation
            newCommits,
            warnings
        );
    }
    
    /**
     * Reconciles commits from multiple repositories.
     * 
     * @param commitsByRepo map of repository full name to commits
     * @return aggregated reconciliation result
     */
    public ReconciliationResult reconcileAllCommits(Map<String, List<GitHubCommit>> commitsByRepo) {
        int totalNew = 0;
        int totalDuplicates = 0;
        int totalConflicts = 0;
        List<GitHubCommit> allNewCommits = new ArrayList<>();
        List<String> allWarnings = new ArrayList<>();
        
        for (Map.Entry<String, List<GitHubCommit>> entry : commitsByRepo.entrySet()) {
            ReconciliationResult result = reconcileCommits(entry.getValue(), entry.getKey());
            totalNew += result.newCommitsAdded();
            totalDuplicates += result.duplicatesSkipped();
            totalConflicts += result.conflictsResolved();
            allNewCommits.addAll(result.newCommits());
            allWarnings.addAll(result.warnings());
        }
        
        return new ReconciliationResult(
            totalNew,
            totalDuplicates,
            totalConflicts,
            allNewCommits,
            allWarnings
        );
    }

    
    /**
     * Detects duplicate commits across all cached repositories.
     * Useful for identifying commits that appear in multiple repos (forks, mirrors).
     * 
     * @return list of duplicate commit SHAs
     */
    public List<String> detectDuplicates() {
        List<GitHubCommit> allCommits = commitCache.getAllCachedCommits();
        
        Map<String, Integer> shaCount = new HashMap<>();
        for (GitHubCommit commit : allCommits) {
            shaCount.merge(commit.sha(), 1, Integer::sum);
        }
        
        List<String> duplicates = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : shaCount.entrySet()) {
            if (entry.getValue() > 1) {
                duplicates.add(entry.getKey());
            }
        }
        
        if (!duplicates.isEmpty()) {
            LOGGER.info("Found " + duplicates.size() + " duplicate commits across repositories");
        }
        
        return duplicates;
    }
    
    /**
     * Preserves Pokemon state during offline periods.
     * Ensures XP and evolution progress are not lost.
     * 
     * Requirement 7.5: Preserve Pokemon state and XP progress locally
     * 
     * @param species the current Pokemon species
     * @param stage the current evolution stage
     * @param currentXP the current XP
     * @param currentStreak the current commit streak
     */
    public void preservePokemonState(PokemonSpecies species, EvolutionStage stage,
                                     int currentXP, int currentStreak) {
        cacheManager.updatePokemonState(species, stage, currentXP, currentStreak);
        LOGGER.info("Pokemon state preserved: " + species + " at " + stage + 
                   " with " + currentXP + " XP, " + currentStreak + " day streak");
    }
    
    /**
     * Restores Pokemon state from cache.
     * 
     * Requirement 7.5: Preserve Pokemon state and XP progress locally
     * 
     * @return the cached Pokemon state, or null if not available
     */
    public PokemonStateSnapshot restorePokemonState() {
        return cacheManager.getUserPreferences()
            .map(prefs -> new PokemonStateSnapshot(
                prefs.selectedPokemon,
                prefs.evolutionStage,
                prefs.totalXP,
                prefs.currentStreak,
                prefs.lastUpdated
            ))
            .orElse(null);
    }
    
    /**
     * Snapshot of Pokemon state for restoration.
     */
    public record PokemonStateSnapshot(
        PokemonSpecies species,
        EvolutionStage stage,
        int totalXP,
        int currentStreak,
        Instant lastUpdated
    ) {}

    
    /**
     * Reconciles commit history after an offline period.
     * Merges cached commits with newly fetched commits, handling any conflicts.
     * 
     * Requirement 7.6: Reconcile local and remote commit data
     * 
     * @param fetchedCommits newly fetched commits from GitHub
     * @param offlineStartTime when the offline period started
     * @return reconciliation result with merged commits
     */
    public ReconciliationResult reconcileAfterOffline(List<GitHubCommit> fetchedCommits,
                                                       Instant offlineStartTime) {
        if (fetchedCommits == null || fetchedCommits.isEmpty()) {
            return new ReconciliationResult(0, 0, 0, List.of(), List.of());
        }
        
        LOGGER.info("Reconciling commits after offline period starting " + offlineStartTime);
        
        // Get all cached commits
        List<GitHubCommit> cachedCommits = commitCache.getAllCachedCommits();
        
        // Build lookup set
        Set<String> cachedShas = new HashSet<>();
        for (GitHubCommit cached : cachedCommits) {
            cachedShas.add(cached.sha());
        }
        
        // Identify commits made during offline period that we don't have
        List<GitHubCommit> newCommits = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int duplicatesSkipped = 0;
        
        for (GitHubCommit fetched : fetchedCommits) {
            if (cachedShas.contains(fetched.sha())) {
                duplicatesSkipped++;
                continue;
            }
            
            // Check if commit was made during offline period
            if (fetched.committedAt() != null && 
                fetched.committedAt().isAfter(offlineStartTime)) {
                newCommits.add(fetched);
                LOGGER.fine("Found commit made during offline period: " + fetched.sha());
            } else {
                // Commit from before offline period that we somehow missed
                newCommits.add(fetched);
                warnings.add("Found missed commit from before offline period: " + fetched.sha());
            }
        }
        
        // Group new commits by repository and cache them
        Map<String, List<GitHubCommit>> byRepo = new HashMap<>();
        for (GitHubCommit commit : newCommits) {
            byRepo.computeIfAbsent(commit.repositoryFullName(), k -> new ArrayList<>())
                  .add(commit);
        }
        
        for (Map.Entry<String, List<GitHubCommit>> entry : byRepo.entrySet()) {
            commitCache.cacheCommits(entry.getKey(), entry.getValue(), null);
        }
        
        LOGGER.info("Reconciliation complete: " + newCommits.size() + " new commits, " + 
                   duplicatesSkipped + " duplicates skipped");
        
        return new ReconciliationResult(
            newCommits.size(),
            duplicatesSkipped,
            0,
            newCommits,
            warnings
        );
    }
    
    /**
     * Converts GitHub commits to domain Commit objects.
     * 
     * @param githubCommits the GitHub commits to convert
     * @return list of domain Commit objects
     */
    public List<Commit> convertToDomainCommits(List<GitHubCommit> githubCommits) {
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
                gc.url()
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
    
    /**
     * Gets statistics about the current cache state.
     * 
     * @return cache statistics
     */
    public CacheStatistics getCacheStatistics() {
        List<GitHubCommit> allCommits = commitCache.getAllCachedCommits();
        
        // Count commits by repository
        Map<String, Integer> commitsByRepo = new HashMap<>();
        Instant oldestCommit = null;
        Instant newestCommit = null;
        
        for (GitHubCommit commit : allCommits) {
            commitsByRepo.merge(commit.repositoryFullName(), 1, Integer::sum);
            
            if (commit.committedAt() != null) {
                if (oldestCommit == null || commit.committedAt().isBefore(oldestCommit)) {
                    oldestCommit = commit.committedAt();
                }
                if (newestCommit == null || commit.committedAt().isAfter(newestCommit)) {
                    newestCommit = commit.committedAt();
                }
            }
        }
        
        return new CacheStatistics(
            allCommits.size(),
            commitsByRepo.size(),
            oldestCommit,
            newestCommit,
            commitCache.getLastUpdated().orElse(null)
        );
    }
    
    /**
     * Statistics about the commit cache.
     */
    public record CacheStatistics(
        int totalCommits,
        int repositoryCount,
        Instant oldestCommit,
        Instant newestCommit,
        Instant lastUpdated
    ) {}
}
