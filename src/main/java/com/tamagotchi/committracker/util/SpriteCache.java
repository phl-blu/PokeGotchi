package com.tamagotchi.committracker.util;

import javafx.scene.image.Image;
import com.tamagotchi.committracker.pokemon.PokemonSpecies;
import com.tamagotchi.committracker.pokemon.PokemonState;
import com.tamagotchi.committracker.pokemon.EvolutionStage;
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
    
    // Cache storage
    private final ConcurrentHashMap<String, List<Image>> spriteCache;
    private final ConcurrentLinkedQueue<String> accessOrder; // For LRU eviction
    
    // Evolution effect frames cache (separate from sprite cache for persistence)
    private volatile List<Image> evolutionEffectFrames;
    
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
        PokemonSpecies[] starters = {
            PokemonSpecies.CHARMANDER, PokemonSpecies.CYNDAQUIL, PokemonSpecies.MUDKIP,
            PokemonSpecies.PIPLUP, PokemonSpecies.SNIVY, PokemonSpecies.FROAKIE,
            PokemonSpecies.ROWLET, PokemonSpecies.GROOKEY, PokemonSpecies.FUECOCO
        };
        
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