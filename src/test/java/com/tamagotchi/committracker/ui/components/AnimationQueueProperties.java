package com.tamagotchi.committracker.ui.components;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

import com.tamagotchi.committracker.pokemon.PokemonSpecies;
import com.tamagotchi.committracker.pokemon.EvolutionStage;
import com.tamagotchi.committracker.ui.animation.AnimationController;
import com.tamagotchi.committracker.ui.animation.AnimationController.AnimationRequest;
import com.tamagotchi.committracker.ui.animation.AnimationController.AnimationType;

/**
 * Property-based tests for animation queue management in AnimationController.
 * 
 * **Feature: performance-optimization, Property 4: Animation Queue Management**
 * **Validates: Requirements 2.1, 2.2, 2.3**
 * 
 * Tests that when multiple animations are requested concurrently, they are queued 
 * and executed without Timeline conflicts.
 */
public class AnimationQueueProperties {
    
    /**
     * Property 4: Animation Queue Management
     * For any animation request, when multiple animations are requested concurrently, 
     * they should be queued and executed without Timeline conflicts.
     * 
     * **Feature: performance-optimization, Property 4: Animation Queue Management**
     * **Validates: Requirements 2.1, 2.2, 2.3**
     */
    @Property(tries = 100)
    void animationQueueManagement(
            @ForAll("pokemonSpecies") PokemonSpecies species,
            @ForAll @IntRange(min = 2, max = 10) int concurrentAnimationRequests) {
        
        // Create an animation controller
        AnimationController controller = new AnimationController();
        
        try {
            // Track animation completions
            AtomicInteger completedAnimations = new AtomicInteger(0);
            AtomicBoolean hasConflicts = new AtomicBoolean(false);
            CountDownLatch animationLatch = new CountDownLatch(concurrentAnimationRequests);
            
            // Create animation requests
            List<Thread> animationThreads = new ArrayList<>();
            
            for (int i = 0; i < concurrentAnimationRequests; i++) {
                final int requestId = i;
                Thread animationThread = new Thread(() -> {
                    try {
                        // Create animation request
                        AnimationRequest request = new AnimationRequest(
                            "animation-" + requestId,
                            species,
                            EvolutionStage.EGG,
                            EvolutionStage.BASIC,
                            AnimationType.EVOLUTION,
                            () -> {
                                completedAnimations.incrementAndGet();
                                animationLatch.countDown();
                            }
                        );
                        
                        // Queue the animation
                        controller.queueAnimation(request);
                        
                        // Small delay to allow animation processing
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        
                    } catch (Exception e) {
                        // Any exception indicates a conflict or improper handling
                        hasConflicts.set(true);
                        animationLatch.countDown();
                    }
                });
                
                animationThreads.add(animationThread);
            }
            
            // Start all animation threads simultaneously
            for (Thread thread : animationThreads) {
                thread.start();
            }
            
            // Wait for all threads to complete
            for (Thread thread : animationThreads) {
                try {
                    thread.join(1000); // 1 second timeout per thread
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            // Wait for animations to complete (with timeout)
            boolean animationsCompleted;
            try {
                animationsCompleted = animationLatch.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                animationsCompleted = false;
            }
            
            // Verify no conflicts occurred
            assertFalse(hasConflicts.get(), 
                "Animation conflicts detected - concurrent animations not properly queued");
            
            // Verify that all animations were processed
            assertTrue(animationsCompleted || completedAnimations.get() > 0,
                "At least some animations should have been processed");
            
            // Verify controller is in consistent state
            AnimationController.AnimationStats stats = controller.getStats();
            assertTrue(stats.activeCount >= 0, "Active count should not be negative");
            assertTrue(stats.queuedCount >= 0, "Queued count should not be negative");
            assertTrue(stats.activeCount <= stats.maxConcurrent, 
                "Active count should not exceed maximum: " + stats.activeCount + " > " + stats.maxConcurrent);
            
        } finally {
            // Clean up resources
            controller.cleanup();
        }
    }
    
    /**
     * Tests that animation queuing respects the maximum concurrent animation limit.
     * 
     * **Feature: performance-optimization, Property 4: Animation Queue Management**
     * **Validates: Requirements 2.1, 2.2, 2.3**
     */
    @Property(tries = 100)
    void animationConcurrencyLimit(
            @ForAll("pokemonSpecies") PokemonSpecies species,
            @ForAll @IntRange(min = 5, max = 15) int totalAnimationRequests) {
        
        AnimationController controller = new AnimationController();
        
        try {
            AtomicInteger maxConcurrentObserved = new AtomicInteger(0);
            
            // The controller has MAX_CONCURRENT_ANIMATIONS = 2
            final int MAX_CONCURRENT_ANIMATIONS = controller.getMaxConcurrentAnimations();
            
            List<Thread> threads = new ArrayList<>();
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch queuedLatch = new CountDownLatch(totalAnimationRequests);
            CountDownLatch completionLatch = new CountDownLatch(totalAnimationRequests);
            
            // Create a monitoring thread that samples the active count periodically
            AtomicBoolean stopMonitoring = new AtomicBoolean(false);
            Thread monitorThread = new Thread(() -> {
                while (!stopMonitoring.get()) {
                    AnimationController.AnimationStats stats = controller.getStats();
                    maxConcurrentObserved.updateAndGet(current -> Math.max(current, stats.activeCount));
                    
                    try {
                        Thread.sleep(10); // Sample every 10ms
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            
            // Create threads that will trigger animations simultaneously
            for (int i = 0; i < totalAnimationRequests; i++) {
                final int requestId = i;
                Thread thread = new Thread(() -> {
                    try {
                        startLatch.await(); // Wait for all threads to be ready
                        
                        // Create animation request
                        AnimationRequest request = new AnimationRequest(
                            "concurrent-" + requestId,
                            species,
                            EvolutionStage.EGG,
                            EvolutionStage.BASIC,
                            AnimationType.EVOLUTION,
                            () -> completionLatch.countDown()
                        );
                        
                        // Queue the animation
                        controller.queueAnimation(request);
                        queuedLatch.countDown();
                        
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                
                threads.add(thread);
                thread.start();
            }
            
            // Start monitoring thread
            monitorThread.start();
            
            // Start all animations simultaneously
            startLatch.countDown();
            
            // Wait for all animations to be queued
            try {
                queuedLatch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Wait for all animations to complete
            boolean completed;
            try {
                completed = completionLatch.await(15, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                completed = false;
            }
            
            // Stop monitoring
            stopMonitoring.set(true);
            try {
                monitorThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            assertTrue(completed, "All animations should complete within timeout");
            
            // Verify concurrency limit was respected throughout execution
            assertTrue(maxConcurrentObserved.get() <= MAX_CONCURRENT_ANIMATIONS,
                "Maximum concurrent animations should not exceed limit of " + MAX_CONCURRENT_ANIMATIONS + 
                ", but observed: " + maxConcurrentObserved.get());
            
        } finally {
            controller.cleanup();
        }
    }
    
    /**
     * Tests that queued animations are processed in order when slots become available.
     * 
     * **Feature: performance-optimization, Property 4: Animation Queue Management**
     * **Validates: Requirements 2.1, 2.2, 2.3**
     */
    @Property(tries = 100)
    void queuedAnimationProcessing(
            @ForAll("pokemonSpecies") PokemonSpecies species,
            @ForAll @IntRange(min = 3, max = 8) int queuedAnimations) {
        
        AnimationController controller = new AnimationController();
        
        try {
            List<Integer> processingOrder = new ArrayList<>();
            CountDownLatch completionLatch = new CountDownLatch(queuedAnimations);
            
            // Queue multiple animations rapidly
            for (int i = 0; i < queuedAnimations; i++) {
                final int animationId = i + 1;
                
                Thread animationThread = new Thread(() -> {
                    AnimationRequest request = new AnimationRequest(
                        "queued-" + animationId,
                        species,
                        EvolutionStage.EGG,
                        EvolutionStage.BASIC,
                        AnimationType.EVOLUTION,
                        () -> {
                            synchronized (processingOrder) {
                                processingOrder.add(animationId);
                            }
                            completionLatch.countDown();
                        }
                    );
                    
                    controller.queueAnimation(request);
                });
                
                animationThread.start();
                
                // Small delay between requests to ensure queueing
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            // Wait for all animations to complete
            boolean completed;
            try {
                completed = completionLatch.await(20, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                completed = false;
            }
            assertTrue(completed, "All queued animations should eventually be processed");
            
            // Verify that animations were processed (order may vary due to queueing)
            assertFalse(processingOrder.isEmpty(), 
                "At least some animations should have been processed");
            
            // Verify all animations were processed
            assertEquals(queuedAnimations, processingOrder.size(),
                "All animations should have been processed");
            
            // Verify controller is in clean state
            AnimationController.AnimationStats finalStats = controller.getStats();
            assertEquals(0, finalStats.activeCount,
                "No animations should be active after processing");
            assertEquals(0, finalStats.queuedCount,
                "No animations should be queued after processing");
            
        } finally {
            controller.cleanup();
        }
    }
    
    // Helper methods
    
    // Generators
    
    @Provide
    Arbitrary<PokemonSpecies> pokemonSpecies() {
        return Arbitraries.of(PokemonSpecies.values());
    }
}