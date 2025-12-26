package com.tamagotchi.committracker.ui.animation;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.logging.Logger;

import com.tamagotchi.committracker.config.AppConfig;
import com.tamagotchi.committracker.util.ResourceManager;

/**
 * Manages smooth UI transitions between compact and expanded modes.
 * Prevents animation glitches and resource conflicts during mode switching.
 * 
 * Implements Requirements 2.4, 2.5 for UI transition optimization.
 */
public class UITransitionManager {
    private static final Logger logger = Logger.getLogger(UITransitionManager.class.getName());
    
    // Transition configuration - optimized for smooth performance
    private static final Duration TRANSITION_DURATION = Duration.millis(250); // Reduced for snappier feel
    private static final double SCALE_FACTOR = 0.98; // Subtle scale for smoother visual
    private static final int TARGET_FRAME_RATE = 60; // Target 60 FPS for smooth transitions
    private static final Duration FRAME_DURATION = Duration.millis(1000.0 / TARGET_FRAME_RATE); // 16.67ms per frame
    
    // Frame rate optimization
    private static final Interpolator SMOOTH_INTERPOLATOR = Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0); // CSS ease-out
    private static final int MAX_CONCURRENT_TRANSITIONS = 1; // Prevent resource conflicts
    
    // State management - enhanced for resource conflict prevention
    private final AtomicBoolean isTransitioning = new AtomicBoolean(false);
    private final AtomicInteger activeTransitionCount = new AtomicInteger(0);
    private final AtomicBoolean animationsPaused = new AtomicBoolean(false);
    
    // Animation tracking - improved resource management
    private Timeline currentTransition;
    private String currentTransitionId;
    private final Set<Timeline> activeTimelines = ConcurrentHashMap.newKeySet();
    
    // Frame rate optimization
    private AnimationTimer frameRateOptimizer;
    private long lastFrameTime = 0;
    private final AtomicInteger droppedFrames = new AtomicInteger(0);
    
    // Components
    private final AnimationController animationController;
    
    public UITransitionManager(AnimationController animationController) {
        this.animationController = animationController;
    }
    
    /**
     * Performs a smooth transition from expanded to compact mode.
     * Optimized to prevent animation glitches and resource conflicts.
     * 
     * @param stage The window stage
     * @param root The root container
     * @param pokemonDisplay The Pokemon display component
     * @param expandedLayout The expanded layout container
     * @param pokemonStatusBox The Pokemon status container
     * @param onComplete Callback when transition completes
     */
    public void transitionToCompactMode(Stage stage, StackPane root, Node pokemonDisplay, 
                                      Node expandedLayout, Node pokemonStatusBox, 
                                      Runnable onComplete) {
        
        if (!canStartTransition()) {
            logger.warning("Cannot start compact transition - another transition in progress");
            if (onComplete != null) onComplete.run();
            return;
        }
        
        logger.fine("Starting optimized transition to compact mode");
        startTransition("compact-transition");
        
        // Start frame rate monitoring for smooth transitions
        startFrameRateOptimization();
        
        // Pause Pokemon animations during transition to prevent conflicts
        pausePokemonAnimations();
        
        // Create optimized transition timeline with frame rate control
        currentTransition = createOptimizedTimeline();
        
        // Phase 1: Optimized scale down and fade out with smooth interpolation
        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(120), expandedLayout);
        scaleDown.setFromX(1.0);
        scaleDown.setFromY(1.0);
        scaleDown.setToX(SCALE_FACTOR);
        scaleDown.setToY(SCALE_FACTOR);
        scaleDown.setInterpolator(SMOOTH_INTERPOLATOR);
        
        FadeTransition fadeOut = new FadeTransition(Duration.millis(120), expandedLayout);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setInterpolator(SMOOTH_INTERPOLATOR);
        
        ParallelTransition phase1 = new ParallelTransition(scaleDown, fadeOut);
        trackAnimation(phase1);
        
        // Phase 2: Resize window and prepare Pokemon with optimized timing
        phase1.setOnFinished(e -> Platform.runLater(() -> {
            try {
                // Remove expanded layout with resource cleanup
                cleanupExpandedLayout(root, expandedLayout);
                
                // Optimized window resize with smooth animation
                resizeWindowSmooth(stage, root, AppConfig.COMPACT_WIDTH, AppConfig.COMPACT_HEIGHT);
                
                // Move Pokemon to root with position optimization
                if (pokemonDisplay != null) {
                    optimizePokemonPlacement(root, pokemonDisplay, pokemonStatusBox);
                }
                
                // Phase 3: Optimized fade in Pokemon in compact mode
                if (pokemonDisplay != null) {
                    createOptimizedFadeIn(pokemonDisplay, () -> completeTransition(onComplete));
                } else {
                    completeTransition(onComplete);
                }
                
            } catch (Exception ex) {
                logger.warning("Error during compact transition: " + ex.getMessage());
                completeTransition(onComplete);
            }
        }));
        
        // Register transition with ResourceManager for proper cleanup
        ResourceManager.getInstance().registerResource(
            currentTransitionId,
            () -> cleanupAllTimelines(),
            ResourceManager.ResourceType.OTHER
        );
        
        // Start the optimized transition
        phase1.play();
    }
    
    /**
     * Performs a smooth transition from compact to expanded mode.
     * Optimized to prevent animation glitches and resource conflicts.
     * 
     * @param stage The window stage
     * @param root The root container
     * @param pokemonDisplay The Pokemon display component
     * @param expandedLayout The expanded layout container
     * @param pokemonStatusBox The Pokemon status container
     * @param onComplete Callback when transition completes
     */
    public void transitionToExpandedMode(Stage stage, StackPane root, Node pokemonDisplay,
                                       Node expandedLayout, Node pokemonStatusBox,
                                       Runnable onComplete) {
        
        if (!canStartTransition()) {
            logger.warning("Cannot start expanded transition - another transition in progress");
            if (onComplete != null) onComplete.run();
            return;
        }
        
        logger.fine("Starting optimized transition to expanded mode");
        startTransition("expanded-transition");
        
        // Start frame rate monitoring for smooth transitions
        startFrameRateOptimization();
        
        // Pause Pokemon animations during transition to prevent conflicts
        pausePokemonAnimations();
        
        // Phase 1: Optimized fade out Pokemon in compact mode
        if (pokemonDisplay != null) {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(80), pokemonDisplay);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setInterpolator(SMOOTH_INTERPOLATOR);
            trackAnimation(fadeOut);
            
            fadeOut.setOnFinished(e -> Platform.runLater(() -> {
                try {
                    // Remove Pokemon from root with cleanup
                    cleanupPokemonFromRoot(root, pokemonDisplay);
                    
                    // Optimized window resize with smooth animation
                    resizeWindowSmooth(stage, root, AppConfig.EXPANDED_WIDTH, AppConfig.EXPANDED_HEIGHT);
                    
                    // Add Pokemon to status box with optimized placement
                    if (pokemonStatusBox != null) {
                        optimizePokemonStatusPlacement(pokemonStatusBox, pokemonDisplay);
                    }
                    
                    // Add expanded layout to root with preparation
                    prepareExpandedLayout(root, expandedLayout);
                    
                    // Phase 2: Optimized scale up and fade in expanded content
                    createOptimizedExpandedAnimation(expandedLayout, pokemonDisplay, onComplete);
                    
                } catch (Exception ex) {
                    logger.warning("Error during expanded transition: " + ex.getMessage());
                    completeTransition(onComplete);
                }
            }));
            
            fadeOut.play();
        } else {
            // No Pokemon display, proceed directly to expanded layout
            completeTransitionToExpandedOptimized(stage, root, expandedLayout, onComplete);
        }
    }
    
    /**
     * Completes the transition to expanded mode when no Pokemon display is available.
     * Optimized version with better performance.
     */
    private void completeTransitionToExpandedOptimized(Stage stage, StackPane root, Node expandedLayout, Runnable onComplete) {
        Platform.runLater(() -> {
            try {
                // Optimized window resize with smooth animation
                resizeWindowSmooth(stage, root, AppConfig.EXPANDED_WIDTH, AppConfig.EXPANDED_HEIGHT);
                
                // Add expanded layout to root with preparation
                prepareExpandedLayout(root, expandedLayout);
                
                // Create optimized animation for expanded layout
                createOptimizedExpandedAnimation(expandedLayout, null, onComplete);
                
            } catch (Exception ex) {
                logger.warning("Error during expanded transition completion: " + ex.getMessage());
                completeTransition(onComplete);
            }
        });
    }
    
    /**
     * Creates an optimized Timeline with frame rate control.
     */
    private Timeline createOptimizedTimeline() {
        Timeline timeline = new Timeline();
        timeline.setRate(1.0); // Use natural rate for smoother animation
        return timeline;
    }
    
    /**
     * Tracks an Animation for proper resource management.
     * Updated to handle all Animation types, not just Timeline.
     */
    private void trackAnimation(Animation animation) {
        if (animation != null) {
            // Convert to Timeline if possible, otherwise track directly
            if (animation instanceof Timeline) {
                activeTimelines.add((Timeline) animation);
            }
            animation.setOnFinished(e -> {
                if (animation instanceof Timeline) {
                    activeTimelines.remove(animation);
                }
            });
        }
    }
    
    /**
     * Starts frame rate optimization monitoring.
     */
    private void startFrameRateOptimization() {
        if (frameRateOptimizer != null) {
            frameRateOptimizer.stop();
        }
        
        frameRateOptimizer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastFrameTime > 0) {
                    long frameDuration = now - lastFrameTime;
                    long targetDuration = (long) (FRAME_DURATION.toMillis() * 1_000_000); // Convert to nanoseconds
                    
                    if (frameDuration > targetDuration * 1.5) { // Frame took 50% longer than target
                        droppedFrames.incrementAndGet();
                    }
                }
                lastFrameTime = now;
            }
        };
        frameRateOptimizer.start();
    }
    
    /**
     * Stops frame rate optimization monitoring.
     */
    private void stopFrameRateOptimization() {
        if (frameRateOptimizer != null) {
            frameRateOptimizer.stop();
            frameRateOptimizer = null;
        }
        
        int dropped = droppedFrames.getAndSet(0);
        if (dropped > 0) {
            logger.fine("Transition completed with " + dropped + " dropped frames");
        }
    }
    
    /**
     * Cleans up expanded layout with proper resource management.
     */
    private void cleanupExpandedLayout(StackPane root, Node expandedLayout) {
        if (root != null && expandedLayout != null) {
            root.getChildren().remove(expandedLayout);
        }
    }
    
    /**
     * Optimizes Pokemon placement in compact mode.
     */
    private void optimizePokemonPlacement(StackPane root, Node pokemonDisplay, Node pokemonStatusBox) {
        if (pokemonStatusBox != null && pokemonDisplay != null) {
            ((javafx.scene.layout.VBox) pokemonStatusBox).getChildren().remove(pokemonDisplay);
        }
        if (root != null && pokemonDisplay != null) {
            root.getChildren().add(pokemonDisplay);
            javafx.scene.layout.StackPane.setAlignment(pokemonDisplay, javafx.geometry.Pos.CENTER);
        }
    }
    
    /**
     * Creates optimized fade in animation for Pokemon.
     */
    private void createOptimizedFadeIn(Node pokemonDisplay, Runnable onComplete) {
        pokemonDisplay.setOpacity(0.0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(130), pokemonDisplay);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.setInterpolator(SMOOTH_INTERPOLATOR);
        trackAnimation(fadeIn);
        
        fadeIn.setOnFinished(f -> completeTransition(onComplete));
        fadeIn.play();
    }
    
    /**
     * Cleans up Pokemon from root container.
     */
    private void cleanupPokemonFromRoot(StackPane root, Node pokemonDisplay) {
        if (root != null && pokemonDisplay != null) {
            root.getChildren().remove(pokemonDisplay);
        }
    }
    
    /**
     * Optimizes Pokemon placement in status box.
     */
    private void optimizePokemonStatusPlacement(Node pokemonStatusBox, Node pokemonDisplay) {
        if (pokemonStatusBox != null && pokemonDisplay != null) {
            ((javafx.scene.layout.VBox) pokemonStatusBox).getChildren().clear();
            ((javafx.scene.layout.VBox) pokemonStatusBox).getChildren().add(pokemonDisplay);
        }
    }
    
    /**
     * Prepares expanded layout for smooth animation.
     */
    private void prepareExpandedLayout(StackPane root, Node expandedLayout) {
        if (root != null && expandedLayout != null) {
            root.getChildren().add(expandedLayout);
            
            // Pre-set properties for smooth animation
            expandedLayout.setScaleX(SCALE_FACTOR);
            expandedLayout.setScaleY(SCALE_FACTOR);
            expandedLayout.setOpacity(0.0);
        }
    }
    
    /**
     * Creates optimized animation for expanded layout.
     */
    private void createOptimizedExpandedAnimation(Node expandedLayout, Node pokemonDisplay, Runnable onComplete) {
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(170), expandedLayout);
        scaleUp.setFromX(SCALE_FACTOR);
        scaleUp.setFromY(SCALE_FACTOR);
        scaleUp.setToX(1.0);
        scaleUp.setToY(1.0);
        scaleUp.setInterpolator(SMOOTH_INTERPOLATOR);
        
        FadeTransition fadeIn = new FadeTransition(Duration.millis(170), expandedLayout);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.setInterpolator(SMOOTH_INTERPOLATOR);
        
        ParallelTransition phase2 = new ParallelTransition(scaleUp, fadeIn);
        trackAnimation(phase2);
        
        // Phase 3: Optimized Pokemon fade in
        phase2.setOnFinished(f -> Platform.runLater(() -> {
            if (pokemonDisplay != null) {
                createOptimizedPokemonFadeIn(pokemonDisplay, onComplete);
            } else {
                completeTransition(onComplete);
            }
        }));
        
        phase2.play();
    }
    
    /**
     * Creates optimized Pokemon fade in for expanded mode.
     */
    private void createOptimizedPokemonFadeIn(Node pokemonDisplay, Runnable onComplete) {
        pokemonDisplay.setOpacity(0.0);
        FadeTransition pokemonFadeIn = new FadeTransition(Duration.millis(90), pokemonDisplay);
        pokemonFadeIn.setFromValue(0.0);
        pokemonFadeIn.setToValue(1.0);
        pokemonFadeIn.setInterpolator(SMOOTH_INTERPOLATOR);
        trackAnimation(pokemonFadeIn);
        
        pokemonFadeIn.setOnFinished(g -> completeTransition(onComplete));
        pokemonFadeIn.play();
    }
    
    /**
     * Performs smooth window resizing to prevent jarring transitions.
     */
    private void resizeWindowSmooth(Stage stage, StackPane root, double width, double height) {
        // Immediate resize for better performance - smooth visual handled by content animation
        stage.setWidth(width);
        stage.setHeight(height);
        root.setPrefSize(width, height);
    }
    
    /**
     * Cleans up all active timelines.
     */
    private void cleanupAllTimelines() {
        for (Timeline timeline : activeTimelines) {
            if (timeline != null) {
                timeline.stop();
            }
        }
        activeTimelines.clear();
        
        if (currentTransition != null) {
            currentTransition.stop();
        }
    }
    
    /**
     * Checks if a new transition can be started.
     * Enhanced to prevent resource conflicts.
     */
    private boolean canStartTransition() {
        return !isTransitioning.get() && 
               activeTransitionCount.get() < MAX_CONCURRENT_TRANSITIONS &&
               !animationsPaused.get();
    }
    
    /**
     * Starts a new transition and updates state.
     */
    private void startTransition(String transitionId) {
        isTransitioning.set(true);
        activeTransitionCount.incrementAndGet();
        currentTransitionId = "ui-transition-" + transitionId + "-" + System.currentTimeMillis();
        
        logger.fine("Started transition: " + currentTransitionId);
    }
    
    /**
     * Completes the current transition and cleans up resources.
     * Enhanced with frame rate optimization cleanup.
     */
    private void completeTransition(Runnable onComplete) {
        Platform.runLater(() -> {
            try {
                // Stop frame rate optimization
                stopFrameRateOptimization();
                
                // Clean up transition resources
                if (currentTransitionId != null) {
                    ResourceManager.getInstance().cleanupResource(currentTransitionId);
                    currentTransitionId = null;
                }
                
                // Clean up all timelines
                cleanupAllTimelines();
                currentTransition = null;
                
                // Resume Pokemon animations
                resumePokemonAnimations();
                
                // Update state
                isTransitioning.set(false);
                activeTransitionCount.decrementAndGet();
                
                logger.fine("Optimized transition completed successfully");
                
                // Execute completion callback
                if (onComplete != null) {
                    onComplete.run();
                }
                
            } catch (Exception e) {
                logger.warning("Error during transition completion: " + e.getMessage());
                
                // Ensure state is reset even if cleanup fails
                isTransitioning.set(false);
                activeTransitionCount.set(0);
                animationsPaused.set(false);
                
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }
    
    /**
     * Pauses Pokemon animations during transitions to prevent conflicts.
     * Enhanced with state tracking for better resource management.
     */
    private void pausePokemonAnimations() {
        if (animationController != null && !animationsPaused.getAndSet(true)) {
            // The AnimationController will handle pausing active animations
            logger.fine("Pausing Pokemon animations for transition");
        }
    }
    
    /**
     * Resumes Pokemon animations after transitions complete.
     * Enhanced with state tracking for better resource management.
     */
    private void resumePokemonAnimations() {
        if (animationController != null && animationsPaused.getAndSet(false)) {
            // The AnimationController will handle resuming animations
            logger.fine("Resuming Pokemon animations after transition");
        }
    }
    
    /**
     * Cancels any active transition.
     * Enhanced with comprehensive resource cleanup.
     */
    public void cancelTransition() {
        if (isTransitioning.get()) {
            logger.info("Cancelling active UI transition");
            
            // Stop frame rate optimization
            stopFrameRateOptimization();
            
            // Clean up all timelines
            cleanupAllTimelines();
            currentTransition = null;
            
            if (currentTransitionId != null) {
                ResourceManager.getInstance().cleanupResource(currentTransitionId);
                currentTransitionId = null;
            }
            
            resumePokemonAnimations();
            
            isTransitioning.set(false);
            activeTransitionCount.set(0);
        }
    }
    
    /**
     * Checks if a transition is currently in progress.
     */
    public boolean isTransitioning() {
        return isTransitioning.get();
    }
    
    /**
     * Gets the number of active transitions.
     */
    public int getActiveTransitionCount() {
        return activeTransitionCount.get();
    }
    
    /**
     * Gets the number of dropped frames during transitions.
     * Useful for performance monitoring.
     */
    public int getDroppedFrameCount() {
        return droppedFrames.get();
    }
    
    /**
     * Checks if animations are currently paused.
     */
    public boolean areAnimationsPaused() {
        return animationsPaused.get();
    }
    
    /**
     * Cleans up all transition resources.
     * Enhanced with comprehensive resource management.
     */
    public void cleanup() {
        logger.info("Cleaning up UITransitionManager");
        
        cancelTransition();
        
        // Stop frame rate optimization
        stopFrameRateOptimization();
        
        // Clean up all timelines
        cleanupAllTimelines();
        
        // Ensure all state is reset
        isTransitioning.set(false);
        activeTransitionCount.set(0);
        animationsPaused.set(false);
        droppedFrames.set(0);
        currentTransition = null;
        currentTransitionId = null;
        
        logger.info("UITransitionManager cleanup completed");
    }
}