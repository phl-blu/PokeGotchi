package com.tamagotchi.committracker.git;

import com.tamagotchi.committracker.domain.Commit;
import com.tamagotchi.committracker.domain.CommitHistory;
import com.tamagotchi.committracker.domain.Repository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CommitService functionality.
 * Uses a TestableRepositoryScanner to avoid real file system scanning.
 */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class CommitServiceTest {
    
    private CommitService commitService;
    private TestableRepositoryScanner testScanner;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        // Use a testable scanner that doesn't scan the real file system
        testScanner = new TestableRepositoryScanner();
        commitService = new CommitService(testScanner);
    }
    
    @AfterEach
    void tearDown() {
        // Ensure monitoring is stopped and resources are cleaned up
        if (commitService != null && commitService.isMonitoring()) {
            commitService.stopMonitoring();
        }
    }
    
    @Test
    void testCommitServiceInitialization() {
        assertNotNull(commitService);
        assertFalse(commitService.isMonitoring());
        assertNotNull(commitService.getCommitHistory());
        assertNotNull(commitService.getRepositoryScanner());
    }
    
    @Test
    void testProcessNewCommits() {
        // Arrange
        List<Commit> newCommits = createTestCommits();
        
        // Act
        commitService.processNewCommits(newCommits);
        
        // Assert
        CommitHistory history = commitService.getCommitHistory();
        assertNotNull(history);
        assertEquals(2, history.getRecentCommits().size());
        // The most recent commit is 15 minutes ago, so check it's within the last hour
        assertNotNull(history.getLastCommitTime());
        assertTrue(history.getLastCommitTime().isAfter(LocalDateTime.now().minusHours(1)));
    }
    
    @Test
    void testProcessEmptyCommitList() {
        // Arrange
        List<Commit> emptyCommits = new ArrayList<>();
        
        // Act
        commitService.processNewCommits(emptyCommits);
        
        // Assert
        CommitHistory history = commitService.getCommitHistory();
        assertNotNull(history);
        assertEquals(0, history.getRecentCommits().size());
    }
    
    @Test
    void testCommitListener() {
        // Arrange
        TestCommitListener listener = new TestCommitListener();
        commitService.addCommitListener(listener);
        List<Commit> newCommits = createTestCommits();
        
        // Act
        commitService.processNewCommits(newCommits);
        
        // Assert
        assertTrue(listener.wasNotified);
        assertEquals(2, listener.receivedCommits.size());
    }
    
    @Test
    void testRemoveCommitListener() {
        // Arrange
        TestCommitListener listener = new TestCommitListener();
        commitService.addCommitListener(listener);
        commitService.removeCommitListener(listener);
        List<Commit> newCommits = createTestCommits();
        
        // Act
        commitService.processNewCommits(newCommits);
        
        // Assert - listener should NOT be notified after removal
        assertFalse(listener.wasNotified);
        assertEquals(0, listener.receivedCommits.size());
    }
    
    @Test
    void testStartAndStopMonitoring() {
        // Use a scanner that returns empty results immediately (no file system access)
        testScanner.setReturnEmptyResults(true);
        
        // Act - start monitoring
        commitService.startMonitoring();
        
        // Assert - should be monitoring
        assertTrue(commitService.isMonitoring());
        
        // Act - stop monitoring
        commitService.stopMonitoring();
        
        // Assert - should no longer be monitoring
        assertFalse(commitService.isMonitoring());
    }
    
    @Test
    void testStartMonitoringTwiceDoesNothing() {
        testScanner.setReturnEmptyResults(true);
        
        // Start monitoring twice
        commitService.startMonitoring();
        commitService.startMonitoring(); // Should log warning but not fail
        
        assertTrue(commitService.isMonitoring());
        
        // Cleanup
        commitService.stopMonitoring();
    }
    
    @Test
    void testStopMonitoringWhenNotStarted() {
        // Should not throw exception
        assertDoesNotThrow(() -> commitService.stopMonitoring());
        assertFalse(commitService.isMonitoring());
    }
    
    @Test
    void testCommitsSortedByTimestamp() {
        // Arrange - commits in reverse order
        List<Commit> commits = new ArrayList<>();
        commits.add(new Commit("newer", "newer commit", "Author", 
            LocalDateTime.now().minusMinutes(5), "repo", tempDir.toString()));
        commits.add(new Commit("older", "older commit", "Author", 
            LocalDateTime.now().minusMinutes(30), "repo", tempDir.toString()));
        
        // Act
        commitService.processNewCommits(commits);
        
        // Assert - commits should be sorted oldest first
        List<Commit> recentCommits = commitService.getCommitHistory().getRecentCommits();
        assertEquals(2, recentCommits.size());
        assertTrue(recentCommits.get(0).getTimestamp().isBefore(recentCommits.get(1).getTimestamp()));
    }
    
    private List<Commit> createTestCommits() {
        List<Commit> commits = new ArrayList<>();
        
        commits.add(new Commit(
            "abc123",
            "feat: add new feature",
            "Test Author",
            LocalDateTime.now().minusMinutes(30),
            "test-repo",
            tempDir.toString()
        ));
        
        commits.add(new Commit(
            "def456",
            "fix: resolve bug",
            "Test Author",
            LocalDateTime.now().minusMinutes(15),
            "test-repo",
            tempDir.toString()
        ));
        
        return commits;
    }
    
    /**
     * A testable version of RepositoryScanner that doesn't scan the real file system.
     */
    private static class TestableRepositoryScanner extends RepositoryScanner {
        private boolean returnEmptyResults = false;
        
        public TestableRepositoryScanner() {
            super();
        }
        
        public void setReturnEmptyResults(boolean returnEmpty) {
            this.returnEmptyResults = returnEmpty;
        }
        
        @Override
        public List<Repository> discoverRepositories() {
            if (returnEmptyResults) {
                return Collections.emptyList();
            }
            return super.discoverRepositories();
        }
    }
    
    private static class TestCommitListener implements CommitService.CommitListener {
        boolean wasNotified = false;
        List<Commit> receivedCommits = new ArrayList<>();
        
        @Override
        public void onNewCommits(List<Commit> commits) {
            wasNotified = true;
            receivedCommits.addAll(commits);
        }
    }
}
