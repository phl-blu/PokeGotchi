package com.tamagotchi.committracker.pokemon;

/**
 * Represents the emotional/health state of the Pokemon based on commit activity.
 */
public enum PokemonState {
    THRIVING,    // Regular commits, high activity
    HAPPY,       // Recent commits, good activity
    CONTENT,     // Moderate activity
    CONCERNED,   // Declining activity
    SAD,         // No recent commits
    NEGLECTED,   // Extended period without commits
    EVOLVING     // Currently undergoing evolution animation
}