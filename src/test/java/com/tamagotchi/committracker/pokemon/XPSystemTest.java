package com.tamagotchi.committracker.pokemon;

import com.tamagotchi.committracker.domain.Commit;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for XPSystem.
 */
class XPSystemTest {

    @Test
    void testXPSystemInitialization() {
        XPSystem xpSystem = new XPSystem();
        assertEquals(0, xpSystem.getCurrentXP());
        assertEquals(0, xpSystem.getLevel());
        assertEquals(EvolutionStage.EGG, xpSystem.getEvolutionStage());
    }
    
    @Test
    void testXPSystemWithInitialXP() {
        XPSystem xpSystem = new XPSystem(500);
        assertEquals(500, xpSystem.getCurrentXP());
        assertEquals(2, xpSystem.getLevel()); // 500 XP is level 2 (STAGE_1) with new thresholds
        assertEquals(EvolutionStage.STAGE_1, xpSystem.getEvolutionStage());
    }
    
    @Test
    void testCalculateXPFromCommit() {
        XPSystem xpSystem = new XPSystem();
        
        // Test basic commit (short message, no bonuses) - 6 base XP
        Commit basicCommit = new Commit("hash1", "fix", "author", 
                                        LocalDateTime.now(), "repo", "/path");
        assertEquals(6, xpSystem.calculateXPFromCommit(basicCommit));
        
        // Test longer basic commit (gets length bonus) - 6 base + 1 length = 7
        Commit longerCommit = new Commit("hash1", "basic commit", "author", 
                                        LocalDateTime.now(), "repo", "/path");
        assertEquals(7, xpSystem.calculateXPFromCommit(longerCommit));
        
        // Test feature commit - 6 base + 1 length + 2 feat + 1 add = 10 (capped)
        Commit featureCommit = new Commit("hash2", "feat: add new feature with detailed description", 
                                          "author", LocalDateTime.now(), "repo", "/path");
        assertEquals(10, xpSystem.calculateXPFromCommit(featureCommit));
        
        // Test null commit
        assertEquals(0, xpSystem.calculateXPFromCommit(null));
    }
    
    @Test
    void testAddXP() {
        XPSystem xpSystem = new XPSystem();
        
        xpSystem.addXP(50);
        assertEquals(50, xpSystem.getCurrentXP());
        assertEquals(0, xpSystem.getLevel()); // Still at EGG level (need 60 for BASIC)
        
        xpSystem.addXP(20);
        assertEquals(70, xpSystem.getCurrentXP());
        assertEquals(1, xpSystem.getLevel()); // Should be at BASIC level (70 >= 60, but < 500)
    }
    
    @Test
    void testEvolutionThresholds() {
        XPSystem xpSystem = new XPSystem();
        
        // Test EGG stage (0 XP)
        assertEquals(EvolutionStage.EGG, xpSystem.getEvolutionStage());
        assertFalse(xpSystem.canEvolveToStage(EvolutionStage.BASIC));
        
        // Test BASIC stage (60 XP threshold)
        xpSystem.setCurrentXP(60);
        assertEquals(EvolutionStage.BASIC, xpSystem.getEvolutionStage());
        assertTrue(xpSystem.canEvolveToStage(EvolutionStage.BASIC));
        
        // Test STAGE_1 (500 XP threshold)
        xpSystem.setCurrentXP(500);
        assertEquals(EvolutionStage.STAGE_1, xpSystem.getEvolutionStage());
        assertTrue(xpSystem.canEvolveToStage(EvolutionStage.STAGE_1));
        
        // Test STAGE_2 (1200 XP threshold)
        xpSystem.setCurrentXP(1200);
        assertEquals(EvolutionStage.STAGE_2, xpSystem.getEvolutionStage());
        assertTrue(xpSystem.canEvolveToStage(EvolutionStage.STAGE_2));
    }
    
    @Test
    void testEvolutionProgress() {
        XPSystem xpSystem = new XPSystem();
        
        // At 0 XP, should be 0% progress to BASIC (60 XP threshold)
        assertEquals(0.0, xpSystem.getEvolutionProgress(), 0.01);
        
        // At 30 XP, should be 50% progress to BASIC (30/60 = 0.5)
        xpSystem.setCurrentXP(30);
        assertEquals(0.5, xpSystem.getEvolutionProgress(), 0.01);
        
        // At max level (1200 XP), should be 100%
        xpSystem.setCurrentXP(1200);
        assertEquals(1.0, xpSystem.getEvolutionProgress(), 0.01);
    }
    
    @Test
    void testEvolutionBoundaryConditions() {
        XPSystem xpSystem = new XPSystem();
        
        // Test EGG -> BASIC boundary (59/60)
        xpSystem.setCurrentXP(59);
        assertEquals(EvolutionStage.EGG, xpSystem.getEvolutionStage());
        assertEquals(0, xpSystem.getLevel());
        assertFalse(xpSystem.canEvolveToStage(EvolutionStage.BASIC));
        
        xpSystem.setCurrentXP(60);
        assertEquals(EvolutionStage.BASIC, xpSystem.getEvolutionStage());
        assertEquals(1, xpSystem.getLevel());
        assertTrue(xpSystem.canEvolveToStage(EvolutionStage.BASIC));
        
        // Test BASIC -> STAGE_1 boundary (499/500)
        xpSystem.setCurrentXP(499);
        assertEquals(EvolutionStage.BASIC, xpSystem.getEvolutionStage());
        assertEquals(1, xpSystem.getLevel());
        assertFalse(xpSystem.canEvolveToStage(EvolutionStage.STAGE_1));
        
        xpSystem.setCurrentXP(500);
        assertEquals(EvolutionStage.STAGE_1, xpSystem.getEvolutionStage());
        assertEquals(2, xpSystem.getLevel());
        assertTrue(xpSystem.canEvolveToStage(EvolutionStage.STAGE_1));
        
        // Test STAGE_1 -> STAGE_2 boundary (1199/1200)
        xpSystem.setCurrentXP(1199);
        assertEquals(EvolutionStage.STAGE_1, xpSystem.getEvolutionStage());
        assertEquals(2, xpSystem.getLevel());
        assertFalse(xpSystem.canEvolveToStage(EvolutionStage.STAGE_2));
        
        xpSystem.setCurrentXP(1200);
        assertEquals(EvolutionStage.STAGE_2, xpSystem.getEvolutionStage());
        assertEquals(3, xpSystem.getLevel());
        assertTrue(xpSystem.canEvolveToStage(EvolutionStage.STAGE_2));
    }
}