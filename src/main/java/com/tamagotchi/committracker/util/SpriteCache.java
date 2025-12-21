package com.tamagotchi.committracker.util;

import javafx.scene.image.Image;
import com.tamagotchi.committracker.pokemon.PokemonSpecies;
import com.tamagotchi.committracker.pokemon.PokemonState;
import com.tamagotchi.committracker.pokemon.EvolutionStage;
import com.tamagotchi.committracker.config.AppConfig;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
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
    
    // Cache storage
    private final ConcurrentHashMap<String, List<Image>> spriteCache;
    private final ConcurrentLinkedQueue<String> accessOrder; // For LRU eviction
    
    // Cache statistics
    private volatile long cacheHits = 0;
    private volatile long cacheMisses = 0;
    
    private SpriteCache() {
        this.spriteCache = new ConcurrentHashMap<>();
        this.accessOrder = new ConcurrentLinkedQueue<>();
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
     * Implements lazy loading with LRU eviction.
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
        
        // Cache miss - load sprites
        cacheMisses++;
        logger.fine("Cache miss for: " + cacheKey + " - loading sprites");
        
        List<Image> frames = AnimationUtils.loadSpriteFramesDirect(species, stage, state);
        
        // Store in cache if enabled
        if (AppConfig.isLazyLoadingEnabled()) {
            putInCache(cacheKey, frames);
        }
        
        return frames;
    }
    
    /**
     * Gets egg sprite frames from cache or loads them if not cached.
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
        
        cacheMisses++;
        logger.fine("Cache miss for egg: " + cacheKey + " - loading sprites");
        
        List<Image> frames = AnimationUtils.loadPokemonEggSpriteFramesForStageDirect(species, eggStage, state);
        
        if (AppConfig.isLazyLoadingEnabled()) {
            putInCache(cacheKey, frames);
        }
        
        return frames;
    }
    
    /**
     * Puts sprite frames in cache with LRU eviction.
     */
    private void putInCache(String cacheKey, List<Image> frames) {
        if (frames == null || frames.isEmpty()) {
            return;
        }
        
        // Check if we need to evict old entries
        int maxCacheSize = AppConfig.getMaxCachedSprites();
        while (spriteCache.size() >= maxCacheSize) {
            evictLeastRecentlyUsed();
        }
        
        spriteCache.put(cacheKey, frames);
        updateAccessOrder(cacheKey);
        
        logger.fine("Cached sprites for: " + cacheKey + " (cache size: " + spriteCache.size() + ")");
    }
    
    /**
     * Updates the access order for LRU tracking.
     */
    private void updateAccessOrder(String cacheKey) {
        // Remove from current position and add to end
        accessOrder.remove(cacheKey);
        accessOrder.offer(cacheKey);
    }
    
    /**
     * Evicts the least recently used cache entry.
     */
    private void evictLeastRecentlyUsed() {
        String lruKey = accessOrder.poll();
        if (lruKey != null) {
            spriteCache.remove(lruKey);
            logger.fine("Evicted LRU cache entry: " + lruKey);
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
     * Preloads commonly used sprites to improve performance.
     * Should be called during application startup.
     */
    public void preloadCommonSprites() {
        if (!AppConfig.isSpriteCachingEnabled()) {
            return;
        }
        
        logger.info("Preloading common sprites...");
        
        // Preload egg sprites for all starter Pokemon (most commonly used)
        PokemonSpecies[] starters = {
            PokemonSpecies.CHARMANDER, PokemonSpecies.CYNDAQUIL, PokemonSpecies.MUDKIP,
            PokemonSpecies.PIPLUP, PokemonSpecies.SNIVY, PokemonSpecies.FROAKIE,
            PokemonSpecies.ROWLET, PokemonSpecies.GROOKEY, PokemonSpecies.FUECOCO
        };
        
        for (PokemonSpecies starter : starters) {
            // Preload egg stages 1-4 for each starter
            for (int stage = 1; stage <= 4; stage++) {
                int xp = (stage - 1) * 15; // Approximate XP for each stage
                getSpriteFrames(starter, EvolutionStage.EGG, PokemonState.CONTENT);
                getEggSpriteFrames(starter, xp, PokemonState.CONTENT);
            }
            
            // Preload basic Pokemon form
            getSpriteFrames(starter, EvolutionStage.BASIC, PokemonState.CONTENT);
            getSpriteFrames(starter, EvolutionStage.BASIC, PokemonState.HAPPY);
        }
        
        logger.info("Preloading completed. Cache size: " + spriteCache.size());
    }
    
    /**
     * Clears the entire sprite cache.
     */
    public void clearCache() {
        spriteCache.clear();
        accessOrder.clear();
        cacheHits = 0;
        cacheMisses = 0;
        logger.info("Sprite cache cleared");
    }
    
    /**
     * Gets cache statistics for monitoring.
     */
    public CacheStats getCacheStats() {
        return new CacheStats(
            spriteCache.size(),
            AppConfig.getMaxCachedSprites(),
            cacheHits,
            cacheMisses,
            calculateHitRatio()
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
     * Cache statistics data class.
     */
    public static class CacheStats {
        public final int currentSize;
        public final int maxSize;
        public final long hits;
        public final long misses;
        public final double hitRatio;
        
        public CacheStats(int currentSize, int maxSize, long hits, long misses, double hitRatio) {
            this.currentSize = currentSize;
            this.maxSize = maxSize;
            this.hits = hits;
            this.misses = misses;
            this.hitRatio = hitRatio;
        }
        
        @Override
        public String toString() {
            return String.format("CacheStats{size=%d/%d, hits=%d, misses=%d, hitRatio=%.2f%%}", 
                               currentSize, maxSize, hits, misses, hitRatio * 100);
        }
    }
}