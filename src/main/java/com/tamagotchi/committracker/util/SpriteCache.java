package com.tamagotchi.committracker.util;

import javafx.scene.image.Image;
import com.tamagotchi.committracker.pokemon.PokemonSpecies;
import com.tamagotchi.committracker.pokemon.PokemonState;
import com.tamagotchi.committracker.pokemon.EvolutionStage;
import com.tamagotchi.committracker.pokemon.PokemonSelectionData;
import com.tamagotchi.committracker.config.AppConfig;

import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Sprite caching system for optimized memory usage and performance.
 * Implements LRU cache with configurable size limits and lazy loading.
 */
public class SpriteCache {
    private static final Logger logger = Logger.getLogger(SpriteCache.class.getName());
    
    // Singleton instance
    private static volatile SpriteCache instance;
    
    // Cache storage with strict LRU eviction
    private final ConcurrentHashMap<String, List<Image>> spriteCache;
    private final ConcurrentLinkedQueue<String> accessOrder; // For LRU eviction
    
    // Evolution effect frames cache (separate from sprite cache for persistence)
    private volatile List<Image> evolutionEffectFrames;
    
    // Cache statistics
    private volatile long cacheHits = 0;
    private volatile long cacheMisses = 0;
    private volatile long evictions = 0;
    
    // Background loading support
    private final java.util.concurrent.ExecutorService backgroundLoader;
    private final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.Future<List<Image>>> loadingTasks;
    
    private SpriteCache() {
        this.spriteCache = new ConcurrentHashMap<>();
        this.accessOrder = new ConcurrentLinkedQueue<>();
        this.backgroundLoader = java.util.concurrent.Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "SpriteCache-Background-Loader");
            t.setDaemon(true);
            return t;
        });
        this.loadingTasks = new java.util.concurrent.ConcurrentHashMap<>();
    }
    
    /**
     * Gets the singleton instance of the sprite cache.
     */
    public static SpriteCache getInstance() {
        if (instance == null) {
            synchronized (SpriteCache.class) {
                if (instance == null) {
                    instance = new SpriteCache();
                }
            }
        }
        return instance;
    }
    
    /**
     * Gets sprite frames from cache or loads them if not cached.
     * Implements lazy loading with strict LRU eviction and background loading.
     */
    public List<Image> getSpriteFrames(PokemonSpecies species, EvolutionStage stage, PokemonState state) {
        if (!AppConfig.isSpriteCachingEnabled()) {
            // If caching is disabled, always load fresh
            return AnimationUtils.loadSpriteFramesDirect(species, stage, state);
        }
        
        String cacheKey = createCacheKey(species, stage, state);
        
        // Check cache first
        List<Image> cachedFrames = spriteCache.get(cacheKey);
        if (cachedFrames != null) {
            cacheHits++;
            updateAccessOrder(cacheKey);
            logger.fine("Cache hit for: " + cacheKey);
            return cachedFrames;
        }
        
        // Check if already loading in background
        java.util.concurrent.Future<List<Image>> loadingTask = loadingTasks.get(cacheKey);
        if (loadingTask != null) {
            try {
                // Wait for background loading to complete (with timeout)
                List<Image> frames = loadingTask.get(100, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (frames != null) {
                    cacheHits++; // Count as hit since it was being loaded
                    updateAccessOrder(cacheKey);
                    logger.fine("Background load completed for: " + cacheKey);
                    return frames;
                }
            } catch (java.util.concurrent.TimeoutException e) {
                // Background loading taking too long, load synchronously
                logger.fine("Background loading timeout for: " + cacheKey + ", loading synchronously");
            } catch (Exception e) {
                logger.warning("Background loading failed for: " + cacheKey + " - " + e.getMessage());
            }
        }
        
        // Cache miss - load sprites synchronously
        cacheMisses++;
        logger.fine("Cache miss for: " + cacheKey + " - loading sprites");
        
        List<Image> frames = AnimationUtils.loadSpriteFramesDirect(species, stage, state);
        
        // Store in cache if enabled and frames loaded successfully
        if (AppConfig.isLazyLoadingEnabled() && frames != null && !frames.isEmpty()) {
            putInCache(cacheKey, frames);
        }
        
        return frames;
    }
    
    /**
     * Gets egg sprite frames from cache or loads them if not cached.
     * Implements lazy loading with background loading support.
     */
    public List<Image> getEggSpriteFrames(PokemonSpecies species, int totalXP, PokemonState state) {
        if (!AppConfig.isSpriteCachingEnabled()) {
            return AnimationUtils.loadEggSpriteFramesForXP(species, totalXP, state);
        }
        
        int eggStage = AnimationUtils.getEggStageFromXPDays(totalXP);
        String cacheKey = createEggCacheKey(species, eggStage, state);
        
        List<Image> cachedFrames = spriteCache.get(cacheKey);
        if (cachedFrames != null) {
            cacheHits++;
            updateAccessOrder(cacheKey);
            logger.fine("Cache hit for egg: " + cacheKey);
            return cachedFrames;
        }
        
        // Check if already loading in background
        java.util.concurrent.Future<List<Image>> loadingTask = loadingTasks.get(cacheKey);
        if (loadingTask != null) {
            try {
                List<Image> frames = loadingTask.get(100, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (frames != null) {
                    cacheHits++;
                    updateAccessOrder(cacheKey);
                    logger.fine("Background egg load completed for: " + cacheKey);
                    return frames;
                }
            } catch (java.util.concurrent.TimeoutException e) {
                logger.fine("Background egg loading timeout for: " + cacheKey + ", loading synchronously");
            } catch (Exception e) {
                logger.warning("Background egg loading failed for: " + cacheKey + " - " + e.getMessage());
            }
        }
        
        cacheMisses++;
        logger.fine("Cache miss for egg: " + cacheKey + " - loading sprites");
        
        List<Image> frames = AnimationUtils.loadPokemonEggSpriteFramesForStageDirect(species, eggStage, state);
        
        if (AppConfig.isLazyLoadingEnabled() && frames != null && !frames.isEmpty()) {
            putInCache(cacheKey, frames);
        }
        
        return frames;
    }
    
    /**
     * Puts sprite frames in cache with strict LRU eviction and size monitoring.
     */
    private void putInCache(String cacheKey, List<Image> frames) {
        if (frames == null || frames.isEmpty()) {
            return;
        }
        
        // Enforce strict cache size limits with automatic cleanup
        int maxCacheSize = AppConfig.getMaxCachedSprites();
        
        // Perform aggressive eviction if we're at or near the limit
        while (spriteCache.size() >= maxCacheSize) {
            if (!evictLeastRecentlyUsed()) {
                // If eviction fails, we can't add more items
                logger.warning("Cache eviction failed, cannot add new entry: " + cacheKey);
                return;
            }
        }
        
        // Add to cache and update access order
        spriteCache.put(cacheKey, frames);
        updateAccessOrder(cacheKey);
        
        logger.fine("Cached sprites for: " + cacheKey + " (cache size: " + spriteCache.size() + "/" + maxCacheSize + ")");
        
        // Monitor cache size and trigger cleanup if needed
        monitorCacheSize();
    }
    
    /**
     * Updates the access order for strict LRU tracking.
     * Ensures proper ordering for eviction decisions.
     */
    private void updateAccessOrder(String cacheKey) {
        // Remove from current position and add to end (most recently used)
        accessOrder.remove(cacheKey);
        accessOrder.offer(cacheKey);
    }
    
    /**
     * Evicts the least recently used cache entry with proper cleanup.
     * Returns true if eviction was successful, false otherwise.
     */
    private boolean evictLeastRecentlyUsed() {
        String lruKey = accessOrder.poll();
        if (lruKey != null) {
            List<Image> evictedFrames = spriteCache.remove(lruKey);
            if (evictedFrames != null) {
                evictions++;
                logger.fine("Evicted LRU cache entry: " + lruKey + " (" + evictedFrames.size() + " frames)");
                
                // Help GC by clearing references
                evictedFrames.clear();
                return true;
            }
        }
        return false;
    }
    
    /**
     * Monitors cache size and triggers automatic cleanup when needed.
     */
    private void monitorCacheSize() {
        int currentSize = spriteCache.size();
        int maxSize = AppConfig.getMaxCachedSprites();
        
        // Trigger proactive cleanup if cache is getting full (80% threshold)
        if (currentSize > maxSize * 0.8) {
            logger.fine("Cache size approaching limit (" + currentSize + "/" + maxSize + "), triggering proactive cleanup");
            
            // Evict 20% of cache entries to make room
            int targetEvictions = Math.max(1, (int) (currentSize * 0.2));
            for (int i = 0; i < targetEvictions && !accessOrder.isEmpty(); i++) {
                evictLeastRecentlyUsed();
            }
        }
    }
    
    /**
     * Creates a cache key for Pokemon sprites.
     */
    private String createCacheKey(PokemonSpecies species, EvolutionStage stage, PokemonState state) {
        return species.name() + "_" + stage.name() + "_" + state.name();
    }
    
    /**
     * Creates a cache key for egg sprites.
     */
    private String createEggCacheKey(PokemonSpecies species, int eggStage, PokemonState state) {
        return species.name() + "_EGG_STAGE" + eggStage + "_" + state.name();
    }
    
    /**
     * Preloads only essential sprites to improve performance during startup.
     * Optimized strategy: loads only the most commonly used sprites.
     * Requirements: 4.1, 4.2 - lazy loading for faster startup
     */
    public void preloadEssentialSprites() {
        if (!AppConfig.isSpriteCachingEnabled()) {
            return;
        }
        
        logger.info("Preloading essential sprites only...");
        
        // Only preload the most essential sprites for immediate use
        PokemonSpecies[] starters = PokemonSelectionData.getStarterOptions();
        
        // Limit to first 3 starters to minimize startup time
        int maxStartersToPreload = Math.min(3, starters.length);
        
        for (int i = 0; i < maxStartersToPreload; i++) {
            PokemonSpecies starter = starters[i];
            
            // Preload only stage 1 egg (most common initial state)
            getSpriteFrames(starter, EvolutionStage.EGG, PokemonState.CONTENT);
            getEggSpriteFrames(starter, 0, PokemonState.CONTENT); // Stage 1 egg
            
            // Preload basic Pokemon form (for immediate evolution)
            getSpriteFrames(starter, EvolutionStage.BASIC, PokemonState.CONTENT);
        }
        
        logger.info("Essential preloading completed. Cache size: " + spriteCache.size());
    }
    
    /**
     * Preloads sprites in the background for smooth user experience.
     * Loads non-essential sprites without blocking the UI.
     * Requirements: 4.1, 4.4 - background loading for smooth experience
     */
    public void preloadSpritesInBackground(PokemonSpecies species) {
        if (!AppConfig.isSpriteCachingEnabled() || !AppConfig.isLazyLoadingEnabled()) {
            return;
        }
        
        // Submit background loading tasks for all stages of this species
        backgroundLoader.submit(() -> {
            try {
                // Load all egg stages for this species
                for (int stage = 1; stage <= 4; stage++) {
                    String eggKey = createEggCacheKey(species, stage, PokemonState.CONTENT);
                    if (!spriteCache.containsKey(eggKey)) {
                        List<Image> frames = AnimationUtils.loadPokemonEggSpriteFramesForStageDirect(species, stage, PokemonState.CONTENT);
                        if (frames != null && !frames.isEmpty()) {
                            putInCache(eggKey, frames);
                        }
                    }
                }
                
                // Load basic and evolved forms
                String basicKey = createCacheKey(species, EvolutionStage.BASIC, PokemonState.CONTENT);
                if (!spriteCache.containsKey(basicKey)) {
                    List<Image> frames = AnimationUtils.loadSpriteFramesDirect(species, EvolutionStage.BASIC, PokemonState.CONTENT);
                    if (frames != null && !frames.isEmpty()) {
                        putInCache(basicKey, frames);
                    }
                }
                
                logger.fine("Background preloading completed for: " + species);
            } catch (Exception e) {
                logger.warning("Background preloading failed for " + species + ": " + e.getMessage());
            }
        });
    }
    
    /**
     * Loads sprites asynchronously and returns a Future.
     * Enables non-blocking sprite loading for better performance.
     */
    public java.util.concurrent.Future<List<Image>> loadSpritesAsync(PokemonSpecies species, EvolutionStage stage, PokemonState state) {
        String cacheKey = createCacheKey(species, stage, state);
        
        // Check if already loading
        java.util.concurrent.Future<List<Image>> existingTask = loadingTasks.get(cacheKey);
        if (existingTask != null && !existingTask.isDone()) {
            return existingTask;
        }
        
        // Submit new loading task
        java.util.concurrent.Future<List<Image>> loadingTask = backgroundLoader.submit(() -> {
            try {
                List<Image> frames = AnimationUtils.loadSpriteFramesDirect(species, stage, state);
                if (frames != null && !frames.isEmpty()) {
                    putInCache(cacheKey, frames);
                }
                return frames;
            } finally {
                // Remove from loading tasks when complete
                loadingTasks.remove(cacheKey);
            }
        });
        
        loadingTasks.put(cacheKey, loadingTask);
        return loadingTask;
    }
    
    /**
     * Disposes of sprites that are no longer needed to free memory.
     * Requirements: 4.4 - sprite disposal when no longer needed
     */
    public void disposeUnusedSprites(java.util.Set<String> activeKeys) {
        if (!AppConfig.isSpriteCachingEnabled()) {
            return;
        }
        
        java.util.List<String> keysToRemove = new java.util.ArrayList<>();
        
        // Find keys that are not in the active set
        for (String key : spriteCache.keySet()) {
            if (!activeKeys.contains(key)) {
                keysToRemove.add(key);
            }
        }
        
        // Remove unused sprites
        int disposedCount = 0;
        for (String key : keysToRemove) {
            List<Image> frames = spriteCache.remove(key);
            if (frames != null) {
                accessOrder.remove(key);
                frames.clear(); // Help GC
                disposedCount++;
            }
        }
        
        if (disposedCount > 0) {
            logger.info("Disposed " + disposedCount + " unused sprite entries. Cache size: " + spriteCache.size());
        }
    }
    
    /**
     * Preloads evolution effect frames and pre-computes silhouettes for all starter Pokemon.
     * Should be called during application startup to ensure smooth evolution animations.
     * 
     * Requirements: 1.4
     */
    public void preloadEvolutionEffects() {
        if (!AppConfig.isSpriteCachingEnabled()) {
            logger.info("Sprite caching disabled - skipping evolution effects preload");
            return;
        }
        
        logger.info("Preloading evolution effects...");
        
        // Load all 6 evolution effect frames
        evolutionEffectFrames = loadEvolutionEffectFramesDirect();
        logger.info("Loaded " + evolutionEffectFrames.size() + " evolution effect frames");
        
        // Pre-compute silhouettes for all 9 starter Pokemon (EGG and BASIC stages)
        PokemonSpecies[] starters = PokemonSelectionData.getStarterOptions();
        
        int silhouettesPreloaded = 0;
        for (PokemonSpecies starter : starters) {
            // Pre-compute silhouette for EGG stage (stage 4 - the hatching stage)
            Image eggSprite = loadSingleSprite(starter, EvolutionStage.EGG, 4);
            if (eggSprite != null) {
                String eggSilhouetteKey = EvolutionFrameCache.createSilhouetteCacheKey(starter, EvolutionStage.EGG);
                EvolutionFrameCache.getCachedSilhouette(eggSprite, eggSilhouetteKey);
                silhouettesPreloaded++;
            }
            
            // Pre-compute silhouette for BASIC stage
            Image basicSprite = loadSingleSprite(starter, EvolutionStage.BASIC, 0);
            if (basicSprite != null) {
                String basicSilhouetteKey = EvolutionFrameCache.createSilhouetteCacheKey(starter, EvolutionStage.BASIC);
                EvolutionFrameCache.getCachedSilhouette(basicSprite, basicSilhouetteKey);
                silhouettesPreloaded++;
            }
        }
        
        logger.info("Preloaded " + silhouettesPreloaded + " silhouettes for starter Pokemon");
        logger.info("Evolution effects preloading completed");
    }
    
    /**
     * Gets the cached evolution effect frames.
     * Returns null if not preloaded yet.
     * 
     * @return List of evolution effect frames, or null if not loaded
     */
    public List<Image> getEvolutionEffectFrames() {
        if (evolutionEffectFrames == null) {
            // Lazy load if not preloaded
            evolutionEffectFrames = loadEvolutionEffectFramesDirect();
        }
        return evolutionEffectFrames;
    }
    
    /**
     * Directly loads evolution effect frames from resources.
     * 
     * @return List of evolution effect frame images
     */
    private List<Image> loadEvolutionEffectFramesDirect() {
        List<Image> frames = new ArrayList<>();
        int spriteSize = 64;
        
        for (int i = 1; i <= 6; i++) {
            String effectPath = "/pokemon/sprites/effects/evolution/frame" + i + ".png";
            try (InputStream effectStream = getClass().getResourceAsStream(effectPath)) {
                if (effectStream != null) {
                    Image frame = new Image(effectStream, spriteSize, spriteSize, true, true);
                    if (!frame.isError()) {
                        frames.add(frame);
                    } else {
                        logger.warning("Error loading evolution effect frame: " + effectPath);
                    }
                } else {
                    logger.warning("Evolution effect frame not found: " + effectPath);
                }
            } catch (Exception e) {
                logger.warning("Failed to load evolution effect frame " + i + ": " + e.getMessage());
            }
        }
        
        return frames;
    }
    
    /**
     * Loads a single sprite for silhouette pre-computation.
     * 
     * @param species The Pokemon species
     * @param stage The evolution stage
     * @param eggStage The egg stage (1-4) if stage is EGG, otherwise ignored
     * @return The loaded sprite image, or null if not found
     */
    private Image loadSingleSprite(PokemonSpecies species, EvolutionStage stage, int eggStage) {
        int spriteSize = 64;
        String speciesFolder = species.name().toLowerCase();
        String path;
        
        if (stage == EvolutionStage.EGG) {
            path = "/pokemon/sprites/" + speciesFolder + "/egg/stage" + eggStage + "/frame1.png";
        } else {
            path = "/pokemon/sprites/" + speciesFolder + "/" + stage.name().toLowerCase() + "/content/frame1.png";
        }
        
        try (InputStream stream = getClass().getResourceAsStream(path)) {
            if (stream != null) {
                Image sprite = new Image(stream, spriteSize, spriteSize, true, true);
                if (!sprite.isError()) {
                    return sprite;
                }
            }
        } catch (Exception e) {
            logger.fine("Could not load sprite for silhouette: " + path);
        }
        
        return null;
    }
    
    /**
     * Clears the entire sprite cache and shuts down background loading.
     */
    public void clearCache() {
        // Cancel all pending background loading tasks
        for (java.util.concurrent.Future<List<Image>> task : loadingTasks.values()) {
            task.cancel(true);
        }
        loadingTasks.clear();
        
        // Clear cache and statistics
        spriteCache.clear();
        accessOrder.clear();
        cacheHits = 0;
        cacheMisses = 0;
        evictions = 0;
        
        logger.info("Sprite cache cleared and background tasks cancelled");
    }
    
    /**
     * Shuts down the background loading executor.
     * Should be called when the application is closing.
     */
    public void shutdown() {
        backgroundLoader.shutdown();
        try {
            if (!backgroundLoader.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                backgroundLoader.shutdownNow();
            }
        } catch (InterruptedException e) {
            backgroundLoader.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("SpriteCache background loader shut down");
    }
    
    /**
     * Gets comprehensive cache statistics for monitoring.
     */
    public CacheStats getCacheStats() {
        return new CacheStats(
            spriteCache.size(),
            AppConfig.getMaxCachedSprites(),
            cacheHits,
            cacheMisses,
            evictions,
            calculateHitRatio(),
            loadingTasks.size()
        );
    }
    
    /**
     * Calculates the cache hit ratio.
     */
    private double calculateHitRatio() {
        long totalAccesses = cacheHits + cacheMisses;
        return totalAccesses > 0 ? (double) cacheHits / totalAccesses : 0.0;
    }
    
    /**
     * Enhanced cache statistics data class with comprehensive monitoring.
     */
    public static class CacheStats {
        public final int currentSize;
        public final int maxSize;
        public final long hits;
        public final long misses;
        public final long evictions;
        public final double hitRatio;
        public final int backgroundTasks;
        
        public CacheStats(int currentSize, int maxSize, long hits, long misses, long evictions, double hitRatio, int backgroundTasks) {
            this.currentSize = currentSize;
            this.maxSize = maxSize;
            this.hits = hits;
            this.misses = misses;
            this.evictions = evictions;
            this.hitRatio = hitRatio;
            this.backgroundTasks = backgroundTasks;
        }
        
        /**
         * Gets the cache utilization percentage.
         */
        public double getUtilization() {
            return maxSize > 0 ? (double) currentSize / maxSize : 0.0;
        }
        
        /**
         * Gets the total number of cache accesses.
         */
        public long getTotalAccesses() {
            return hits + misses;
        }
        
        /**
         * Checks if the cache is under memory pressure.
         */
        public boolean isUnderPressure() {
            return getUtilization() > 0.8; // 80% threshold
        }
        
        @Override
        public String toString() {
            return String.format("CacheStats{size=%d/%d (%.1f%%), hits=%d, misses=%d, evictions=%d, hitRatio=%.2f%%, backgroundTasks=%d}", 
                               currentSize, maxSize, getUtilization() * 100, hits, misses, evictions, hitRatio * 100, backgroundTasks);
        }
    }
}