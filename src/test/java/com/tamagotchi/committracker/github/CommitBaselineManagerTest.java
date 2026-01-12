package com.tamagotchi.committracker.github;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CommitBaselineManager.
 * 
 * Requirements: 4.2, 4.3
 */
class CommitBaselineManagerTest {
    
    @TempDir
    Path tempDir;
    
    private CommitBaselineManager manager;
    
    @BeforeEach
    void setUp() {
        manager = new CommitBaselineManager(tempDir);
    }
    
    @Test
    void testSetBaseline_storesBaseline() {
        // Arrange
        Instant baseline = Instant.now();
        
        // Act
        manager.setBaseline(baseline);
        
        // Assert
        assertTrue(manager.hasBaseline());
        assertEquals(baseline, manager.getBaseline().orElse(null));
    }
    
    @Test
    void testSetBaseline_throwsOnNull() {
        assertThrows(IllegalArgumentException.class, () -> manager.setBaseline(null));
    }
    
    @Test
    void testHasBaseline_returnsFalseInitially() {
        assertFalse(manager.hasBaseline());
    }
    
    @Test
    void testGetBaseline_returnsEmptyInitially() {
        assertTrue(manager.getBaseline().isEmpty());
    }
    
    @Test
    void testClearBaseline_removesBaseline() {
        // Arrange
        manager.setBaseline(Instant.now());
        assertTrue(manager.hasBaseline());
        
        // Act
        manager.clearBaseline();
        
        // Assert
        assertFalse(manager.hasBaseline());
        assertTrue(manager.getBaseline().isEmpty());
    }
    
    @Test
    void testIsAfterBaseline_returnsTrueWhenNoBaseline() {
        // No baseline set
        assertTrue(manager.isAfterBaseline(Instant.now()));
    }
    
    @Test
    void testIsAfterBaseline_returnsTrueForCommitAfterBaseline() {
        // Arrange
        Instant baseline = Instant.now().minusSeconds(3600);
        manager.setBaseline(baseline);
        Instant commitTime = Instant.now();
        
        // Act & Assert
        assertTrue(manager.isAfterBaseline(commitTime));
    }
    
    @Test
    void testIsAfterBaseline_returnsFalseForCommitBeforeBaseline() {
        // Arrange
        Instant baseline = Instant.now();
        manager.setBaseline(baseline);
        Instant commitTime = Instant.now().minusSeconds(3600);
        
        // Act & Assert
        assertFalse(manager.isAfterBaseline(commitTime));
    }
    
    @Test
    void testIsAfterBaseline_returnsTrueForCommitAtBaseline() {
        // Arrange
        Instant baseline = Instant.now();
        manager.setBaseline(baseline);
        
        // Act & Assert - commit at exact baseline time should be included
        assertTrue(manager.isAfterBaseline(baseline));
    }
    
    @Test
    void testIsAfterBaseline_returnsTrueForNullCommitTime() {
        // Arrange
        manager.setBaseline(Instant.now());
        
        // Act & Assert - null commit time should be accepted
        assertTrue(manager.isAfterBaseline(null));
    }
    
    @Test
    void testPersistence_baselinePersistedAcrossInstances() {
        // Arrange
        Instant baseline = Instant.now().minusSeconds(3600);
        manager.setBaseline(baseline);
        
        // Act - create new instance with same directory
        CommitBaselineManager newManager = new CommitBaselineManager(tempDir);
        
        // Assert
        assertTrue(newManager.hasBaseline());
        assertEquals(baseline, newManager.getBaseline().orElse(null));
    }
    
    @Test
    void testPersistence_clearBaselineRemovesPersistedData() {
        // Arrange
        manager.setBaseline(Instant.now());
        manager.clearBaseline();
        
        // Act - create new instance with same directory
        CommitBaselineManager newManager = new CommitBaselineManager(tempDir);
        
        // Assert
        assertFalse(newManager.hasBaseline());
    }
}
