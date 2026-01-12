package com.tamagotchi.committracker.github;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.tamagotchi.committracker.pokemon.EvolutionStage;
import com.tamagotchi.committracker.pokemon.PokemonSpecies;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralized cache manager for offline mode support.
 * Manages caching of repositories, commits, user preferences, and Pokemon state.
 * 
 * Requirements: 7.1, 7.4, 7.5
 * - 7.1: Continue displaying cached commit data when offline
 * - 7.4: Load cached data when application starts offline
 * - 7.5: Preserve Pokemon state and XP progress locally
 */
public class OfflineCacheManager {
    
    private static final Logger LOGGER = Logger.getLogger(OfflineCacheManager.class.getName());
    private static final String USER_PREFERENCES_FILE = "user_preferences.json";
    private static final String SYNC_STATE_FILE = "sync_state.json";
    
    private final Path cacheDirectory;
    private final Gson gson;
    private final ReadWriteLock lock;
    
    // Component caches
    private final CommitCache commitCache;
    private final RepositoryCache repositoryCache;
    private final CommitBaselineManager baselineManager;
    
    // User preferences cache
    private UserPreferencesCache userPreferences;
    private SyncState syncState;
    
    /**
     * Creates an OfflineCacheManager with the default cache directory.
     */
    public OfflineCacheManager() {
        this(getDefaultCacheDirectory());
    }
    
    /**
     * Creates an OfflineCacheManager with a custom cache directory.
     * 
     * @param cacheDirectory the directory to store cache files
     */
    public OfflineCacheManager(Path cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
        this.lock = new ReentrantReadWriteLock();
        
        // Initialize component caches
        this.commitCache = new CommitCache(cacheDirectory);
        this.repositoryCache = new RepositoryCache(cacheDirectory);
        this.baselineManager = new CommitBaselineManager(cacheDirectory.resolve("config"));
        
        // Load user preferences and sync state
        loadUserPreferences();
        loadSyncState();
    }

    
    /**
     * Gets the default cache directory.
     */
    private static Path getDefaultCacheDirectory() {
        String userHome = System.getProperty("user.home");
        return Path.of(userHome, ".pokemon-commit-tracker", "cache");
    }
    
    // ==================== Component Cache Accessors ====================
    
    /**
     * Gets the commit cache for offline access.
     * 
     * @return the commit cache
     */
    public CommitCache getCommitCache() {
        return commitCache;
    }
    
    /**
     * Gets the repository cache for offline access.
     * 
     * @return the repository cache
     */
    public RepositoryCache getRepositoryCache() {
        return repositoryCache;
    }
    
    /**
     * Gets the commit baseline manager.
     * 
     * @return the baseline manager
     */
    public CommitBaselineManager getBaselineManager() {
        return baselineManager;
    }
    
    // ==================== User Preferences Management ====================
    
    /**
     * Saves user preferences including GitHub user info and Pokemon state.
     * 
     * Requirement 7.5: Preserve Pokemon state and XP progress locally
     * 
     * @param preferences the user preferences to save
     */
    public void saveUserPreferences(UserPreferencesCache preferences) {
        lock.writeLock().lock();
        try {
            this.userPreferences = preferences;
            saveUserPreferencesToDisk();
            LOGGER.info("User preferences saved");
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Gets the cached user preferences.
     * 
     * @return Optional containing user preferences if cached
     */
    public Optional<UserPreferencesCache> getUserPreferences() {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(userPreferences);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Updates the Pokemon state in user preferences.
     * 
     * Requirement 7.5: Preserve Pokemon state and XP progress locally
     * 
     * @param species the current Pokemon species
     * @param stage the current evolution stage
     * @param currentXP the current XP
     * @param currentStreak the current commit streak
     */
    public void updatePokemonState(PokemonSpecies species, EvolutionStage stage, 
                                   int currentXP, int currentStreak) {
        lock.writeLock().lock();
        try {
            if (userPreferences == null) {
                userPreferences = new UserPreferencesCache();
            }
            userPreferences.selectedPokemon = species;
            userPreferences.evolutionStage = stage;
            userPreferences.totalXP = currentXP;
            userPreferences.currentStreak = currentStreak;
            userPreferences.lastUpdated = Instant.now();
            
            saveUserPreferencesToDisk();
            LOGGER.fine("Pokemon state updated: " + species + " at " + stage + " with " + currentXP + " XP");
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Updates the GitHub user info in preferences.
     * 
     * @param username the GitHub username
     * @param userId the GitHub user ID
     */
    public void updateGitHubUserInfo(String username, long userId) {
        lock.writeLock().lock();
        try {
            if (userPreferences == null) {
                userPreferences = new UserPreferencesCache();
            }
            userPreferences.githubUsername = username;
            userPreferences.githubUserId = userId;
            userPreferences.lastUpdated = Instant.now();
            
            saveUserPreferencesToDisk();
            LOGGER.info("GitHub user info updated: " + username);
        } finally {
            lock.writeLock().unlock();
        }
    }

    
    // ==================== Sync State Management ====================
    
    /**
     * Updates the last sync timestamp.
     * 
     * @param syncTime the time of the last successful sync
     */
    public void updateLastSyncTime(Instant syncTime) {
        lock.writeLock().lock();
        try {
            if (syncState == null) {
                syncState = new SyncState();
            }
            syncState.lastSyncTime = syncTime;
            syncState.lastSyncSuccess = true;
            
            saveSyncStateToDisk();
            LOGGER.fine("Last sync time updated: " + syncTime);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Records a sync failure.
     * 
     * @param error the error message
     */
    public void recordSyncFailure(String error) {
        lock.writeLock().lock();
        try {
            if (syncState == null) {
                syncState = new SyncState();
            }
            syncState.lastSyncSuccess = false;
            syncState.lastError = error;
            syncState.lastErrorTime = Instant.now();
            
            saveSyncStateToDisk();
            LOGGER.warning("Sync failure recorded: " + error);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Gets the last sync time.
     * 
     * @return Optional containing the last sync time
     */
    public Optional<Instant> getLastSyncTime() {
        lock.readLock().lock();
        try {
            return syncState != null ? Optional.ofNullable(syncState.lastSyncTime) : Optional.empty();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Checks if the last sync was successful.
     * 
     * @return true if last sync was successful
     */
    public boolean wasLastSyncSuccessful() {
        lock.readLock().lock();
        try {
            return syncState != null && syncState.lastSyncSuccess;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Gets the sync state.
     * 
     * @return Optional containing the sync state
     */
    public Optional<SyncState> getSyncState() {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(syncState);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    // ==================== Cache Status Methods ====================
    
    /**
     * Checks if there is any cached data available for offline use.
     * 
     * Requirement 7.4: Load cached data when application starts offline
     * 
     * @return true if cached data is available
     */
    public boolean hasCachedData() {
        return commitCache.hasCachedData() || 
               repositoryCache.hasCachedData() || 
               userPreferences != null;
    }
    
    /**
     * Checks if the cache is stale (older than specified duration).
     * 
     * @param maxAgeMinutes maximum age in minutes before cache is considered stale
     * @return true if cache is stale or no sync has occurred
     */
    public boolean isCacheStale(long maxAgeMinutes) {
        lock.readLock().lock();
        try {
            if (syncState == null || syncState.lastSyncTime == null) {
                return true;
            }
            
            Instant staleThreshold = Instant.now().minusSeconds(maxAgeMinutes * 60);
            return syncState.lastSyncTime.isBefore(staleThreshold);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Gets a summary of cached data for display.
     * 
     * @return cache summary
     */
    public CacheSummary getCacheSummary() {
        lock.readLock().lock();
        try {
            return new CacheSummary(
                commitCache.getTotalCommitCount(),
                repositoryCache.getCachedRepositories().size(),
                commitCache.getLastUpdated().orElse(null),
                repositoryCache.getLastUpdated().orElse(null),
                syncState != null ? syncState.lastSyncTime : null,
                userPreferences != null
            );
        } finally {
            lock.readLock().unlock();
        }
    }

    
    // ==================== Cache Clearing ====================
    
    /**
     * Clears all cached data.
     * Should be called when user logs out or requests data deletion.
     */
    public void clearAllCaches() {
        lock.writeLock().lock();
        try {
            commitCache.clearCache();
            repositoryCache.clearCache();
            baselineManager.clearBaseline();
            
            userPreferences = null;
            syncState = null;
            
            // Delete preference files
            deleteFile(USER_PREFERENCES_FILE);
            deleteFile(SYNC_STATE_FILE);
            
            LOGGER.info("All caches cleared");
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Clears only commit and repository caches, preserving user preferences.
     */
    public void clearDataCaches() {
        lock.writeLock().lock();
        try {
            commitCache.clearCache();
            repositoryCache.clearCache();
            
            LOGGER.info("Data caches cleared (preferences preserved)");
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    // ==================== Persistence Methods ====================
    
    private void saveUserPreferencesToDisk() {
        try {
            Files.createDirectories(cacheDirectory);
            Path file = cacheDirectory.resolve(USER_PREFERENCES_FILE);
            String json = gson.toJson(userPreferences);
            Files.writeString(file, json);
            LOGGER.fine("User preferences saved to disk");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to save user preferences", e);
        }
    }
    
    private void loadUserPreferences() {
        Path file = cacheDirectory.resolve(USER_PREFERENCES_FILE);
        if (!Files.exists(file)) {
            LOGGER.fine("No user preferences file found");
            return;
        }
        
        try {
            String json = Files.readString(file);
            Type type = new TypeToken<UserPreferencesCache>(){}.getType();
            userPreferences = gson.fromJson(json, type);
            LOGGER.info("User preferences loaded from disk");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load user preferences", e);
            userPreferences = null;
        }
    }
    
    private void saveSyncStateToDisk() {
        try {
            Files.createDirectories(cacheDirectory);
            Path file = cacheDirectory.resolve(SYNC_STATE_FILE);
            String json = gson.toJson(syncState);
            Files.writeString(file, json);
            LOGGER.fine("Sync state saved to disk");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to save sync state", e);
        }
    }
    
    private void loadSyncState() {
        Path file = cacheDirectory.resolve(SYNC_STATE_FILE);
        if (!Files.exists(file)) {
            LOGGER.fine("No sync state file found");
            return;
        }
        
        try {
            String json = Files.readString(file);
            Type type = new TypeToken<SyncState>(){}.getType();
            syncState = gson.fromJson(json, type);
            LOGGER.info("Sync state loaded from disk");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load sync state", e);
            syncState = null;
        }
    }
    
    private void deleteFile(String fileName) {
        try {
            Path file = cacheDirectory.resolve(fileName);
            Files.deleteIfExists(file);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to delete file: " + fileName, e);
        }
    }

    
    // ==================== Inner Classes ====================
    
    /**
     * Cached user preferences including GitHub info and Pokemon state.
     * 
     * Requirement 7.5: Preserve Pokemon state and XP progress locally
     */
    public static class UserPreferencesCache {
        public String githubUsername;
        public long githubUserId;
        public Instant commitBaseline;
        public PokemonSpecies selectedPokemon;
        public EvolutionStage evolutionStage;
        public int totalXP;
        public int currentStreak;
        public Instant lastUpdated;
        
        public UserPreferencesCache() {
            this.lastUpdated = Instant.now();
        }
        
        public UserPreferencesCache(String githubUsername, long githubUserId, 
                                    PokemonSpecies selectedPokemon, EvolutionStage evolutionStage,
                                    int totalXP, int currentStreak) {
            this.githubUsername = githubUsername;
            this.githubUserId = githubUserId;
            this.selectedPokemon = selectedPokemon;
            this.evolutionStage = evolutionStage;
            this.totalXP = totalXP;
            this.currentStreak = currentStreak;
            this.lastUpdated = Instant.now();
        }
    }
    
    /**
     * Tracks sync state for offline/online transitions.
     */
    public static class SyncState {
        public Instant lastSyncTime;
        public boolean lastSyncSuccess;
        public String lastError;
        public Instant lastErrorTime;
        public int pendingSyncOperations;
        
        public SyncState() {
            this.lastSyncSuccess = true;
            this.pendingSyncOperations = 0;
        }
    }
    
    /**
     * Summary of cached data for display purposes.
     */
    public record CacheSummary(
        int totalCommits,
        int totalRepositories,
        Instant commitsLastUpdated,
        Instant repositoriesLastUpdated,
        Instant lastSyncTime,
        boolean hasUserPreferences
    ) {
        public boolean hasData() {
            return totalCommits > 0 || totalRepositories > 0 || hasUserPreferences;
        }
    }
}
