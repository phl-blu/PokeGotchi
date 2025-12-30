package com.tamagotchi.committracker.github;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Client interface for interacting with GitHub's REST API.
 * Handles repository fetching, commit tracking, and rate limit management.
 * 
 * Requirements: 3.1, 3.2, 4.1, 5.1
 */
public interface GitHubApiClient {
    
    /**
     * Fetches all repositories accessible to the authenticated user.
     * Handles pagination automatically.
     * 
     * Requirements: 3.1, 3.2, 3.4
     * 
     * @return CompletableFuture containing list of repositories
     */
    CompletableFuture<List<GitHubRepository>> fetchUserRepositories();
    
    /**
     * Fetches commits for a specific repository since a given date.
     * Uses conditional requests (ETag/If-Modified-Since) for efficiency.
     * 
     * Requirements: 4.1, 4.3, 6.1, 6.2
     * 
     * @param owner repository owner
     * @param repo repository name
     * @param since only fetch commits after this date (can be null for all commits)
     * @param etag ETag from previous request for conditional fetch (can be null)
     * @return CompletableFuture containing the fetch result
     */
    CompletableFuture<CommitFetchResult> fetchCommits(
        String owner,
        String repo,
        Instant since,
        String etag
    );
    
    /**
     * Fetches the authenticated user's profile information.
     * 
     * @return CompletableFuture containing the user profile
     */
    CompletableFuture<GitHubUser> fetchAuthenticatedUser();
    
    /**
     * Gets current rate limit status.
     * 
     * Requirement: 5.1
     * 
     * @return the current rate limit status
     */
    RateLimitStatus getRateLimitStatus();
    
    /**
     * Checks if the client is authenticated and ready to make requests.
     * 
     * @return true if authenticated
     */
    boolean isAuthenticated();
    
    /**
     * Sets the access token for authentication.
     * 
     * @param accessToken the OAuth access token
     */
    void setAccessToken(String accessToken);
    
    /**
     * Clears the current authentication.
     */
    void clearAuthentication();
}
