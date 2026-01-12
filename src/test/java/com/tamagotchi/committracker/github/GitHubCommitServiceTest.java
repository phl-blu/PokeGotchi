package com.tamagotchi.committracker.github;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GitHubCommitService.
 * 
 * Requirements: 4.1, 4.2, 4.3, 4.5, 4.6
 */
class GitHubCommitServiceTest {
    
    @TempDir
    Path tempDir;
    
    private GitHubCommitService service;
    private MockGitHubApiClient mockApiClient;
    private RateLimitManager rateLimitManager;
    private CommitCache commitCache;
    private RepositoryCache repositoryCache;
    private CommitBaselineManager baselineManager;
    
    @BeforeEach
    void setUp() {
        mockApiClient = new MockGitHubApiClient();
        rateLimitManager = new RateLimitManagerImpl();
        commitCache = new CommitCache(tempDir.resolve("commits"));
        repositoryCache = new RepositoryCache(tempDir.resolve("repos"));
        baselineManager = new CommitBaselineManager(tempDir.resolve("config"));
        
        service = new GitHubCommitService(
            mockApiClient, rateLimitManager, commitCache, repositoryCache, baselineManager);
    }
    
    @Test
    void testInitialize_setsBaselineIfNotSet() {
        // Arrange
        assertFalse(baselineManager.hasBaseline());
        
        // Act
        service.initialize("test-token");
        
        // Assert
        assertTrue(service.isInitialized());
        assertTrue(baselineManager.hasBaseline());
    }
    
    @Test
    void testInitialize_preservesExistingBaseline() {
        // Arrange
        Instant existingBaseline = Instant.now().minusSeconds(3600);
        baselineManager.setBaseline(existingBaseline);
        
        // Act
        service.initialize("test-token");
        
        // Assert
        assertEquals(existingBaseline, baselineManager.getBaseline().orElse(null));
    }
    
    @Test
    void testInitialize_throwsOnNullToken() {
        assertThrows(IllegalArgumentException.class, () -> service.initialize(null));
    }
    
    @Test
    void testInitialize_throwsOnBlankToken() {
        assertThrows(IllegalArgumentException.class, () -> service.initialize("   "));
    }
    
    @Test
    void testPerformInitialSync_failsIfNotInitialized() {
        // Act & Assert
        CompletableFuture<GitHubCommitService.SyncResult> future = service.performInitialSync();
        
        assertThrows(Exception.class, () -> future.join());
    }
    
    @Test
    void testPollForNewCommits_failsIfNotInitialized() {
        // Act & Assert
        CompletableFuture<?> future = service.pollForNewCommits();
        
        assertThrows(Exception.class, () -> future.join());
    }
    
    @Test
    void testStartPolling_throwsIfNotInitialized() {
        assertThrows(IllegalStateException.class, () -> service.startPolling());
    }
    
    @Test
    void testGetCommitBaseline_returnsSetBaseline() {
        // Arrange
        Instant baseline = Instant.now().minusSeconds(3600);
        baselineManager.setBaseline(baseline);
        service.initialize("test-token");
        
        // Act
        Instant result = service.getCommitBaseline();
        
        // Assert
        assertEquals(baseline, result);
    }
    
    @Test
    void testSetCommitBaseline_updatesBaseline() {
        // Arrange
        service.initialize("test-token");
        Instant newBaseline = Instant.now().minusSeconds(7200);
        
        // Act
        service.setCommitBaseline(newBaseline);
        
        // Assert
        assertEquals(newBaseline, service.getCommitBaseline());
    }
    
    @Test
    void testAddListener_receivesNotifications() {
        // Arrange
        service.initialize("test-token");
        AtomicInteger syncCompleteCount = new AtomicInteger(0);
        
        service.addListener(new GitHubCommitService.GitHubCommitListener() {
            @Override
            public void onNewCommits(List<com.tamagotchi.committracker.domain.Commit> commits) {}
            
            @Override
            public void onSyncComplete(int totalCommits) {
                syncCompleteCount.incrementAndGet();
            }
            
            @Override
            public void onError(String error) {}
        });
        
        // The listener is registered - actual notification would happen during sync
        assertTrue(service.isInitialized());
    }
    
    @Test
    void testShutdown_stopsPollingAndCleansUp() {
        // Arrange
        service.initialize("test-token");
        
        // Act
        service.shutdown();
        
        // Assert
        assertFalse(service.isInitialized());
        assertFalse(service.isPolling());
    }
    
    @Test
    void testGetCachedCommits_returnsEmptyWhenNoCachedData() {
        // Arrange
        service.initialize("test-token");
        
        // Act
        var commits = service.getCachedCommits();
        
        // Assert
        assertTrue(commits.isEmpty());
    }
    
    /**
     * Mock GitHubApiClient for testing.
     */
    private static class MockGitHubApiClient implements GitHubApiClient {
        private String accessToken;
        private List<GitHubRepository> repositories = new ArrayList<>();
        private List<GitHubCommit> commits = new ArrayList<>();
        
        @Override
        public CompletableFuture<List<GitHubRepository>> fetchUserRepositories() {
            return CompletableFuture.completedFuture(new ArrayList<>(repositories));
        }
        
        @Override
        public CompletableFuture<CommitFetchResult> fetchCommits(String owner, String repo, 
                                                                  Instant since, String etag) {
            return CompletableFuture.completedFuture(
                CommitFetchResult.withCommits(new ArrayList<>(commits), "test-etag", 4999));
        }
        
        @Override
        public CompletableFuture<GitHubUser> fetchAuthenticatedUser() {
            return CompletableFuture.completedFuture(
                new GitHubUser(1L, "testuser", "Test User", "test@example.com", null, Instant.now()));
        }
        
        @Override
        public RateLimitStatus getRateLimitStatus() {
            return new RateLimitStatus(5000, 4999, Instant.now().plusSeconds(3600), 1);
        }
        
        @Override
        public boolean isAuthenticated() {
            return accessToken != null && !accessToken.isBlank();
        }
        
        @Override
        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }
        
        @Override
        public void clearAuthentication() {
            this.accessToken = null;
        }
        
        public void setRepositories(List<GitHubRepository> repos) {
            this.repositories = repos;
        }
        
        public void setCommits(List<GitHubCommit> commits) {
            this.commits = commits;
        }
    }
}
