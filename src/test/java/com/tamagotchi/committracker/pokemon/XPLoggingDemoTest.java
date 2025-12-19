package com.tamagotchi.committracker.pokemon;

import com.tamagotchi.committracker.domain.Commit;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Demonstration test showing the XP logging format for debugging.
 */
class XPLoggingDemoTest {

    @Test
    void demonstrateXPLogging() {
        PokemonStateManager manager = new PokemonStateManager(PokemonSpecies.CHARMANDER, EvolutionStage.EGG, 0);
        
        List<Commit> commits = List.of(
            new Commit("hash1", "fix", "dev", LocalDateTime.now(), "repo", "/path"),
            new Commit("hash2", "feat: add new feature", "dev", LocalDateTime.now(), "repo", "/path"),
            new Commit("hash3", "feat: implement auth", "dev", LocalDateTime.now(), "repo", "/path")
        );
        
        manager.processNewCommits(commits);
    }
    
    @Test
    void demonstrateMultipleCommitsLogging() {
        System.out.println("Multiple Commits Logging:");
        System.out.println();
        
        PokemonStateManager manager = new PokemonStateManager(PokemonSpecies.MUDKIP, EvolutionStage.EGG, 0);
        
        List<Commit> multipleCommits = List.of(
            new Commit("hash1", "docs: update README", "dev", LocalDateTime.now(), "repo", "/path"),
            new Commit("hash2", "test: add unit tests", "dev", LocalDateTime.now(), "repo", "/path"),
            new Commit("hash3", "refactor: improve code structure", "dev", LocalDateTime.now(), "repo", "/path")
        );
        
        manager.processNewCommits(multipleCommits);
    }
}