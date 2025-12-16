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
            System.out.println("🎉 Found " + newCommits.size() + " new commits!");
            
            // Calculate XP from new commits
            int totalXP = 0;
            for (var commit : newCommits) {
                int commitXP = xpSystem.calculateXPFromCommit(commit);
                totalXP += commitXP;
                System.out.println("📈 Commit: \"" + commit.getMessage() + "\" +XP: " + commitXP);
            }
            
            // Add XP to the system
            xpSystem.addXP(totalXP);
            
            // Update Pokemon state based on commit history
            var commitHistory = commitService.getCommitHistory();
            var pokemonState = pokemonStateManager.calculateState(commitHistory);
            
            // Update the Pokemon display
            if (widgetWindow.getPokemonDisplay() != null) {
                widgetWindow.getPokemonDisplay().updateState(pokemonState);
                
                // Check for evolution based on XP and streak
                int currentXP = xpSystem.getCurrentXP();
                int currentStreak = commitHistory.getCurrentStreak();
                
                boolean evolved = widgetWindow.getPokemonDisplay().checkEvolutionRequirements(currentXP, currentStreak);
                if (evolved) {
                    System.out.println("🌟 Pokemon evolved! XP: " + currentXP + ", Streak: " + currentStreak + " days");
                }
            }
        });
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