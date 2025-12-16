package com.tamagotchi.committracker.pokemon;

/**
 * Represents the evolution stage of a Pokemon.
 */
public enum EvolutionStage {
    EGG(0),        // Starting as egg
    BASIC(1),      // Hatched form (4+ day streak)
    STAGE_1(2),    // First evolution (11+ day streak)
    STAGE_2(3);    // Final evolution (22+ day streak)
    
    private final int level;
    
    EvolutionStage(int level) {
        this.level = level;
    }
    
    public int getLevel() {
        return level;
    }
}