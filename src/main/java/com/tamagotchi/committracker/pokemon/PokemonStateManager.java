package com.tamagotchi.committracker.pokemon;

import com.tamagotchi.committracker.domain.CommitHistory;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Determines Pokemon's emotional state and evolution progress based on commit activity.
 */
public class PokemonStateManager {
    
    /**
     * Calculates the current Pokemon state based on commit history.
     * This is a basic implementation for task 2 - will be enhanced in task 6.
     */
    public PokemonState calculateState(CommitHistory history) {
        if (history == null) {
            return PokemonState.CONTENT;
        }
        
        LocalDateTime lastCommit = history.getLastCommitTime();
        int currentStreak = history.getCurrentStreak();
        double averageCommits = history.getAverageCommitsPerDay();
        
        // If no commits ever
        if (lastCommit == null) {
            return PokemonState.NEGLECTED;
        }
        
        long daysSinceLastCommit = ChronoUnit.DAYS.between(lastCommit, LocalDateTime.now());
        
        // Basic state calculation logic
        if (daysSinceLastCommit > 7) {
            return PokemonState.NEGLECTED;
        } else if (daysSinceLastCommit > 3) {
            return PokemonState.SAD;
        } else if (daysSinceLastCommit > 1) {
            return PokemonState.CONCERNED;
        } else if (currentStreak >= 7 && averageCommits >= 2.0) {
            return PokemonState.THRIVING;
        } else if (currentStreak >= 3 || averageCommits >= 1.0) {
            return PokemonState.HAPPY;
        } else {
            return PokemonState.CONTENT;
        }
    }
    
    /**
     * Checks if evolution criteria are met based on XP and streak requirements.
     */
    public boolean checkEvolutionCriteria(int xp, int streakDays, EvolutionStage currentStage) {
        if (currentStage == null) {
            return false;
        }
        
        // Evolution requirements based on design document
        switch (currentStage) {
            case EGG:
                // Hatch from egg: 4+ day streak + reach XP threshold (200)
                return streakDays >= 4 && xp >= XPSystem.getXPThresholdForStage(EvolutionStage.BASIC);
                
            case BASIC:
                // Evolve to Stage 1: 11+ day streak + reach XP threshold (800)
                return streakDays >= 11 && xp >= XPSystem.getXPThresholdForStage(EvolutionStage.STAGE_1);
                
            case STAGE_1:
                // Evolve to Stage 2: 22+ day streak + reach XP threshold (2000)
                return streakDays >= 22 && xp >= XPSystem.getXPThresholdForStage(EvolutionStage.STAGE_2);
                
            case STAGE_2:
                // Already at final evolution
                return false;
                
            default:
                return false;
        }
    }
    
    /**
     * Gets the next evolution stage if criteria are met.
     */
    public EvolutionStage getNextEvolutionStage(EvolutionStage currentStage) {
        if (currentStage == null) {
            return EvolutionStage.EGG;
        }
        
        switch (currentStage) {
            case EGG:
                return EvolutionStage.BASIC;
            case BASIC:
                return EvolutionStage.STAGE_1;
            case STAGE_1:
                return EvolutionStage.STAGE_2;
            case STAGE_2:
                return EvolutionStage.STAGE_2; // Already at max
            default:
                return currentStage;
        }
    }
}