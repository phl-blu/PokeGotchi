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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Local cache for GitHub commits.
 * Provides offline access to commit history and reduces API calls.
 * 
 * Requirements: 4.6, 7.1, 7.4, 7.5
 */
public class CommitCache {
    
    private static final Logger LOGGER = Logger.getLogger(CommitCache.class.getName());
    private static final String CACHE_FILE_NAME = "github_commits.json";
    
    private final Path cacheDirectory;
    private final Gson gson;
    private final ReadWriteLock lock;
    
    // In-memory cache: repository full name -> list of commits
    private Map<String, List<GitHubCommit>> cachedCommits;
    // ETag cache: repository full name -> ETag
    private Map<String, String> etagCache;
    private Instant lastUpdated;
    
    /**
     * Creates a CommitCache with the default cache directory.
     */
    public CommitCache() {
        this(getDefaultCacheDirectory());
    }
    
    /**
     * Creates a CommitCache with a custom cache directory.
     * 
     * @param cacheDirectory the directory to store cache files
     */
    public CommitCache(Path cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
        this.lock = new ReentrantReadWriteLock();
        this.cachedCommits = new HashMap<>();
        this.etagCache = new HashMap<>();
        this.lastUpdated = null;
        
        // Load cache from disk on initialization
        loadFromDisk();
    }
    
    /**
     * Gets the default cache directory.
     */
    private static Path getDefaultCacheDirectory() {
        String userHome = System.getProperty("user.home");
        return Path.of(userHome, ".pokemon-commit-tracker", "cache");
    }
    
    /**
     * Caches commits for a repository.
     * 
     * @param repositoryFullName the full name (owner/repo)
     * @param commits the commits to cache
     * @param etag the ETag for conditional requests
     */
    public void cacheCommits(String repositoryFullName, List<GitHubCommit> commits, String etag) {
        lock.writeLock().lock();
        try {
            // Merge with existing commits, avoiding duplicates
            List<GitHubCommit> existing = cachedCommits.getOrDefault(repositoryFullName, new ArrayList<>());
            Map<String, GitHubCommit> commitMap = new HashMap<>();
            
            // Add existing commits
            for (GitHubCommit commit : existing) {
                commitMap.put(commit.sha(), commit);
            }
            
            // Add/update with new commits
            for (GitHubCommit commit : commits) {
                commitMap.put(commit.sha(), commit);
            }
            
            // Sort by date (newest first)
            List<GitHubCommit> merged = new ArrayList<>(commitMap.values());
            merged.sort((a, b) -> {
                if (a.committedAt() == null && b.committedAt() == null) return 0;
                if (a.committedAt() == null) return 1;
                if (b.committedAt() == null) return -1;
                return b.committedAt().compareTo(a.committedAt());
            });
            
            cachedCommits.put(repositoryFullName, merged);
            
            if (etag != null) {
                etagCache.put(repositoryFullName, etag);
            }
            
            this.lastUpdated = Instant.now();
            saveToDisk();
            
            LOGGER.fine("Cached " + commits.size() + " commits for " + repositoryFullName + 
                       " (total: " + merged.size() + ")");
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Gets cached commits for a repository.
     * 
     * @param repositoryFullName the full name (owner/repo)
     * @return list of cached commits, or empty list if none cached
     */
    public List<GitHubCommit> getCachedCommits(String repositoryFullName) {
        lock.readLock().lock();
        try {
            List<GitHubCommit> commits = cachedCommits.get(repositoryFullName);
            return commits != null ? Collections.unmodifiableList(new ArrayList<>(commits)) : Collections.emptyList();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Gets all cached commits across all repositories.
     * 
     * @return list of all cached commits, sorted by date (newest first)
     */
    public List<GitHubCommit> getAllCachedCommits() {
        lock.readLock().lock();
        try {
            List<GitHubCommit> allCommits = new ArrayList<>();
            for (List<GitHubCommit> commits : cachedCommits.values()) {
                allCommits.addAll(commits);
            }
            
            // Sort by date (newest first)
            allCommits.sort((a, b) -> {
                if (a.committedAt() == null && b.committedAt() == null) return 0;
                if (a.committedAt() == null) return 1;
                if (b.committedAt() == null) return -1;
                return b.committedAt().compareTo(a.committedAt());
            });
            
            return Collections.unmodifiableList(allCommits);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Gets commits since a specific date.
     * 
     * @param since only return commits after this date
     * @return list of commits after the specified date
     */
    public List<GitHubCommit> getCommitsSince(Instant since) {
        lock.readLock().lock();
        try {
            return getAllCachedCommits().stream()
                .filter(c -> c.committedAt() != null && c.committedAt().isAfter(since))
                .toList();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Gets the cached ETag for a repository.
     * 
     * @param repositoryFullName the full name (owner/repo)
     * @return Optional containing the ETag if cached
     */
    public Optional<String> getEtag(String repositoryFullName) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(etagCache.get(repositoryFullName));
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Checks if the cache has any commits.
     * 
     * @return true if cache is not empty
     */
    public boolean hasCachedData() {
        lock.readLock().lock();
        try {
            return !cachedCommits.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Gets the time when the cache was last updated.
     * 
     * @return Optional containing the last update time, or empty if never updated
     */
    public Optional<Instant> getLastUpdated() {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(lastUpdated);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Gets the total number of cached commits.
     * 
     * @return total commit count
     */
    public int getTotalCommitCount() {
        lock.readLock().lock();
        try {
            return cachedCommits.values().stream()
                .mapToInt(List::size)
                .sum();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Clears all cached data.
     */
    public void clearCache() {
        lock.writeLock().lock();
        try {
            cachedCommits.clear();
            etagCache.clear();
            lastUpdated = null;
            
            // Delete cache file
            Path cacheFile = cacheDirectory.resolve(CACHE_FILE_NAME);
            try {
                Files.deleteIfExists(cacheFile);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to delete cache file", e);
            }
            
            LOGGER.info("Commit cache cleared");
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Saves the cache to disk.
     */
    private void saveToDisk() {
        try {
            // Ensure cache directory exists
            Files.createDirectories(cacheDirectory);
            
            Path cacheFile = cacheDirectory.resolve(CACHE_FILE_NAME);
            
            CacheData data = new CacheData(cachedCommits, etagCache, lastUpdated);
            String json = gson.toJson(data);
            
            Files.writeString(cacheFile, json);
            LOGGER.fine("Saved commit cache to disk");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to save commit cache", e);
        }
    }
    
    /**
     * Loads the cache from disk.
     */
    private void loadFromDisk() {
        Path cacheFile = cacheDirectory.resolve(CACHE_FILE_NAME);
        
        if (!Files.exists(cacheFile)) {
            LOGGER.fine("No commit cache file found");
            return;
        }
        
        try {
            String json = Files.readString(cacheFile);
            Type type = new TypeToken<CacheData>(){}.getType();
            CacheData data = gson.fromJson(json, type);
            
            if (data != null) {
                this.cachedCommits = data.commits != null ? new HashMap<>(data.commits) : new HashMap<>();
                this.etagCache = data.etags != null ? new HashMap<>(data.etags) : new HashMap<>();
                this.lastUpdated = data.lastUpdated;
                LOGGER.info("Loaded " + getTotalCommitCount() + " commits from cache");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load commit cache", e);
            // Clear corrupted cache
            cachedCommits = new HashMap<>();
            etagCache = new HashMap<>();
            lastUpdated = null;
        }
    }
    
    /**
     * Internal class for serializing cache data.
     */
    private static class CacheData {
        Map<String, List<GitHubCommit>> commits;
        Map<String, String> etags;
        Instant lastUpdated;
        
        CacheData(Map<String, List<GitHubCommit>> commits, Map<String, String> etags, Instant lastUpdated) {
            this.commits = commits;
            this.etags = etags;
            this.lastUpdated = lastUpdated;
        }
    }
}
