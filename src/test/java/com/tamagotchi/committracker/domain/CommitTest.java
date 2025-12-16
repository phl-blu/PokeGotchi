package com.tamagotchi.committracker.domain;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Commit domain model.
 */
class CommitTest {

    @Test
    void testCommitClassExists() {
        // Verify the Commit class can be instantiated
        assertDoesNotThrow(() -> {
            Commit commit = new Commit();
            assertNotNull(commit);
        });
    }
    
    @Test
    void testCommitConstructorAndGetters() {
        LocalDateTime timestamp = LocalDateTime.now();
        Commit commit = new Commit("abc123", "feat: add new feature", "developer", 
                                   timestamp, "test-repo", "/path/to/repo");
        
        assertEquals("abc123", commit.getHash());
        assertEquals("feat: add new feature", commit.getMessage());
        assertEquals("developer", commit.getAuthor());
        assertEquals(timestamp, commit.getTimestamp());
        assertEquals("test-repo", commit.getRepositoryName());
        assertEquals("/path/to/repo", commit.getRepositoryPath());
    }
    
    @Test
    void testCommitEquality() {
        LocalDateTime timestamp = LocalDateTime.now();
        Commit commit1 = new Commit("abc123", "message", "author", timestamp, "repo", "/path");
        Commit commit2 = new Commit("abc123", "different message", "different author", 
                                    timestamp.plusHours(1), "different repo", "/path");
        
        // Commits are equal if they have same hash and repository path
        assertEquals(commit1, commit2);
        assertEquals(commit1.hashCode(), commit2.hashCode());
    }
}