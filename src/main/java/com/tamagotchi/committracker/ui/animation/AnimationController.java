package com.tamagotchi.committracker.ui.animation;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.tamagotchi.committracker.pokemon.PokemonSpecies;
import com.tamagotchi.committracker.pokemon.EvolutionStage;
import com.tamagotchi.committracker.util.ResourceManager;

/**
 * Manages animation queuing and prevents Timeline conflicts.
 * Implements Requirements 2.1, 2.2, 2.3 for animation system optimization.
 */
public class AnimationController {
    private static final Logger logger = Logger.getLogger(AnimationController.class.getName());
    
    // Animation concurrency control (Requirements 2.1)
    private static final int MAX_CONCURRENT_ANIMATIONS = 2;
    private final AtomicInteger activeAnimationCount = new AtomicInteger(0);
    private final Queue<AnimationRequest> animationQueue = new ConcurrentLinkedQueue<>();
    
    // Active animation tracking
    private final Map<String, AnimationInfo> activeAnimations = new ConcurrentHashMap<>();
    private final AtomicBoolean isProcessingQueue = new AtomicBoolean(false);
    
    /**
     * Represents an animation request that can be queued.
     */
    public static class AnimationRequest {
        public final String id;
        public final PokemonSpecies species;
        public final EvolutionStage fromStage;
        public final EvolutionStage toStage;
        public final AnimationType type;
        public final Runnable callback;
        public final long requestTime;
        
        public AnimationRequest(String id, PokemonSpecies species, EvolutionStage fromStage, 
                              EvolutionStage toStage, AnimationType type, Runnable callback) {
            this.id = id;
            this.species = species;
            this.fromStage = fromStage;
            this.toStage = toStage;
            this.type = type;
            this.callback = callback;
            this.requestTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Types of animations that can be requested.
     */
    public enum AnimationType {
        EVOLUTION,
        COMMIT_CELEBRATION,
        STATE_CHANGE
    }
    
    /**
     * Information about an active animation.
     */
    private static class AnimationInfo {
        final AnimationRequest request;
        final long startTime;
        volatile boolean isCompleted;
        
        AnimationInfo(AnimationRequest request) {
            this.request = request;
            this.startTime = System.currentTimeMillis();
            this.isCompleted = false;
        }
    }
    
    /**
     * Queues an animation request. If concurrent limit is not reached, starts immediately.
     * Otherwise, queues for later processing.
     * 
     * @param request The animation request to queue
     * @return true if animation started immediately, false if queued
     */
    public boolean queueAnimation(AnimationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Animation request cannot be null");
        }
        
        logger.fine("Queuing animation request: " + request.id + " (type: " + request.type + ")");
        
        // Try to start immediately - startAnimation handles the atomic check
        boolean started = startAnimation(request);
        if (!started) {
            // Queue for later processing if we couldn't start
            animationQueue.offer(request);
            logger.fine("Animation queued: " + request.id + " (queue size: " + animationQueue.size() + ")");
        }
        return started;
    }
    
    /**
     * Starts an animation immediately if possible.
     * 
     * @param request The animation request to start
     * @return true if animation was started, false if limit reached
     */
    private boolean startAnimation(AnimationRequest request) {
        // Atomically check and increment to prevent race conditions
        int newCount;
        int currentCount;
        do {
            currentCount = activeAnimationCount.get();
            if (currentCount >= MAX_CONCURRENT_ANIMATIONS) {
                return false;
            }
            newCount = currentCount + 1;
        } while (!activeAnimationCount.compareAndSet(currentCount, newCount));
        
        // Track the animation
        AnimationInfo info = new AnimationInfo(request);
        activeAnimations.put(request.id, info);
        
        logger.fine("Starting animation: " + request.id + " (active count: " + 
                   activeAnimationCount.get() + "/" + MAX_CONCURRENT_ANIMATIONS + ")");
        
        // Register with ResourceManager for cleanup tracking
        ResourceManager.getInstance().registerResource(
            "animation-" + request.id, 
            () -> completeAnimation(request.id, false),
            ResourceManager.ResourceType.OTHER
        );
        
        // Simulate animation start (in real implementation, this would create Timeline)
        if (request.callback != null) {
            // Execute callback asynchronously to simulate animation completion
            new Thread(() -> {
                try {
                    // Simulate animation duration
                    Thread.sleep(100 + (int)(Math.random() * 200)); // 100-300ms
                    completeAnimation(request.id, true);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    completeAnimation(request.id, false);
                }
            }).start();
        }
        
        return true;
    }
    
    /**
     * Marks an animation as complete and processes the queue.
     * 
     * @param animationId The ID of the completed animation
     * @param success Whether the animation completed successfully
     */
    public void completeAnimation(String animationId, boolean success) {
        AnimationInfo info = activeAnimations.remove(animationId);
        if (info == null) {
            logger.warning("Attempted to complete unknown animation: " + animationId);
            return;
        }
        
        info.isCompleted = true;
        
        // Decrement active count
        int newCount = activeAnimationCount.decrementAndGet();
        
        logger.fine("Animation completed: " + animationId + " (success: " + success + 
                   ", active count: " + newCount + "/" + MAX_CONCURRENT_ANIMATIONS + ")");
        
        // Clean up ResourceManager registration
        ResourceManager.getInstance().cleanupResource("animation-" + animationId);
        
        // Execute callback if successful
        if (success && info.request.callback != null) {
            try {
                info.request.callback.run();
            } catch (Exception e) {
                logger.warning("Animation callback failed for " + animationId + ": " + e.getMessage());
            }
        }
        
        // Process queued animations
        processQueue();
    }
    
    /**
     * Processes the animation queue, starting animations if slots are available.
     */
    private void processQueue() {
        // Prevent concurrent queue processing
        if (!isProcessingQueue.compareAndSet(false, true)) {
            return;
        }
        
        try {
            while (activeAnimationCount.get() < MAX_CONCURRENT_ANIMATIONS && !animationQueue.isEmpty()) {
                AnimationRequest nextRequest = animationQueue.poll();
                if (nextRequest != null) {
                    logger.fine("Processing queued animation: " + nextRequest.id + 
                               " (remaining in queue: " + animationQueue.size() + ")");
                    
                    if (!startAnimation(nextRequest)) {
                        // If we couldn't start it, put it back and break
                        animationQueue.offer(nextRequest);
                        break;
                    }
                }
            }
        } finally {
            isProcessingQueue.set(false);
        }
    }
    
    /**
     * Cancels a queued or active animation.
     * 
     * @param animationId The ID of the animation to cancel
     * @return true if animation was found and cancelled
     */
    public boolean cancelAnimation(String animationId) {
        // Check if it's in the queue
        boolean removedFromQueue = animationQueue.removeIf(request -> request.id.equals(animationId));
        
        if (removedFromQueue) {
            logger.fine("Cancelled queued animation: " + animationId);
            return true;
        }
        
        // Check if it's active
        AnimationInfo info = activeAnimations.get(animationId);
        if (info != null && !info.isCompleted) {
            completeAnimation(animationId, false);
            logger.fine("Cancelled active animation: " + animationId);
            return true;
        }
        
        return false;
    }
    
    /**
     * Gets the current number of active animations.
     */
    public int getActiveAnimationCount() {
        return activeAnimationCount.get();
    }
    
    /**
     * Gets the current number of queued animations.
     */
    public int getQueuedAnimationCount() {
        return animationQueue.size();
    }
    
    /**
     * Gets the maximum number of concurrent animations allowed.
     */
    public int getMaxConcurrentAnimations() {
        return MAX_CONCURRENT_ANIMATIONS;
    }
    
    /**
     * Checks if the animation system is at capacity.
     */
    public boolean isAtCapacity() {
        return activeAnimationCount.get() >= MAX_CONCURRENT_ANIMATIONS;
    }
    
    /**
     * Gets statistics about the animation system.
     */
    public AnimationStats getStats() {
        return new AnimationStats(
            activeAnimationCount.get(),
            animationQueue.size(),
            MAX_CONCURRENT_ANIMATIONS,
            activeAnimations.size()
        );
    }
    
    /**
     * Cleans up all animations and clears the queue.
     */
    public void cleanup() {
        logger.info("Cleaning up animation controller");
        
        // Cancel all queued animations
        int queuedCount = animationQueue.size();
        animationQueue.clear();
        
        // Complete all active animations
        int activeCount = activeAnimations.size();
        for (String animationId : activeAnimations.keySet()) {
            completeAnimation(animationId, false);
        }
        
        // Reset counters
        activeAnimationCount.set(0);
        isProcessingQueue.set(false);
        
        logger.info("Animation cleanup completed. Cancelled " + queuedCount + 
                   " queued and " + activeCount + " active animations");
    }
    
    /**
     * Statistics about the animation system.
     */
    public static class AnimationStats {
        public final int activeCount;
        public final int queuedCount;
        public final int maxConcurrent;
        public final int trackedCount;
        
        public AnimationStats(int activeCount, int queuedCount, int maxConcurrent, int trackedCount) {
            this.activeCount = activeCount;
            this.queuedCount = queuedCount;
            this.maxConcurrent = maxConcurrent;
            this.trackedCount = trackedCount;
        }
        
        @Override
        public String toString() {
            return String.format("AnimationStats{active=%d, queued=%d, max=%d, tracked=%d}", 
                               activeCount, queuedCount, maxConcurrent, trackedCount);
        }
    }
}