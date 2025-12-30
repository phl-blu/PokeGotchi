package com.tamagotchi.committracker.pokemon;

import com.tamagotchi.committracker.domain.Commit;
import java.util.List;

/**
 * Manages XP calculation and evolution thresholds for Pokemon.
 * 
 * Evolution XP Thresholds (alternative to streak-based evolution):
 * - EGG → BASIC: 60 XP (~8 commits)
 * - BASIC → STAGE_1: 500 XP (~63 commits total)
 * - STAGE_1 → STAGE_2: 1200 XP (~150 commits total)
 * 
 * These thresholds provide a "grinder" path for high-volume committers,
 * complementing the streak-based "consistency" path.
 */
public class XPSystem {
    private int currentXP;
    private int level;
    
    /**
     * XP thresholds for each evolution stage.
     * Index 0 = EGG (0 XP to start)
     * Index 1 = BASIC (60 XP to evolve from EGG)
     * Index 2 = STAGE_1 (500 XP to evolve from BASIC)
     * Index 3 = STAGE_2 (1200 XP to evolve from STAGE_1)
     */
    private static final int[] EVOLUTION_XP_THRESHOLDS = {0, 60, 500, 1200};
    
    public XPSystem() {
        this.currentXP = 0;
        this.level = 0;
    }
    
    public XPSystem(int currentXP) {
        this.currentXP = Math.max(0, currentXP);
        this.level = calculateLevelFromXP(this.currentXP);
    }
    
    /**
     * Calculates XP gained from a commit based on various factors.
     * XP range: 6-10 based on commit quality and size.
     */
    public int calculateXPFromCommit(Commit commit) {
        if (commit == null || commit.getMessage() == null) {
            return 0;
        }
        
        int baseXP = 6; // Minimum XP for any commit
        int bonusXP = 0; // Up to 4 bonus XP (total max: 10)
        
        String message = commit.getMessage().toLowerCase().trim();
        
        // Bonus for meaningful commit messages (longer than 10 characters)
        if (message.length() > 10) {
            bonusXP += 1;
        }
        
        // Bonus for conventional commit prefixes (high quality commits)
        if (message.startsWith("feat:") || message.startsWith("feature:")) {
            bonusXP += 2; // New features get more XP
        } else if (message.startsWith("fix:") || message.startsWith("bugfix:")) {
            bonusXP += 1; // Bug fixes get moderate XP
        } else if (message.startsWith("docs:") || message.startsWith("doc:")) {
            bonusXP += 1; // Documentation gets some XP
        } else if (message.startsWith("refactor:") || message.startsWith("style:")) {
            bonusXP += 1; // Code improvements get moderate XP
        } else if (message.startsWith("test:") || message.startsWith("tests:")) {
            bonusXP += 1; // Tests are valuable
        }
        
        // Bonus for descriptive messages (contains common development keywords)
        if (message.contains("implement") || message.contains("add") || 
            message.contains("create") || message.contains("update")) {
            bonusXP += 1;
        }
        
        // Ensure XP stays within 6-10 range
        int totalXP = baseXP + bonusXP;
        return Math.min(totalXP, 10); // Cap at 10 XP maximum
    }
    
    /**
     * Adds XP and updates level accordingly.
     */
    public void addXP(int xp) {
        if (xp > 0) {
            currentXP += xp;
            level = calculateLevelFromXP(currentXP);
        }
    }
    
    /**
     * Recalculates XP from a list of commits.
     * This replaces the current XP with the calculated value from actual commits.
     * 
     * @param commits List of commits to calculate XP from
     * @return The total XP calculated from commits
     */
    public int recalculateFromCommits(List<Commit> commits) {
        if (commits == null || commits.isEmpty()) {
            currentXP = 0;
            level = 0;
            return 0;
        }
        
        int totalXP = 0;
        for (Commit commit : commits) {
            totalXP += calculateXPFromCommit(commit);
        }
        
        currentXP = totalXP;
        level = calculateLevelFromXP(currentXP);
        
        System.out.println("📊 XP recalculated from " + commits.size() + " commits: " + totalXP + " XP");
        return totalXP;
    }
    
    /**
     * Calculates the current level based on XP thresholds.
     */
    private int calculateLevelFromXP(int xp) {
        for (int i = EVOLUTION_XP_THRESHOLDS.length - 1; i >= 0; i--) {
            if (xp >= EVOLUTION_XP_THRESHOLDS[i]) {
                return i;
            }
        }
        return 0;
    }
    
    /**
     * Gets the evolution stage based on current XP.
     */
    public EvolutionStage getEvolutionStage() {
        return EvolutionStage.values()[Math.min(level, EvolutionStage.values().length - 1)];
    }
    
    /**
     * Checks if the Pokemon can evolve to the next stage based on XP.
     */
    public boolean canEvolveToStage(EvolutionStage targetStage) {
        return currentXP >= EVOLUTION_XP_THRESHOLDS[targetStage.getLevel()];
    }
    
    /**
     * Checks if the Pokemon can evolve to the next stage based on current XP.
     * This method is used for XP-based evolution eligibility checks.
     * 
     * @param currentStage the current evolution stage of the Pokemon
     * @return true if XP meets or exceeds the threshold for the next stage
     */
    public boolean canEvolveViaXP(EvolutionStage currentStage) {
        int nextStageIndex = currentStage.getLevel() + 1;
        if (nextStageIndex >= EVOLUTION_XP_THRESHOLDS.length) {
            return false; // Already at max stage
        }
        return currentXP >= EVOLUTION_XP_THRESHOLDS[nextStageIndex];
    }
    
    /**
     * Gets XP required for the next evolution stage.
     */
    public int getXPForNextEvolution() {
        if (level >= EVOLUTION_XP_THRESHOLDS.length - 1) {
            return 0; // Already at max level
        }
        return EVOLUTION_XP_THRESHOLDS[level + 1] - currentXP;
    }
    
    /**
     * Gets XP progress towards next evolution as a percentage (0.0 to 1.0).
     */
    public double getEvolutionProgress() {
        if (level >= EVOLUTION_XP_THRESHOLDS.length - 1) {
            return 1.0; // Already at max level
        }
        
        int currentThreshold = EVOLUTION_XP_THRESHOLDS[level];
        int nextThreshold = EVOLUTION_XP_THRESHOLDS[level + 1];
        int progressXP = currentXP - currentThreshold;
        int requiredXP = nextThreshold - currentThreshold;
        
        return (double) progressXP / requiredXP;
    }
    
    // Getters and setters
    public int getCurrentXP() {
        return currentXP;
    }
    
    public void setCurrentXP(int currentXP) {
        this.currentXP = Math.max(0, currentXP);
        this.level = calculateLevelFromXP(this.currentXP);
    }
    
    public int getLevel() {
        return level;
    }
    
    public static int[] getEvolutionXPThresholds() {
        return EVOLUTION_XP_THRESHOLDS.clone();
    }
    
    public static int getXPThresholdForStage(EvolutionStage stage) {
        if (stage.getLevel() < EVOLUTION_XP_THRESHOLDS.length) {
            return EVOLUTION_XP_THRESHOLDS[stage.getLevel()];
        }
        return EVOLUTION_XP_THRESHOLDS[EVOLUTION_XP_THRESHOLDS.length - 1];
    }
    
    /**
     * FOR TESTING ONLY: Resets XP and level back to 0 for testing purposes.
     * TODO: REMOVE THIS METHOD BEFORE PRODUCTION - See TODO.md
     */
    public void resetForTesting() {
        System.out.println("🧪 TESTING: Resetting XP system - XP: " + currentXP + " -> 0, Level: " + level + " -> 0");
        this.currentXP = 0;
        this.level = 0;
    }
    
    @Override
    public String toString() {
        return "XPSystem{" +
               "currentXP=" + currentXP +
               ", level=" + level +
               ", evolutionStage=" + getEvolutionStage() +
               ", progressToNext=" + String.format("%.1f%%", getEvolutionProgress() * 100) +
               '}';
    }
}