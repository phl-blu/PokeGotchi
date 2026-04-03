package com.tamagotchi.committracker;

import javafx.application.Application;
import javafx.stage.Stage;
import com.tamagotchi.committracker.ui.widget.WidgetWindow;
import com.tamagotchi.committracker.git.CommitService;
import com.tamagotchi.committracker.pokemon.EvolutionStage;
import com.tamagotchi.committracker.pokemon.PokemonSelectionData;
import com.tamagotchi.committracker.pokemon.PokemonSpecies;
import com.tamagotchi.committracker.pokemon.PokemonStateManager;
import com.tamagotchi.committracker.pokemon.XPSystem;
import com.tamagotchi.committracker.util.SpriteCache;
import com.tamagotchi.committracker.util.ResourceManager;
import com.tamagotchi.committracker.config.AppConfig;

/**
 * Main JavaFX Application class for the Tamagotchi Commit Tracker.
 * This desktop widget monitors Git repositories and displays a Pokemon
 * whose evolution and state reflect the user's coding activity.
 * Optimized with sprite caching and configurable performance settings.
 */
public class TamagotchiCommitTrackerApp extends Application {

    private WidgetWindow widgetWindow;
    private CommitService commitService;
    private PokemonStateManager pokemonStateManager;
    private XPSystem xpSystem;
    private ResourceManager resourceManager;

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Tamagotchi Commit Tracker");
        
        // Initialize resource manager for lifecycle management
        resourceManager = ResourceManager.getInstance();
        
        // Initialize sprite cache and preload common sprites for better performance
        if (AppConfig.isSpriteCachingEnabled()) {
            System.out.println("🖼️ Initializing sprite cache...");
            SpriteCache.getInstance().preloadEssentialSprites();
            System.out.println("✅ Sprite cache initialized: " + SpriteCache.getInstance().getCacheStats());
            
            // Preload evolution effects for smooth evolution animations
            System.out.println("✨ Preloading evolution effects...");
            SpriteCache.getInstance().preloadEvolutionEffects();
            System.out.println("✅ Evolution effects preloaded");
        }
        
        // Initialize core services
        commitService = new CommitService();
        xpSystem = new XPSystem();
        pokemonStateManager = new PokemonStateManager();
        
        // Register core services with resource manager
        resourceManager.registerResource("commitService", commitService::stopMonitoring, ResourceManager.ResourceType.OTHER);
        resourceManager.registerResource("spriteCache", () -> {
            if (AppConfig.isSpriteCachingEnabled()) {
                SpriteCache.getInstance().clearCache();
            }
        }, ResourceManager.ResourceType.CACHE);
        
        // Log performance configuration
        logPerformanceSettings();
        
        // Initialize the widget window with transparency and dragging
        // Pass callback for when Pokemon is selected (to update state manager)
        // Also pass shared XP system and state manager instances
        widgetWindow = new WidgetWindow(primaryStage, selectedSpecies -> {
            // Update the Pokemon state manager with the selected species
            pokemonStateManager.setCurrentSpecies(selectedSpecies);
            System.out.println("🎮 Pokemon state manager updated with: " + selectedSpecies);
            
            // Re-setup reset listener for the new Pokemon display
            setupResetListener();
        }, xpSystem, pokemonStateManager);
        
        // Check if this is a first-time user
        if (widgetWindow.isFirstTimeUser()) {
            // Show the widget first (so selection screen has an owner)
            widgetWindow.show();
            
            // Show Pokemon selection screen (blocks until selection is made)
            widgetWindow.showPokemonSelectionScreen();
        } else {
            // Returning user - use saved Pokemon selection
            PokemonSpecies savedSpecies = widgetWindow.getSelectedPokemonSpecies();
            if (savedSpecies != null) {
                pokemonStateManager.setCurrentSpecies(savedSpecies);
                System.out.println("🎮 Welcome back! Continuing with " + 
                    PokemonSelectionData.getDisplayName(savedSpecies));
            }
            widgetWindow.show();
        }
        
        // Set up reset listener to reset XP when Pokemon is reset (R key pressed)
        // TODO: REMOVE THIS LISTENER SETUP BEFORE PRODUCTION - See TODO.md
        setupResetListener();
        
        // Connect commit monitoring to Pokemon updates
        setupCommitMonitoring();
        
        // Pass the commit service to the widget for manual scanning
        widgetWindow.setCommitService(commitService);
        
        // Start monitoring Git repositories
        commitService.startMonitoring();
        
        // IMPORTANT: Pass the commit service's commit history to the widget
        // This ensures the widget displays real streak and commit data
        widgetWindow.setCommitHistory(commitService.getCommitHistory());
        
        // Debug: Log initial commit history state
        var initialHistory = commitService.getCommitHistory();
        System.out.println("🔍 Initial commit history state:");
        System.out.println("   Total commits: " + initialHistory.getRecentCommits().size());
        System.out.println("   Current streak: " + initialHistory.getCurrentStreak() + " days");
        System.out.println("   Daily commit counts: " + initialHistory.getDailyCommitCounts().size() + " days");
        System.out.println("   Last commit time: " + initialHistory.getLastCommitTime());
        System.out.println("   Memory usage: " + initialHistory.getMemoryUsageStats());
        
        // Log Windows integration status
        if (widgetWindow.isWindowsIntegrationEnabled()) {
            System.out.println("🪟 Windows system integration enabled successfully");
        } else {
            System.out.println("⚠️ Windows system integration not available or failed");
        }
        
        System.out.println("🚀 Tamagotchi Commit Tracker started!");
        System.out.println("📊 Monitoring Git repositories every " + AppConfig.getPollingIntervalSeconds() + " seconds for real-time updates!");
    }
    
    /**
     * Logs the current performance configuration settings.
     */
    private void logPerformanceSettings() {
        System.out.println("⚙️ Performance Configuration:");
        System.out.println("   Polling interval: " + AppConfig.getPollingIntervalSeconds() + " seconds");
        System.out.println("   Max commits per repo: " + AppConfig.getMaxCommitsPerRepo());
        System.out.println("   Max repositories: " + AppConfig.getMaxRepositories());
        System.out.println("   Commit history limit: " + AppConfig.getCommitHistoryLimit());
        System.out.println("   Scan depth: " + AppConfig.getScanDepth());
        System.out.println("   Scan timeout: " + AppConfig.getScanTimeoutSeconds() + " seconds");
        System.out.println("   Sprite caching: " + (AppConfig.isSpriteCachingEnabled() ? "enabled" : "disabled"));
        System.out.println("   Lazy loading: " + (AppConfig.isLazyLoadingEnabled() ? "enabled" : "disabled"));
        System.out.println("   Smooth transitions: " + (AppConfig.areSmoothTransitionsEnabled() ? "enabled" : "disabled"));
        
        if (AppConfig.isSpriteCachingEnabled()) {
            System.out.println("   Max cached sprites: " + AppConfig.getMaxCachedSprites());
        }
    }
    
    /**
     * Sets up the reset listener for the Pokemon display.
     * TODO: REMOVE THIS METHOD BEFORE PRODUCTION - See TODO.md
     */
    private void setupResetListener() {
        if (widgetWindow.getPokemonDisplay() != null) {
            widgetWindow.getPokemonDisplay().setResetListener(() -> {
                System.out.println("🧪 TESTING: Reset listener triggered - resetting XP to 0");
                if (xpSystem != null) {
                    xpSystem.resetForTesting();
                }
            });
            
            // Set up evolution listener via PokedexFrame so PokedexMainDisplay.updateNameFromStage() fires
            var pokedexFrameForListener = widgetWindow.getPokedexFrame();
            if (pokedexFrameForListener != null) {
                pokedexFrameForListener.setEvolutionListener((newSpecies, newStage) -> {
                    System.out.println("🌟 Evolution complete! Updating UI with new stage: " + newStage);
                    
                    // Record the evolution in statistics service
                    var statisticsService = widgetWindow.getStatisticsService();
                    if (statisticsService != null) {
                        EvolutionStage fromStage = getPreviousStage(newStage);
                        int currentXP = xpSystem != null ? xpSystem.getCurrentXP() : 0;
                        int currentStreak = commitService != null ? commitService.getCommitHistory().getCurrentStreak() : 0;
                        statisticsService.recordEvolution(newSpecies, fromStage, newStage, currentXP, currentStreak);
                    }
                    
                    // PokedexMainDisplay.setEvolutionListener already called updateNameFromStage().
                    // Just trigger a full status refresh on the FX thread.
                    javafx.application.Platform.runLater(() -> {
                        widgetWindow.updatePokemonStatusDisplay();
                    });
                });
            }
        }
    }
    
    /**
     * Gets the previous evolution stage.
     * 
     * @param currentStage The current stage
     * @return The previous stage
     */
    private EvolutionStage getPreviousStage(EvolutionStage currentStage) {
        switch (currentStage) {
            case BASIC: return EvolutionStage.EGG;
            case STAGE_1: return EvolutionStage.BASIC;
            case STAGE_2: return EvolutionStage.STAGE_1;
            default: return EvolutionStage.EGG;
        }
    }
    
    /**
     * Sets up the connection between commit monitoring and Pokemon state updates.
     */
    private void setupCommitMonitoring() {
        commitService.addCommitListener(newCommits -> {
            boolean isInitialScan = newCommits.size() > 5; // Assume initial scan if many commits found
            
            if (isInitialScan) {
                System.out.println("🔍 Initial repository scan found " + newCommits.size() + " commits from last 30 days");
            } else {
                System.out.println("🎉 Found " + newCommits.size() + " new commits!");
            }
            
            // Calculate XP from new commits
            int totalXP = 0;
            for (var commit : newCommits) {
                int commitXP = xpSystem.calculateXPFromCommit(commit);
                totalXP += commitXP;
                if (!isInitialScan) {
                    System.out.println("📈 Commit: \"" + commit.getMessage() + "\" +XP: " + commitXP);
                }
            }
            
            // Add XP to the system
            xpSystem.addXP(totalXP);
            
            // Get current stats
            var commitHistory = commitService.getCommitHistory();
            int currentXP = xpSystem.getCurrentXP();
            int currentStreak = commitHistory.getCurrentStreak();
            
            // Log current XP and streak
            System.out.println("💎 Total XP accumulated: " + currentXP);
            System.out.println("🔥 Current day streak: " + currentStreak + " days");
            
            // Update Pokemon state based on commit history
            var pokemonState = pokemonStateManager.calculateState(commitHistory);
            
            // Update the Pokemon display
            if (widgetWindow.getPokemonDisplay() != null) {
                // Get current evolution stage before checking requirements
                var currentStage = widgetWindow.getPokemonDisplay().getCurrentStage();
                
                if (isInitialScan) {
                    System.out.println("🔍 Pokemon started as: " + currentStage + " (XP: " + currentXP + ", Streak: " + currentStreak + " days)");
                    if (currentStage == com.tamagotchi.committracker.pokemon.EvolutionStage.EGG) {
                        System.out.println("🔍 Checking evolution requirements: 4+ day streak OR 50+ XP for hatching");
                    } else {
                        System.out.println("🔍 Checking evolution requirements: streak-based only (11+ days for Stage1, 22+ days for Stage2)");
                    }
                }
                
                // IMPORTANT: Trigger commit animation for eggs/Pokemon
                if (!isInitialScan) {
                    widgetWindow.getPokemonDisplay().triggerCommitAnimation(currentXP, currentStreak);
                }
                
                // Update Pokemon state
                widgetWindow.getPokemonDisplay().updateState(pokemonState);
                
                // Check for evolution based on XP and streak
                // Use instant mode for initial scan to skip animation and set directly to target stage
                boolean evolved = widgetWindow.getPokemonDisplay().checkEvolutionRequirements(currentXP, currentStreak, isInitialScan);
                if (evolved) {
                    var newStage = widgetWindow.getPokemonDisplay().getCurrentStage();
                    if (isInitialScan && currentStage == com.tamagotchi.committracker.pokemon.EvolutionStage.EGG) {
                        System.out.println("🥚➡️🐣 " + currentStreak + " day streak found! " + currentXP + " XP accumulated! Automatic hatching commenced!");
                        System.out.println("🌟 Pokemon evolved from " + currentStage + " to " + newStage);
                    } else if (isInitialScan) {
                        System.out.println("🌟 " + currentStreak + " day streak found! " + currentXP + " XP accumulated! Automatic evolution triggered!");
                        System.out.println("🌟 Pokemon evolved from " + currentStage + " to " + newStage);
                    } else {
                        System.out.println("🌟 Pokemon evolved! XP: " + currentXP + ", Streak: " + currentStreak + " days");
                    }
                } else {
                    if (isInitialScan) {
                        System.out.println("🥚 Evolution requirements not met. Current: " + currentXP + " XP, " + currentStreak + " day streak");
                        if (currentStage == com.tamagotchi.committracker.pokemon.EvolutionStage.EGG) {
                            System.out.println("🥚 Need: 4+ day streak OR 60+ XP for hatching");
                        } else if (currentStage == com.tamagotchi.committracker.pokemon.EvolutionStage.BASIC) {
                            System.out.println("🐣 Need: 11+ day streak for Stage1 evolution");
                        } else if (currentStage == com.tamagotchi.committracker.pokemon.EvolutionStage.STAGE_1) {
                            System.out.println("🌟 Need: 22+ day streak for Stage2 evolution");
                        }
                    } else {
                        System.out.println("🥚 Evolution requirements not met yet. Need more streak days.");
                    }
                }
                
                // IMPORTANT: Update the widget's commit history with the latest data FIRST
                // This ensures real-time updates of streak and commit list
                widgetWindow.setCommitHistory(commitService.getCommitHistory());
                
                // CRITICAL: Update the Pokemon status display AFTER evolution check completes
                // This ensures the UI shows the correct evolved stage, not the old stage
                // Use Platform.runLater to ensure this happens after any evolution animations
                javafx.application.Platform.runLater(() -> {
                    widgetWindow.updatePokemonStatusDisplay();
                    System.out.println("🔄 UI updated with current Pokemon stage: " + 
                        widgetWindow.getPokemonDisplay().getCurrentStage());
                });
            }
        });
    }

    /**
     * FOR TESTING ONLY: Resets Pokemon to egg stage and XP to 0 for testing purposes.
     * This allows testing of egg animations and evolution progression from scratch.
     * TODO: REMOVE THIS METHOD BEFORE PRODUCTION - See TODO.md
     */
    public void resetPokemonForTesting() {
        System.out.println("🧪 TESTING: Performing complete Pokemon reset for testing");
        
        // Reset XP system
        if (xpSystem != null) {
            xpSystem.resetForTesting();
        }
        
        // Reset Pokemon display to egg stage
        if (widgetWindow != null && widgetWindow.getPokemonDisplay() != null) {
            widgetWindow.getPokemonDisplay().forceDeevolutionToEggForTesting();
        }
        
        System.out.println("🥚 TESTING: Complete reset finished - Pokemon is now an egg with 0 XP");
    }
    
    /**
     * FOR TESTING ONLY: Forces Pokemon evolution for testing purposes.
     * TODO: REMOVE THIS METHOD BEFORE PRODUCTION - See TODO.md
     */
    public void forceEvolutionForTesting() {
        if (widgetWindow != null && widgetWindow.getPokemonDisplay() != null) {
            widgetWindow.getPokemonDisplay().forceEvolutionForTesting();
        }
    }

    @Override
    public void stop() throws Exception {
        System.out.println("🛑 Shutting down Tamagotchi Commit Tracker...");
        
        // Use ResourceManager for comprehensive cleanup
        if (resourceManager != null) {
            resourceManager.cleanupAll();
            System.out.println("✅ All resources cleaned up via ResourceManager");
            
            // Log resource statistics
            var stats = resourceManager.getResourceStats();
            System.out.println("📊 Resource Statistics: " + stats);
            
            if (resourceManager.hasPotentialLeaks()) {
                System.out.println("⚠️ Potential resource leaks detected - check logs");
            }
        }
        
        // Legacy cleanup for compatibility
        if (commitService != null) {
            commitService.stopMonitoring();
        }
        
        // Clear sprite cache to free memory
        if (AppConfig.isSpriteCachingEnabled()) {
            SpriteCache.getInstance().clearCache();
            System.out.println("🖼️ Sprite cache cleared");
        }
        
        // Cleanup Pokemon display
        if (widgetWindow != null && widgetWindow.getPokemonDisplay() != null) {
            widgetWindow.getPokemonDisplay().cleanup();
        }
        
        System.out.println("✅ Shutdown complete");
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}