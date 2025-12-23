package com.tamagotchi.committracker.pokemon;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for XP evolution rescaling feature.
 * 
 * These tests validate the correctness properties defined in the design document
 * for the XP-based evolution system.
 */
class XPEvolutionProperties {

    // XP thresholds as defined in requirements
    private static final int EGG_TO_BASIC_XP = 60;
    private static final int BASIC_TO_STAGE1_XP = 500;
    private static final int STAGE1_TO_STAGE2_XP = 1200;

    // Streak thresholds as defined in requirements
    private static final int EGG_TO_BASIC_STREAK = 4;
    private static final int BASIC_TO_STAGE1_STREAK = 11;
    private static final int STAGE1_TO_STAGE2_STREAK = 22;

    /**
     * **Feature: xp-evolution-rescaling, Property 1: XP threshold evolution eligibility**
     * **Validates: Requirements 1.1, 1.2, 1.3**
     * 
     * For any evolution stage and XP value, the system allows XP-based evolution
     * if and only if XP is greater than or equal to the threshold for the next stage
     * (60 for EGG, 500 for BASIC, 1200 for STAGE_1).
     */
    @Property(tries = 100)
    void xpThresholdEvolutionEligibility(
            @ForAll @IntRange(min = 0, max = 2000) int xpValue,
            @ForAll("evolvableStages") EvolutionStage currentStage) {
        
        // Create XPSystem with the given XP value
        XPSystem xpSystem = new XPSystem(xpValue);
        
        // Determine expected evolution eligibility based on stage and XP
        boolean expectedCanEvolve = switch (currentStage) {
            case EGG -> xpValue >= EGG_TO_BASIC_XP;
            case BASIC -> xpValue >= BASIC_TO_STAGE1_XP;
            case STAGE_1 -> xpValue >= STAGE1_TO_STAGE2_XP;
            case STAGE_2 -> false; // Cannot evolve from final stage
        };
        
        // Verify the XPSystem correctly determines evolution eligibility
        boolean actualCanEvolve = xpSystem.canEvolveViaXP(currentStage);
        
        assertEquals(expectedCanEvolve, actualCanEvolve,
                String.format("For stage %s with %d XP: expected canEvolve=%b but got %b",
                        currentStage, xpValue, expectedCanEvolve, actualCanEvolve));
    }

    /**
     * Provides evolution stages that can potentially evolve (excludes STAGE_2).
     */
    @Provide
    Arbitrary<EvolutionStage> evolvableStages() {
        return Arbitraries.of(EvolutionStage.EGG, EvolutionStage.BASIC, EvolutionStage.STAGE_1);
    }

    /**
     * Property test for STAGE_2 - should never be able to evolve via XP.
     * This is a separate property to ensure the final stage is handled correctly.
     */
    @Property(tries = 100)
    void stage2CannotEvolveViaXP(@ForAll @IntRange(min = 0, max = 5000) int xpValue) {
        XPSystem xpSystem = new XPSystem(xpValue);
        
        // STAGE_2 should never be able to evolve, regardless of XP
        assertFalse(xpSystem.canEvolveViaXP(EvolutionStage.STAGE_2),
                String.format("STAGE_2 should not be able to evolve even with %d XP", xpValue));
    }

    /**
     * **Feature: xp-evolution-rescaling, Property 2: Streak threshold evolution eligibility**
     * **Validates: Requirements 2.1, 2.2, 2.3**
     * 
     * For any evolution stage and streak value, the system allows streak-based evolution
     * if and only if streak is greater than or equal to the threshold for the next stage
     * (4 days for EGG, 11 days for BASIC, 22 days for STAGE_1).
     */
    @Property(tries = 100)
    void streakThresholdEvolutionEligibility(
            @ForAll @IntRange(min = 0, max = 30) int streakDays,
            @ForAll("evolvableStages") EvolutionStage currentStage) {
        
        PokemonStateManager stateManager = new PokemonStateManager();
        
        // Use 0 XP to isolate streak-based evolution testing
        int xp = 0;
        
        // Determine expected evolution eligibility based on stage and streak
        boolean expectedCanEvolve = switch (currentStage) {
            case EGG -> streakDays >= EGG_TO_BASIC_STREAK;
            case BASIC -> streakDays >= BASIC_TO_STAGE1_STREAK;
            case STAGE_1 -> streakDays >= STAGE1_TO_STAGE2_STREAK;
            case STAGE_2 -> false; // Cannot evolve from final stage
        };
        
        // Verify the PokemonStateManager correctly determines evolution eligibility
        boolean actualCanEvolve = stateManager.checkEvolutionCriteria(xp, streakDays, currentStage);
        
        assertEquals(expectedCanEvolve, actualCanEvolve,
                String.format("For stage %s with %d streak days (0 XP): expected canEvolve=%b but got %b",
                        currentStage, streakDays, expectedCanEvolve, actualCanEvolve));
    }

    /**
     * **Feature: xp-evolution-rescaling, Property 3: OR logic for evolution criteria**
     * **Validates: Requirements 3.1, 3.2, 3.3**
     * 
     * For any evolution stage, XP value, and streak value, evolution is allowed
     * if and only if either the XP threshold OR the streak threshold is met for that stage.
     */
    @Property(tries = 100)
    void orLogicForEvolutionCriteria(
            @ForAll @IntRange(min = 0, max = 2000) int xpValue,
            @ForAll @IntRange(min = 0, max = 30) int streakDays,
            @ForAll("evolvableStages") EvolutionStage currentStage) {
        
        PokemonStateManager stateManager = new PokemonStateManager();
        
        // Determine expected evolution eligibility using OR logic
        boolean xpMeetsThreshold = switch (currentStage) {
            case EGG -> xpValue >= EGG_TO_BASIC_XP;
            case BASIC -> xpValue >= BASIC_TO_STAGE1_XP;
            case STAGE_1 -> xpValue >= STAGE1_TO_STAGE2_XP;
            case STAGE_2 -> false;
        };
        
        boolean streakMeetsThreshold = switch (currentStage) {
            case EGG -> streakDays >= EGG_TO_BASIC_STREAK;
            case BASIC -> streakDays >= BASIC_TO_STAGE1_STREAK;
            case STAGE_1 -> streakDays >= STAGE1_TO_STAGE2_STREAK;
            case STAGE_2 -> false;
        };
        
        // OR logic: evolution allowed if EITHER threshold is met
        boolean expectedCanEvolve = xpMeetsThreshold || streakMeetsThreshold;
        
        // Verify the PokemonStateManager correctly applies OR logic
        boolean actualCanEvolve = stateManager.checkEvolutionCriteria(xpValue, streakDays, currentStage);
        
        assertEquals(expectedCanEvolve, actualCanEvolve,
                String.format("For stage %s with %d XP and %d streak days: expected canEvolve=%b (xp=%b OR streak=%b) but got %b",
                        currentStage, xpValue, streakDays, expectedCanEvolve, xpMeetsThreshold, streakMeetsThreshold, actualCanEvolve));
    }
}
