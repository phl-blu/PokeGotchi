package com.tamagotchi.committracker.pokemon;

import com.tamagotchi.committracker.domain.CommitHistory;
import com.tamagotchi.committracker.domain.Commit;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Determines Pokemon's emotional state and evolution progress based on commit activity.
 * Enhanced implementation that connects commit activity to Pokemon emotional states
 * and manages evolution triggers and animation sequences.
 */
public class PokemonStateManager {
    
    private XPSystem xpSystem;
    private PokemonSpecies currentSpecies;
    private EvolutionStage currentStage;
    
    /**
     * Creates a PokemonStateManager with no species selected.
     * The species must be set via setCurrentSpecies() before use.
     */
    public PokemonStateManager() {
        this.xpSystem = new XPSystem();
        this.currentSpecies = null; // No default - must be set after Pokemon selection
        this.currentStage = EvolutionStage.EGG;
    }
    
    public PokemonStateManager(PokemonSpecies species) {
        this.xpSystem = new XPSystem();
        this.currentSpecies = species;
        this.currentStage = EvolutionStage.EGG;
    }
    
    public PokemonStateManager(PokemonSpecies species, EvolutionStage stage, int currentXP) {
        this.xpSystem = new XPSystem(currentXP);
        this.currentSpecies = species;
        this.currentStage = stage;
    }
    
    /**
     * Calculates the current Pokemon state based on commit history and activity patterns.
     * Enhanced logic that considers multiple factors for more nuanced emotional states.
     */
    public PokemonState calculateState(CommitHistory history) {
        if (history == null) {
            return PokemonState.CONTENT;
        }
        
        LocalDateTime lastCommit = history.getLastCommitTime();
        int currentStreak = history.getCurrentStreak();
        double averageCommits = history.getAverageCommitsPerDay();
        List<Commit> recentCommits = history.getRecentCommits();
        
        // If no commits ever
        if (lastCommit == null) {
            return PokemonState.NEGLECTED;
        }
        
        long daysSinceLastCommit = ChronoUnit.DAYS.between(lastCommit, LocalDateTime.now());
        long hoursSinceLastCommit = ChronoUnit.HOURS.between(lastCommit, LocalDateTime.now());
        
        // Calculate recent activity (commits in last 24 hours)
        int commitsToday = getCommitsInLastHours(recentCommits, 24);
        int commitsThisWeek = getCommitsInLastDays(recentCommits, 7);
        
        // Enhanced state calculation with more nuanced logic
        if (daysSinceLastCommit > 7) {
            return PokemonState.NEGLECTED; // No commits for over a week
        } else if (daysSinceLastCommit > 3) {
            return PokemonState.SAD; // No commits for 3-7 days
        } else if (daysSinceLastCommit > 1) {
            // 1-3 days since last commit - check if this is unusual for the user
            if (averageCommits >= 1.5) {
                return PokemonState.CONCERNED; // User usually commits more frequently
            } else {
                return PokemonState.CONTENT; // Normal for this user's pattern
            }
        } else if (hoursSinceLastCommit <= 1) {
            // Very recent commit activity
            if (commitsToday >= 5) {
                return PokemonState.THRIVING; // Very active today
            } else if (commitsToday >= 3) {
                return PokemonState.HAPPY; // Good activity today
            } else {
                return PokemonState.CONTENT; // Normal activity
            }
        } else {
            // Commit within last day - evaluate overall patterns
            if (currentStreak >= 14 && averageCommits >= 2.0) {
                return PokemonState.THRIVING; // Long streak + high activity
            } else if (currentStreak >= 7 && averageCommits >= 1.5) {
                return PokemonState.HAPPY; // Good streak + decent activity
            } else if (currentStreak >= 3 || commitsThisWeek >= 5) {
                return PokemonState.HAPPY; // Either good streak or good weekly activity
            } else if (averageCommits >= 1.0 || commitsThisWeek >= 3) {
                return PokemonState.CONTENT; // Moderate activity
            } else {
                return PokemonState.CONCERNED; // Low activity pattern
            }
        }
    }
    
    /**
     * Calculates Pokemon state specifically for commit-triggered animations.
     * Returns more energetic states when commits are detected.
     */
    public PokemonState calculateStateForCommitTrigger(CommitHistory history, List<Commit> newCommits) {
        if (newCommits == null || newCommits.isEmpty()) {
            return calculateState(history);
        }
        
        // When new commits are detected, Pokemon should be more energetic
        int commitCount = newCommits.size();
        
        if (commitCount >= 3) {
            return PokemonState.THRIVING; // Multiple commits = very happy
        } else if (commitCount >= 2) {
            return PokemonState.HAPPY; // Couple commits = happy
        } else {
            // Single commit - check if it's a high-quality commit
            Commit commit = newCommits.get(0);
            int xpGained = xpSystem.calculateXPFromCommit(commit);
            
            if (xpGained >= 20) {
                return PokemonState.HAPPY; // High-quality commit
            } else {
                return PokemonState.CONTENT; // Regular commit
            }
        }
    }
    
    /**
     * Processes new commits and updates XP system.
     * Returns the total XP gained from the new commits.
     */
    public int processNewCommits(List<Commit> newCommits) {
        if (newCommits == null || newCommits.isEmpty()) {
            return 0;
        }
        
        int totalXPGained = 0;
        for (Commit commit : newCommits) {
            int xpBefore = xpSystem.getCurrentXP();
            int xpGained = xpSystem.calculateXPFromCommit(commit);
            xpSystem.addXP(xpGained);
            int xpAfter = xpSystem.getCurrentXP();
            
            // Clear, consistent logging format for debugging
            System.out.println("XP before commit: " + xpBefore);
            System.out.println("XP earned from commit: " + xpGained);
            System.out.println("XP after commit: " + xpAfter);
            System.out.println("-----------------------------");
            
            totalXPGained += xpGained;
        }
        

        
        return totalXPGained;
    }
    
    /**
     * Counts commits within the last specified number of hours.
     */
    private int getCommitsInLastHours(List<Commit> commits, int hours) {
        if (commits == null) return 0;
        
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
        return (int) commits.stream()
                .filter(commit -> commit.getTimestamp() != null && commit.getTimestamp().isAfter(cutoff))
                .count();
    }
    
    /**
     * Counts commits within the last specified number of days.
     */
    private int getCommitsInLastDays(List<Commit> commits, int days) {
        if (commits == null) return 0;
        
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        return (int) commits.stream()
                .filter(commit -> commit.getTimestamp() != null && commit.getTimestamp().isAfter(cutoff))
                .count();
    }
    
    /**
     * Checks if evolution criteria are met based on XP and streak requirements.
     * Updated XP thresholds: Egg evolves at 60 XP.
     */
    public boolean checkEvolutionCriteria(int xp, int streakDays, EvolutionStage currentStage) {
        if (currentStage == null) {
            return false;
        }
        
        // Evolution requirements with updated XP thresholds
        switch (currentStage) {
            case EGG:
                // Hatch from egg: EITHER 4+ day streak OR 60+ XP
                // XP stages: 0-10=stage1, 11-20=stage2, 21-35=stage3, 36-59=stage4, 60+=evolve
                return streakDays >= 4 || xp >= 60;
                
            case BASIC:
                // Evolve to Stage 1: 11+ day streak (XP not required after hatching)
                return streakDays >= 11;
                
            case STAGE_1:
                // Evolve to Stage 2: 22+ day streak (XP not required after hatching)
                return streakDays >= 22;
                
            case STAGE_2:
                // Already at final evolution
                return false;
                
            default:
                return false;
        }
    }
    
    /**
     * Checks if evolution criteria are met using current internal state.
     */
    public boolean checkEvolutionCriteria(int streakDays) {
        return checkEvolutionCriteria(xpSystem.getCurrentXP(), streakDays, currentStage);
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
    
    /**
     * Gets the next evolution stage for current Pokemon.
     */
    public EvolutionStage getNextEvolutionStage() {
        return getNextEvolutionStage(currentStage);
    }
    
    /**
     * Triggers evolution if criteria are met and returns the new stage.
     * Returns null if evolution is not possible or criteria not met.
     */
    public EvolutionStage triggerEvolutionIfReady(int streakDays) {
        if (checkEvolutionCriteria(streakDays)) {
            EvolutionStage nextStage = getNextEvolutionStage();
            if (nextStage != currentStage) {
                System.out.println("🌟 Evolution triggered! " + currentStage + " -> " + nextStage);
                System.out.println("📊 Requirements met - XP: " + xpSystem.getCurrentXP() + ", Streak: " + streakDays + " days");
                
                // Update current stage
                EvolutionStage previousStage = currentStage;
                currentStage = nextStage;
                
                // Update species if needed (for display purposes)
                currentSpecies = getEvolvedSpecies(currentSpecies, nextStage);
                
                return nextStage;
            }
        }
        return null;
    }
    
    /**
     * Gets the evolved species based on the current species and target stage.
     */
    private PokemonSpecies getEvolvedSpecies(PokemonSpecies baseSpecies, EvolutionStage targetStage) {
        if (targetStage == EvolutionStage.EGG) {
            return baseSpecies; // Eggs don't change species
        }
        
        // Map each starter to its evolution line
        switch (baseSpecies) {
            // 1. Charmander line (Kanto Fire)
            case CHARMANDER:
                return targetStage == EvolutionStage.BASIC ? PokemonSpecies.CHARMANDER :
                       targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.CHARMELEON : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.CHARIZARD : baseSpecies;
            
            // 2. Cyndaquil line (Johto Fire)
            case CYNDAQUIL:
                return targetStage == EvolutionStage.BASIC ? PokemonSpecies.CYNDAQUIL :
                       targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.QUILAVA : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.TYPHLOSION : baseSpecies;
            
            // 3. Mudkip line (Hoenn Water)
            case MUDKIP:
                return targetStage == EvolutionStage.BASIC ? PokemonSpecies.MUDKIP :
                       targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.MARSHTOMP : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.SWAMPERT : baseSpecies;
            
            // 4. Piplup line (Sinnoh Water)
            case PIPLUP:
                return targetStage == EvolutionStage.BASIC ? PokemonSpecies.PIPLUP :
                       targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.PRINPLUP : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.EMPOLEON : baseSpecies;
            
            // 5. Snivy line (Unova Grass)
            case SNIVY:
                return targetStage == EvolutionStage.BASIC ? PokemonSpecies.SNIVY :
                       targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.SERVINE : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.SERPERIOR : baseSpecies;
            
            // 6. Froakie line (Kalos Water)
            case FROAKIE:
                return targetStage == EvolutionStage.BASIC ? PokemonSpecies.FROAKIE :
                       targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.FROGADIER : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.GRENINJA : baseSpecies;
            
            // 7. Rowlet line (Alola Grass)
            case ROWLET:
                return targetStage == EvolutionStage.BASIC ? PokemonSpecies.ROWLET :
                       targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.DARTRIX : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.DECIDUEYE : baseSpecies;
            
            // 8. Grookey line (Galar Grass)
            case GROOKEY:
                return targetStage == EvolutionStage.BASIC ? PokemonSpecies.GROOKEY :
                       targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.THWACKEY : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.RILLABOOM : baseSpecies;
            
            // 9. Fuecoco line (Paldea Fire)
            case FUECOCO:
                return targetStage == EvolutionStage.BASIC ? PokemonSpecies.FUECOCO :
                       targetStage == EvolutionStage.STAGE_1 ? PokemonSpecies.CROCALOR : 
                       targetStage == EvolutionStage.STAGE_2 ? PokemonSpecies.SKELEDIRGE : baseSpecies;
            
            default:
                return baseSpecies; // Return same species if no evolution found
        }
    }
    
    /**
     * Gets the current egg stage based on accumulated XP.
     * Returns the visual stage of the egg (1-4) based on XP thresholds.
     * 
     * @return Egg stage (1-4), or 0 if not in egg stage
     */
    public int getCurrentEggStage() {
        if (currentStage != EvolutionStage.EGG) {
            return 0; // Not an egg
        }
        
        int currentXP = xpSystem.getCurrentXP();
        if (currentXP <= 10) {
            return 1; // Stage 1: Fresh egg (0-10 XP)
        } else if (currentXP <= 25) {
            return 2; // Stage 2: Barely cracked (11-25 XP)
        } else if (currentXP <= 40) {
            return 3; // Stage 3: More cracked (26-40 XP)
        } else {
            return 4; // Stage 4: Very cracked, ready to hatch (41-60 XP)
        }
    }
    
    /**
     * Checks if the egg is ready to hatch based on XP.
     * 
     * @return true if XP >= 60 and still in egg stage
     */
    public boolean isEggReadyToHatch() {
        return currentStage == EvolutionStage.EGG && xpSystem.getCurrentXP() >= 60;
    }
    
    /**
     * Gets the animation state that should be used for the current Pokemon state.
     * Maps complex emotional states to animation-friendly states.
     */
    public PokemonState getAnimationState(PokemonState pokemonState) {
        // Most states map directly to animations
        switch (pokemonState) {
            case THRIVING:
            case HAPPY:
            case CONTENT:
            case CONCERNED:
            case SAD:
            case NEGLECTED:
            case EVOLVING:
                return pokemonState;
            default:
                return PokemonState.CONTENT; // Default fallback
        }
    }
    
    /**
     * Calculates health metrics based on long-term commit patterns.
     * Returns a value between 0.0 (very unhealthy) and 1.0 (very healthy).
     */
    public double calculateHealthMetrics(CommitHistory history) {
        if (history == null) {
            return 0.5; // Neutral health
        }
        
        double health = 0.5; // Start at neutral
        
        // Factor 1: Recent activity (40% of health)
        LocalDateTime lastCommit = history.getLastCommitTime();
        if (lastCommit != null) {
            long daysSinceLastCommit = ChronoUnit.DAYS.between(lastCommit, LocalDateTime.now());
            if (daysSinceLastCommit == 0) {
                health += 0.2; // Committed today
            } else if (daysSinceLastCommit <= 1) {
                health += 0.15; // Committed yesterday
            } else if (daysSinceLastCommit <= 3) {
                health += 0.05; // Recent activity
            } else if (daysSinceLastCommit > 7) {
                health -= 0.2; // No recent activity
            }
        } else {
            health -= 0.2; // No commits ever
        }
        
        // Factor 2: Consistency (30% of health)
        int currentStreak = history.getCurrentStreak();
        if (currentStreak >= 14) {
            health += 0.15; // Excellent consistency
        } else if (currentStreak >= 7) {
            health += 0.1; // Good consistency
        } else if (currentStreak >= 3) {
            health += 0.05; // Some consistency
        }
        
        // Factor 3: Average activity (30% of health)
        double averageCommits = history.getAverageCommitsPerDay();
        if (averageCommits >= 2.0) {
            health += 0.15; // High activity
        } else if (averageCommits >= 1.0) {
            health += 0.1; // Good activity
        } else if (averageCommits >= 0.5) {
            health += 0.05; // Moderate activity
        } else if (averageCommits < 0.2) {
            health -= 0.1; // Low activity
        }
        
        // Clamp between 0.0 and 1.0
        return Math.max(0.0, Math.min(1.0, health));
    }
    
    // Getters and setters
    public XPSystem getXpSystem() {
        return xpSystem;
    }
    
    public PokemonSpecies getCurrentSpecies() {
        return currentSpecies;
    }
    
    public void setCurrentSpecies(PokemonSpecies species) {
        this.currentSpecies = species;
    }
    
    public EvolutionStage getCurrentStage() {
        return currentStage;
    }
    
    public void setCurrentStage(EvolutionStage stage) {
        this.currentStage = stage;
    }
    
    public int getCurrentXP() {
        return xpSystem.getCurrentXP();
    }
    
    public int getCurrentLevel() {
        return xpSystem.getLevel();
    }
}