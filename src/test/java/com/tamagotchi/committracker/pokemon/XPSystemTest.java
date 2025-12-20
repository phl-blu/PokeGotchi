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
        assertEquals(1, xpSystem.getLevel()); // 500 XP is level 1 (BASIC stage)
        assertEquals(EvolutionStage.BASIC, xpSystem.getEvolutionStage());
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
        
        xpSystem.addXP(150);
        assertEquals(150, xpSystem.getCurrentXP());
        assertEquals(0, xpSystem.getLevel()); // Still at EGG level (need 200 for BASIC)
        
        xpSystem.addXP(100);
        assertEquals(250, xpSystem.getCurrentXP());
        assertEquals(1, xpSystem.getLevel()); // Should be at BASIC level (250 >= 200, but < 800)
    }
    
    @Test
    void testEvolutionThresholds() {
        XPSystem xpSystem = new XPSystem();
        
        // Test EGG stage
        assertEquals(EvolutionStage.EGG, xpSystem.getEvolutionStage());
        assertFalse(xpSystem.canEvolveToStage(EvolutionStage.BASIC));
        
        // Test BASIC stage
        xpSystem.setCurrentXP(200);
        assertEquals(EvolutionStage.BASIC, xpSystem.getEvolutionStage());
        assertTrue(xpSystem.canEvolveToStage(EvolutionStage.BASIC));
        
        // Test STAGE_1
        xpSystem.setCurrentXP(800);
        assertEquals(EvolutionStage.STAGE_1, xpSystem.getEvolutionStage());
        assertTrue(xpSystem.canEvolveToStage(EvolutionStage.STAGE_1));
        
        // Test STAGE_2
        xpSystem.setCurrentXP(2000);
        assertEquals(EvolutionStage.STAGE_2, xpSystem.getEvolutionStage());
        assertTrue(xpSystem.canEvolveToStage(EvolutionStage.STAGE_2));
    }
    
    @Test
    void testEvolutionProgress() {
        XPSystem xpSystem = new XPSystem();
        
        // At 0 XP, should be 0% progress to BASIC (200 XP)
        assertEquals(0.0, xpSystem.getEvolutionProgress(), 0.01);
        
        // At 100 XP, should be 50% progress to BASIC
        xpSystem.setCurrentXP(100);
        assertEquals(0.5, xpSystem.getEvolutionProgress(), 0.01);
        
        // At max level, should be 100%
        xpSystem.setCurrentXP(2000);
        assertEquals(1.0, xpSystem.getEvolutionProgress(), 0.01);
    }
}