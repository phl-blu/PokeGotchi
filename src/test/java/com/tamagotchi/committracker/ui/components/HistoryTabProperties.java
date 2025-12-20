package com.tamagotchi.committracker.ui.components;

import com.tamagotchi.committracker.domain.Commit;
import com.tamagotchi.committracker.domain.CommitHistory;
import com.tamagotchi.committracker.pokemon.XPSystem;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * **Feature: tamagotchi-commit-tracker, Property 4: Commit Display Completeness**
 * **Validates: Requirements 5.1, 5.2, 5.3**
 * 
 * Property-based tests for commit display functionality.
 * Tests that all commits are displayed with complete information:
 * message, timestamp, repository, author, and XP gained.
 */
class HistoryTabProperties {
    
    /**
     * Property: For any commit with complete information, the formatted display
     * should contain all required fields: message, timestamp, repository, author, and XP.
     */
    @Property(tries = 100)
    @Label("Formatted commit display contains all required information")
    void formattedCommitContainsAllRequiredInfo(
        @ForAll("validCommits") Commit commit
    ) {
        // Act: Format the commit for display
        String formatted = HistoryTab.formatCommitForDisplay(commit);
        
        // Assert: All required information is present
        assertThat(HistoryTab.containsAllRequiredInfo(formatted, commit))
            .as("Formatted commit should contain all required information")
            .isTrue();
        
        // Additional assertions for specific fields
        if (commit.getMessage() != null && !commit.getMessage().isEmpty()) {
            assertThat(formatted)
                .as("Formatted commit should contain message")
                .contains("message:" + commit.getMessage());
        }
        
        if (commit.getTimestamp() != null) {
            assertThat(formatted)
                .as("Formatted commit should contain timestamp")
                .contains("timestamp:" + commit.getTimestamp().toString());
        }
        
        if (commit.getRepositoryName() != null && !commit.getRepositoryName().isEmpty()) {
            assertThat(formatted)
                .as("Formatted commit should contain repository name")
                .contains("repository:" + commit.getRepositoryName());
        }
        
        if (commit.getAuthor() != null && !commit.getAuthor().isEmpty()) {
            assertThat(formatted)
                .as("Formatted commit should contain author")
                .contains("author:" + commit.getAuthor());
        }
        
        // XP should always be present
        assertThat(formatted)
            .as("Formatted commit should contain XP")
            .contains("xp:");
    }
    
    /**
     * Property: For any commit, the XP displayed should match the XP calculated
     * by the XPSystem for that commit.
     */
    @Property(tries = 100)
    @Label("Displayed XP matches calculated XP from XPSystem")
    void displayedXPMatchesCalculatedXP(
        @ForAll("validCommits") Commit commit
    ) {
        // Arrange
        XPSystem xpSystem = new XPSystem();
        int expectedXP = xpSystem.calculateXPFromCommit(commit);
        
        // Act
        String formatted = HistoryTab.formatCommitForDisplay(commit);
        
        // Assert: XP in formatted string matches calculated XP
        assertThat(formatted)
            .as("Formatted commit should contain correct XP value")
            .contains("xp:" + expectedXP);
    }
    
    /**
     * Property: For any list of commits, all commits should be represented
     * in the formatted output with complete information.
     */
    @Property(tries = 50)
    @Label("All commits in history are displayed with complete information")
    void allCommitsInHistoryAreDisplayed(
        @ForAll("commitLists") List<Commit> commits
    ) {
        // Act: Format each commit
        List<String> formattedCommits = new ArrayList<>();
        for (Commit commit : commits) {
            formattedCommits.add(HistoryTab.formatCommitForDisplay(commit));
        }
        
        // Assert: Each commit has complete information
        for (int i = 0; i < commits.size(); i++) {
            Commit commit = commits.get(i);
            String formatted = formattedCommits.get(i);
            
            assertThat(HistoryTab.containsAllRequiredInfo(formatted, commit))
                .as("Commit at index " + i + " should have complete information")
                .isTrue();
        }
    }
    
    /**
     * Property: Commits with conventional commit prefixes should receive
     * appropriate XP bonuses (6-10 XP range).
     */
    @Property(tries = 100)
    @Label("XP is within valid range (6-10) for all commits")
    void xpIsWithinValidRange(
        @ForAll("validCommits") Commit commit
    ) {
        // Arrange
        XPSystem xpSystem = new XPSystem();
        
        // Act
        int xp = xpSystem.calculateXPFromCommit(commit);
        
        // Assert: XP should be between 6 and 10
        assertThat(xp)
            .as("XP should be within valid range")
            .isBetween(6, 10);
    }
    
    /**
     * Property: Null commits should result in empty formatted string.
     */
    @Property(tries = 10)
    @Label("Null commit returns empty formatted string")
    void nullCommitReturnsEmptyString() {
        // Act
        String formatted = HistoryTab.formatCommitForDisplay(null);
        
        // Assert
        assertThat(formatted)
            .as("Null commit should return empty string")
            .isEmpty();
    }
    
    /**
     * Property: Commits with null message should still be formatted
     * with other available information.
     */
    @Property(tries = 50)
    @Label("Commits with null message are still formatted")
    void commitsWithNullMessageAreFormatted(
        @ForAll("commitsWithNullMessage") Commit commit
    ) {
        // Act
        String formatted = HistoryTab.formatCommitForDisplay(commit);
        
        // Assert: Should still contain other fields
        assertThat(formatted)
            .as("Formatted commit should not be empty")
            .isNotEmpty();
        
        assertThat(formatted)
            .as("Should contain message field (even if empty)")
            .contains("message:");
        
        assertThat(formatted)
            .as("Should contain xp field")
            .contains("xp:");
    }
    
    // ==================== Arbitrary Providers ====================
    
    @Provide
    Arbitrary<Commit> validCommits() {
        Arbitrary<String> hashes = Arbitraries.strings()
            .withCharRange('a', 'f')
            .withCharRange('0', '9')
            .ofLength(40);
        
        Arbitrary<String> messages = Arbitraries.oneOf(
            // Conventional commit messages
            Arbitraries.of(
                "feat: add new feature",
                "fix: resolve bug in parser",
                "docs: update README",
                "refactor: improve code structure",
                "test: add unit tests",
                "style: format code"
            ),
            // Regular messages
            Arbitraries.strings()
                .alpha()
                .ofMinLength(5)
                .ofMaxLength(50)
        );
        
        Arbitrary<String> authors = Arbitraries.strings()
            .alpha()
            .ofMinLength(3)
            .ofMaxLength(20);
        
        Arbitrary<String> repoNames = Arbitraries.strings()
            .alpha()
            .ofMinLength(3)
            .ofMaxLength(15)
            .map(s -> s.toLowerCase() + "-repo");
        
        Arbitrary<LocalDateTime> timestamps = Arbitraries.longs()
            .between(0, 365 * 24 * 60) // Up to 1 year in minutes
            .map(minutes -> LocalDateTime.now().minusMinutes(minutes));
        
        return Combinators.combine(hashes, messages, authors, timestamps, repoNames)
            .as((hash, message, author, timestamp, repoName) -> {
                Commit commit = new Commit();
                commit.setHash(hash);
                commit.setMessage(message);
                commit.setAuthor(author);
                commit.setTimestamp(timestamp);
                commit.setRepositoryName(repoName);
                commit.setRepositoryPath("/path/to/" + repoName);
                return commit;
            });
    }
    
    @Provide
    Arbitrary<List<Commit>> commitLists() {
        return validCommits().list().ofMinSize(1).ofMaxSize(10);
    }
    
    @Provide
    Arbitrary<Commit> commitsWithNullMessage() {
        Arbitrary<String> hashes = Arbitraries.strings()
            .withCharRange('a', 'f')
            .withCharRange('0', '9')
            .ofLength(40);
        
        Arbitrary<String> authors = Arbitraries.strings()
            .alpha()
            .ofMinLength(3)
            .ofMaxLength(20);
        
        Arbitrary<String> repoNames = Arbitraries.strings()
            .alpha()
            .ofMinLength(3)
            .ofMaxLength(15)
            .map(s -> s.toLowerCase() + "-repo");
        
        Arbitrary<LocalDateTime> timestamps = Arbitraries.longs()
            .between(0, 365 * 24 * 60)
            .map(minutes -> LocalDateTime.now().minusMinutes(minutes));
        
        return Combinators.combine(hashes, authors, timestamps, repoNames)
            .as((hash, author, timestamp, repoName) -> {
                Commit commit = new Commit();
                commit.setHash(hash);
                commit.setMessage(null); // Null message
                commit.setAuthor(author);
                commit.setTimestamp(timestamp);
                commit.setRepositoryName(repoName);
                commit.setRepositoryPath("/path/to/" + repoName);
                return commit;
            });
    }
}
