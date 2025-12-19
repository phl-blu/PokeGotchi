package com.tamagotchi.committracker.pokemon;

import com.tamagotchi.committracker.domain.CommitHistory;
import com.tamagotchi.committracker.domain.Commit;
import com.tamagotchi.committracker.ui.components.PokemonDisplayComponent;
import java.util.List;
import java.util.function.Consumer;

/**
 * High-level manager that orchestrates Pokemon state management, evolution logic,
 * and animation triggers. This class connects commit activity to Pokemon emotional
 * states and manages evolution sequences.
 */
public class PokemonEvolutionManager {
    
    private PokemonStateManager stateManager;
    private PokemonDisplayComponent displayComponent;
    private Consumer<EvolutionStage> evolutionCallback;
    
    // Current state tracking
    private PokemonState lastKnownState;
    private EvolutionStage lastKnownStage;
    private boolean evolutionInProgress;
    
    public PokemonEvolutionManager(PokemonSpecies selectedSpecies) {
        this.stateManager = new PokemonStateManager(selectedSpecies);
        this.lastKnownState = PokemonState.CONTENT;
        this.lastKnownStage = EvolutionStage.EGG;
        this.evolutionInProgress = false;
    }
    
    public PokemonEvolutionManager(PokemonSpecies selectedSpecies, EvolutionStage currentStage, int currentXP) {
        this.stateManager = new PokemonStateManager(selectedSpecies, currentStage, currentXP);
        this.lastKnownState = PokemonState.CONTENT;
        this.lastKnownStage = currentStage;
        this.evolutionInProgress = false;
    }
    
    /**
     * Sets the display component that will show the Pokemon animations.
     */
    public void setDisplayComponent(PokemonDisplayComponent displayComponent) {
        this.displayComponent = displayComponent;
    }
    
    /**
     * Sets a callback that will be called when evolution occurs.
     */
    public void setEvolutionCallback(Consumer<EvolutionStage> callback) {
        this.evolutionCallback = callback;
    }
    
    /**
     * Processes new commits and updates Pokemon state accordingly.
     * This is the main method called when new commits are detected.
     * 
     * @param commitHistory Current commit history
     * @param newCommits List of newly detected commits
     * @return true if any state changes occurred
     */
    public boolean processCommitActivity(CommitHistory commitHistory, List<Commit> newCommits) {
        if (evolutionInProgress) {
            System.out.println("🌟 Evolution in progress, skipping commit processing");
            return false;
        }
        
        boolean stateChanged = false;
        
        // Process new commits and gain XP
        if (newCommits != null && !newCommits.isEmpty()) {
            int xpGained = stateManager.processNewCommits(newCommits);
            // Note: Detailed XP logging is handled by stateManager.processNewCommits()
            
            // Trigger commit animation if display component is available
            if (displayComponent != null) {
                displayComponent.triggerCommitAnimation(stateManager.getCurrentXP(), commitHistory.getCurrentStreak());
            }
            
            stateChanged = true;
        }
        
        // Calculate new Pokemon state based on commit activity
        PokemonState newState;
        if (newCommits != null && !newCommits.isEmpty()) {
            // Use commit-triggered state calculation for more energetic response
            newState = stateManager.calculateStateForCommitTrigger(commitHistory, newCommits);
        } else {
            // Use regular state calculation
            newState = stateManager.calculateState(commitHistory);
        }
        
        // Update state if it changed
        if (newState != lastKnownState) {
            System.out.println("😊 Pokemon state changed: " + lastKnownState + " -> " + newState);
            lastKnownState = newState;
            
            if (displayComponent != null) {
                displayComponent.updateStateWithXP(newState, stateManager.getCurrentXP());
            }
            
            stateChanged = true;
        }
        
        // Check for evolution
        if (checkAndTriggerEvolution(commitHistory.getCurrentStreak())) {
            stateChanged = true;
        }
        
        return stateChanged;
    }
    
    /**
     * Checks evolution criteria and triggers evolution if ready.
     * 
     * @param currentStreak Current commit streak in days
     * @return true if evolution was triggered
     */
    public boolean checkAndTriggerEvolution(int currentStreak) {
        if (evolutionInProgress) {
            return false;
        }
        
        EvolutionStage newStage = stateManager.triggerEvolutionIfReady(currentStreak);
        if (newStage != null && newStage != lastKnownStage) {
            System.out.println("🌟 Evolution triggered! " + lastKnownStage + " -> " + newStage);
            
            evolutionInProgress = true;
            lastKnownStage = newStage;
            
            // Trigger evolution animation if display component is available
            if (displayComponent != null) {
                displayComponent.checkEvolutionRequirements(stateManager.getCurrentXP(), currentStreak);
            }
            
            // Call evolution callback if set
            if (evolutionCallback != null) {
                evolutionCallback.accept(newStage);
            }
            
            // Reset evolution flag after a delay (would be handled by animation completion in real usage)
            // For now, we'll reset it immediately
            evolutionInProgress = false;
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Updates Pokemon state based on current commit history without new commits.
     * Used for periodic state updates when no new commits are detected.
     * 
     * @param commitHistory Current commit history
     * @return true if state changed
     */
    public boolean updateStateFromHistory(CommitHistory commitHistory) {
        if (evolutionInProgress) {
            return false;
        }
        
        PokemonState newState = stateManager.calculateState(commitHistory);
        
        if (newState != lastKnownState) {
            System.out.println("😊 Pokemon state updated: " + lastKnownState + " -> " + newState);
            lastKnownState = newState;
            
            if (displayComponent != null) {
                displayComponent.updateStateWithXP(newState, stateManager.getCurrentXP());
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Gets the current Pokemon emotional state.
     */
    public PokemonState getCurrentState() {
        return lastKnownState;
    }
    
    /**
     * Gets the current evolution stage.
     */
    public EvolutionStage getCurrentStage() {
        return lastKnownStage;
    }
    
    /**
     * Gets the current Pokemon species.
     */
    public PokemonSpecies getCurrentSpecies() {
        return stateManager.getCurrentSpecies();
    }
    
    /**
     * Gets the current XP amount.
     */
    public int getCurrentXP() {
        return stateManager.getCurrentXP();
    }
    
    /**
     * Gets the current level.
     */
    public int getCurrentLevel() {
        return stateManager.getCurrentLevel();
    }
    
    /**
     * Calculates health metrics based on commit patterns.
     */
    public double calculateHealthMetrics(CommitHistory commitHistory) {
        return stateManager.calculateHealthMetrics(commitHistory);
    }
    
    /**
     * Gets the XP system for direct access if needed.
     */
    public XPSystem getXPSystem() {
        return stateManager.getXpSystem();
    }
    
    /**
     * Gets the state manager for direct access if needed.
     */
    public PokemonStateManager getStateManager() {
        return stateManager;
    }
    
    /**
     * Checks if evolution is currently in progress.
     */
    public boolean isEvolutionInProgress() {
        return evolutionInProgress;
    }
    
    /**
     * Forces evolution to the next stage (for testing purposes).
     * TODO: Remove this method before production.
     */
    public void forceEvolutionForTesting() {
        if (!evolutionInProgress) {
            EvolutionStage nextStage = stateManager.getNextEvolutionStage();
            if (nextStage != lastKnownStage) {
                System.out.println("🧪 TESTING: Forcing evolution from " + lastKnownStage + " to " + nextStage);
                
                evolutionInProgress = true;
                lastKnownStage = nextStage;
                stateManager.setCurrentStage(nextStage);
                
                if (displayComponent != null) {
                    displayComponent.forceEvolutionForTesting();
                }
                
                if (evolutionCallback != null) {
                    evolutionCallback.accept(nextStage);
                }
                
                evolutionInProgress = false;
            }
        }
    }
    
    /**
     * Resets Pokemon to egg stage (for testing purposes).
     * TODO: Remove this method before production.
     */
    public void resetToEggForTesting() {
        System.out.println("🧪 TESTING: Resetting Pokemon to egg stage");
        
        evolutionInProgress = false;
        lastKnownStage = EvolutionStage.EGG;
        lastKnownState = PokemonState.CONTENT;
        
        stateManager.setCurrentStage(EvolutionStage.EGG);
        stateManager.getXpSystem().resetForTesting();
        
        if (displayComponent != null) {
            displayComponent.forceDeevolutionToEggForTesting();
        }
    }
    
    @Override
    public String toString() {
        return "PokemonEvolutionManager{" +
               "species=" + getCurrentSpecies() +
               ", stage=" + getCurrentStage() +
               ", state=" + getCurrentState() +
               ", xp=" + getCurrentXP() +
               ", level=" + getCurrentLevel() +
               ", evolutionInProgress=" + evolutionInProgress +
               '}';
    }
}