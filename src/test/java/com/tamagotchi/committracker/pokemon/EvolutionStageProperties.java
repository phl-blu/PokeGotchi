package com.tamagotchi.committracker.pokemon;

import net.jqwik.api.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Property-based tests for EvolutionStage enum to verify setup.
 */
class EvolutionStageProperties {

    @Property
    void evolutionStageHasValidLevel(@ForAll EvolutionStage stage) {
        // Verify all evolution stages have non-negative levels
        assertTrue(stage.getLevel() >= 0, "Evolution stage level should be non-negative");
        assertTrue(stage.getLevel() <= 3, "Evolution stage level should not exceed 3");
    }
    
    @Property
    void evolutionStageOrderIsCorrect() {
        // Verify the evolution stages are in correct order
        assertTrue(EvolutionStage.EGG.getLevel() < EvolutionStage.BASIC.getLevel());
        assertTrue(EvolutionStage.BASIC.getLevel() < EvolutionStage.STAGE_1.getLevel());
        assertTrue(EvolutionStage.STAGE_1.getLevel() < EvolutionStage.STAGE_2.getLevel());
    }
}