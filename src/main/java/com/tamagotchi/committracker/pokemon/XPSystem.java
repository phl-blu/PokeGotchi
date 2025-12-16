package com.tamagotchi.committracker.pokemon;

import com.tamagotchi.committracker.domain.Commit;

/**
 * Manages XP calculation and evolution thresholds for Pokemon.
 */
public class XPSystem {
    private int currentXP;
    private int level;
    private static final int[] EVOLUTION_XP_THRESHOLDS = {0, 200, 800, 2000}; // Egg, Basic, Stage1, Stage2
    
    // TODO: Implement XP calculation logic in task 2
    
    public int calculateXPFromCommit(Commit commit) {
        // Base XP + bonuses for commit size, message quality, etc.
        return 0; // Placeholder
    }
}