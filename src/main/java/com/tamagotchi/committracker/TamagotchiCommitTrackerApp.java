package com.tamagotchi.committracker;

import javafx.application.Application;
import javafx.stage.Stage;
import com.tamagotchi.committracker.ui.widget.WidgetWindow;
import com.tamagotchi.committracker.git.CommitService;
import com.tamagotchi.committracker.pokemon.PokemonStateManager;
import com.tamagotchi.committracker.pokemon.XPSystem;

/**
 * Main JavaFX Application class for the Tamagotchi Commit Tracker.
 * This desktop widget monitors Git repositories and displays a Pokemon
 * whose evolution and state reflect the user's coding activity.
 */
public class TamagotchiCommitTrackerApp extends Application {

    private WidgetWindow widgetWindow;
    private CommitService commitService;
    private PokemonStateManager pokemonStateManager;
    private XPSystem xpSystem;

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Tamagotchi Commit Tracker");
        
        // Initialize core services
        commitService = new CommitService();
        xpSystem = new XPSystem();
        pokemonStateManager = new PokemonStateManager();
        
        // Initialize the widget window with transparency and dragging
        widgetWindow = new WidgetWindow(primaryStage);
        
        // Connect commit monitoring to Pokemon updates
        setupCommitMonitoring();
        
        // Start monitoring Git repositories
        commitService.startMonitoring();
        
        widgetWindow.show();
        
        System.out.println("🚀 Tamagotchi Commit Tracker started!");
        System.out.println("📊 Monitoring Git repositories every 5 minutes...");
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
                
                // IMPORTANT: Trigger commit animation for eggs/Pokemon
                if (!isInitialScan) {
                    widgetWindow.getPokemonDisplay().triggerCommitAnimation(currentXP, currentStreak);
                }
                
                // Update Pokemon state
                widgetWindow.getPokemonDisplay().updateState(pokemonState);
                
                // Check for evolution based on XP and streak
                boolean evolved = widgetWindow.getPokemonDisplay().checkEvolutionRequirements(currentXP, currentStreak);
                if (evolved) {
                    if (isInitialScan && currentStage == com.tamagotchi.committracker.pokemon.EvolutionStage.EGG) {
                        System.out.println("🥚➡️🐣 " + currentStreak + " day streak found! " + currentXP + " XP accumulated! Automatic hatching commenced!");
                    } else if (isInitialScan) {
                        System.out.println("🌟 " + currentStreak + " day streak found! " + currentXP + " XP accumulated! Automatic evolution triggered!");
                    } else {
                        System.out.println("🌟 Pokemon evolved! XP: " + currentXP + ", Streak: " + currentStreak + " days");
                    }
                } else {
                    if (!isInitialScan) {
                        System.out.println("🥚 Evolution requirements not met yet. Need more XP or longer streak.");
                    }
                }
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
        // Clean shutdown
        if (commitService != null) {
            commitService.stopMonitoring();
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}