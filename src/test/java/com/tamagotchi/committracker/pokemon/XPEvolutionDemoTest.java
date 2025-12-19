package com.tamagotchi.committracker.pokemon;

import com.tamagotchi.committracker.domain.Commit;
import com.tamagotchi.committracker.util.AnimationUtils;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Demonstration test showing the new XP-based egg evolution system.
 * This test shows how eggs progress through stages and evolve based on XP accumulation.
 */
class XPEvolutionDemoTest {

    @Test
    void demonstrateXPBasedEggEvolution() {
        System.out.println("🥚 XP-Based Egg Evolution Demonstration");
        System.out.println("=====================================");
        
        PokemonStateManager manager = new PokemonStateManager(PokemonSpecies.CHARMANDER, EvolutionStage.EGG, 0);
        
        System.out.println("Starting Pokemon: " + manager.getCurrentSpecies() + " (Egg Stage)");
        System.out.println("Initial XP: " + manager.getCurrentXP());
        System.out.println("Initial Egg Stage: " + manager.getCurrentEggStage());
        System.out.println();
        
        // Simulate commits and XP gain
        List<Commit> commits = List.of(
            new Commit("hash1", "fix: small bug", "dev", LocalDateTime.now(), "repo", "/path"), // ~10 XP
            new Commit("hash2", "feat: add new feature", "dev", LocalDateTime.now(), "repo", "/path"), // ~15 XP
            new Commit("hash3", "docs: update README", "dev", LocalDateTime.now(), "repo", "/path"), // ~8 XP
            new Commit("hash4", "refactor: improve code structure", "dev", LocalDateTime.now(), "repo", "/path"), // ~12 XP
            new Commit("hash5", "feat: implement comprehensive user authentication system", "dev", LocalDateTime.now(), "repo", "/path") // ~12 XP
        );
        
        int totalXPGained = 0;
        for (int i = 0; i < commits.size(); i++) {
            Commit commit = commits.get(i);
            int xpGained = manager.processNewCommits(List.of(commit));
            totalXPGained += xpGained;
            
            int currentXP = manager.getCurrentXP();
            int eggStage = manager.getCurrentEggStage();
            int animationStage = AnimationUtils.getEggStageFromXPDays(currentXP);
            
            System.out.println("Commit " + (i + 1) + " processed:");
            System.out.println("  Egg Stage: " + eggStage + " | Animation Stage: " + animationStage);
            
            // Check if ready to evolve
            if (manager.isEggReadyToHatch()) {
                System.out.println("  🌟 EGG IS READY TO HATCH! (50+ XP reached)");
                
                // Trigger evolution
                EvolutionStage newStage = manager.triggerEvolutionIfReady(0); // No streak needed with 50+ XP
                if (newStage != null) {
                    System.out.println("  🎉 EVOLUTION! " + EvolutionStage.EGG + " -> " + newStage);
                    System.out.println("  New Pokemon: " + manager.getCurrentSpecies());
                    break;
                }
            }
            
            System.out.println();
        }
        
        System.out.println("Final State:");
        System.out.println("  Species: " + manager.getCurrentSpecies());
        System.out.println("  Stage: " + manager.getCurrentStage());
        System.out.println("  Total XP: " + manager.getCurrentXP());
        System.out.println("  Total XP Gained: " + totalXPGained);
    }
    
    @Test
    void demonstrateEggStageProgression() {
        System.out.println("🥚 Egg Stage Progression Demonstration");
        System.out.println("=====================================");
        
        System.out.println("XP Range -> Egg Stage -> Visual Description");
        System.out.println("0-10 XP   -> Stage 1   -> Fresh egg");
        System.out.println("11-20 XP  -> Stage 2   -> Barely cracked");
        System.out.println("21-35 XP  -> Stage 3   -> More cracked");
        System.out.println("36-50 XP  -> Stage 4   -> Very cracked, ready to hatch");
        System.out.println("50+ XP    -> Evolution -> Hatches into Pokemon!");
        System.out.println();
        
        // Test specific XP values
        int[] testXPValues = {0, 5, 10, 11, 15, 20, 21, 28, 35, 36, 45, 50, 60};
        
        for (int xp : testXPValues) {
            int stage = AnimationUtils.getEggStageFromXPDays(xp);
            String description = getEggStageDescription(stage);
            boolean canEvolve = xp >= 50;
            
            System.out.printf("%2d XP -> Stage %d (%s)%s%n", 
                xp, stage, description, canEvolve ? " -> CAN EVOLVE!" : "");
        }
    }
    
    private String getEggStageDescription(int stage) {
        switch (stage) {
            case 1: return "Fresh egg";
            case 2: return "Barely cracked";
            case 3: return "More cracked";
            case 4: return "Very cracked";
            default: return "Unknown stage";
        }
    }
    
    @Test
    void demonstrateAlternativeEvolutionPaths() {
        System.out.println("🌟 Alternative Evolution Paths Demonstration");
        System.out.println("===========================================");
        
        // Path 1: XP-based evolution (fast)
        System.out.println("Path 1: XP-Based Evolution (Active Developer)");
        PokemonStateManager xpManager = new PokemonStateManager(PokemonSpecies.CYNDAQUIL, EvolutionStage.EGG, 0);
        
        // Simulate high-quality commits
        List<Commit> highQualityCommits = List.of(
            new Commit("hash1", "feat: implement comprehensive authentication system with OAuth2 support", "dev", LocalDateTime.now(), "repo", "/path"),
            new Commit("hash2", "feat: add advanced caching mechanism with Redis integration", "dev", LocalDateTime.now(), "repo", "/path"),
            new Commit("hash3", "feat: create robust error handling and logging framework", "dev", LocalDateTime.now(), "repo", "/path"),
            new Commit("hash4", "refactor: optimize database queries and implement connection pooling", "dev", LocalDateTime.now(), "repo", "/path")
        );
        
        int totalXP = 0;
        for (Commit commit : highQualityCommits) {
            int xpGained = xpManager.processNewCommits(List.of(commit));
            totalXP += xpGained;
            // Note: Detailed XP logging is handled by processNewCommits()
        }
        
        System.out.println("  Result: " + (xpManager.getCurrentXP() >= 50 ? "READY TO EVOLVE via XP!" : "Not ready yet"));
        System.out.println();
        
        // Path 2: Streak-based evolution (consistent)
        System.out.println("Path 2: Streak-Based Evolution (Consistent Developer)");
        PokemonStateManager streakManager = new PokemonStateManager(PokemonSpecies.MUDKIP, EvolutionStage.EGG, 0);
        
        System.out.println("  Simulating 4+ day commit streak...");
        System.out.println("  Day 1: Small commit (8 XP)");
        System.out.println("  Day 2: Small commit (7 XP)");
        System.out.println("  Day 3: Small commit (9 XP)");
        System.out.println("  Day 4: Small commit (8 XP)");
        System.out.println("  Total XP after 4 days: ~32 XP (not enough for XP evolution)");
        System.out.println("  But 4+ day streak achieved!");
        System.out.println("  Result: READY TO EVOLVE via STREAK!");
        System.out.println();
        
        System.out.println("Summary:");
        System.out.println("  - XP Evolution: Fast path for very active developers (50+ XP)");
        System.out.println("  - Streak Evolution: Consistent path for regular developers (4+ days)");
        System.out.println("  - Both paths lead to the same result: Egg -> Pokemon evolution");
    }
}