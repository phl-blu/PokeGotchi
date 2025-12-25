package com.tamagotchi.committracker.util;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import com.tamagotchi.committracker.pokemon.PokemonSpecies;
import com.tamagotchi.committracker.pokemon.EvolutionStage;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * A dedicated cache for pre-computed evolution animation frames.
 * Implements caching for composite frames and silhouettes to eliminate lag
 * during evolution animations.
 * 
 * Requirements: 1.1, 1.5
 */
public class EvolutionFrameCache {
    private static final Logger logger = Logger.getLogger(EvolutionFrameCache.class.getName());
    private static final int SPRITE_SIZE = 64;
    private static final long CACHE_EXPIRATION_MS = 5 * 60 * 1000; // 5 minutes
    private static final long CLEANUP_INTERVAL_MS = 60 * 1000; // 1 minute cleanup interval
    
    // Cache for pre-computed evolution frame sequences
    private static final Map<String, CachedEvolutionSequence> frameCache = new ConcurrentHashMap<>();
    
    // Separate cache for silhouette images
    private static final Map<String, Image> silhouetteCache = new ConcurrentHashMap<>();
    
    // Track last access time for cache expiration
    private static final Map<String, Long> lastAccessTime = new ConcurrentHashMap<>();
    
    // Scheduled executor for automatic cache cleanup
    private static final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "EvolutionFrameCache-Cleanup");
        t.setDaemon(true); // Don't prevent JVM shutdown
        return t;
    });
    
    // Track current Pokemon to clear silhouette cache when it changes
    private static volatile PokemonSpecies currentPokemon = null;
    
    static {
        // Start automatic cache cleanup
        startAutomaticCleanup();
    }
    
    /**
     * Starts automatic cache cleanup that runs every minute.
     * Removes expired entries to prevent memory leaks.
     */
    private static void startAutomaticCleanup() {
        cleanupExecutor.scheduleAtFixedRate(() -> {
            try {
                evictExpiredEntries();
            } catch (Exception e) {
                logger.warning("Error during automatic cache cleanup: " + e.getMessage());
            }
        }, CLEANUP_INTERVAL_MS, CLEANUP_INTERVAL_MS, TimeUnit.MILLISECONDS);
        
        logger.info("Started automatic cache cleanup (interval: " + CLEANUP_INTERVAL_MS + "ms)");
    }
    
    /**
     * Notifies the cache that the current Pokemon has changed.
     * This triggers silhouette cache clearing to free memory from old Pokemon.
     * 
     * Requirements: 1.5
     * 
     * @param newPokemon The new current Pokemon species
     */
    public static void notifyPokemonChange(PokemonSpecies newPokemon) {
        PokemonSpecies oldPokemon = currentPokemon;
        currentPokemon = newPokemon;
        
        if (oldPokemon != null && !oldPokemon.equals(newPokemon)) {
            // Clear silhouette cache when Pokemon changes to free memory
            clearSilhouetteCache();
            logger.info("Cleared silhouette cache due to Pokemon change: " + oldPokemon + " -> " + newPokemon);
        }
    }
    
    /**
     * Generates a cache key for evolution frame sequences.
     * 
     * @param fromSpecies Starting Pokemon species
     * @param toSpecies Target Pokemon species
     * @param fromStage Starting evolution stage
     * @param toStage Target evolution stage
     * @return Unique cache key string
     */
    public static String createEvolutionCacheKey(PokemonSpecies fromSpecies, PokemonSpecies toSpecies,
                                                  EvolutionStage fromStage, EvolutionStage toStage) {
        return String.format("EVOLUTION_%s_%s_%s_%s", 
            fromSpecies.name(), toSpecies.name(), fromStage.name(), toStage.name());
    }
    
    /**
     * Generates a cache key for silhouette images.
     * 
     * @param species Pokemon species
     * @param stage Evolution stage
     * @return Unique cache key string
     */
    public static String createSilhouetteCacheKey(PokemonSpecies species, EvolutionStage stage) {
        return String.format("SILHOUETTE_%s_%s", species.name(), stage.name());
    }

    
    /**
     * Gets or creates a cached silhouette for the given sprite.
     * Avoids repeated pixel manipulation by caching silhouette versions.
     * 
     * Requirements: 1.5
     * 
     * @param sprite The original sprite image
     * @param cacheKey Unique key for caching
     * @return Cached or newly created white silhouette image
     */
    public static Image getCachedSilhouette(Image sprite, String cacheKey) {
        if (sprite == null || cacheKey == null) {
            return null;
        }
        
        // Check cache first
        Image cached = silhouetteCache.get(cacheKey);
        if (cached != null) {
            lastAccessTime.put(cacheKey, System.currentTimeMillis());
            logger.fine("Silhouette cache hit for: " + cacheKey);
            return cached;
        }
        
        // Create new silhouette
        Image silhouette = createWhiteSilhouette(sprite);
        if (silhouette != null) {
            silhouetteCache.put(cacheKey, silhouette);
            lastAccessTime.put(cacheKey, System.currentTimeMillis());
            logger.fine("Silhouette cached for: " + cacheKey);
        }
        
        return silhouette;
    }
    
    /**
     * Creates a white silhouette version of a Pokemon sprite.
     * Used during evolution transition frames.
     * 
     * @param sprite The original Pokemon sprite
     * @return White silhouette version of the sprite
     */
    public static Image createWhiteSilhouette(Image sprite) {
        if (sprite == null) return null;
        
        try {
            int width = (int) sprite.getWidth();
            int height = (int) sprite.getHeight();
            
            if (width <= 0 || height <= 0) {
                width = SPRITE_SIZE;
                height = SPRITE_SIZE;
            }
            
            WritableImage silhouette = new WritableImage(width, height);
            PixelWriter writer = silhouette.getPixelWriter();
            PixelReader reader = sprite.getPixelReader();
            
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    Color original = reader.getColor(x, y);
                    
                    if (original.getOpacity() > 0.1) {
                        // Make it white but keep the alpha (silhouette effect)
                        // Blend 85% white with 15% original color for slight form visibility
                        double whiteness = 0.85;
                        double r = original.getRed() * (1 - whiteness) + whiteness;
                        double g = original.getGreen() * (1 - whiteness) + whiteness;
                        double b = original.getBlue() * (1 - whiteness) + whiteness;
                        
                        writer.setColor(x, y, Color.color(r, g, b, original.getOpacity()));
                    } else {
                        writer.setColor(x, y, Color.TRANSPARENT);
                    }
                }
            }
            
            return silhouette;
        } catch (Exception e) {
            logger.warning("Failed to create white silhouette: " + e.getMessage());
            return sprite; // Fallback to original
        }
    }
    
    /**
     * Checks if a silhouette is cached for the given key.
     * 
     * @param cacheKey The cache key to check
     * @return true if silhouette is cached, false otherwise
     */
    public static boolean hasCachedSilhouette(String cacheKey) {
        return silhouetteCache.containsKey(cacheKey);
    }
    
    /**
     * Pre-computes all composite frames for an evolution sequence.
     * Stores the result in cache for later use during animation.
     * 
     * Requirements: 1.1, 1.2
     * 
     * @param fromSpecies Starting Pokemon species
     * @param toSpecies Target Pokemon species after evolution
     * @param fromStage Starting evolution stage
     * @param toStage Target evolution stage
     * @param oldPokemon The old Pokemon sprite
     * @param newPokemon The new Pokemon sprite
     * @param evolutionEffects List of evolution effect frames
     * @return CachedEvolutionSequence containing pre-computed frames
     */
    public static CachedEvolutionSequence preComputeEvolutionFrames(
            PokemonSpecies fromSpecies, PokemonSpecies toSpecies,
            EvolutionStage fromStage, EvolutionStage toStage,
            Image oldPokemon, Image newPokemon, List<Image> evolutionEffects) {
        
        String cacheKey = createEvolutionCacheKey(fromSpecies, toSpecies, fromStage, toStage);
        
        // Check if already cached
        CachedEvolutionSequence cached = frameCache.get(cacheKey);
        if (cached != null && !cached.isExpired(CACHE_EXPIRATION_MS)) {
            lastAccessTime.put(cacheKey, System.currentTimeMillis());
            logger.fine("Evolution frame cache hit for: " + cacheKey);
            return cached;
        }
        
        // Pre-compute all frames
        List<Image> frames = new ArrayList<>();
        double[] durations = {300, 300, 200, 150, 200, 400};
        
        // Pre-compute silhouettes
        String oldSilhouetteKey = createSilhouetteCacheKey(fromSpecies, fromStage);
        String newSilhouetteKey = createSilhouetteCacheKey(toSpecies, toStage);
        Image oldSilhouette = getCachedSilhouette(oldPokemon, oldSilhouetteKey);
        Image newSilhouette = getCachedSilhouette(newPokemon, newSilhouetteKey);
        
        // Frame 1: Old Pokemon with small sparkles
        if (oldPokemon != null && evolutionEffects.size() > 0) {
            frames.add(createCompositeFrame(oldPokemon, evolutionEffects.get(0), 1.0));
        }
        
        // Frame 2: Old Pokemon with intensifying glow
        if (oldPokemon != null && evolutionEffects.size() > 1) {
            frames.add(createCompositeFrame(oldPokemon, evolutionEffects.get(1), 1.0));
        }
        
        // Frame 3: Old Pokemon as white silhouette
        if (oldSilhouette != null && evolutionEffects.size() > 2) {
            frames.add(createCompositeFrame(oldSilhouette, evolutionEffects.get(2), 1.0));
        }
        
        // Frame 4: Peak white flash - pure effect
        if (evolutionEffects.size() > 3) {
            frames.add(evolutionEffects.get(3));
        }
        
        // Frame 5: New Pokemon as white silhouette
        if (newSilhouette != null && evolutionEffects.size() > 4) {
            frames.add(createCompositeFrame(newSilhouette, evolutionEffects.get(4), 1.0));
        }
        
        // Frame 6: New Pokemon fully visible with final sparkles
        if (newPokemon != null && evolutionEffects.size() > 5) {
            frames.add(createCompositeFrame(newPokemon, evolutionEffects.get(5), 1.0));
        }
        
        // Frame 7: New Pokemon without effects (final frame)
        if (newPokemon != null) {
            frames.add(newPokemon);
        }
        
        // Create and cache the sequence
        CachedEvolutionSequence sequence = new CachedEvolutionSequence(frames, durations);
        frameCache.put(cacheKey, sequence);
        lastAccessTime.put(cacheKey, System.currentTimeMillis());
        
        logger.info("Pre-computed " + frames.size() + " evolution frames for: " + cacheKey);
        
        return sequence;
    }
    
    /**
     * Gets a cached evolution sequence if available.
     * 
     * @param fromSpecies Starting Pokemon species
     * @param toSpecies Target Pokemon species
     * @param fromStage Starting evolution stage
     * @param toStage Target evolution stage
     * @return Cached sequence or null if not cached
     */
    public static CachedEvolutionSequence getCachedEvolutionFrames(
            PokemonSpecies fromSpecies, PokemonSpecies toSpecies,
            EvolutionStage fromStage, EvolutionStage toStage) {
        
        String cacheKey = createEvolutionCacheKey(fromSpecies, toSpecies, fromStage, toStage);
        CachedEvolutionSequence cached = frameCache.get(cacheKey);
        
        if (cached != null && !cached.isExpired(CACHE_EXPIRATION_MS)) {
            lastAccessTime.put(cacheKey, System.currentTimeMillis());
            return cached;
        }
        
        return null;
    }
    
    /**
     * Checks if evolution frames are cached for the given parameters.
     * 
     * @param fromSpecies Starting Pokemon species
     * @param toSpecies Target Pokemon species
     * @param fromStage Starting evolution stage
     * @param toStage Target evolution stage
     * @return true if frames are cached, false otherwise
     */
    public static boolean hasCachedEvolutionFrames(
            PokemonSpecies fromSpecies, PokemonSpecies toSpecies,
            EvolutionStage fromStage, EvolutionStage toStage) {
        
        String cacheKey = createEvolutionCacheKey(fromSpecies, toSpecies, fromStage, toStage);
        CachedEvolutionSequence cached = frameCache.get(cacheKey);
        return cached != null && !cached.isExpired(CACHE_EXPIRATION_MS);
    }
    
    /**
     * Creates a composite frame by overlaying an effect on top of a Pokemon sprite.
     * 
     * @param pokemonSprite The base Pokemon sprite
     * @param effectSprite The effect overlay sprite
     * @param pokemonOpacity Opacity of the Pokemon sprite (0.0 to 1.0)
     * @return Combined image with effect overlaid on Pokemon
     */
    public static Image createCompositeFrame(Image pokemonSprite, Image effectSprite, double pokemonOpacity) {
        if (pokemonSprite == null) return effectSprite;
        if (effectSprite == null) return pokemonSprite;
        
        try {
            int width = SPRITE_SIZE;
            int height = SPRITE_SIZE;
            
            WritableImage composite = new WritableImage(width, height);
            PixelWriter writer = composite.getPixelWriter();
            PixelReader pokemonReader = pokemonSprite.getPixelReader();
            PixelReader effectReader = effectSprite.getPixelReader();
            
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    Color pokemonColor = pokemonReader.getColor(x, y);
                    Color effectColor = effectReader.getColor(x, y);
                    
                    // Apply opacity to Pokemon
                    double pAlpha = pokemonColor.getOpacity() * pokemonOpacity;
                    double eAlpha = effectColor.getOpacity();
                    
                    // Blend: effect on top of Pokemon (standard alpha compositing)
                    double outAlpha = eAlpha + pAlpha * (1 - eAlpha);
                    
                    if (outAlpha > 0) {
                        double r = (effectColor.getRed() * eAlpha + pokemonColor.getRed() * pAlpha * (1 - eAlpha)) / outAlpha;
                        double g = (effectColor.getGreen() * eAlpha + pokemonColor.getGreen() * pAlpha * (1 - eAlpha)) / outAlpha;
                        double b = (effectColor.getBlue() * eAlpha + pokemonColor.getBlue() * pAlpha * (1 - eAlpha)) / outAlpha;
                        
                        writer.setColor(x, y, Color.color(
                            Math.min(1.0, r), Math.min(1.0, g), Math.min(1.0, b), outAlpha
                        ));
                    } else {
                        writer.setColor(x, y, Color.TRANSPARENT);
                    }
                }
            }
            
            return composite;
        } catch (Exception e) {
            logger.warning("Failed to create composite frame: " + e.getMessage());
            return pokemonSprite; // Fallback to Pokemon sprite
        }
    }

    
    /**
     * Clears the entire frame cache.
     */
    public static void clearCache() {
        frameCache.clear();
        silhouetteCache.clear();
        lastAccessTime.clear();
        logger.info("Evolution frame cache cleared");
    }
    
    /**
     * Clears only the silhouette cache.
     * Useful when Pokemon changes and old silhouettes are no longer needed.
     */
    public static void clearSilhouetteCache() {
        silhouetteCache.clear();
        // Remove silhouette-related access times
        lastAccessTime.keySet().removeIf(key -> key.startsWith("SILHOUETTE_"));
        logger.info("Silhouette cache cleared");
    }
    
    /**
     * Removes expired cache entries.
     * Entries older than CACHE_EXPIRATION_MS are removed.
     */
    public static void evictExpiredEntries() {
        long now = System.currentTimeMillis();
        int evictedFrames = 0;
        int evictedSilhouettes = 0;
        
        // Evict expired frame sequences
        frameCache.entrySet().removeIf(entry -> {
            Long lastAccess = lastAccessTime.get(entry.getKey());
            if (lastAccess != null && (now - lastAccess) > CACHE_EXPIRATION_MS) {
                lastAccessTime.remove(entry.getKey());
                logger.fine("Evicted expired frame sequence: " + entry.getKey());
                return true;
            }
            return false;
        });
        
        // Count evicted frame sequences
        evictedFrames = frameCache.size();
        int initialFrameSize = evictedFrames;
        
        // Evict expired silhouettes
        silhouetteCache.entrySet().removeIf(entry -> {
            Long lastAccess = lastAccessTime.get(entry.getKey());
            if (lastAccess != null && (now - lastAccess) > CACHE_EXPIRATION_MS) {
                lastAccessTime.remove(entry.getKey());
                logger.fine("Evicted expired silhouette: " + entry.getKey());
                return true;
            }
            return false;
        });
        
        // Count evicted silhouettes
        evictedSilhouettes = silhouetteCache.size();
        int initialSilhouetteSize = evictedSilhouettes;
        
        // Calculate actual evicted counts
        evictedFrames = initialFrameSize - frameCache.size();
        evictedSilhouettes = initialSilhouetteSize - silhouetteCache.size();
        
        if (evictedFrames > 0 || evictedSilhouettes > 0) {
            logger.info("Cache cleanup completed - evicted " + evictedFrames + " frame sequences, " + 
                       evictedSilhouettes + " silhouettes");
        }
    }
    
    /**
     * Shuts down the automatic cleanup executor.
     * Should be called when the application is shutting down.
     */
    public static void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("Evolution frame cache cleanup executor shut down");
    }
    
    /**
     * Gets the current size of the frame cache.
     * 
     * @return Number of cached frame sequences
     */
    public static int getFrameCacheSize() {
        return frameCache.size();
    }
    
    /**
     * Gets the current size of the silhouette cache.
     * 
     * @return Number of cached silhouettes
     */
    public static int getSilhouetteCacheSize() {
        return silhouetteCache.size();
    }
    
    /**
     * Data class for cached evolution sequences.
     */
    public static class CachedEvolutionSequence {
        public final List<Image> frames;
        public final double[] durations;
        public final int totalFrameCount;
        public final long computedAtMs;
        
        public CachedEvolutionSequence(List<Image> frames, double[] durations) {
            this.frames = frames;
            this.durations = durations;
            this.totalFrameCount = frames.size();
            this.computedAtMs = System.currentTimeMillis();
        }
        
        public boolean isExpired(long maxAgeMs) {
            return System.currentTimeMillis() - computedAtMs > maxAgeMs;
        }
    }
}
