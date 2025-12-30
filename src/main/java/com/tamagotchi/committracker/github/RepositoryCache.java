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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Local cache for GitHub repositories.
 * Provides offline access to repository list and reduces API calls.
 * 
 * Requirements: 3.3, 7.1, 7.4
 */
public class RepositoryCache {
    
    private static final Logger LOGGER = Logger.getLogger(RepositoryCache.class.getName());
    private static final String CACHE_FILE_NAME = "github_repositories.json";
    
    private final Path cacheDirectory;
    private final Gson gson;
    private final ReadWriteLock lock;
    
    // In-memory cache
    private List<GitHubRepository> cachedRepositories;
    private Instant lastUpdated;
    
    /**
     * Creates a RepositoryCache with the default cache directory.
     */
    public RepositoryCache() {
        this(getDefaultCacheDirectory());
    }
    
    /**
     * Creates a RepositoryCache with a custom cache directory.
     * 
     * @param cacheDirectory the directory to store cache files
     */
    public RepositoryCache(Path cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
        this.lock = new ReentrantReadWriteLock();
        this.cachedRepositories = new ArrayList<>();
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
     * Caches a list of repositories.
     * 
     * @param repositories the repositories to cache
     */
    public void cacheRepositories(List<GitHubRepository> repositories) {
        lock.writeLock().lock();
        try {
            this.cachedRepositories = new ArrayList<>(repositories);
            this.lastUpdated = Instant.now();
            saveToDisk();
            LOGGER.info("Cached " + repositories.size() + " repositories");
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Gets the cached repositories.
     * 
     * @return list of cached repositories, or empty list if none cached
     */
    public List<GitHubRepository> getCachedRepositories() {
        lock.readLock().lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(cachedRepositories));
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Gets a specific repository by full name.
     * 
     * @param fullName the full name (owner/repo)
     * @return Optional containing the repository if found
     */
    public Optional<GitHubRepository> getRepository(String fullName) {
        lock.readLock().lock();
        try {
            return cachedRepositories.stream()
                .filter(r -> r.fullName().equals(fullName))
                .findFirst();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Updates the ETag for a specific repository.
     * 
     * @param fullName the full name (owner/repo)
     * @param etag the new ETag
     */
    public void updateRepositoryEtag(String fullName, String etag) {
        lock.writeLock().lock();
        try {
            for (int i = 0; i < cachedRepositories.size(); i++) {
                GitHubRepository repo = cachedRepositories.get(i);
                if (repo.fullName().equals(fullName)) {
                    cachedRepositories.set(i, repo.withEtag(etag));
                    saveToDisk();
                    break;
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Checks if the cache has any repositories.
     * 
     * @return true if cache is not empty
     */
    public boolean hasCachedData() {
        lock.readLock().lock();
        try {
            return !cachedRepositories.isEmpty();
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
     * Clears all cached data.
     */
    public void clearCache() {
        lock.writeLock().lock();
        try {
            cachedRepositories.clear();
            lastUpdated = null;
            
            // Delete cache file
            Path cacheFile = cacheDirectory.resolve(CACHE_FILE_NAME);
            try {
                Files.deleteIfExists(cacheFile);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to delete cache file", e);
            }
            
            LOGGER.info("Repository cache cleared");
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
            
            CacheData data = new CacheData(cachedRepositories, lastUpdated);
            String json = gson.toJson(data);
            
            Files.writeString(cacheFile, json);
            LOGGER.fine("Saved repository cache to disk");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to save repository cache", e);
        }
    }
    
    /**
     * Loads the cache from disk.
     */
    private void loadFromDisk() {
        Path cacheFile = cacheDirectory.resolve(CACHE_FILE_NAME);
        
        if (!Files.exists(cacheFile)) {
            LOGGER.fine("No repository cache file found");
            return;
        }
        
        try {
            String json = Files.readString(cacheFile);
            Type type = new TypeToken<CacheData>(){}.getType();
            CacheData data = gson.fromJson(json, type);
            
            if (data != null && data.repositories != null) {
                this.cachedRepositories = new ArrayList<>(data.repositories);
                this.lastUpdated = data.lastUpdated;
                LOGGER.info("Loaded " + cachedRepositories.size() + " repositories from cache");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load repository cache", e);
            // Clear corrupted cache
            cachedRepositories = new ArrayList<>();
            lastUpdated = null;
        }
    }
    
    /**
     * Internal class for serializing cache data.
     */
    private static class CacheData {
        List<GitHubRepository> repositories;
        Instant lastUpdated;
        
        CacheData(List<GitHubRepository> repositories, Instant lastUpdated) {
            this.repositories = repositories;
            this.lastUpdated = lastUpdated;
        }
    }
}
